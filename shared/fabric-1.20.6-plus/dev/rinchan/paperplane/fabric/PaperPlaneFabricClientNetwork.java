package dev.rinchan.paperplane.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

@Environment(EnvType.CLIENT)
public final class PaperPlaneFabricClientNetwork {
    private PaperPlaneFabricClientNetwork() {}
    public static void send(CustomPacketPayload payload) { ClientPlayNetworking.send(payload); }
}
