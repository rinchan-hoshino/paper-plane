package dev.rinchan.paperplane.registry;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.PlaneKind;
import dev.rinchan.paperplane.entity.PaperPlaneEntity;
import dev.rinchan.paperplane.forge.PaperPlaneForgeCreativeTabs;
import dev.rinchan.paperplane.item.PaperPlaneForgeItem;
import dev.rinchan.paperplane.item.SoggyPaperPlaneItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PaperPlaneRegistries {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PaperPlane.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, PaperPlane.MOD_ID);

    public static final RegistryObject<Item> PAPER_PLANE = ITEMS.register(
        "paper_plane", () -> new PaperPlaneForgeItem(PaperPlaneForgeCreativeTabs.configure(new Item.Properties().stacksTo(16)), PlaneKind.NORMAL)
    );
    public static final RegistryObject<Item> SOGGY_PAPER_PLANE = ITEMS.register(
        "soggy_paper_plane", () -> new SoggyPaperPlaneItem(PaperPlaneForgeCreativeTabs.configure(new Item.Properties().stacksTo(16)))
    );
    public static final RegistryObject<Item> ENDER_PAPER_PLANE = ITEMS.register(
        "ender_paper_plane", () -> new PaperPlaneForgeItem(PaperPlaneForgeCreativeTabs.configure(new Item.Properties().stacksTo(1).fireResistant()), PlaneKind.ENDER)
    );
    public static final RegistryObject<EntityType<PaperPlaneEntity>> PAPER_PLANE_ENTITY = ENTITY_TYPES.register(
        "paper_plane",
        () -> EntityType.Builder.<PaperPlaneEntity>of(PaperPlaneEntity::new, MobCategory.MISC)
            .sized(0.55F, 0.12F)
            .clientTrackingRange(64)
            .updateInterval(1)
            .build("paper_plane")
    );

    private PaperPlaneRegistries() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
    }
}
