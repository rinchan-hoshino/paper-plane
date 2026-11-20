package dev.rinchan.paperplane.forge;

import dev.rinchan.paperplane.client.PaperPlaneConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

final class PaperPlaneForgeClient {
    private PaperPlaneForgeClient() {
    }

    static void register() {
        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new PaperPlaneConfigScreen(parent))
        );
    }
}
