package dev.rinchan.paperplane;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AnswerTeleportRequestPacket(UUID requestId, boolean accepted) implements CustomPacketPayload {
    public static final Type<AnswerTeleportRequestPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PaperPlane.MOD_ID, "answer_teleport_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnswerTeleportRequestPacket> CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        AnswerTeleportRequestPacket::requestId,
        ByteBufCodecs.BOOL,
        AnswerTeleportRequestPacket::accepted,
        AnswerTeleportRequestPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
