package dev.rinchan.paperplane.client;

import dev.rinchan.paperplane.FallbackTeleportResponsePacket;
import dev.rinchan.paperplane.OpenTeleportScreenPacket;
import dev.rinchan.paperplane.PaperPlaneNetworking;
import dev.rinchan.paperplane.TeleportResponseCommand;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

public final class PaperPlaneClient {
    private static final Set<TeleportResponseCommand> FALLBACK_COMMANDS = new HashSet<>();

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
        Optional<TeleportResponseCommand> parsed = TeleportResponseCommand.parse(command);
        if (parsed.isEmpty()) return false;

        TeleportResponseCommand response = parsed.get();
        if (FALLBACK_COMMANDS.remove(response)) return false;

        PaperPlaneNetworking.sendTeleportResponse(response.requestId(), response.accept());
        return true;
    }

    public static void fallbackTeleportResponse(FallbackTeleportResponsePacket packet) {
        TeleportResponseCommand response = new TeleportResponseCommand(packet.requestId(), packet.accept());
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return;

        FALLBACK_COMMANDS.add(response);
        try {
            connection.sendUnsignedCommand(response.command());
        } finally {
            FALLBACK_COMMANDS.remove(response);
        }
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(PaperPlaneClientItemExtensions.INSTANCE, PaperPlaneRegistries.PAPER_PLANE.get(), PaperPlaneRegistries.ENDER_PAPER_PLANE.get());
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PaperPlaneRegistries.PAPER_PLANE_ENTITY.get(), PaperPlaneEntityRenderer::new);
    }
}
