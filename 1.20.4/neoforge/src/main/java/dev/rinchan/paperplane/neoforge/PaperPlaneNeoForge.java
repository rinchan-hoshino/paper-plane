package dev.rinchan.paperplane.neoforge;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.PaperPlaneNetworking;
import dev.rinchan.paperplane.client.PaperPlaneClient;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;

@Mod(PaperPlane.MOD_ID)
public final class PaperPlaneNeoForge {
    public PaperPlaneNeoForge(IEventBus modBus) {
        PaperPlaneRegistries.register(modBus);
        modBus.addListener(this::addCreativeTabContents);
        modBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.addListener(this::playerLoggedOut);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            PaperPlaneClient.register(modBus);
        }
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PaperPlaneRegistries.PAPER_PLANE.get());
            event.accept(PaperPlaneRegistries.SOGGY_PAPER_PLANE.get());
            event.accept(PaperPlaneRegistries.ENDER_PAPER_PLANE.get());
        }
    }

    private void registerPayloads(RegisterPayloadHandlerEvent event) {
        PaperPlaneNetworking.register(event.registrar(PaperPlane.MOD_ID));
    }

    private void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PaperPlane.clearPlayer(player);
        }
    }
}
