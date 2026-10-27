package dev.rinchan.paperplane;

import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

final class FtbPlayerData {
    private FtbPlayerData() {}
    static Optional<FTBEPlayerData> get(ServerPlayer player) { return Optional.of(FTBEPlayerData.get(player)); }
}
