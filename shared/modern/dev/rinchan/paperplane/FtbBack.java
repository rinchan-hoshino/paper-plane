package dev.rinchan.paperplane;

import dev.rinchan.paperplane.mixin.BackCommandInvoker;
import net.minecraft.server.level.ServerPlayer;

final class FtbBack {
    private FtbBack() {}
    static int run(ServerPlayer player) { return BackCommandInvoker.paperPlane$back(player); }
}
