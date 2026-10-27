package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;
import dev.ftb.mods.ftbessentials.api.TeleportResult;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import dev.ftb.mods.ftbessentials.util.WarmupCooldownTeleporter;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.*;

/** Run by the native Mixin proof launcher with Sponge-transformed FTB classes. */
class NativeAppliedTeleportTest {
    @BeforeAll static void bootstrap() {
        assumeTrue(Arrays.stream(WarmupCooldownTeleporter.class.getDeclaredMethods()).anyMatch(m -> m.getName().contains("paperPlane$")), "Requires the Sponge native proof launcher");
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var registry = (net.minecraft.core.MappedRegistry<net.minecraft.world.item.Item>) net.minecraft.core.registries.BuiltInRegistries.ITEM;
        registry.unfreeze();
        for (String id : List.of("paper_plane", "soggy_paper_plane", "ender_paper_plane", "back_paper_plane", "ender_back_paper_plane")) {
            var key = net.minecraft.resources.ResourceLocation.parse("paper_plane:" + id);
            if (registry.getOptional(key).isEmpty()) net.minecraft.core.Registry.register(registry, key, new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties()));
        }
    }

    @org.junit.jupiter.api.io.TempDir java.nio.file.Path directory;

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = PlaneKind.class, names = {"NORMAL", "ENDER"})
    void delayedTpaCompletesWithItsOriginalPlaneKind(PlaneKind kind) throws Exception {
        try (var f = new Fixture(kind)) {
            var target = mock(ServerPlayer.class);
            when(target.getUUID()).thenReturn(UUID.randomUUID());
            var server = ServerPlayer.class.getField("server"); server.setAccessible(true); server.set(target, f.server);
            var field = PaperPlane.class.getDeclaredField("REQUESTS"); field.setAccessible(true);
            var requests = (TeleportRequestLedger) field.get(null);
            var request = UUID.randomUUID();
            requests.add(new TeleportRequestLedger.Pending(request, f.id, target.getUUID(), kind));
            assertEquals(1, PaperPlane.acceptTeleportRequest(target, request.toString(),
                () -> f.teleporter.teleport(f.player, p -> f.destination).runCommand(f.player)));
            assertTrue(requests.hasPending(f.id));
            assertEquals(3, f.stack.getCount());
            f.finishWarmup();
            assertEquals(kind.consumable() ? 2 : 3, f.stack.getCount());
            assertFalse(requests.hasPending(f.id));
        }
    }

    @Test void delayedBackConsumesOnlyAfterNativeCompletionAndKeepsNativeCooldown() throws Exception {
        try (var f = new Fixture(PlaneKind.BACK)) {
            PaperPlane.openTeleportScreen(f.player, PlaneKind.BACK);
            assertEquals(3, f.stack.getCount());
            assertTrue(TeleportPayments.pending(f.id));
            f.finishWarmup();
            assertEquals(2, f.stack.getCount());
            assertFalse(TeleportPayments.pending(f.id));
            assertFalse(f.teleporter.checkCooldown(f.player).isSuccess());
            PaperPlane.openTeleportScreen(f.player, PlaneKind.BACK);
            assertEquals(2, f.stack.getCount(), "Native cooldown blocks immediate second use");
            assertFalse(TeleportPayments.pending(f.id));
        }
    }

    @Test void movementInterruptionLeavesThePlaneAndClearsBusyState() throws Exception {
        try (var f = new Fixture(PlaneKind.BACK)) {
            PaperPlane.openTeleportScreen(f.player, PlaneKind.BACK);
            when(f.player.position()).thenReturn(new Vec3(2, 0, 0));
            WarmupCooldownTeleporter.tickWarmups(f.server);
            assertEquals(3, f.stack.getCount());
            assertFalse(TeleportPayments.pending(f.id));
        }
    }

    @Test void losingThePlaneDuringWarmupPreventsMovementAndCooldownCharge() throws Exception {
        try (var f = new Fixture(PlaneKind.BACK)) {
            PaperPlane.openTeleportScreen(f.player, PlaneKind.BACK);
            f.stack.setCount(0);
            f.finishWarmup();
            assertFalse(TeleportPayments.pending(f.id));
            verify(f.destination, never()).teleport(any());
            assertTrue(f.teleporter.checkCooldown(f.player).isSuccess());
        }
    }

    @Test void reusableBackKeepsItsItemAfterNativeCompletion() throws Exception {
        try (var f = new Fixture(PlaneKind.ENDER_BACK)) {
            PaperPlane.openTeleportScreen(f.player, PlaneKind.ENDER_BACK);
            f.finishWarmup();
            assertEquals(3, f.stack.getCount());
            assertFalse(TeleportPayments.pending(f.id));
        }
    }

    @Test void failedNativeMovementReturnsTheReservedPlane() throws Exception {
        try (var f = new Fixture(PlaneKind.BACK)) {
            when(f.destination.teleport(f.player)).thenReturn(p -> 0);
            PaperPlane.openTeleportScreen(f.player, PlaneKind.BACK);
            f.finishWarmup();
            assertEquals(3, f.stack.getCount());
            assertFalse(TeleportPayments.pending(f.id));
        }
    }

    @Test void serverBackToggleStopsTheNativeOperation() throws Exception {
        java.nio.file.Files.writeString(directory.resolve("paper_plane.toml"), "enableTpa = true\nenableBack = false\n");
        PaperPlane.configure(directory);
        try (var f = new Fixture(PlaneKind.BACK)) {
            PaperPlane.openTeleportScreen(f.player, PlaneKind.BACK);
            assertEquals(3, f.stack.getCount());
            assertFalse(TeleportPayments.pending(f.id));
            assertFalse(nativeMap("pendingAdditions").containsKey(f.id));
        } finally {
            java.nio.file.Files.writeString(directory.resolve("paper_plane.toml"), "enableTpa = true\nenableBack = true\n");
            PaperPlane.configure(directory);
        }
    }

    @SuppressWarnings("unchecked") static Map<UUID,Object> nativeMap(String name) throws Exception {
        var field = WarmupCooldownTeleporter.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Map<UUID,Object>) field.get(null);
    }

    private static final class Fixture implements AutoCloseable {
        final UUID id = UUID.randomUUID();
        final ServerPlayer player = mock(ServerPlayer.class);
        final MinecraftServer server = mock(MinecraftServer.class);
        final TeleportPos destination = mock(TeleportPos.class);
        final FTBEPlayerData data = mock(FTBEPlayerData.class);
        final WarmupCooldownTeleporter teleporter;
        final ItemStack stack;
        final org.mockito.MockedStatic<FTBEPlayerData> dataBoundary;

        Fixture(PlaneKind kind) throws Exception {
            teleporter = new WarmupCooldownTeleporter(data, p -> 47, p -> 9, kind.returning());
            when(player.getUUID()).thenReturn(id);
            when(player.position()).thenReturn(Vec3.ZERO);
            when(player.blockPosition()).thenReturn(BlockPos.ZERO);
            when(player.getAbilities()).thenReturn(new Abilities());
            var level = mock(ServerLevel.class);
            when(level.dimension()).thenReturn(Level.OVERWORLD);
            when(player.level()).thenReturn(level);
            var inventory = mock(Inventory.class);
            when(player.getInventory()).thenReturn(inventory);
            when(inventory.getContainerSize()).thenReturn(1);
            stack = new ItemStack(switch (kind) {
                case NORMAL -> PaperPlaneRegistries.PAPER_PLANE.get();
                case SOGGY -> PaperPlaneRegistries.SOGGY_PAPER_PLANE.get();
                case ENDER -> PaperPlaneRegistries.ENDER_PAPER_PLANE.get();
                case BACK -> PaperPlaneRegistries.BACK_PAPER_PLANE.get();
                case ENDER_BACK -> PaperPlaneRegistries.ENDER_BACK_PAPER_PLANE.get();
            }, 3);
            when(inventory.getItem(0)).thenReturn(stack);
            when(inventory.add(any())).thenAnswer(call -> { var refund = (ItemStack)call.getArgument(0); stack.grow(refund.getCount()); refund.setCount(0); return true; });
            var players = mock(PlayerList.class);
            when(server.getPlayerList()).thenReturn(players);
            when(players.getPlayer(id)).thenReturn(player);
            when(server.overworld()).thenReturn(level);
            var field = ServerPlayer.class.getField("server"); field.setAccessible(true); field.set(player, server);
            when(destination.safeForPlayer(player)).thenReturn(destination);
            when(destination.getPos()).thenReturn(BlockPos.ZERO);
            when(destination.checkDimensionBlacklist(player)).thenReturn(TeleportResult.SUCCESS);
            when(destination.teleport(player)).thenReturn(TeleportResult.SUCCESS);
            field = FTBEPlayerData.class.getField(kind.returning() ? "backTeleporter" : "tpaTeleporter"); field.setAccessible(true); field.set(data, teleporter);
            field = FTBEPlayerData.class.getField("teleportHistory"); field.setAccessible(true); field.set(data, new LinkedList<>(List.of(destination)));
            dataBoundary = mockStatic(FTBEPlayerData.class);
            dataBoundary.when(() -> FTBEPlayerData.getOrCreate(player)).thenReturn(Optional.of(data));
        }

        void finishWarmup() throws Exception {
            var additions = nativeMap("pendingAdditions");
            var warmup = additions.get(id);
            assertNotNull(warmup, "Native Back must queue a warmup");
            var components = warmup.getClass().getRecordComponents();
            var types = Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new);
            var values = new Object[components.length];
            for (int i = 0; i < components.length; i++) {
                var accessor = components[i].getAccessor(); accessor.setAccessible(true);
                values[i] = components[i].getName().equals("when") ? 0L : accessor.invoke(warmup);
            }
            var constructor = warmup.getClass().getDeclaredConstructor(types); constructor.setAccessible(true);
            additions.put(id, constructor.newInstance(values));
            WarmupCooldownTeleporter.tickWarmups(server);
        }

        public void close() throws Exception {
            PaperPlane.clearPlayer(player);
            nativeMap("pendingAdditions").remove(id);
            nativeMap("WARMUPS").remove(id);
            var field = WarmupCooldownTeleporter.class.getDeclaredField("pendingRemovals"); field.setAccessible(true); ((Set<?>) field.get(null)).remove(id);
            dataBoundary.close();
        }
    }
}
