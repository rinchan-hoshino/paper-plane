package dev.rinchan.paperplane.client;

import dev.rinchan.paperplane.OpenTeleportScreenPacket;
import dev.rinchan.paperplane.PaperPlaneNetworking;
import dev.rinchan.paperplane.TrackTeleportRequestPacket;
import java.util.HashSet;
import java.util.Set;

public final class PaperPlaneClient {
    private static final Set<String> TRACKED_REQUESTS = new HashSet<>();

    private PaperPlaneClient() {
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

        if (requestId.isEmpty() || !TRACKED_REQUESTS.contains(requestId)) {
            return false;
        }
        PaperPlaneNetworking.sendTeleportResponse(requestId, accept);
        return true;
    }
}
