package dev.rinchan.paperplane.neoforge;

import dev.rinchan.paperplane.AnswerTeleportRequestPacket;
import dev.rinchan.paperplane.OpenTeleportScreenPacket;
import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.RequestTeleportPacket;
import dev.rinchan.paperplane.TeleportRequestPromptPacket;
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
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(PaperPlane.MOD_ID)
public class PaperPlaneNeoForge {
    public PaperPlaneNeoForge(IEventBus modBus) {
        PaperPlaneRegistries.register(modBus);
        modBus.addListener(this::registerPayloads);
        modBus.addListener(this::addCreativeTabItems);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogout);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            PaperPlaneClient.register(modBus);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(OpenTeleportScreenPacket.TYPE, OpenTeleportScreenPacket.CODEC, (packet, context) -> {
            context.enqueueWork(() -> PaperPlaneClient.openTeleportScreen(packet)).exceptionally(throwable -> null);
        });
        registrar.playToClient(TeleportRequestPromptPacket.TYPE, TeleportRequestPromptPacket.CODEC, (packet, context) -> {
            context.enqueueWork(() -> PaperPlaneClient.openRequestPrompt(packet)).exceptionally(throwable -> null);
        });
        registrar.playToServer(RequestTeleportPacket.TYPE, RequestTeleportPacket.CODEC, (packet, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    PaperPlane.requestTeleport(player, packet.targetId(), packet.enderPlane());
                }
            }).exceptionally(throwable -> null);
        });
        registrar.playToServer(AnswerTeleportRequestPacket.TYPE, AnswerTeleportRequestPacket.CODEC, (packet, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    PaperPlane.answerTeleport(player, packet.requestId(), packet.accepted());
                }
            }).exceptionally(throwable -> null);
        });
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PaperPlaneRegistries.PAPER_PLANE.get());
            event.accept(PaperPlaneRegistries.SOGGY_PAPER_PLANE.get());
            event.accept(PaperPlaneRegistries.ENDER_PAPER_PLANE.get());
        }
    }

    private void onServerTick(ServerTickEvent.Post event) {
        PaperPlane.tick(event.getServer());
    }

    private void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PaperPlane.clearPlayer(player);
        }
    }
}
