package dev.rinchan.paperplane;

import dev.rinchan.paperplane.fabric.PaperPlaneFabricClientNetwork;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class PaperPlaneNetworking {
    private PaperPlaneNetworking() {}

    public static OpenTeleportScreenPacket playerListPacket(MinecraftServer server, UUID requester, UUID sessionId, PlaneKind planeKind) {
        List<PlayerEntry> players = server.getPlayerList().getPlayers().stream().filter(player -> !player.getUUID().equals(requester)).map(player -> new PlayerEntry(player.getUUID(), player.getGameProfile().getName())).toList();
        return new OpenTeleportScreenPacket(sessionId, players, planeKind == PlaneKind.ENDER);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) { ServerPlayNetworking.send(player, payload); }
    public static void sendTeleportRequest(UUID sessionId, UUID targetId) { PaperPlaneFabricClientNetwork.send(new RequestTeleportPacket(sessionId, targetId)); }
    public static void sendTeleportResponse(UUID requestId, boolean accept) { PaperPlaneFabricClientNetwork.send(new RespondTeleportPacket(requestId, accept)); }
}
