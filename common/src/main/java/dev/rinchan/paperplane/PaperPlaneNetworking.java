package dev.rinchan.paperplane;

import dev.rinchan.paperplane.client.PaperPlaneClient;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

public final class PaperPlaneNetworking {
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

    public static void register(IPayloadRegistrar registrar) {
        IPayloadRegistrar versioned = registrar.versioned("1");
        versioned.play(OpenTeleportScreenPacket.ID, OpenPayload::new,
            handlers -> handlers.client(PaperPlaneNetworking::handleOpen));
        versioned.play(TrackTeleportRequestPacket.ID, TrackPayload::new,
            handlers -> handlers.client(PaperPlaneNetworking::handleTrack));
        versioned.play(RequestTeleportPacket.ID, RequestPayload::new,
            handlers -> handlers.server(PaperPlaneNetworking::handleRequest));
        versioned.play(RespondTeleportPacket.ID, RespondPayload::new,
            handlers -> handlers.server(PaperPlaneNetworking::handleResponse));
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        PacketDistributor.PLAYER.with(player).send(toPayload(packet));
    }

    public static void sendTeleportRequest(UUID sessionId, UUID targetId) {
        PacketDistributor.SERVER.noArg().send(new RequestPayload(new RequestTeleportPacket(sessionId, targetId)));
    }

    public static void sendTeleportResponse(UUID requestId, boolean accept) {
        PacketDistributor.SERVER.noArg().send(new RespondPayload(new RespondTeleportPacket(requestId, accept)));
    }

    private static CustomPacketPayload toPayload(Object packet) {
        if (packet instanceof OpenTeleportScreenPacket open) {
            return new OpenPayload(open);
        }
        if (packet instanceof TrackTeleportRequestPacket track) {
            return new TrackPayload(track);
        }
        throw new IllegalArgumentException("Unsupported Paper Plane clientbound packet: " + packet.getClass().getName());
    }

    private static void handleOpen(OpenPayload payload, PlayPayloadContext context) {
        observeNetworkWork(context.workHandler().submitAsync(() -> PaperPlaneClient.open(payload.packet())), "open teleport screen");
    }

    private static void handleTrack(TrackPayload payload, PlayPayloadContext context) {
        observeNetworkWork(context.workHandler().submitAsync(() -> PaperPlaneClient.track(payload.packet())), "track teleport request");
    }

    private static void handleRequest(RequestPayload payload, PlayPayloadContext context) {
        ServerPlayer sender = context.player().filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast).orElse(null);
        if (sender != null) {
            observeNetworkWork(
                context.workHandler().submitAsync(() -> PaperPlane.requestTeleport(sender, payload.packet().sessionId(), payload.packet().targetId())),
                "handle teleport selection"
            );
        }
    }

    private static void handleResponse(RespondPayload payload, PlayPayloadContext context) {
        ServerPlayer sender = context.player().filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast).orElse(null);
        if (sender != null) {
            observeNetworkWork(
                context.workHandler().submitAsync(() -> PaperPlane.respondToTeleportRequest(sender, payload.packet().requestId(), payload.packet().accept())),
                "handle teleport response"
            );
        }
    }

    private static void observeNetworkWork(CompletableFuture<?> work, String operation) {
        work.whenComplete((ignored, error) -> {
            if (error != null) {
                PaperPlane.LOGGER.error("Paper Plane network work failed while attempting to {}", operation, error);
            }
        });
    }

    private record OpenPayload(OpenTeleportScreenPacket packet) implements CustomPacketPayload {
        private OpenPayload(FriendlyByteBuf buffer) {
            this(OpenTeleportScreenPacket.decode(buffer));
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            packet.encode(buffer);
        }

        @Override
        public net.minecraft.resources.ResourceLocation id() {
            return OpenTeleportScreenPacket.ID;
        }
    }

    private record TrackPayload(TrackTeleportRequestPacket packet) implements CustomPacketPayload {
        private TrackPayload(FriendlyByteBuf buffer) {
            this(TrackTeleportRequestPacket.decode(buffer));
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            packet.encode(buffer);
        }

        @Override
        public net.minecraft.resources.ResourceLocation id() {
            return TrackTeleportRequestPacket.ID;
        }
    }

    private record RequestPayload(RequestTeleportPacket packet) implements CustomPacketPayload {
        private RequestPayload(FriendlyByteBuf buffer) {
            this(RequestTeleportPacket.decode(buffer));
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            packet.encode(buffer);
        }

        @Override
        public net.minecraft.resources.ResourceLocation id() {
            return RequestTeleportPacket.ID;
        }
    }

    private record RespondPayload(RespondTeleportPacket packet) implements CustomPacketPayload {
        private RespondPayload(FriendlyByteBuf buffer) {
            this(RespondTeleportPacket.decode(buffer));
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            packet.encode(buffer);
        }

        @Override
        public net.minecraft.resources.ResourceLocation id() {
            return RespondTeleportPacket.ID;
        }
    }
}
