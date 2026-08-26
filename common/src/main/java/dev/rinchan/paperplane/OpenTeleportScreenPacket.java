package dev.rinchan.paperplane;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenTeleportScreenPacket(UUID sessionId, List<PlayerEntry> players, boolean enderPlane) implements CustomPacketPayload {
    public static final Type<OpenTeleportScreenPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PaperPlane.MOD_ID, "open_teleport_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTeleportScreenPacket> CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        OpenTeleportScreenPacket::sessionId,
        PlayerEntry.CODEC.apply(ByteBufCodecs.list()),
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
