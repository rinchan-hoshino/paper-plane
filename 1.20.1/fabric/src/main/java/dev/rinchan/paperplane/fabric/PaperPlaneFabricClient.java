package dev.rinchan.paperplane.fabric;

import dev.rinchan.paperplane.PaperPlaneNetworking;
import net.fabricmc.api.ClientModInitializer;

public final class PaperPlaneFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PaperPlaneNetworking.registerClient();
    }
}
