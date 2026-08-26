package dev.rinchan.paperplane;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record RequestTeleportPacket(UUID sessionId, UUID targetId) {
    public static final ResourceLocation ID = new ResourceLocation(PaperPlane.MOD_ID, "request_teleport");

    public static RequestTeleportPacket decode(FriendlyByteBuf buffer) {
        return new RequestTeleportPacket(buffer.readUUID(), buffer.readUUID());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(sessionId);
        buffer.writeUUID(targetId);
    }
}
