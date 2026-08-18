package dev.rinchan.paperplane;

import dev.ftb.mods.ftbessentials.api.records.TPARequest;
import dev.ftb.mods.ftbessentials.commands.impl.teleporting.TPACommand;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PaperPlane {
    public static final String MOD_ID = "paper_plane";

    private static final TPACommand FTB_TPA = new TPACommand();
    private static final Map<UUID, RequestData> REQUESTS = new ConcurrentHashMap<>();

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

        int result = FTB_TPA.tpa(requester, target, false);
        if (result <= 0) {
            return;
        }

        TPARequest request = findFtbRequest(requester, target);
        if (request != null) {
            REQUESTS.put(request.id(), new RequestData(requester.getUUID(), enderPlane));
        }
    }

    public static int respondToTeleportRequest(ServerPlayer target, UUID requestId, boolean accept) {
        return accept
            ? FTB_TPA.tpaccept(target, requestId.toString())
            : FTB_TPA.tpdeny(target, requestId.toString());
    }

    public static void finishAcceptedRequest(ServerPlayer target, UUID requestId) {
        RequestData data = REQUESTS.remove(requestId);
        if (data == null || data.enderPlane()) {
            return;
        }
        ServerPlayer requester = target.server.getPlayerList().getPlayer(data.requesterId());
        if (requester != null) {
            consumePlane(requester);
        }
    }

    public static void forgetRequest(UUID requestId) {
        REQUESTS.remove(requestId);
    }

    public static void clearPlayer(ServerPlayer player) {
        REQUESTS.entrySet().removeIf(entry -> {
            TPARequest request = TPACommand.requests().get(entry.getKey());
            return request == null || request.source().getUuid().equals(player.getUUID()) || request.target().getUuid().equals(player.getUUID());
        });
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

    private static TPARequest findFtbRequest(ServerPlayer requester, ServerPlayer target) {
        for (TPARequest request : TPACommand.requests().values()) {
            if (request.source().getUuid().equals(requester.getUUID()) && request.target().getUuid().equals(target.getUUID()) && !request.here()) {
                return request;
            }
        }
        return null;
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

    private record RequestData(UUID requesterId, boolean enderPlane) {
    }
}
