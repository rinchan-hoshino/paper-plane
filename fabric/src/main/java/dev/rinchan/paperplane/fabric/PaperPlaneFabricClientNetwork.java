package dev.rinchan.paperplane.fabric;

import dev.rinchan.paperplane.RequestTeleportPacket;
import dev.rinchan.paperplane.RespondTeleportPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;

public final class PaperPlaneFabricClientNetwork {
    private PaperPlaneFabricClientNetwork() {
    }

    public static void send(Object packet) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        if (packet instanceof RequestTeleportPacket request) {
            request.encode(buffer);
            ClientPlayNetworking.send(RequestTeleportPacket.ID, buffer);
        } else if (packet instanceof RespondTeleportPacket response) {
            response.encode(buffer);
            ClientPlayNetworking.send(RespondTeleportPacket.ID, buffer);
        } else {
            throw new IllegalArgumentException("Unsupported Paper Plane serverbound packet: " + packet.getClass().getName());
        }
    }
}
