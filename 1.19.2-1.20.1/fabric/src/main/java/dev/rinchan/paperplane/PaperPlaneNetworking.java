package dev.rinchan.paperplane;

import dev.rinchan.paperplane.client.PaperPlaneClient;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class PaperPlaneNetworking {
    private PaperPlaneNetworking() {
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(RequestTeleportPacket.ID, (server, player, handler, buffer, responseSender) -> {
            RequestTeleportPacket packet = RequestTeleportPacket.decode(buffer);
            server.execute(() -> PaperPlane.requestTeleport(player, packet.sessionId(), packet.targetId()));
        });
        ServerPlayNetworking.registerGlobalReceiver(RespondTeleportPacket.ID, (server, player, handler, buffer, responseSender) -> {
            RespondTeleportPacket packet = RespondTeleportPacket.decode(buffer);
            server.execute(() -> PaperPlane.respondToTeleportRequest(player, packet.requestId(), packet.accept()));
        });
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(OpenTeleportScreenPacket.ID, (client, handler, buffer, responseSender) -> {
            OpenTeleportScreenPacket packet = OpenTeleportScreenPacket.decode(buffer);
            client.execute(() -> PaperPlaneClient.openTeleportScreen(packet));
        });
        ClientPlayNetworking.registerGlobalReceiver(TrackTeleportRequestPacket.ID, (client, handler, buffer, responseSender) -> {
            TrackTeleportRequestPacket packet = TrackTeleportRequestPacket.decode(buffer);
            client.execute(() -> PaperPlaneClient.trackTeleportRequest(packet));
        });
    }

    public static OpenTeleportScreenPacket playerListPacket(MinecraftServer server, UUID requester, UUID sessionId, PlaneKind planeKind) {
        List<PlayerEntry> players = server.getPlayerList().getPlayers().stream()
            .filter(player -> !player.getUUID().equals(requester))
            .map(player -> new PlayerEntry(player.getUUID(), player.getGameProfile().getName()))
            .toList();
        return new OpenTeleportScreenPacket(sessionId, players, planeKind == PlaneKind.ENDER);
    }

    public static void sendToPlayer(ServerPlayer player, Object payload) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        if (payload instanceof OpenTeleportScreenPacket packet) {
            packet.encode(buffer);
            ServerPlayNetworking.send(player, OpenTeleportScreenPacket.ID, buffer);
        } else if (payload instanceof TrackTeleportRequestPacket packet) {
            packet.encode(buffer);
            ServerPlayNetworking.send(player, TrackTeleportRequestPacket.ID, buffer);
        } else {
            throw new IllegalArgumentException("Unsupported Paper Plane client packet " + payload.getClass().getName());
        }
    }

    public static void sendTeleportRequest(UUID sessionId, UUID targetId) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        new RequestTeleportPacket(sessionId, targetId).encode(buffer);
        ClientPlayNetworking.send(RequestTeleportPacket.ID, buffer);
    }

    public static void sendTeleportResponse(String requestId, boolean accept) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        new RespondTeleportPacket(requestId, accept).encode(buffer);
        ClientPlayNetworking.send(RespondTeleportPacket.ID, buffer);
    }
}
