package dev.rinchan.paperplane.registry;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.PlaneKind;
import dev.rinchan.paperplane.fabric.PaperPlaneFabricCreativeTabs;
import dev.rinchan.paperplane.item.PaperPlaneItem;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class PaperPlaneRegistries {
    public static final Supplier<Item> PAPER_PLANE = memoize(() -> Registry.register(
        BuiltInRegistries.ITEM,
        id("paper_plane"),
        new PaperPlaneItem(PaperPlaneFabricCreativeTabs.configure(new Item.Properties().stacksTo(16)), PlaneKind.NORMAL)
    ));
    public static final Supplier<Item> ENDER_PAPER_PLANE = memoize(() -> Registry.register(
        BuiltInRegistries.ITEM,
        id("ender_paper_plane"),
        new PaperPlaneItem(PaperPlaneFabricCreativeTabs.configure(new Item.Properties().stacksTo(1).fireResistant()), PlaneKind.ENDER)
    ));
    public static final Supplier<Item> BACK_PAPER_PLANE = memoize(() -> Registry.register(
        BuiltInRegistries.ITEM,
        id("back_paper_plane"),
        new PaperPlaneItem(PaperPlaneFabricCreativeTabs.configure(new Item.Properties().stacksTo(16)), PlaneKind.BACK)
    ));
    public static final Supplier<Item> ENDER_BACK_PAPER_PLANE = memoize(() -> Registry.register(
        BuiltInRegistries.ITEM,
        id("ender_back_paper_plane"),
        new PaperPlaneItem(PaperPlaneFabricCreativeTabs.configure(new Item.Properties().stacksTo(1).fireResistant()), PlaneKind.ENDER_BACK)
    ));

    private PaperPlaneRegistries() {
    }

    public static void register() {
        PAPER_PLANE.get();
        ENDER_PAPER_PLANE.get();
        BACK_PAPER_PLANE.get();
        ENDER_BACK_PAPER_PLANE.get();
        // Lossless save migration: remap legacy soggy_paper_plane items to standard paper_plane.
        BuiltInRegistries.ITEM.addAlias(id("soggy_paper_plane"), id("paper_plane"));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(PaperPlane.MOD_ID, path);
    }

    private static <T> Supplier<T> memoize(Supplier<T> factory) {
        return new Supplier<>() {
            private T value;

            @Override
            public T get() {
                if (value == null) {
                    value = factory.get();
                }
                return value;
            }
        };
    }
}
