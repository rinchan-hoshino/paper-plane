package dev.rinchan.paperplane.item;

import dev.rinchan.paperplane.PlaneKind;
import dev.rinchan.paperplane.client.PaperPlaneClientItemExtensions;
import java.util.function.Consumer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class PaperPlaneForgeItem extends PaperPlaneItem {
    public PaperPlaneForgeItem(Properties properties, PlaneKind planeKind) {
        super(properties, planeKind);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new PaperPlaneClientItemExtensions());
    }
}
