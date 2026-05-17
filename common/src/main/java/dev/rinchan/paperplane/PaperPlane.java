package dev.rinchan.paperplane;

import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PaperPlane {
    public static final String MOD_ID = "paper_plane";
    public static final int REQUEST_TIMEOUT_TICKS = 60 * 20;

    private static final Map<UUID, TeleportRequest> REQUESTS = new ConcurrentHashMap<>();

    private PaperPlane() {
    }

    public static void openTeleportScreen(ServerPlayer requester, boolean enderPlane) {
        PacketDistributor.sendToPlayer(requester, PaperPlaneNetworking.playerListPacket(requester.server, requester.getUUID(), enderPlane));
    }

    public static void requestTeleport(ServerPlayer requester, UUID targetId, boolean enderPlane) {
        ServerPlayer target = requester.server.getPlayerList().getPlayer(targetId);
        if (target == null) {
            requester.sendSystemMessage(Component.translatable("message.paper_plane.target_offline").withStyle(ChatFormatting.RED));
            return;
        }
        if (target.getUUID().equals(requester.getUUID())) {
            requester.sendSystemMessage(Component.translatable("message.paper_plane.no_self").withStyle(ChatFormatting.RED));
            return;
        }
        if (!enderPlane && !hasConsumablePlane(requester)) {
            requester.sendSystemMessage(Component.translatable("message.paper_plane.no_plane").withStyle(ChatFormatting.RED));
            return;
        }

        UUID requestId = UUID.randomUUID();
        REQUESTS.put(requestId, new TeleportRequest(requestId, requester.getUUID(), target.getUUID(), enderPlane, REQUEST_TIMEOUT_TICKS));
        requester.sendSystemMessage(Component.translatable("message.paper_plane.request_sent", target.getGameProfile().getName()).withStyle(ChatFormatting.AQUA));
        PacketDistributor.sendToPlayer(target, new TeleportRequestPromptPacket(requestId, requester.getGameProfile().getName(), enderPlane));
    }

    public static void answerTeleport(ServerPlayer target, UUID requestId, boolean accepted) {
        TeleportRequest request = REQUESTS.remove(requestId);
        if (request == null || !request.targetId().equals(target.getUUID())) {
            target.sendSystemMessage(Component.translatable("message.paper_plane.request_missing").withStyle(ChatFormatting.RED));
            return;
        }

        ServerPlayer requester = target.server.getPlayerList().getPlayer(request.requesterId());
        if (requester == null) {
            target.sendSystemMessage(Component.translatable("message.paper_plane.requester_offline").withStyle(ChatFormatting.RED));
            return;
        }

        if (!accepted) {
            requester.sendSystemMessage(Component.translatable("message.paper_plane.request_denied", target.getGameProfile().getName()).withStyle(ChatFormatting.YELLOW));
            target.sendSystemMessage(Component.translatable("message.paper_plane.denied").withStyle(ChatFormatting.YELLOW));
            return;
        }

        if (!request.enderPlane() && !consumePlane(requester)) {
            requester.sendSystemMessage(Component.translatable("message.paper_plane.no_plane").withStyle(ChatFormatting.RED));
            target.sendSystemMessage(Component.translatable("message.paper_plane.requester_no_plane").withStyle(ChatFormatting.RED));
            return;
        }

        ServerLevel destination = target.serverLevel();
        requester.teleportTo(destination, target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        destination.playSound(null, target.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);
        requester.sendSystemMessage(Component.translatable("message.paper_plane.teleported", target.getGameProfile().getName()).withStyle(ChatFormatting.GREEN));
        target.sendSystemMessage(Component.translatable("message.paper_plane.accepted", requester.getGameProfile().getName()).withStyle(ChatFormatting.GREEN));
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, TeleportRequest>> iterator = REQUESTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TeleportRequest> entry = iterator.next();
            TeleportRequest request = entry.getValue().tick();
            if (request.remainingTicks() <= 0) {
                iterator.remove();
                ServerPlayer requester = server.getPlayerList().getPlayer(request.requesterId());
                if (requester != null) {
                    requester.sendSystemMessage(Component.translatable("message.paper_plane.request_expired").withStyle(ChatFormatting.YELLOW));
                }
            } else {
                entry.setValue(request);
            }
        }
    }

    public static void clearPlayer(ServerPlayer player) {
        REQUESTS.entrySet().removeIf(entry -> entry.getValue().requesterId().equals(player.getUUID()) || entry.getValue().targetId().equals(player.getUUID()));
    }

    public static boolean hasConsumablePlane(ServerPlayer player) {
        return findConsumablePlaneSlot(player) >= 0;
    }

    public static boolean consumePlane(ServerPlayer player) {
        int slot = findConsumablePlaneSlot(player);
        if (slot < 0) {
            return false;
        }
        player.getInventory().getItem(slot).shrink(1);
        return true;
    }

    private static int findConsumablePlaneSlot(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(PaperPlaneRegistries.PAPER_PLANE.get()) || stack.is(PaperPlaneRegistries.SOGGY_PAPER_PLANE.get())) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isServerPlayer(Entity entity) {
        return entity instanceof ServerPlayer;
    }
}
