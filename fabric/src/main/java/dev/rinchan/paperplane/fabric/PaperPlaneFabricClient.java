package dev.rinchan.paperplane.fabric;

import dev.rinchan.paperplane.OpenTeleportScreenPacket;
import dev.rinchan.paperplane.TrackTeleportRequestPacket;
import dev.rinchan.paperplane.client.PaperPlaneClient;
import dev.rinchan.paperplane.client.PaperPlaneEntityRenderer;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class PaperPlaneFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(PaperPlaneRegistries.PAPER_PLANE_ENTITY.get(), PaperPlaneEntityRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(OpenTeleportScreenPacket.ID, (client, handler, buffer, responseSender) -> {
            OpenTeleportScreenPacket packet = OpenTeleportScreenPacket.decode(buffer);
            client.execute(() -> PaperPlaneClient.open(packet));
        });
        ClientPlayNetworking.registerGlobalReceiver(TrackTeleportRequestPacket.ID, (client, handler, buffer, responseSender) -> {
            TrackTeleportRequestPacket packet = TrackTeleportRequestPacket.decode(buffer);
            client.execute(() -> PaperPlaneClient.track(packet));
        });
    }
}
