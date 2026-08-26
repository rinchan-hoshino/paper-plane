package dev.rinchan.paperplane.fabric;

import dev.rinchan.paperplane.PaperPlaneNetworking;
import dev.rinchan.paperplane.client.PaperPlaneEntityRenderer;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class PaperPlaneFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(PaperPlaneRegistries.PAPER_PLANE_ENTITY.get(), PaperPlaneEntityRenderer::new);
        PaperPlaneNetworking.registerClient();
    }
}
