package dev.rinchan.paperplane;

import dev.ftb.mods.ftbessentials.command.TeleportCommands;
import net.minecraft.server.level.ServerPlayer;

final class FtbBack {
    private FtbBack() {}
    static int run(ServerPlayer player) { return TeleportCommands.back(player); }
}
