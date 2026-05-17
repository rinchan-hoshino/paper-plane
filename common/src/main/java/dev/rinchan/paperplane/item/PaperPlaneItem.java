package dev.rinchan.paperplane.item;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.entity.PaperPlaneEntity;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PaperPlaneItem extends Item {
    private final boolean enderPlane;

    public PaperPlaneItem(boolean enderPlane, Properties properties) {
        super(properties);
        this.enderPlane = enderPlane;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            throwPlane(level, player, stack, hand);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (player instanceof ServerPlayer serverPlayer) {
            PaperPlane.openTeleportScreen(serverPlayer, enderPlane);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!enderPlane && !level.isClientSide() && entity instanceof Player player && entity.isInWater()) {
            player.getInventory().setItem(slot, stack.transmuteCopy(PaperPlaneRegistries.SOGGY_PAPER_PLANE.get(), stack.getCount()));
        }
    }

    private void throwPlane(Level level, Player player, ItemStack stack, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        PaperPlaneEntity entity = new PaperPlaneEntity(serverLevel, player, enderPlane);
        entity.setItem(stack.copyWithCount(1));
        entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0F, 0.85F, 0.35F);
        serverLevel.addFreshEntity(entity);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5F, 1.35F);

        if (!enderPlane && !player.getAbilities().instabuild) {
            stack.shrink(1);
            player.setItemInHand(hand, stack);
        }
    }
}
