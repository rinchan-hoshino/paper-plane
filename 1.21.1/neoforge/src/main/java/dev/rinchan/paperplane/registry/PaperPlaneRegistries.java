package dev.rinchan.paperplane.registry;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.PlaneKind;
import dev.rinchan.paperplane.item.PaperPlaneItem;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PaperPlaneRegistries {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, PaperPlane.MOD_ID);

    public static final Supplier<Item> PAPER_PLANE = ITEMS.register(
        "paper_plane",
        () -> new PaperPlaneItem(new Item.Properties().stacksTo(16), PlaneKind.NORMAL)
    );
    public static final Supplier<Item> ENDER_PAPER_PLANE = ITEMS.register(
        "ender_paper_plane",
        () -> new PaperPlaneItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), PlaneKind.ENDER)
    );
    public static final Supplier<Item> BACK_PAPER_PLANE = ITEMS.register(
        "back_paper_plane",
        () -> new PaperPlaneItem(new Item.Properties().stacksTo(16), PlaneKind.BACK)
    );
    public static final Supplier<Item> ENDER_BACK_PAPER_PLANE = ITEMS.register(
        "ender_back_paper_plane",
        () -> new PaperPlaneItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), PlaneKind.ENDER_BACK)
    );

    private PaperPlaneRegistries() {
    }

    public static void register(IEventBus modBus) {
        // Lossless save migration: remap legacy soggy_paper_plane items to standard paper_plane.
        ITEMS.addAlias(
            ResourceLocation.fromNamespaceAndPath(PaperPlane.MOD_ID, "soggy_paper_plane"),
            ResourceLocation.fromNamespaceAndPath(PaperPlane.MOD_ID, "paper_plane")
        );
        ITEMS.register(modBus);
    }
}
