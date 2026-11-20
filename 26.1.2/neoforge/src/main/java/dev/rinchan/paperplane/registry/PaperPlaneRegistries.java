package dev.rinchan.paperplane.registry;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.PlaneKind;
import dev.rinchan.paperplane.item.PaperPlaneItem;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PaperPlaneRegistries {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PaperPlane.MOD_ID);

    public static final Supplier<Item> PAPER_PLANE = ITEMS.registerItem(
        "paper_plane",
        properties -> new PaperPlaneItem(properties, PlaneKind.NORMAL),
        properties -> properties.stacksTo(16)
    );
    public static final Supplier<Item> ENDER_PAPER_PLANE = ITEMS.registerItem(
        "ender_paper_plane",
        properties -> new PaperPlaneItem(properties, PlaneKind.ENDER),
        properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON)
    );
    public static final Supplier<Item> BACK_PAPER_PLANE = ITEMS.registerItem(
        "back_paper_plane",
        properties -> new PaperPlaneItem(properties, PlaneKind.BACK),
        properties -> properties.stacksTo(16)
    );
    public static final Supplier<Item> ENDER_BACK_PAPER_PLANE = ITEMS.registerItem(
        "ender_back_paper_plane",
        properties -> new PaperPlaneItem(properties, PlaneKind.ENDER_BACK),
        properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON)
    );

    private PaperPlaneRegistries() {
    }

    public static void register(IEventBus modBus) {
        // Lossless save migration: remap legacy soggy_paper_plane items to standard paper_plane.
        ITEMS.addAlias(
            Identifier.fromNamespaceAndPath(PaperPlane.MOD_ID, "soggy_paper_plane"),
            Identifier.fromNamespaceAndPath(PaperPlane.MOD_ID, "paper_plane")
        );
        ITEMS.register(modBus);
    }
}
