package dev.rinchan.paperplane.registry;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.PlaneKind;
import dev.rinchan.paperplane.forge.PaperPlaneForgeCreativeTabs;
import dev.rinchan.paperplane.item.PaperPlaneItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PaperPlaneRegistries {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PaperPlane.MOD_ID);

    public static final RegistryObject<Item> PAPER_PLANE = ITEMS.register(
        "paper_plane", () -> new PaperPlaneItem(PaperPlaneForgeCreativeTabs.configure(new Item.Properties().stacksTo(16)), PlaneKind.NORMAL)
    );
    public static final RegistryObject<Item> ENDER_PAPER_PLANE = ITEMS.register(
        "ender_paper_plane", () -> new PaperPlaneItem(PaperPlaneForgeCreativeTabs.configure(new Item.Properties().stacksTo(1).fireResistant()), PlaneKind.ENDER)
    );
    public static final RegistryObject<Item> BACK_PAPER_PLANE = ITEMS.register(
        "back_paper_plane", () -> new PaperPlaneItem(PaperPlaneForgeCreativeTabs.configure(new Item.Properties().stacksTo(16)), PlaneKind.BACK)
    );
    public static final RegistryObject<Item> ENDER_BACK_PAPER_PLANE = ITEMS.register(
        "ender_back_paper_plane", () -> new PaperPlaneItem(PaperPlaneForgeCreativeTabs.configure(new Item.Properties().stacksTo(1).fireResistant()), PlaneKind.ENDER_BACK)
    );

    private PaperPlaneRegistries() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
