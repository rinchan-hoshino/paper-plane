package dev.rinchan.paperplane.item;

import dev.rinchan.paperplane.PaperPlane;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SoggyPaperPlaneItem extends Item {
    public SoggyPaperPlaneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            PaperPlane.openTeleportScreen(serverPlayer, false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
