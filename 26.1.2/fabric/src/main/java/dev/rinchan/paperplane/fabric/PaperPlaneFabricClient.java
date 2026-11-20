package dev.rinchan.paperplane.fabric;

import dev.rinchan.paperplane.OpenTeleportScreenPacket;
import dev.rinchan.paperplane.client.PaperPlaneClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class PaperPlaneFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
            OpenTeleportScreenPacket.TYPE,
            (packet, context) -> context.client().execute(() -> PaperPlaneClient.openTeleportScreen(packet))
        );
    }
}
