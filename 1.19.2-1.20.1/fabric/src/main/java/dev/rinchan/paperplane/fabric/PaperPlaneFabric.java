package dev.rinchan.paperplane.fabric;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.PaperPlaneNetworking;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class PaperPlaneFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PaperPlaneRegistries.register();
        PaperPlaneFabricCreativeTabs.register();
        PaperPlaneNetworking.registerServer();
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PaperPlane.clearPlayer(handler.player));
        PaperPlane.LOGGER.info("Paper Plane initialized on Fabric");
    }
}
