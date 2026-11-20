package dev.rinchan.paperplane.neoforge;

import dev.rinchan.paperplane.client.PaperPlaneConfigScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

final class PaperPlaneNeoForgeClient {
    private PaperPlaneNeoForgeClient() {
    }

    static void register(ModContainer modContainer) {
        modContainer.registerExtensionPoint(
            IConfigScreenFactory.class,
            (container, parent) -> new PaperPlaneConfigScreen(parent)
        );
    }
}
