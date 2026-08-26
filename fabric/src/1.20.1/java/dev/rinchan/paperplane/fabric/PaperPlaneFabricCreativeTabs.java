package dev.rinchan.paperplane.fabric;

import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class PaperPlaneFabricCreativeTabs {
    private PaperPlaneFabricCreativeTabs() {
    }

    public static Item.Properties configure(Item.Properties properties) {
        return properties;
    }

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(PaperPlaneRegistries.PAPER_PLANE.get());
            entries.accept(PaperPlaneRegistries.SOGGY_PAPER_PLANE.get());
            entries.accept(PaperPlaneRegistries.ENDER_PAPER_PLANE.get());
        });
    }
}
