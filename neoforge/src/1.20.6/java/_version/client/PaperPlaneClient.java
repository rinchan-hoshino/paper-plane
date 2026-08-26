package dev.rinchan.paperplane.client;

import dev.rinchan.paperplane.OpenTeleportScreenPacket;
import dev.rinchan.paperplane.PaperPlaneNetworking;
import dev.rinchan.paperplane.TrackTeleportRequestPacket;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class PaperPlaneClient {
    private static final Set<UUID> TRACKED_REQUESTS = new HashSet<>();

    private PaperPlaneClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(PaperPlaneClient::registerEntityRenderers);
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
            PaperPlaneNetworking.sendTeleportResponse(id, accept);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PaperPlaneRegistries.PAPER_PLANE_ENTITY.get(), PaperPlaneEntityRenderer::new);
    }
}
