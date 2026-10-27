package dev.rinchan.paperplane;

import dev.ftb.mods.ftbessentials.command.TPACommands;
import java.util.UUID;

final class FtbRequestTarget {
    private FtbRequestTarget() {}

    static UUID id(TPACommands.TPARequest request) {
        return request.target().uuid;
    }
}
