package dev.rinchan.paperplane;

import dev.rinchan.paperplane.client.PaperPlaneClient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PaperPlaneNetworking {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(PaperPlane.MOD_ID, "main"),
        () -> PROTOCOL,
        PROTOCOL::equals,
        PROTOCOL::equals
    );

    private PaperPlaneNetworking() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(
            id++, OpenTeleportScreenPacket.class,
            OpenTeleportScreenPacket::encode, OpenTeleportScreenPacket::decode,
            PaperPlaneNetworking::handleOpenScreen,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, TrackTeleportRequestPacket.class,
            TrackTeleportRequestPacket::encode, TrackTeleportRequestPacket::decode,
            PaperPlaneNetworking::handleTrackRequest,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, RequestTeleportPacket.class,
            RequestTeleportPacket::encode, RequestTeleportPacket::decode,
            PaperPlaneNetworking::handleTeleportRequest,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id, RespondTeleportPacket.class,
            RespondTeleportPacket::encode, RespondTeleportPacket::decode,
            PaperPlaneNetworking::handleTeleportResponse,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }

    public static OpenTeleportScreenPacket playerListPacket(MinecraftServer server, UUID requester, UUID sessionId, PlaneKind planeKind) {
        List<PlayerEntry> players = server.getPlayerList().getPlayers().stream()
            .filter(player -> !player.getUUID().equals(requester))
            .map(player -> new PlayerEntry(player.getUUID(), player.getGameProfile().getName()))
            .toList();
        return new OpenTeleportScreenPacket(sessionId, players, planeKind == PlaneKind.ENDER);
    }

    public static void sendToPlayer(ServerPlayer player, Object payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    public static void sendTeleportRequest(UUID sessionId, UUID targetId) {
        CHANNEL.sendToServer(new RequestTeleportPacket(sessionId, targetId));
    }

    public static void sendTeleportResponse(String requestId, boolean accept) {
        CHANNEL.sendToServer(new RespondTeleportPacket(requestId, accept));
    }

    private static void handleOpenScreen(OpenTeleportScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> PaperPlaneClient.openTeleportScreen(packet));
        context.setPacketHandled(true);
    }

    private static void handleTrackRequest(TrackTeleportRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> PaperPlaneClient.trackTeleportRequest(packet));
        context.setPacketHandled(true);
    }

    private static void handleTeleportRequest(RequestTeleportPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> PaperPlane.requestTeleport(sender, packet.sessionId(), packet.targetId()));
        }
        context.setPacketHandled(true);
    }

    private static void handleTeleportResponse(RespondTeleportPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> PaperPlane.respondToTeleportRequest(sender, packet.requestId(), packet.accept()));
        }
        context.setPacketHandled(true);
    }
}
