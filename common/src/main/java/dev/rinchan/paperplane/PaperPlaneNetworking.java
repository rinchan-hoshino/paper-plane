package dev.rinchan.paperplane;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PaperPlaneNetworking {
    private PaperPlaneNetworking() {
    }

    public static OpenTeleportScreenPacket playerListPacket(MinecraftServer server, java.util.UUID requesterId, boolean enderPlane) {
        List<PlayerEntry> entries = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.getUUID().equals(requesterId)) {
                entries.add(new PlayerEntry(player.getUUID(), player.getGameProfile().getName()));
            }
        }
        return new OpenTeleportScreenPacket(entries, enderPlane);
    }

    public static void sendTeleportRequest(UUIDLikeTarget target, boolean enderPlane) {
        PacketDistributor.sendToServer(new RequestTeleportPacket(target.id(), enderPlane));
    }

    public static void answerRequest(java.util.UUID requestId, boolean accepted) {
        PacketDistributor.sendToServer(new AnswerTeleportRequestPacket(requestId, accepted));
    }

    public interface UUIDLikeTarget {
        java.util.UUID id();
    }
}
