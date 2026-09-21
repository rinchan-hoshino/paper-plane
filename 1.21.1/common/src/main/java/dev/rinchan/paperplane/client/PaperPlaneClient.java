package dev.rinchan.paperplane.client;

import dev.rinchan.paperplane.FallbackTeleportResponsePacket;
import dev.rinchan.paperplane.OpenTeleportScreenPacket;
import dev.rinchan.paperplane.PaperPlaneNetworking;
import dev.rinchan.paperplane.TeleportResponseCommand;
import dev.rinchan.rinlib.state.ReentrantFlag;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public final class PaperPlaneClient {
    private static final ReentrantFlag FALLBACK = new ReentrantFlag();

    private PaperPlaneClient() {}

    public static void openTeleportScreen(OpenTeleportScreenPacket packet) {
        new TeleportPlayerScreen(packet.sessionId(), packet.players(), packet.enderPlane()).openGui();
    }

    public static boolean handleTeleportResponseCommand(String command) {
        Optional<TeleportResponseCommand> parsed = TeleportResponseCommand.parse(command);
        if (parsed.isEmpty()) return false;

        TeleportResponseCommand response = parsed.get();
        if (FALLBACK.isSet()) return false;

        PaperPlaneNetworking.sendTeleportResponse(response.requestId(), response.accept());
        return true;
    }

    public static void fallbackTeleportResponse(FallbackTeleportResponsePacket packet) {
        TeleportResponseCommand response = new TeleportResponseCommand(packet.requestId(), packet.accept());
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return;

        try (ReentrantFlag.Scope ignored = FALLBACK.enter()) {
            connection.sendUnsignedCommand(response.command());
        }
    }
}
