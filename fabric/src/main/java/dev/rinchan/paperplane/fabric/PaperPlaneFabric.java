package dev.rinchan.paperplane.fabric;

import dev.rinchan.paperplane.*;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class PaperPlaneFabric implements ModInitializer {
    @Override public void onInitialize() {
        PaperPlaneRegistries.register();
        PayloadTypeRegistry.serverboundPlay().register(RequestTeleportPacket.TYPE, RequestTeleportPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RespondTeleportPacket.TYPE, RespondTeleportPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenTeleportScreenPacket.TYPE, OpenTeleportScreenPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TrackTeleportRequestPacket.TYPE, TrackTeleportRequestPacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RequestTeleportPacket.TYPE, (packet, context) -> context.server().execute(() -> {
            try { PaperPlane.requestTeleport(context.player(), packet.sessionId(), packet.targetId()); }
            catch (Throwable failure) { PaperPlane.LOGGER.error("Failed to request teleport", failure); }
        }));
        ServerPlayNetworking.registerGlobalReceiver(RespondTeleportPacket.TYPE, (packet, context) -> context.server().execute(() -> {
            try { PaperPlane.respondToTeleportRequest(context.player(), packet.requestId(), packet.accept()); }
            catch (Throwable failure) { PaperPlane.LOGGER.error("Failed to respond to teleport request", failure); }
        }));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PaperPlane.clearPlayer(handler.player));
    }
}
