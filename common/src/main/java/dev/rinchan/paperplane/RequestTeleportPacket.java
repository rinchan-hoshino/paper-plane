package dev.rinchan.paperplane;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestTeleportPacket(UUID targetId, boolean enderPlane) implements CustomPacketPayload {
    public static final Type<RequestTeleportPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PaperPlane.MOD_ID, "request_teleport"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestTeleportPacket> CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        RequestTeleportPacket::targetId,
        ByteBufCodecs.BOOL,
        RequestTeleportPacket::enderPlane,
        RequestTeleportPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
