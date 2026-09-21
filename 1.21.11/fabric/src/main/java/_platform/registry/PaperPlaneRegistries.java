package dev.rinchan.paperplane.registry;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.PlaneKind;
import dev.rinchan.paperplane.entity.PaperPlaneEntity;
import dev.rinchan.paperplane.item.PaperPlaneItem;
import dev.rinchan.paperplane.item.SoggyPaperPlaneItem;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class PaperPlaneRegistries {
    private static Item normal;
    private static Item soggy;
    private static Item ender;
    private static EntityType<PaperPlaneEntity> entity;

    public static final Supplier<Item> PAPER_PLANE = () -> normal;
    public static final Supplier<Item> SOGGY_PAPER_PLANE = () -> soggy;
    public static final Supplier<Item> ENDER_PAPER_PLANE = () -> ender;
    public static final Supplier<EntityType<PaperPlaneEntity>> PAPER_PLANE_ENTITY = () -> entity;

    private PaperPlaneRegistries() {
    }

    public static void register() {
        normal = registerItem("paper_plane", properties -> new PaperPlaneItem(properties, PlaneKind.NORMAL), properties -> properties.stacksTo(16));
        soggy = registerItem("soggy_paper_plane", SoggyPaperPlaneItem::new, properties -> properties.stacksTo(16));
        ender = registerItem("ender_paper_plane", properties -> new PaperPlaneItem(properties, PlaneKind.ENDER), properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));

        Identifier identifier = id("paper_plane");
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, identifier);
        entity = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            identifier,
            EntityType.Builder.<PaperPlaneEntity>of(PaperPlaneEntity::new, MobCategory.MISC)
                .sized(0.35F, 0.15F)
                .clientTrackingRange(4)
                .updateInterval(10)
                .build(key)
        );
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
