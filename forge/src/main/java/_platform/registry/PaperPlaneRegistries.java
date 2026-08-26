package dev.rinchan.paperplane.registry;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.PlaneKind;
import dev.rinchan.paperplane.entity.PaperPlaneEntity;
import dev.rinchan.paperplane.item.PaperPlaneItem;
import dev.rinchan.paperplane.item.SoggyPaperPlaneItem;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class PaperPlaneRegistries {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PaperPlane.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, PaperPlane.MOD_ID);

    public static final Supplier<Item> PAPER_PLANE = ITEMS.register("paper_plane", () ->
        new PaperPlaneItem(new Item.Properties().stacksTo(16), PlaneKind.NORMAL));
    public static final Supplier<Item> SOGGY_PAPER_PLANE = ITEMS.register("soggy_paper_plane", () ->
        new SoggyPaperPlaneItem(new Item.Properties().stacksTo(16)));
    public static final Supplier<Item> ENDER_PAPER_PLANE = ITEMS.register("ender_paper_plane", () ->
        new PaperPlaneItem(new Item.Properties().stacksTo(16), PlaneKind.ENDER));
    public static final Supplier<EntityType<PaperPlaneEntity>> PAPER_PLANE_ENTITY = ENTITY_TYPES.register("paper_plane", () ->
        EntityType.Builder.<PaperPlaneEntity>of(PaperPlaneEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.125F)
            .clientTrackingRange(8)
            .updateInterval(1)
            .build("paper_plane:paper_plane"));

    private PaperPlaneRegistries() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
    }
}
