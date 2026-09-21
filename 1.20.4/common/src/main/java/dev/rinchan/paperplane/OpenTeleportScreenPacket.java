package dev.rinchan.paperplane;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record OpenTeleportScreenPacket(UUID sessionId, List<PlayerEntry> players, boolean ender) {
    public static final ResourceLocation ID = new ResourceLocation(PaperPlane.MOD_ID, "open_teleport_screen");
    private static final int MAX_PLAYER_ENTRIES = 4096;

    public static OpenTeleportScreenPacket decode(FriendlyByteBuf buffer) {
        UUID sessionId = buffer.readUUID();
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_PLAYER_ENTRIES) {
            throw new IllegalArgumentException("Invalid Paper Plane player count: " + size);
        }
        List<PlayerEntry> players = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            players.add(PlayerEntry.decode(buffer));
        }
        return new OpenTeleportScreenPacket(sessionId, List.copyOf(players), buffer.readBoolean());
    }

    public void encode(FriendlyByteBuf buffer) {
        if (players.size() > MAX_PLAYER_ENTRIES) {
            throw new IllegalArgumentException("Too many Paper Plane player entries: " + players.size());
        }
        buffer.writeUUID(sessionId);
        buffer.writeVarInt(players.size());
        players.forEach(player -> player.encode(buffer));
        buffer.writeBoolean(ender);
    }
}
