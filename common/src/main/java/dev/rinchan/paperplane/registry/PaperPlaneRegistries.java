package dev.rinchan.paperplane.registry;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.entity.PaperPlaneEntity;
import dev.rinchan.paperplane.item.PaperPlaneItem;
import dev.rinchan.paperplane.item.SoggyPaperPlaneItem;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PaperPlaneRegistries {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, PaperPlane.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, PaperPlane.MOD_ID);

    public static final Supplier<Item> PAPER_PLANE = ITEMS.register(
        "paper_plane",
        () -> new PaperPlaneItem(false, new Item.Properties().stacksTo(16))
    );
    public static final Supplier<Item> SOGGY_PAPER_PLANE = ITEMS.register(
        "soggy_paper_plane",
        () -> new SoggyPaperPlaneItem(new Item.Properties().stacksTo(16))
    );
    public static final Supplier<Item> ENDER_PAPER_PLANE = ITEMS.register(
        "ender_paper_plane",
        () -> new PaperPlaneItem(true, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON))
    );

    public static final Supplier<EntityType<PaperPlaneEntity>> PAPER_PLANE_ENTITY = ENTITY_TYPES.register(
        "paper_plane",
        () -> EntityType.Builder.<PaperPlaneEntity>of(PaperPlaneEntity::new, MobCategory.MISC)
            .sized(0.35F, 0.15F)
            .clientTrackingRange(4)
            .updateInterval(10)
            .build(PaperPlane.MOD_ID + ":paper_plane")
    );

    private PaperPlaneRegistries() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
    }
}
