package dev.rinchan.paperplane;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenTeleportScreenPacket(List<PlayerEntry> players, boolean enderPlane) implements CustomPacketPayload {
    public static final Type<OpenTeleportScreenPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PaperPlane.MOD_ID, "open_teleport_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTeleportScreenPacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.collection(ArrayList::new, PlayerEntry.CODEC),
        OpenTeleportScreenPacket::players,
        ByteBufCodecs.BOOL,
        OpenTeleportScreenPacket::enderPlane,
        OpenTeleportScreenPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
