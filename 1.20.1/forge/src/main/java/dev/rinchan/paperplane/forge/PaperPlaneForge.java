package dev.rinchan.paperplane.forge;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.PaperPlaneNetworking;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;

@Mod(PaperPlane.MOD_ID)
public final class PaperPlaneForge {
    public PaperPlaneForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        PaperPlane.configure(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
        PaperPlaneRegistries.register(modBus);
        PaperPlaneForgeCreativeTabs.register(modBus);
        PaperPlaneNetworking.register();
        MinecraftForge.EVENT_BUS.addListener(PaperPlaneForge::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(PaperPlaneForge::onMissingMappings);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> PaperPlaneForgeClient::register);
        PaperPlane.LOGGER.info("Paper Plane initialized on Forge");
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            PaperPlane.clearPlayer(player);
        }
    }

    private static void onMissingMappings(MissingMappingsEvent event) {
        for (MissingMappingsEvent.Mapping<Item> mapping : event.getMappings(ForgeRegistries.Keys.ITEMS, PaperPlane.MOD_ID)) {
            if ("soggy_paper_plane".equals(mapping.getKey().getPath())) {
                mapping.remap(PaperPlaneRegistries.PAPER_PLANE.get());
            }
        }
    }
}
