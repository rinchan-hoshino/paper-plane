package dev.rinchan.paperplane.client;

import dev.rinchan.paperplane.OpenTeleportScreenPacket;
import dev.rinchan.paperplane.RespondTeleportPacket;
import dev.rinchan.paperplane.TrackTeleportRequestPacket;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PaperPlaneClient {
    private static final Set<UUID> TRACKED_REQUESTS = new HashSet<>();

    private PaperPlaneClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(PaperPlaneClient::registerEntityRenderers);
        modBus.addListener(PaperPlaneClient::registerClientExtensions);
    }

    public static void openTeleportScreen(OpenTeleportScreenPacket packet) {
        new TeleportPlayerScreen(packet.sessionId(), packet.players(), packet.enderPlane()).openGui();
    }

    public static void trackTeleportRequest(TrackTeleportRequestPacket packet) {
        if (packet.active()) {
            TRACKED_REQUESTS.add(packet.requestId());
        } else {
            TRACKED_REQUESTS.remove(packet.requestId());
        }
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
            UUID id = UUID.fromString(requestId);
            if (!TRACKED_REQUESTS.contains(id)) {
                return false;
            }
            PacketDistributor.sendToServer(new RespondTeleportPacket(id, accept));
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(PaperPlaneClientItemExtensions.INSTANCE, PaperPlaneRegistries.PAPER_PLANE.get(), PaperPlaneRegistries.ENDER_PAPER_PLANE.get());
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PaperPlaneRegistries.PAPER_PLANE_ENTITY.get(), PaperPlaneEntityRenderer::new);
    }
}
