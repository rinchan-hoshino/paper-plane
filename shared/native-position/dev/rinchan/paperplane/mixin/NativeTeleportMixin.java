package dev.rinchan.paperplane.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ftb.mods.ftbessentials.util.WarmupCooldownTeleporter;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import dev.ftb.mods.ftbessentials.util.TeleportPos.TeleportResult;
import dev.rinchan.paperplane.TeleportPayments;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = WarmupCooldownTeleporter.class, remap = false)
public class NativeTeleportMixin {
    @WrapMethod(method = "teleportNow")
    private TeleportResult paperPlane$complete(ServerPlayer player, TeleportPos destination,
                                               Operation<TeleportResult> original) {
        return TeleportPayments.teleport(player, this, () -> original.call(player, destination),
            TeleportResult::isSuccess, () -> recipient -> {
                recipient.sendSystemMessage(Component.translatable("message.paper_plane.no_selected_plane").withStyle(ChatFormatting.RED));
                return 0;
            });
    }
}
