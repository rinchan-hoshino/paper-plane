package dev.rinchan.paperplane.neoforge;

import dev.rinchan.paperplane.client.PaperPlaneClientItemExtensions;
import dev.rinchan.paperplane.client.PaperPlaneEntityRenderer;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

final class PaperPlaneNeoForgeClient {
    private PaperPlaneNeoForgeClient() {}
    static void register(IEventBus modBus) {
        modBus.addListener(PaperPlaneNeoForgeClient::registerEntityRenderers);
        modBus.addListener(PaperPlaneNeoForgeClient::registerClientExtensions);
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(PaperPlaneClientItemExtensions.INSTANCE, PaperPlaneRegistries.PAPER_PLANE.get(), PaperPlaneRegistries.ENDER_PAPER_PLANE.get());
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PaperPlaneRegistries.PAPER_PLANE_ENTITY.get(), PaperPlaneEntityRenderer::new);
    }
}
