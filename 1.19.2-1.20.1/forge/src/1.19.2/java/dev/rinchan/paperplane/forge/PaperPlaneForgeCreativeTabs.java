package dev.rinchan.paperplane.forge;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;

public final class PaperPlaneForgeCreativeTabs {
    private PaperPlaneForgeCreativeTabs() {
    }

    public static Item.Properties configure(Item.Properties properties) {
        return properties.tab(CreativeModeTab.TAB_MISC);
    }

    public static void register(IEventBus modBus) {
        // In 1.19.2 the item's properties own vanilla creative-tab placement.
    }
}
