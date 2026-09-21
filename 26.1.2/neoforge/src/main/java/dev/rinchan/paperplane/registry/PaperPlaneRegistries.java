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
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PaperPlaneRegistries {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PaperPlane.MOD_ID);
    private static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(PaperPlane.MOD_ID);

    public static final Supplier<Item> PAPER_PLANE = ITEMS.registerItem(
        "paper_plane",
        properties -> new PaperPlaneItem(properties, PlaneKind.NORMAL),
        properties -> properties.stacksTo(16)
    );
    public static final Supplier<Item> SOGGY_PAPER_PLANE = ITEMS.registerItem(
        "soggy_paper_plane",
        SoggyPaperPlaneItem::new,
        properties -> properties.stacksTo(16)
    );
    public static final Supplier<Item> ENDER_PAPER_PLANE = ITEMS.registerItem(
        "ender_paper_plane",
        properties -> new PaperPlaneItem(properties, PlaneKind.ENDER),
        properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON)
    );

    public static final Supplier<EntityType<PaperPlaneEntity>> PAPER_PLANE_ENTITY = ENTITY_TYPES.registerEntityType(
        "paper_plane",
        PaperPlaneEntity::new,
        MobCategory.MISC,
        builder -> builder.sized(0.35F, 0.15F).clientTrackingRange(4).updateInterval(10)
    );

    private PaperPlaneRegistries() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
    }
}
