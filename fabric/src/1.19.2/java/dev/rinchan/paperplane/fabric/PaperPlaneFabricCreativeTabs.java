package dev.rinchan.paperplane.fabric;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

public final class PaperPlaneFabricCreativeTabs {
    private PaperPlaneFabricCreativeTabs() {
    }

    public static Item.Properties configure(Item.Properties properties) {
        return properties.tab(CreativeModeTab.TAB_MISC);
    }

    public static void register() {
        // In 1.19.2 the item's properties own vanilla creative-tab placement.
    }
}
