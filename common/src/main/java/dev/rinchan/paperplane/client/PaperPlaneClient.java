package dev.rinchan.paperplane.client;

import dev.rinchan.paperplane.OpenTeleportScreenPacket;
import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.TeleportRequestPromptPacket;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class PaperPlaneClient {
    private PaperPlaneClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(PaperPlaneClient::registerEntityRenderers);
    }

    public static void openTeleportScreen(OpenTeleportScreenPacket packet) {
        new TeleportPlayerScreen(packet.players(), packet.enderPlane()).openGui();
    }

    public static void openRequestPrompt(TeleportRequestPromptPacket packet) {
        new TeleportRequestScreen(packet.requestId(), packet.requesterName(), packet.enderPlane()).openGui();
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PaperPlaneRegistries.PAPER_PLANE_ENTITY.get(), ThrownItemRenderer::new);
    }
}
