package dev.rinchan.paperplane;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TrackTeleportRequestPacket(UUID requestId, boolean active) implements CustomPacketPayload {
    public static final Type<TrackTeleportRequestPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(PaperPlane.MOD_ID, "track_teleport_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TrackTeleportRequestPacket> CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        TrackTeleportRequestPacket::requestId,
        ByteBufCodecs.BOOL,
        TrackTeleportRequestPacket::active,
        TrackTeleportRequestPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
