package dev.rinchan.paperplane;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record TrackTeleportRequestPacket(UUID requestId, boolean active) {
    public static final ResourceLocation ID = new ResourceLocation(PaperPlane.MOD_ID, "track_teleport_request");

    public static TrackTeleportRequestPacket decode(FriendlyByteBuf buffer) {
        return new TrackTeleportRequestPacket(buffer.readUUID(), buffer.readBoolean());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(requestId);
        buffer.writeBoolean(active);
    }
}
