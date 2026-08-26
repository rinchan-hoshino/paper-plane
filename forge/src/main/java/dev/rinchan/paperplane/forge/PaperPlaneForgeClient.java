package dev.rinchan.paperplane.forge;

import dev.rinchan.paperplane.client.PaperPlaneEntityRenderer;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

final class PaperPlaneForgeClient {
    private PaperPlaneForgeClient() {
    }

    static void register(IEventBus modBus) {
        modBus.addListener(PaperPlaneForgeClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PaperPlaneRegistries.PAPER_PLANE_ENTITY.get(), PaperPlaneEntityRenderer::new);
    }
}
