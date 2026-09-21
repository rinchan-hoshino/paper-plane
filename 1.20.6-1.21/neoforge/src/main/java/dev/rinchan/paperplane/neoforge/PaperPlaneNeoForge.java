package dev.rinchan.paperplane.neoforge;

import dev.rinchan.paperplane.OpenTeleportScreenPacket;
import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.RequestTeleportPacket;
import dev.rinchan.paperplane.RespondTeleportPacket;
import dev.rinchan.paperplane.TrackTeleportRequestPacket;
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
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(PaperPlane.MOD_ID)
public class PaperPlaneNeoForge {
    public PaperPlaneNeoForge(IEventBus modBus) {
        PaperPlaneRegistries.register(modBus);
        modBus.addListener(this::registerPayloads);
        modBus.addListener(this::addCreativeTabItems);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogout);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            PaperPlaneClient.register(modBus);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToClient(OpenTeleportScreenPacket.TYPE, OpenTeleportScreenPacket.CODEC,
            (packet, context) -> enqueue(context, () -> PaperPlaneClient.openTeleportScreen(packet), "open teleport screen"));
        registrar.playToClient(TrackTeleportRequestPacket.TYPE, TrackTeleportRequestPacket.CODEC,
            (packet, context) -> enqueue(context, () -> PaperPlaneClient.trackTeleportRequest(packet), "track teleport request"));
        registrar.playToServer(RequestTeleportPacket.TYPE, RequestTeleportPacket.CODEC,
            (packet, context) -> enqueue(context, () -> {
                if (context.player() instanceof ServerPlayer player) {
                    PaperPlane.requestTeleport(player, packet.sessionId(), packet.targetId());
                }
            }, "request teleport"));
        registrar.playToServer(RespondTeleportPacket.TYPE, RespondTeleportPacket.CODEC,
            (packet, context) -> enqueue(context, () -> {
                if (context.player() instanceof ServerPlayer player) {
                    PaperPlane.respondToTeleportRequest(player, packet.requestId(), packet.accept());
                }
            }, "respond to teleport request"));
    }

    private static void enqueue(IPayloadContext context, Runnable work, String action) {
        context.enqueueWork(work).whenComplete((unused, failure) -> {
            if (failure != null) {
                PaperPlane.LOGGER.error("Failed to {}", action, failure);
            }
        });
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PaperPlaneRegistries.PAPER_PLANE.get());
            event.accept(PaperPlaneRegistries.SOGGY_PAPER_PLANE.get());
            event.accept(PaperPlaneRegistries.ENDER_PAPER_PLANE.get());
        }
    }

    private void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PaperPlane.clearPlayer(player);
        }
    }
}
