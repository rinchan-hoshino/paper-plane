package dev.rinchan.paperplane;

import dev.ftb.mods.ftbessentials.command.TPACommands;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
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
            ServerPlayer target = server(player).getPlayerList().getPlayer(pending.get().targetId());
            String name = target == null ? pending.get().targetId().toString() : target.getGameProfile().getName();
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

        UUID sessionId = SELECTIONS.open(player.getUUID(), planeKind, server(player).overworld().getGameTime());
        PaperPlaneNetworking.sendToPlayer(
            player,
            PaperPlaneNetworking.playerListPacket(server(player), player.getUUID(), sessionId, planeKind)
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
        Optional<PlaneKind> selection = SELECTIONS.consume(requester.getUUID(), sessionId, server(requester).overworld().getGameTime());
        if (selection.isEmpty()) {
            requester.sendSystemMessage(Component.translatable("message.paper_plane.selection_expired").withStyle(ChatFormatting.RED));
            return;
        }

        reconcilePending(requester);
        if (REQUESTS.hasPending(requester.getUUID()) || TeleportPayments.pending(requester.getUUID())) {
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
        if (findPlaneSlot(requester, selection.get()) < 0) {
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
            requestId, requester.getUUID(), targetId, selection.get()
        );
        if (!REQUESTS.add(pendingRequest)) {
            TPACommands.REQUESTS.remove(requestId);
            LOGGER.error("Rejected duplicate Paper Plane pending state for requester {}", requester.getUUID());
            return;
        }
    }

    /** FTB owns request state; the server validates the accepting recipient. */
    public static int respondToTeleportRequest(ServerPlayer target, String requestId, boolean accept) {
        var request = TPACommands.REQUESTS.get(requestId);
        if (request == null || !FtbRequestTarget.id(request).equals(target.getUUID())) return 0;
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

    public static void forgetDeniedRequest(ServerPlayer target, String requestId, int result) {
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
            TPACommands.REQUESTS.remove(pending.requestId());
        }
    }

    private static void reconcilePending(ServerPlayer player) {
        reconcileRequest(player.getUUID());
    }

    static void reconcileRequest(UUID player) {
        REQUESTS.pendingFor(player)
            .filter(pending -> !TeleportPayments.pending(player) && !TPACommands.REQUESTS.containsKey(pending.requestId()))
            .ifPresent(pending -> clearRequest(pending.requestId()));
    }

    private static void clearRequest(String requestId) {
        REQUESTS.remove(requestId);
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

    private static MinecraftServer server(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            throw new IllegalStateException("Server player is not attached to a server");
        }
        return server;
    }

}
