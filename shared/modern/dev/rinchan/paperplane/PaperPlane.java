package dev.rinchan.paperplane;

import dev.ftb.mods.ftbessentials.api.records.TPARequest;
import dev.ftb.mods.ftbessentials.commands.impl.teleporting.TPACommand;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
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
    private static final TPACommand FTB_TPA = new TPACommand();
    private static final TeleportRequestLedger REQUESTS = new TeleportRequestLedger();
    private static final SelectionSessions SELECTIONS = new SelectionSessions();
    private static PaperPlaneConfig config = new PaperPlaneConfig(true, true);

    private PaperPlane() {
    }

    public static void configure(java.nio.file.Path directory) {
        try {
            config = PaperPlaneConfig.load(directory);
        } catch (java.io.IOException failure) {
            throw new java.io.UncheckedIOException("Cannot load Paper Plane configuration", failure);
        }
    }

    private static boolean enabled(ServerPlayer player, boolean returning) {
        if (returning ? config.enableBack() : config.enableTpa()) return true;
        player.sendSystemMessage(Component.translatable(returning ? "message.paper_plane.back_disabled" : "message.paper_plane.tpa_disabled").withStyle(ChatFormatting.RED));
        return false;
    }

    public static void openTeleportScreen(ServerPlayer player, PlaneKind planeKind) {
        if (!enabled(player, planeKind.returning())) return;
        if (planeKind.returning()) {
            returnToPreviousLocation(player, planeKind);
            return;
        }
        reconcilePending(player);
        Optional<TeleportRequestLedger.Pending> pending = REQUESTS.pendingFor(player.getUUID());
        if (pending.isPresent()) {
            ServerPlayer target = PaperPlanePlatform.server(player).getPlayerList().getPlayer(pending.get().targetId());
            String name = target == null ? pending.get().targetId().toString() : PaperPlanePlatform.playerName(target);
            player.sendSystemMessage(Component.translatable("message.paper_plane.pending", name).withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (TeleportPayments.pending(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.paper_plane.pending_unknown").withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (findPlaneSlot(player, planeKind) < 0) {
            player.sendSystemMessage(Component.translatable("message.paper_plane.no_selected_plane").withStyle(ChatFormatting.RED));
            return;
        }

        UUID sessionId = SELECTIONS.open(player.getUUID(), planeKind, PaperPlanePlatform.server(player).overworld().getGameTime());
        PaperPlaneNetworking.sendToPlayer(
            player,
            PaperPlaneNetworking.playerListPacket(PaperPlanePlatform.server(player), player.getUUID(), sessionId, planeKind)
        );
    }

    private static void returnToPreviousLocation(ServerPlayer player, PlaneKind kind) {
        reconcilePending(player);
        if (REQUESTS.hasPending(player.getUUID()) || TeleportPayments.pending(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.paper_plane.pending_unknown").withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (!hasPlane(player, kind)) {
            player.sendSystemMessage(Component.translatable("message.paper_plane.no_selected_plane").withStyle(ChatFormatting.RED));
            return;
        }
        var data = FtbPlayerData.get(player);
        if (data.isEmpty()) return;
        TeleportPayments.begin(player, kind, data.get().backTeleporter);
        try {
            if (FtbBack.run(player) <= 0) TeleportPayments.failed(player.getUUID());
        } catch (RuntimeException | Error failure) {
            TeleportPayments.failed(player.getUUID());
            throw failure;
        }
    }

    public static void requestTeleport(ServerPlayer requester, UUID sessionId, UUID targetId) {
        if (!enabled(requester, false)) return;
        Optional<PlaneKind> selection = SELECTIONS.consume(requester.getUUID(), sessionId, PaperPlanePlatform.server(requester).overworld().getGameTime());
        if (selection.isEmpty()) {
            requester.sendSystemMessage(Component.translatable("message.paper_plane.selection_expired").withStyle(ChatFormatting.RED));
            return;
        }

        reconcilePending(requester);
        if (REQUESTS.hasPending(requester.getUUID()) || TeleportPayments.pending(requester.getUUID())) {
            requester.sendSystemMessage(Component.translatable("message.paper_plane.pending_unknown").withStyle(ChatFormatting.YELLOW));
            return;
        }

        ServerPlayer target = PaperPlanePlatform.server(requester).getPlayerList().getPlayer(targetId);
        if (target == null) {
            requester.sendSystemMessage(Component.translatable("message.paper_plane.target_offline").withStyle(ChatFormatting.RED));
            return;
        }
        if (target.getUUID().equals(requester.getUUID())) {
            requester.sendSystemMessage(Component.translatable("message.paper_plane.no_self").withStyle(ChatFormatting.RED));
            return;
        }
        if (findPlaneSlot(requester, selection.get()) < 0) {
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
            request.id(), requester.getUUID(), targetId, selection.get()
        );
        if (!REQUESTS.add(pending)) {
            TPACommand.requests().remove(request.id());
            LOGGER.error("Rejected duplicate Paper Plane pending state for requester {}", requester.getUUID());
            return;
        }
    }

    public static void respondToTeleportRequest(ServerPlayer target, UUID requestId, boolean accept) {
        var request = TPACommand.requests().get(requestId);
        if (request == null || !request.target().getUuid().equals(target.getUUID())) return;
        // Validate the native request recipient before delegating to FTB.
        // Falling back to a chat command after a repeat click fails on servers
        // that expose TPA only through Paper Plane rather than public commands.
        if (accept) FTB_TPA.tpaccept(target, requestId.toString());
        else FTB_TPA.tpdeny(target, requestId.toString());
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

        ServerPlayer requester = PaperPlanePlatform.server(target).getPlayerList().getPlayer(pending.requesterId());
        if (requester == null) {
            return operation.getAsInt();
        }

        if (!enabled(requester, false) || TeleportPayments.pending(requester.getUUID())) return 0;
        if (!hasPlane(requester, pending.planeKind())) {
            requester.sendSystemMessage(Component.translatable("message.paper_plane.no_selected_plane").withStyle(ChatFormatting.RED));
            target.sendSystemMessage(Component.translatable("message.paper_plane.requester_no_plane").withStyle(ChatFormatting.RED));
            return 0;
        }
        var data = FtbPlayerData.get(requester);
        if (data.isEmpty()) return 0;
        TeleportPayments.begin(requester, pending.planeKind(), data.get().tpaTeleporter);
        try {
            int result = operation.getAsInt();
            if (result <= 0) TeleportPayments.failed(requester.getUUID());
            return result;
        } catch (RuntimeException | Error failure) {
            TeleportPayments.failed(requester.getUUID());
            throw failure;
        }
    }

    /** Called only after the native teleporter has actually moved the player. */
    public static void finishTeleport(ServerPlayer player, PlaneKind kind) {
        TeleportPayments.finish(player.getUUID());
        discardPendingRequest(player.getUUID());
    }

    static void discardPendingRequest(UUID player) {
        REQUESTS.pendingFor(player).ifPresent(pending -> clearRequest(pending.requestId()));
    }

    static boolean hasPlane(ServerPlayer player, PlaneKind kind) {
        return findPlaneSlot(player, kind) >= 0;
    }

    public static void forgetDeniedRequest(ServerPlayer target, UUID requestId, int result) {
        if (result > 0) {
            REQUESTS.byRequest(requestId)
                .filter(pending -> pending.targetId().equals(target.getUUID()))
                .ifPresent(pending -> clearRequest(requestId));
        }
    }

    public static void clearPlayer(ServerPlayer player) {
        TeleportPayments.finish(player.getUUID());
        SELECTIONS.remove(player.getUUID());
        for (TeleportRequestLedger.Pending pending : REQUESTS.removePlayer(player.getUUID())) {
            TPACommand.requests().remove(pending.requestId());
        }
    }

    private static void reconcilePending(ServerPlayer player) {
        reconcileRequest(player.getUUID());
    }

    static void reconcileRequest(UUID player) {
        REQUESTS.pendingFor(player)
            .filter(pending -> !TeleportPayments.pending(player) && !TPACommand.requests().containsKey(pending.requestId()))
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

    static ItemStack takePlane(ServerPlayer player, PlaneKind planeKind) {
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

    static void refund(ServerPlayer player, ItemStack payment) {
        if (!payment.isEmpty() && !player.getInventory().add(payment)) {
            player.drop(payment, false);
        }
    }

    private static int findPlaneSlot(ServerPlayer player, PlaneKind planeKind) {
        Item item = switch (planeKind) {
            case NORMAL -> PaperPlaneRegistries.PAPER_PLANE.get();
            case SOGGY -> PaperPlaneRegistries.SOGGY_PAPER_PLANE.get();
            case ENDER -> PaperPlaneRegistries.ENDER_PAPER_PLANE.get();
            case BACK -> PaperPlaneRegistries.BACK_PAPER_PLANE.get();
            case ENDER_BACK -> PaperPlaneRegistries.ENDER_BACK_PAPER_PLANE.get();
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

}
