package dev.rinchan.paperplane.mixin;

import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void paperPlane$becomeSoggyInWater(CallbackInfo ci) {
        ItemEntity item = (ItemEntity) (Object) this;
        if (item.level().isClientSide() || !item.isInWater()) {
            return;
        }
        ItemStack stack = item.getItem();
        if (!stack.is(PaperPlaneRegistries.PAPER_PLANE.get())) {
            return;
        }
        item.setItem(new ItemStack(PaperPlaneRegistries.SOGGY_PAPER_PLANE.get(), stack.getCount()));
    }
}
