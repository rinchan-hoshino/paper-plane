package dev.rinchan.paperplane.registry;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.PlaneKind;
import dev.rinchan.paperplane.entity.PaperPlaneEntity;
import dev.rinchan.paperplane.item.PaperPlaneItem;
import dev.rinchan.paperplane.item.SoggyPaperPlaneItem;
import java.util.function.Supplier;
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

    private PaperPlaneRegistries() {}

    public static void register() {
        normal = Registry.register(BuiltInRegistries.ITEM, id("paper_plane"), new PaperPlaneItem(new Item.Properties().stacksTo(16), PlaneKind.NORMAL));
        soggy = Registry.register(BuiltInRegistries.ITEM, id("soggy_paper_plane"), new SoggyPaperPlaneItem(new Item.Properties().stacksTo(16)));
        ender = Registry.register(BuiltInRegistries.ITEM, id("ender_paper_plane"), new PaperPlaneItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), PlaneKind.ENDER));
        entity = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("paper_plane"), EntityType.Builder.<PaperPlaneEntity>of(PaperPlaneEntity::new, MobCategory.MISC).sized(0.35F, 0.15F).clientTrackingRange(4).updateInterval(10).build(ResourceKey.create(Registries.ENTITY_TYPE, id("paper_plane"))));
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(PaperPlane.MOD_ID, path); }
}
