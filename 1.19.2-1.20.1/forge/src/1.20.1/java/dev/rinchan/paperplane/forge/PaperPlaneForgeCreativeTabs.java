package dev.rinchan.paperplane.forge;

import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class PaperPlaneForgeCreativeTabs {
    private PaperPlaneForgeCreativeTabs() {
    }

    public static Item.Properties configure(Item.Properties properties) {
        return properties;
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(PaperPlaneForgeCreativeTabs::addItems);
    }

    private static void addItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PaperPlaneRegistries.PAPER_PLANE.get());
            event.accept(PaperPlaneRegistries.SOGGY_PAPER_PLANE.get());
            event.accept(PaperPlaneRegistries.ENDER_PAPER_PLANE.get());
        }
    }
}
