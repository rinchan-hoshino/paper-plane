package dev.rinchan.paperplane.fabric;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.RequestTeleportPacket;
import dev.rinchan.paperplane.RespondTeleportPacket;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.item.CreativeModeTabs;

public final class PaperPlaneFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PaperPlaneRegistries.register();
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(PaperPlaneRegistries.PAPER_PLANE.get());
            entries.accept(PaperPlaneRegistries.SOGGY_PAPER_PLANE.get());
            entries.accept(PaperPlaneRegistries.ENDER_PAPER_PLANE.get());
        });
        ServerPlayNetworking.registerGlobalReceiver(RequestTeleportPacket.ID, (server, player, handler, buffer, responseSender) -> {
            RequestTeleportPacket packet = RequestTeleportPacket.decode(buffer);
            server.execute(() -> PaperPlane.requestTeleport(player, packet.sessionId(), packet.targetId()));
        });
        ServerPlayNetworking.registerGlobalReceiver(RespondTeleportPacket.ID, (server, player, handler, buffer, responseSender) -> {
            RespondTeleportPacket packet = RespondTeleportPacket.decode(buffer);
            server.execute(() -> PaperPlane.respondToTeleportRequest(player, packet.requestId(), packet.accept()));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            server.execute(() -> PaperPlane.clearPlayer(handler.player)));
    }
}
