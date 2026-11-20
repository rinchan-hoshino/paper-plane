package dev.rinchan.paperplane.client;

import dev.rinchan.paperplane.OpenTeleportScreenPacket;
import dev.rinchan.paperplane.PaperPlaneNetworking;

public final class PaperPlaneClient {

    private PaperPlaneClient() {
    }

    public static void openTeleportScreen(OpenTeleportScreenPacket packet) {
        new TeleportPlayerScreen(packet.sessionId(), packet.players(), packet.enderPlane()).openGui();
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

        if (!requestId.matches("[0-9A-Fa-f]{8}")) {
            return false;
        }
        PaperPlaneNetworking.sendTeleportResponse(requestId, accept);
        return true;
    }
}
