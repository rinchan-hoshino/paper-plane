package dev.rinchan.paperplane.forge;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.PaperPlaneNetworking;
import dev.rinchan.paperplane.client.PaperPlaneClient;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(PaperPlane.MOD_ID)
public final class PaperPlaneForge {
    public PaperPlaneForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        PaperPlaneRegistries.register(modBus);
        PaperPlaneNetworking.register();
        modBus.addListener(this::addCreativeTabContents);
        MinecraftForge.EVENT_BUS.addListener(this::playerLoggedOut);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> PaperPlaneClient.register(modBus));
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PaperPlaneRegistries.PAPER_PLANE.get());
            event.accept(PaperPlaneRegistries.SOGGY_PAPER_PLANE.get());
            event.accept(PaperPlaneRegistries.ENDER_PAPER_PLANE.get());
        }
    }

    private void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PaperPlane.clearPlayer(player);
        }
    }
}
