package dev.rinchan.paperplane.registry;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.PlaneKind;
import dev.rinchan.paperplane.item.PaperPlaneItem;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
        normal = registerItem("paper_plane", properties -> new PaperPlaneItem(properties, PlaneKind.NORMAL), properties -> properties.stacksTo(16));
        ender = registerItem("ender_paper_plane", properties -> new PaperPlaneItem(properties, PlaneKind.ENDER), properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
        back = registerItem("back_paper_plane", properties -> new PaperPlaneItem(properties, PlaneKind.BACK), properties -> properties.stacksTo(16));
        enderBack = registerItem("ender_back_paper_plane", properties -> new PaperPlaneItem(properties, PlaneKind.ENDER_BACK), properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
        // Lossless save migration: remap legacy soggy_paper_plane items to standard paper_plane.
        BuiltInRegistries.ITEM.addAlias(id("soggy_paper_plane"), id("paper_plane"));
    }

    private static <T extends Item> T registerItem(String path, Function<Item.Properties, T> factory, UnaryOperator<Item.Properties> configure) {
        Identifier identifier = id(path);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, identifier);
        Item.Properties properties = configure.apply(new Item.Properties()).setId(key);
        return Registry.register(BuiltInRegistries.ITEM, identifier, factory.apply(properties));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(PaperPlane.MOD_ID, path);
    }
}
