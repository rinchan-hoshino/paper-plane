package dev.rinchan.paperplane;

import dev.rinchan.paperplane.client.PaperPlaneClient;
import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class PaperPlaneNetworking {
    private static final SimpleChannel CHANNEL = ChannelBuilder
        .named(new ResourceLocation(PaperPlane.MOD_ID, "main"))
        .networkProtocolVersion(1)
        .simpleChannel();
    private static boolean registered;

    private PaperPlaneNetworking() {
    }

    public static OpenTeleportScreenPacket playerListPacket(
        MinecraftServer server,
        UUID requester,
        UUID sessionId,
        PlaneKind planeKind
    ) {
        List<PlayerEntry> players = server.getPlayerList().getPlayers().stream()
            .filter(player -> !player.getUUID().equals(requester))
            .map(player -> new PlayerEntry(player.getUUID(), player.getGameProfile().getName()))
            .toList();
        return new OpenTeleportScreenPacket(sessionId, players, planeKind == PlaneKind.ENDER);
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.messageBuilder(OpenTeleportScreenPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(OpenTeleportScreenPacket::encode)
            .decoder(OpenTeleportScreenPacket::decode)
            .consumerMainThread((packet, context) -> PaperPlaneClient.open(packet))
            .add();
        CHANNEL.messageBuilder(TrackTeleportRequestPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(TrackTeleportRequestPacket::encode)
            .decoder(TrackTeleportRequestPacket::decode)
            .consumerMainThread((packet, context) -> PaperPlaneClient.track(packet))
            .add();
        CHANNEL.messageBuilder(RequestTeleportPacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
            .encoder(RequestTeleportPacket::encode)
            .decoder(RequestTeleportPacket::decode)
            .consumerMainThread((packet, context) -> {
                ServerPlayer sender = context.getSender();
                if (sender != null) {
                    PaperPlane.requestTeleport(sender, packet.sessionId(), packet.targetId());
                }
            })
            .add();
        CHANNEL.messageBuilder(RespondTeleportPacket.class, 3, NetworkDirection.PLAY_TO_SERVER)
            .encoder(RespondTeleportPacket::encode)
            .decoder(RespondTeleportPacket::decode)
            .consumerMainThread((packet, context) -> {
                ServerPlayer sender = context.getSender();
                if (sender != null) {
                    PaperPlane.respondToTeleportRequest(sender, packet.requestId(), packet.accept());
                }
            })
            .add();
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        if (!(packet instanceof OpenTeleportScreenPacket) && !(packet instanceof TrackTeleportRequestPacket)) {
            throw new IllegalArgumentException("Unsupported Paper Plane clientbound packet: " + packet.getClass().getName());
        }
        CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
    }

    public static void sendTeleportRequest(UUID sessionId, UUID targetId) {
        CHANNEL.send(new RequestTeleportPacket(sessionId, targetId), PacketDistributor.SERVER.noArg());
    }

    public static void sendTeleportResponse(UUID requestId, boolean accept) {
        CHANNEL.send(new RespondTeleportPacket(requestId, accept), PacketDistributor.SERVER.noArg());
    }
}
