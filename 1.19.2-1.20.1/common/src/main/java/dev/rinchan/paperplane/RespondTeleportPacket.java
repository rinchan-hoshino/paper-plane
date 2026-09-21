package dev.rinchan.paperplane;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record RespondTeleportPacket(String requestId, boolean accept) {
    public static final ResourceLocation ID = new ResourceLocation(PaperPlane.MOD_ID, "respond_teleport");
    private static final int MAX_REQUEST_ID_LENGTH = 64;

    public static RespondTeleportPacket decode(FriendlyByteBuf buffer) {
        return new RespondTeleportPacket(buffer.readUtf(MAX_REQUEST_ID_LENGTH), buffer.readBoolean());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(requestId, MAX_REQUEST_ID_LENGTH);
        buffer.writeBoolean(accept);
    }
}
