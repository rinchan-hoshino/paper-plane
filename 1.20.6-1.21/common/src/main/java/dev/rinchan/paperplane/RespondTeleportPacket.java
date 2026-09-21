package dev.rinchan.paperplane;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RespondTeleportPacket(UUID requestId, boolean accept) implements CustomPacketPayload {
    public static final Type<RespondTeleportPacket> TYPE = new Type<>(
        ResourceLocation.tryBuild(PaperPlane.MOD_ID, "respond_teleport")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RespondTeleportPacket> CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        RespondTeleportPacket::requestId,
        ByteBufCodecs.BOOL,
        RespondTeleportPacket::accept,
        RespondTeleportPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
