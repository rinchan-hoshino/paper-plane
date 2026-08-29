package dev.rinchan.paperplane;

import dev.ftb.mods.ftbessentials.api.records.TPARequest;
import dev.ftb.mods.ftbessentials.commands.impl.teleporting.TPACommand;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PaperPlane {
    public static final String MOD_ID = "paper_plane";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final long SELECTION_SESSION_TICKS = 20L * 30L;
    private static final TPACommand FTB_TPA = new TPACommand();
    private static final TeleportRequestLedger REQUESTS = new TeleportRequestLedger();
    private static final Map<UUID, SelectionSession> SELECTIONS = new HashMap<>();

    private PaperPlane() {
    }

    public static void openTeleportScreen(ServerPlayer player, PlaneKind planeKind) {
        reconcilePending(player);
        Optional<TeleportRequestLedger.Pending> pending = REQUESTS.pendingFor(player.getUUID());
        if (pending.isPresent()) {
            ServerPlayer target = player.server.getPlayerList().getPlayer(pending.get().targetId());
            String name = target == null ? pending.get().targetId().toString() : target.getGameProfile().getName();
            player.sendSystemMessage(Component.translatable("message.paper_plane.pending", name).withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (findPlaneSlot(player, planeKind) < 0) {
            player.sendSystemMessage(Component.translatable("message.paper_plane.no_selected_plane").withStyle(ChatFormatting.RED));
            return;
        }

        UUID sessionId = UUID.randomUUID();
        long expiresAt = player.serverLevel().getGameTime() + SELECTION_SESSION_TICKS;
        SELECTIONS.put(player.getUUID(), new SelectionSession(sessionId, planeKind, expiresAt));
        PaperPlaneNetworking.sendToPlayer(
            player,
            PaperPlaneNetworking.playerListPacket(player.server, player.getUUID(), sessionId, planeKind)
        );
    }

    public static void requestTeleport(ServerPlayer requester, UUID sessionId, UUID targetId) {
        SelectionSession session = SELECTIONS.remove(requester.getUUID());
        if (session == null || !session.sessionId().equals(sessionId) || requester.serverLevel().getGameTime() > session.expiresAt()) {
            requester.sendSystemMessage(Component.translatable("message.paper_plane.selection_expired").withStyle(ChatFormatting.RED));
            return;
        }

        reconcilePending(requester);
        if (REQUESTS.hasPending(requester.getUUID())) {
            requester.sendSystemMessage(Component.translatable("message.paper_plane.pending_unknown").withStyle(ChatFormatting.YELLOW));
            return;
        }

        ServerPlayer target = requester.server.getPlayerList().getPlayer(targetId);
        if (target == null) {
            requester.sendSystemMessage(Component.translatable("message.paper_plane.target_offline").withStyle(ChatFormatting.RED));
            return;
        }
        if (target.getUUID().equals(requester.getUUID())) {
            requester.sendSystemMessage(Component.translatable("message.paper_plane.no_self").withStyle(ChatFormatting.RED));
            return;
        }
        if (findPlaneSlot(requester, session.planeKind()) < 0) {
            requester.sendSystemMessage(Component.translatable("message.paper_plane.no_selected_plane").withStyle(ChatFormatting.RED));
            return;
        }

        Set<UUID> before = Set.copyOf(TPACommand.requests().keySet());
        int result = FTB_TPA.tpa(requester, target, false);
        if (result <= 0) {
            return;
        }

        TPARequest request = findNewFtbRequest(requester, target, before);
        if (request == null) {
            LOGGER.error("FTB Essentials accepted a paper-plane TPA request but exposed no new request id for {} -> {}", requester.getUUID(), targetId);
            return;
        }

        TeleportRequestLedger.Pending pending = new TeleportRequestLedger.Pending(
            request.id(), requester.getUUID(), targetId, session.planeKind()
        );
        if (!REQUESTS.add(pending)) {
            TPACommand.requests().remove(request.id());
            LOGGER.error("Rejected duplicate Paper Plane pending state for requester {}", requester.getUUID());
            return;
        }
    }

    public static boolean respondToTeleportRequest(ServerPlayer target, UUID requestId, boolean accept) {
        if (!REQUESTS.isOwnedByTarget(requestId, target.getUUID())) return false;

        if (accept) FTB_TPA.tpaccept(target, requestId.toString());
        else FTB_TPA.tpdeny(target, requestId.toString());
        return true;
    }

    public static int acceptTeleportRequest(ServerPlayer target, String requestId, IntSupplier operation) {
        UUID id = parseRequestId(requestId);
        if (id == null) {
            return operation.getAsInt();
        }
        Optional<TeleportRequestLedger.Pending> found = REQUESTS.byRequest(id);
        if (found.isEmpty()) {
            return operation.getAsInt();
        }

        TeleportRequestLedger.Pending pending = found.get();
        if (!pending.targetId().equals(target.getUUID())) {
            LOGGER.warn("Player {} tried to answer Paper Plane request {} owned by target {}", target.getUUID(), id, pending.targetId());
            return 0;
        }

        ServerPlayer requester = target.server.getPlayerList().getPlayer(pending.requesterId());
        if (requester == null) {
            return operation.getAsInt();
        }

        ItemStack payment = ItemStack.EMPTY;
        if (pending.planeKind().consumable() && !requester.getAbilities().instabuild) {
            payment = takePlane(requester, pending.planeKind());
            if (payment.isEmpty()) {
                requester.sendSystemMessage(Component.translatable("message.paper_plane.no_selected_plane").withStyle(ChatFormatting.RED));
                target.sendSystemMessage(Component.translatable("message.paper_plane.requester_no_plane").withStyle(ChatFormatting.RED));
                return 0;
            }
        }

        try {
            int result = operation.getAsInt();
            if (result > 0) {
                clearRequest(id);
            } else {
                refund(requester, payment);
                if (!TPACommand.requests().containsKey(id)) {
                    clearRequest(id);
                }
            }
            return result;
        } catch (RuntimeException | Error failure) {
            refund(requester, payment);
            throw failure;
        }
    }

    public static void forgetDeniedRequest(ServerPlayer target, UUID requestId, int result) {
        if (result > 0) {
            REQUESTS.byRequest(requestId)
                .filter(pending -> pending.targetId().equals(target.getUUID()))
                .ifPresent(pending -> clearRequest(requestId));
        }
    }

    public static void clearPlayer(ServerPlayer player) {
        SELECTIONS.remove(player.getUUID());
        for (TeleportRequestLedger.Pending pending : REQUESTS.removePlayer(player.getUUID())) {
            TPACommand.requests().remove(pending.requestId());
        }
    }

    private static void reconcilePending(ServerPlayer player) {
        REQUESTS.pendingFor(player.getUUID())
            .filter(pending -> !TPACommand.requests().containsKey(pending.requestId()))
            .ifPresent(pending -> clearRequest(pending.requestId()));
    }

    private static void clearRequest(UUID requestId) {
        REQUESTS.remove(requestId);
    }

    private static TPARequest findNewFtbRequest(ServerPlayer requester, ServerPlayer target, Set<UUID> before) {
        for (TPARequest request : TPACommand.requests().values()) {
            if (!before.contains(request.id())
                && request.source().getUuid().equals(requester.getUUID())
                && request.target().getUuid().equals(target.getUUID())
                && !request.here()) {
                return request;
            }
        }
        return null;
    }

    private static ItemStack takePlane(ServerPlayer player, PlaneKind planeKind) {
        int slot = findPlaneSlot(player, planeKind);
        if (slot < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack source = player.getInventory().getItem(slot);
        ItemStack payment = source.copy();
        payment.setCount(1);
        source.shrink(1);
        return payment;
    }

    private static void refund(ServerPlayer player, ItemStack payment) {
        if (!payment.isEmpty() && !player.getInventory().add(payment)) {
            player.drop(payment, false);
        }
    }

    private static int findPlaneSlot(ServerPlayer player, PlaneKind planeKind) {
        Item item = switch (planeKind) {
            case NORMAL -> PaperPlaneRegistries.PAPER_PLANE.get();
            case SOGGY -> PaperPlaneRegistries.SOGGY_PAPER_PLANE.get();
            case ENDER -> PaperPlaneRegistries.ENDER_PAPER_PLANE.get();
        };
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(item)) {
                return slot;
            }
        }
        return -1;
    }

    private static UUID parseRequestId(String requestId) {
        try {
            return UUID.fromString(requestId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private record SelectionSession(UUID sessionId, PlaneKind planeKind, long expiresAt) {
    }
}
