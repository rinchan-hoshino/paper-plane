package dev.rinchan.paperplane;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record TrackTeleportRequestPacket(String requestId, boolean active) {
    public static final ResourceLocation ID = new ResourceLocation(PaperPlane.MOD_ID, "track_teleport_request");
    private static final int MAX_REQUEST_ID_LENGTH = 64;

    public static TrackTeleportRequestPacket decode(FriendlyByteBuf buffer) {
        return new TrackTeleportRequestPacket(buffer.readUtf(MAX_REQUEST_ID_LENGTH), buffer.readBoolean());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(requestId, MAX_REQUEST_ID_LENGTH);
        buffer.writeBoolean(active);
    }
}
