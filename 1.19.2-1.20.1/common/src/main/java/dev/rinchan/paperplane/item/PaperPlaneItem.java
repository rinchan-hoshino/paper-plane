package dev.rinchan.paperplane.item;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.PaperPlaneFlightModel;
import dev.rinchan.paperplane.PlaneKind;
import dev.rinchan.paperplane.entity.PaperPlaneEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class PaperPlaneItem extends Item {
    private static final int USE_DURATION = 72_000;
    private final PlaneKind planeKind;

    public PaperPlaneItem(Properties properties, PlaneKind planeKind) {
        super(properties);
        this.planeKind = planeKind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && planeKind.throwable()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            PaperPlane.openTeleportScreen(serverPlayer, planeKind);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.TOOT_HORN;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        int chargeTicks = getUseDuration(stack) - timeLeft;
        if (!PaperPlaneFlightModel.canLaunch(chargeTicks) || !(entity instanceof Player player)) {
            return;
        }
        if (!level.isClientSide) {
            PaperPlaneEntity plane = new PaperPlaneEntity(level, player, planeKind == PlaneKind.ENDER);
            ItemStack thrown = stack.copy();
            thrown.setCount(1);
            plane.setItem(thrown);
            plane.shootFromRotation(
                player,
                player.getXRot(),
                player.getYRot(),
                0.0F,
                (float) PaperPlaneFlightModel.launchSpeed(chargeTicks),
                0.08F
            );
            level.addFreshEntity(plane);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 0.6F, 1.1F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
    }
}
