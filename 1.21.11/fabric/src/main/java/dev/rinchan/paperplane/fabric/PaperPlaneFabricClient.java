package dev.rinchan.paperplane.fabric;

import dev.rinchan.paperplane.OpenTeleportScreenPacket;
import dev.rinchan.paperplane.TrackTeleportRequestPacket;
import dev.rinchan.paperplane.client.PaperPlaneClient;
import dev.rinchan.paperplane.client.PaperPlaneEntityRenderer;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.entity.EntityRenderers;

public final class PaperPlaneFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRenderers.register(PaperPlaneRegistries.PAPER_PLANE_ENTITY.get(), PaperPlaneEntityRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(OpenTeleportScreenPacket.TYPE, (packet, context) -> context.client().execute(() -> PaperPlaneClient.openTeleportScreen(packet)));
        ClientPlayNetworking.registerGlobalReceiver(TrackTeleportRequestPacket.TYPE, (packet, context) -> context.client().execute(() -> PaperPlaneClient.trackTeleportRequest(packet)));
    }
}
