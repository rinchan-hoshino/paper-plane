package dev.rinchan.paperplane;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FallbackTeleportResponsePacket(UUID requestId, boolean accept) implements CustomPacketPayload {
    public static final Type<FallbackTeleportResponsePacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(PaperPlane.MOD_ID, "fallback_teleport_response")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, FallbackTeleportResponsePacket> CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        FallbackTeleportResponsePacket::requestId,
        ByteBufCodecs.BOOL,
        FallbackTeleportResponsePacket::accept,
        FallbackTeleportResponsePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
