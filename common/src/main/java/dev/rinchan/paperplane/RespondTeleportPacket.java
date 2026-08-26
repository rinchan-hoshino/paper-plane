package dev.rinchan.paperplane;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record RespondTeleportPacket(UUID requestId, boolean accept) {
    public static final ResourceLocation ID = new ResourceLocation(PaperPlane.MOD_ID, "respond_teleport");

    public static RespondTeleportPacket decode(FriendlyByteBuf buffer) {
        return new RespondTeleportPacket(buffer.readUUID(), buffer.readBoolean());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(requestId);
        buffer.writeBoolean(accept);
    }
}
