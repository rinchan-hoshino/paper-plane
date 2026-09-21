package dev.rinchan.paperplane;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public record PlayerEntry(UUID id, String name) {
    public static PlayerEntry decode(FriendlyByteBuf buffer) {
        return new PlayerEntry(buffer.readUUID(), buffer.readUtf(64));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(id);
        buffer.writeUtf(name, 64);
    }
}
