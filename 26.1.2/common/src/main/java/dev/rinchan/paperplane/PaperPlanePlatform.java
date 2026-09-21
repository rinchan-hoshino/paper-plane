package dev.rinchan.paperplane;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Minecraft API differences; request and payment rules live in the shared service. */
final class PaperPlanePlatform {
    private PaperPlanePlatform() {}
    static MinecraftServer server(ServerPlayer player) { return player.level().getServer(); }
    static String playerName(ServerPlayer player) { return player.getGameProfile().name(); }
}
