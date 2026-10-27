package dev.rinchan.paperplane.client;

import dev.rinchan.paperplane.OpenTeleportScreenPacket;
import dev.rinchan.paperplane.PaperPlaneNetworking;
import dev.rinchan.paperplane.TeleportResponseCommand;

public final class PaperPlaneClient {

    private PaperPlaneClient() {}

    public static void openTeleportScreen(OpenTeleportScreenPacket packet) {
        new TeleportPlayerScreen(packet.sessionId(), packet.players(), packet.enderPlane()).openGui();
    }

    public static boolean handleTeleportResponseCommand(String command) {
        var response = TeleportResponseCommand.parse(command);
        if (response.isEmpty()) return false;
        PaperPlaneNetworking.sendTeleportResponse(response.get().requestId(), response.get().accept());
        return true;
    }

}
