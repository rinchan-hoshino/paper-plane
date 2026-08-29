package dev.rinchan.paperplane.fabric;

import dev.rinchan.paperplane.FallbackTeleportResponsePacket;
import dev.rinchan.paperplane.OpenTeleportScreenPacket;
import dev.rinchan.paperplane.client.PaperPlaneClient;
import dev.rinchan.paperplane.client.PaperPlaneEntityRenderer;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class PaperPlaneFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(PaperPlaneRegistries.PAPER_PLANE_ENTITY.get(), PaperPlaneEntityRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(OpenTeleportScreenPacket.TYPE,
            (packet, context) -> context.client().execute(() -> PaperPlaneClient.openTeleportScreen(packet)));
        ClientPlayNetworking.registerGlobalReceiver(FallbackTeleportResponsePacket.TYPE,
            (packet, context) -> context.client().execute(() -> PaperPlaneClient.fallbackTeleportResponse(packet)));
    }
}
