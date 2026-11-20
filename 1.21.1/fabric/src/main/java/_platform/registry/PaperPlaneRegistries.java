package dev.rinchan.paperplane.registry;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.PlaneKind;
import dev.rinchan.paperplane.item.PaperPlaneItem;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class PaperPlaneRegistries {
    private static Item normal;
    private static Item ender;
    private static Item back;
    private static Item enderBack;

    public static final Supplier<Item> PAPER_PLANE = () -> normal;
    public static final Supplier<Item> ENDER_PAPER_PLANE = () -> ender;
    public static final Supplier<Item> BACK_PAPER_PLANE = () -> back;
    public static final Supplier<Item> ENDER_BACK_PAPER_PLANE = () -> enderBack;

    private PaperPlaneRegistries() {
    }

    public static void register() {
        normal = Registry.register(BuiltInRegistries.ITEM, id("paper_plane"), new PaperPlaneItem(new Item.Properties().stacksTo(16), PlaneKind.NORMAL));
        ender = Registry.register(BuiltInRegistries.ITEM, id("ender_paper_plane"), new PaperPlaneItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), PlaneKind.ENDER));
        back = Registry.register(BuiltInRegistries.ITEM, id("back_paper_plane"), new PaperPlaneItem(new Item.Properties().stacksTo(16), PlaneKind.BACK));
        enderBack = Registry.register(BuiltInRegistries.ITEM, id("ender_back_paper_plane"), new PaperPlaneItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), PlaneKind.ENDER_BACK));
        // Lossless save migration: remap legacy soggy_paper_plane items to standard paper_plane.
        BuiltInRegistries.ITEM.addAlias(id("soggy_paper_plane"), id("paper_plane"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(PaperPlane.MOD_ID, path);
    }
}
