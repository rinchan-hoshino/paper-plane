package dev.rinchan.paperplane;

import dev.ftb.mods.ftbessentials.command.TPACommands;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PaperPlane {
    public static final String MOD_ID = "paper_plane";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final long SELECTION_SESSION_TICKS = 20L * 30L;
    private static final TeleportRequestLedger REQUESTS = new TeleportRequestLedger();
    private static final Map<UUID, SelectionSession> SELECTIONS = new HashMap<>();

    private PaperPlane() {
    }

    public static void openTeleportScreen(ServerPlayer player, PlaneKind planeKind) {
        reconcilePending(player);
        Optional<TeleportRequestLedger.Pending> pending = REQUESTS.pendingFor(player.getUUID());
        if (pending.isPresent()) {
            ServerPlayer target = server(player).getPlayerList().getPlayer(pending.get().targetId());
            String name = target == null ? pending.get().targetId().toString() : target.getGameProfile().getName();
            player.sendSystemMessage(Component.translatable("message.paper_plane.pending", name).withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (findPlaneSlot(player, planeKind) < 0) {
            player.sendSystemMessage(Component.translatable("message.paper_plane.no_selected_plane").withStyle(ChatFormatting.RED));
            return;
        }

        UUID sessionId = UUID.randomUUID();
        long expiresAt = player.getCommandSenderWorld().getGameTime() + SELECTION_SESSION_TICKS;
        SELECTIONS.put(player.getUUID(), new SelectionSession(sessionId, planeKind, expiresAt));
        PaperPlaneNetworking.sendToPlayer(
            player,
            PaperPlaneNetworking.playerListPacket(server(player), player.getUUID(), sessionId, planeKind)
        );
    }

    public static void requestTeleport(ServerPlayer requester, UUID sessionId, UUID targetId) {
        SelectionSession session = SELECTIONS.remove(requester.getUUID());
        if (session == null || !session.sessionId().equals(sessionId) || requester.getCommandSenderWorld().getGameTime() > session.expiresAt()) {
            requester.sendSystemMessage(Component.translatable("message.paper_plane.selection_expired").withStyle(ChatFormatting.RED));
            return;
        }

        reconcilePending(requester);
        if (REQUESTS.hasPending(requester.getUUID())) {
            requester.sendSystemMessage(Component.translatable("message.paper_plane.pending_unknown").withStyle(ChatFormatting.YELLOW));
            return;
        }

        ServerPlayer target = server(requester).getPlayerList().getPlayer(targetId);
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

        Set<String> before = Set.copyOf(TPACommands.REQUESTS.keySet());
        int result = TPACommands.tpa(requester, target, false);
        if (result <= 0) {
            return;
        }

        String requestId = findNewFtbRequest(before);
        if (requestId == null) {
            LOGGER.error("FTB Essentials accepted a Paper Plane TPA request but exposed no unique request id for {} -> {}", requester.getUUID(), targetId);
            return;
        }

        TeleportRequestLedger.Pending pendingRequest = new TeleportRequestLedger.Pending(
            requestId, requester.getUUID(), targetId, session.planeKind()
        );
        if (!REQUESTS.add(pendingRequest)) {
            TPACommands.REQUESTS.remove(requestId);
            LOGGER.error("Rejected duplicate Paper Plane pending state for requester {}", requester.getUUID());
            return;
        }
        PaperPlaneNetworking.sendToPlayer(target, new TrackTeleportRequestPacket(requestId, true));
    }

    /** Only a server-tracked Paper Plane target may route an FTB response packet. */
    public static int respondToTeleportRequest(ServerPlayer target, String requestId, boolean accept) {
        Optional<TeleportRequestLedger.Pending> pending = REQUESTS.byRequest(requestId);
        if (pending.isEmpty() || !pending.get().targetId().equals(target.getUUID())) {
            return 0;
        }
        return accept ? TPACommands.tpaccept(target, requestId) : TPACommands.tpdeny(target, requestId);
    }

    public static int acceptTeleportRequest(ServerPlayer target, String requestId, IntSupplier operation) {
        Optional<TeleportRequestLedger.Pending> found = REQUESTS.byRequest(requestId);
        if (found.isEmpty()) {
            return operation.getAsInt();
        }

        TeleportRequestLedger.Pending pending = found.get();
        if (!pending.targetId().equals(target.getUUID())) {
            LOGGER.warn("Player {} tried to answer Paper Plane request {} owned by target {}", target.getUUID(), requestId, pending.targetId());
            return 0;
        }

        MinecraftServer server = server(target);
        ServerPlayer requester = server.getPlayerList().getPlayer(pending.requesterId());
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
                clearRequest(server, requestId);
            } else {
                refund(requester, payment);
                if (!TPACommands.REQUESTS.containsKey(requestId)) {
                    clearRequest(server, requestId);
                }
            }
            return result;
        } catch (RuntimeException | Error failure) {
            refund(requester, payment);
            throw failure;
        }
    }

    public static void forgetDeniedRequest(ServerPlayer target, String requestId, int result) {
        if (result > 0) {
            REQUESTS.byRequest(requestId)
                .filter(pending -> pending.targetId().equals(target.getUUID()))
                .ifPresent(pending -> clearRequest(server(target), requestId));
        }
    }

    public static void clearPlayer(ServerPlayer player) {
        SELECTIONS.remove(player.getUUID());
        MinecraftServer server = server(player);
        for (TeleportRequestLedger.Pending pending : REQUESTS.removePlayer(player.getUUID())) {
            TPACommands.REQUESTS.remove(pending.requestId());
            notifyTracking(server, pending, false);
        }
    }

    private static void reconcilePending(ServerPlayer player) {
        REQUESTS.pendingFor(player.getUUID())
            .filter(pending -> !TPACommands.REQUESTS.containsKey(pending.requestId()))
            .ifPresent(pending -> clearRequest(server(player), pending.requestId()));
    }

    private static void clearRequest(MinecraftServer server, String requestId) {
        REQUESTS.remove(requestId).ifPresent(pending -> notifyTracking(server, pending, false));
    }

    private static void notifyTracking(MinecraftServer server, TeleportRequestLedger.Pending pending, boolean active) {
        ServerPlayer target = server.getPlayerList().getPlayer(pending.targetId());
        if (target != null) {
            PaperPlaneNetworking.sendToPlayer(target, new TrackTeleportRequestPacket(pending.requestId(), active));
        }
    }

    private static String findNewFtbRequest(Set<String> before) {
        String found = null;
        for (String requestId : TPACommands.REQUESTS.keySet()) {
            if (!before.contains(requestId)) {
                if (found != null) {
                    return null;
                }
                found = requestId;
            }
        }
        return found;
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

    private static MinecraftServer server(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            throw new IllegalStateException("Server player is not attached to a server");
        }
        return server;
    }

    private record SelectionSession(UUID sessionId, PlaneKind planeKind, long expiresAt) {
    }
}
