package dev.rinchan.paperplane;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TeleportRequestPromptPacket(UUID requestId, String requesterName, boolean enderPlane) implements CustomPacketPayload {
    public static final Type<TeleportRequestPromptPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PaperPlane.MOD_ID, "teleport_request_prompt"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportRequestPromptPacket> CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        TeleportRequestPromptPacket::requestId,
        ByteBufCodecs.STRING_UTF8,
        TeleportRequestPromptPacket::requesterName,
        ByteBufCodecs.BOOL,
        TeleportRequestPromptPacket::enderPlane,
        TeleportRequestPromptPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
