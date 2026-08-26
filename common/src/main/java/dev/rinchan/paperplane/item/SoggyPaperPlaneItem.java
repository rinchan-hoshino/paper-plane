package dev.rinchan.paperplane.item;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.PlaneKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class SoggyPaperPlaneItem extends Item {
    public SoggyPaperPlaneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            PaperPlane.openTeleportScreen(serverPlayer, PlaneKind.SOGGY);
        }
        return InteractionResult.SUCCESS;
    }
}
