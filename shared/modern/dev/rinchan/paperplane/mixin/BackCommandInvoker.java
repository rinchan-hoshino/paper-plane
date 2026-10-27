package dev.rinchan.paperplane.mixin;

import dev.ftb.mods.ftbessentials.commands.groups.TeleportingCommands;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = TeleportingCommands.class, remap = false)
public interface BackCommandInvoker {
    @Invoker("back")
    static int paperPlane$back(ServerPlayer player) { throw new AssertionError(); }
}
