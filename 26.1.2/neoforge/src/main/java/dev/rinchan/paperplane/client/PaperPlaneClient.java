package dev.rinchan.paperplane.client;

import dev.rinchan.paperplane.OpenTeleportScreenPacket;
import dev.rinchan.paperplane.TeleportResponseCommand;
import dev.rinchan.paperplane.PaperPlaneNetworking;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

public final class PaperPlaneClient {

    private PaperPlaneClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(PaperPlaneClient::registerEntityRenderers);
        modBus.addListener(PaperPlaneClient::registerClientExtensions);
    }

    public static void openTeleportScreen(OpenTeleportScreenPacket packet) {
        new TeleportPlayerScreen(packet.sessionId(), packet.players(), packet.enderPlane()).openGui();
    }

    public static boolean handleTeleportResponseCommand(String command) {
        var response = TeleportResponseCommand.parse(command);
        if (response.isEmpty()) return false;
        PaperPlaneNetworking.sendTeleportResponse(response.get().requestId(), response.get().accept());
        return true;
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(PaperPlaneClientItemExtensions.INSTANCE, PaperPlaneRegistries.PAPER_PLANE.get(), PaperPlaneRegistries.ENDER_PAPER_PLANE.get());
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PaperPlaneRegistries.PAPER_PLANE_ENTITY.get(), PaperPlaneEntityRenderer::new);
    }
}
