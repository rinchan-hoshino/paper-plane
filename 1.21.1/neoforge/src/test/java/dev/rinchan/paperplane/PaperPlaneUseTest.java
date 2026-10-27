package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import dev.rinchan.paperplane.item.PaperPlaneItem;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PaperPlaneUseTest {
    @BeforeAll static void bootstrap() {
        net.neoforged.fml.loading.LoadingModList.of(List.of(),List.of(),List.of(),List.of(),Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        ((net.minecraft.core.MappedRegistry<Item>)net.minecraft.core.registries.BuiltInRegistries.ITEM).unfreeze();
    }

    @ParameterizedTest @EnumSource(PlaneKind.class)
    void crouchingUseDelegatesToTheServerWithoutChargingThrowingOrConsuming(PlaneKind kind) {
        var level = mock(ServerLevel.class);
        var player = mock(ServerPlayer.class);
        var held = new ItemStack(Items.PAPER,3);
        when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(held);
        when(player.isShiftKeyDown()).thenReturn(true);
        var item = new PaperPlaneItem(new Item.Properties(),kind);
        try (var paper = mockStatic(PaperPlane.class)) {
            item.use(level,player,InteractionHand.MAIN_HAND);
            paper.verify(()->PaperPlane.openTeleportScreen(player,kind));
            verify(player,never()).startUsingItem(any());
            assertEquals(0,item.getUseDuration(held,player));
            item.releaseUsing(held,level,player,0);
            verify(level,never()).addFreshEntity(any());
            assertEquals(3,held.getCount());
        }
    }
}
