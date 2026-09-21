package dev.rinchan.paperplane.client;

import dev.rinchan.paperplane.OpenTeleportScreenPacket;
import dev.rinchan.paperplane.PaperPlaneNetworking;
import dev.rinchan.paperplane.TrackTeleportRequestPacket;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PaperPlaneClient {
    private static final Set<UUID> TRACKED_REQUESTS = new HashSet<>();

    private PaperPlaneClient() {
    }

    public static void open(OpenTeleportScreenPacket packet) {
        new TeleportPlayerScreen(packet.sessionId(), packet.players(), packet.ender()).openGui();
    }

    public static void track(TrackTeleportRequestPacket packet) {
        if (packet.active()) {
            TRACKED_REQUESTS.add(packet.requestId());
        } else {
            TRACKED_REQUESTS.remove(packet.requestId());
        }
    }

    public static boolean handleTeleportResponseCommand(String command) {
        String prefix;
        boolean accept;
        if (command.startsWith("tpaccept ")) {
            prefix = "tpaccept ";
            accept = true;
        } else if (command.startsWith("tpdeny ")) {
            prefix = "tpdeny ";
            accept = false;
        } else {
            return false;
        }
        try {
            UUID id = UUID.fromString(command.substring(prefix.length()).trim());
            if (!TRACKED_REQUESTS.contains(id)) {
                return false;
            }
            PaperPlaneNetworking.sendTeleportResponse(id, accept);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
