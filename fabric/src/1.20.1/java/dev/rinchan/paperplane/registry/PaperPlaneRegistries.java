package dev.rinchan.paperplane.registry;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.PlaneKind;
import dev.rinchan.paperplane.entity.PaperPlaneEntity;
import dev.rinchan.paperplane.fabric.PaperPlaneFabricCreativeTabs;
import dev.rinchan.paperplane.item.PaperPlaneItem;
import dev.rinchan.paperplane.item.SoggyPaperPlaneItem;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;

public final class PaperPlaneRegistries {
    public static final Supplier<Item> PAPER_PLANE = memoize(() -> Registry.register(
        BuiltInRegistries.ITEM,
        id("paper_plane"),
        new PaperPlaneItem(PaperPlaneFabricCreativeTabs.configure(new Item.Properties().stacksTo(16)), PlaneKind.NORMAL)
    ));
    public static final Supplier<Item> SOGGY_PAPER_PLANE = memoize(() -> Registry.register(
        BuiltInRegistries.ITEM,
        id("soggy_paper_plane"),
        new SoggyPaperPlaneItem(PaperPlaneFabricCreativeTabs.configure(new Item.Properties().stacksTo(16)))
    ));
    public static final Supplier<Item> ENDER_PAPER_PLANE = memoize(() -> Registry.register(
        BuiltInRegistries.ITEM,
        id("ender_paper_plane"),
        new PaperPlaneItem(PaperPlaneFabricCreativeTabs.configure(new Item.Properties().stacksTo(1).fireResistant()), PlaneKind.ENDER)
    ));
    public static final Supplier<EntityType<PaperPlaneEntity>> PAPER_PLANE_ENTITY = memoize(() -> Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        id("paper_plane"),
        FabricEntityTypeBuilder.<PaperPlaneEntity>create(MobCategory.MISC, PaperPlaneEntity::new)
            .dimensions(EntityDimensions.fixed(0.55F, 0.12F))
            .trackRangeBlocks(64)
            .trackedUpdateRate(1)
            .build()
    ));

    private PaperPlaneRegistries() {
    }

    public static void register() {
        PAPER_PLANE.get();
        SOGGY_PAPER_PLANE.get();
        ENDER_PAPER_PLANE.get();
        PAPER_PLANE_ENTITY.get();
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
