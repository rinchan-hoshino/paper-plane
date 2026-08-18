package dev.rinchan.paperplane.client;

import dev.rinchan.paperplane.OpenTeleportScreenPacket;
import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.RespondTeleportPacket;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import java.util.UUID;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PaperPlaneClient {
    private PaperPlaneClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(PaperPlaneClient::registerEntityRenderers);
    }

    public static void openTeleportScreen(OpenTeleportScreenPacket packet) {
        new TeleportPlayerScreen(packet.players(), packet.enderPlane()).openGui();
    }

    public static boolean handleTeleportResponseCommand(String command) {
        boolean accept;
        String requestId;
        if (command.startsWith("tpaccept ")) {
            accept = true;
            requestId = command.substring("tpaccept ".length());
        } else if (command.startsWith("tpdeny ")) {
            accept = false;
            requestId = command.substring("tpdeny ".length());
        } else {
            return false;
        }

        try {
            PacketDistributor.sendToServer(new RespondTeleportPacket(UUID.fromString(requestId), accept));
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PaperPlaneRegistries.PAPER_PLANE_ENTITY.get(), ThrownItemRenderer::new);
    }
}
