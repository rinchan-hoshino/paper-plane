package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.ftb.mods.ftbessentials.api.TeleportResult;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import dev.ftb.mods.ftbessentials.util.WarmupCooldownTeleporter;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NativeWarmupPaymentTest {
  @BeforeAll
  static void bootstrap() {
    net.neoforged.fml.loading.LoadingModList.of(
        java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), Map.of());
    net.minecraft.SharedConstants.tryDetectVersion();
    net.minecraft.server.Bootstrap.bootStrap();
    ((net.minecraft.core.MappedRegistry<net.minecraft.world.item.Item>) net.minecraft.core.registries.BuiltInRegistries.ITEM).unfreeze();
    for (String id : java.util.List.of("paper_plane", "soggy_paper_plane", "ender_paper_plane", "back_paper_plane", "ender_back_paper_plane")) {
      net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM,
          net.minecraft.resources.ResourceLocation.parse("paper_plane:" + id), new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties()));
    }
  }

  @Test
  void nativeWarmupAcceptanceKeepsThePaperRequestPendingUntilActualTeleport() throws Exception {
    var requesterId = UUID.fromString("00000000-0000-0000-0000-000000000011");
    var targetId = UUID.fromString("00000000-0000-0000-0000-000000000012");
    var requestId = UUID.fromString("00000000-0000-0000-0000-000000000013");
    var requester = mock(ServerPlayer.class);
    when(requester.getUUID()).thenReturn(requesterId);
    when(requester.position()).thenReturn(Vec3.ZERO);
    var inventory = mock(net.minecraft.world.entity.player.Inventory.class);
    when(requester.getInventory()).thenReturn(inventory);
    when(inventory.getContainerSize()).thenReturn(1);
    when(inventory.getItem(0)).thenReturn(new net.minecraft.world.item.ItemStack(dev.rinchan.paperplane.registry.PaperPlaneRegistries.ENDER_PAPER_PLANE.get()));
    var target = mock(ServerPlayer.class);
    when(target.getUUID()).thenReturn(targetId);
    var server = mock(MinecraftServer.class);
    var players = mock(PlayerList.class);
    when(server.getPlayerList()).thenReturn(players);
    when(players.getPlayer(requesterId)).thenReturn(requester);
    var serverField = ServerPlayer.class.getField("server");
    serverField.setAccessible(true);
    serverField.set(target, server);
    var nativeData = mock(FTBEPlayerData.class);
    var nativeTeleporter = new WarmupCooldownTeleporter(nativeData, player -> 17, player -> 5);
    var teleporterField = FTBEPlayerData.class.getField("tpaTeleporter");
    teleporterField.setAccessible(true);
    teleporterField.set(nativeData, nativeTeleporter);
    var destination = mock(TeleportPos.class);
    when(destination.checkDimensionBlacklist(requester)).thenReturn(TeleportResult.SUCCESS);
    when(destination.getPos()).thenReturn(BlockPos.ZERO);

    var requestField = PaperPlane.class.getDeclaredField("REQUESTS");
    requestField.setAccessible(true);
    var requests = (TeleportRequestLedger) requestField.get(null);
    requests.add(new TeleportRequestLedger.Pending(requestId, requesterId, targetId, PlaneKind.ENDER));
    try (var data = mockStatic(FTBEPlayerData.class)) {
      data.when(() -> FTBEPlayerData.getOrCreate(requester)).thenReturn(java.util.Optional.of(nativeData));
      dev.ftb.mods.ftbessentials.commands.impl.teleporting.TPACommand.requests().put(requestId,
          mock(dev.ftb.mods.ftbessentials.api.records.TPARequest.class));
      assertEquals(0, PaperPlane.acceptTeleportRequest(target, requestId.toString(), () -> 0));
      assertTrue(requests.hasPending(requesterId),
          "Failed acceptance must retain payment ownership while FTB keeps the request live");
      int result = PaperPlane.acceptTeleportRequest(
          target, requestId.toString(),
          () -> nativeTeleporter.teleport(requester, player -> destination).runCommand(requester));
      assertEquals(1, result, "FTB accepts the five-second warmup before moving the player");
      var warmupsField = WarmupCooldownTeleporter.class.getDeclaredField("pendingAdditions");
      warmupsField.setAccessible(true);
      assertTrue(((Map<?, ?>) warmupsField.get(null)).containsKey(requesterId));
      assertTrue(requests.hasPending(requesterId),
          "A queued warmup must retain the request and prevent another plane use before completion");
      PaperPlane.clearPlayer(requester);
      assertFalse(dev.ftb.mods.ftbessentials.commands.impl.teleporting.TPACommand.requests().containsKey(requestId),
          "Logging out clears both native and Paper Plane request state");
    } finally {
      dev.ftb.mods.ftbessentials.commands.impl.teleporting.TPACommand.requests().remove(requestId);
      TeleportPayments.cancel(requesterId);
      requests.remove(requestId);
      for (String fieldName : java.util.List.of("pendingAdditions", "WARMUPS")) {
        var field = WarmupCooldownTeleporter.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).remove(requesterId);
      }
    }
  }
  @Test
  void backPlaneQueuesNativeBackWithoutOpeningAPlayerPicker() throws Exception {
    var player = mock(ServerPlayer.class);
    var id = UUID.randomUUID();
    when(player.getUUID()).thenReturn(id);
    when(player.position()).thenReturn(Vec3.ZERO);
    var inventory = mock(net.minecraft.world.entity.player.Inventory.class);
    when(player.getInventory()).thenReturn(inventory);
    when(inventory.getContainerSize()).thenReturn(1);
    var plane = new net.minecraft.world.item.ItemStack(dev.rinchan.paperplane.registry.PaperPlaneRegistries.BACK_PAPER_PLANE.get(), 3);
    when(inventory.getItem(0)).thenReturn(plane);
    var server = mock(MinecraftServer.class);
    when(server.getPlayerList()).thenReturn(mock(PlayerList.class));
    when(server.overworld()).thenReturn(mock(net.minecraft.server.level.ServerLevel.class));
    var serverField = ServerPlayer.class.getField("server");
    serverField.setAccessible(true);
    serverField.set(player, server);
    var nativeData = mock(FTBEPlayerData.class);
    var nativeBack = new WarmupCooldownTeleporter(nativeData, p -> 23, p -> 5);
    var teleporterField = FTBEPlayerData.class.getField("backTeleporter");
    teleporterField.setAccessible(true);
    teleporterField.set(nativeData, nativeBack);
    var destination = mock(TeleportPos.class);
    when(destination.safeForPlayer(player)).thenReturn(destination);
    when(destination.checkDimensionBlacklist(player)).thenReturn(TeleportResult.SUCCESS);
    when(destination.getPos()).thenReturn(BlockPos.ZERO);
    var history = FTBEPlayerData.class.getField("teleportHistory");
    history.setAccessible(true);
    history.set(nativeData, new java.util.LinkedList<>(java.util.List.of(destination)));
    var nativeMethod = dev.ftb.mods.ftbessentials.commands.groups.TeleportingCommands.class.getDeclaredMethod("back", ServerPlayer.class);
    nativeMethod.setAccessible(true);
    try (var data = mockStatic(FTBEPlayerData.class);
         var bridge = mockStatic(FtbBack.class);
         var network = mockStatic(PaperPlaneNetworking.class)) {
      data.when(() -> FTBEPlayerData.getOrCreate(player)).thenReturn(java.util.Optional.of(nativeData));
      bridge.when(() -> FtbBack.run(player)).thenAnswer(call -> nativeMethod.invoke(null, player));
      PaperPlane.openTeleportScreen(player, PlaneKind.BACK);
      var additions = WarmupCooldownTeleporter.class.getDeclaredField("pendingAdditions");
      additions.setAccessible(true);
      assertTrue(((Map<?, ?>) additions.get(null)).containsKey(id), "Back must use the native Back warmup and history");
      assertEquals(3, plane.getCount(), "Queuing Back must not consume a plane");
      network.verifyNoInteractions();
    } finally {
      PaperPlane.clearPlayer(player);
      var additions = WarmupCooldownTeleporter.class.getDeclaredField("pendingAdditions");
      additions.setAccessible(true);
      ((Map<?, ?>) additions.get(null)).remove(id);
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.EnumSource(PlaneKind.class)
  void onlyActualSuccessfulNativeMovementConsumesTheSelectedPlane(PlaneKind kind) {
    var player = mock(ServerPlayer.class);
    when(player.getUUID()).thenReturn(UUID.randomUUID());
    when(player.getAbilities()).thenReturn(new net.minecraft.world.entity.player.Abilities());
    var inventory = mock(net.minecraft.world.entity.player.Inventory.class);
    when(player.getInventory()).thenReturn(inventory);
    when(inventory.getContainerSize()).thenReturn(1);
    var item = switch (kind) {
      case NORMAL -> dev.rinchan.paperplane.registry.PaperPlaneRegistries.PAPER_PLANE.get();
      case SOGGY -> dev.rinchan.paperplane.registry.PaperPlaneRegistries.SOGGY_PAPER_PLANE.get();
      case ENDER -> dev.rinchan.paperplane.registry.PaperPlaneRegistries.ENDER_PAPER_PLANE.get();
      case BACK -> dev.rinchan.paperplane.registry.PaperPlaneRegistries.BACK_PAPER_PLANE.get();
      case ENDER_BACK -> dev.rinchan.paperplane.registry.PaperPlaneRegistries.ENDER_BACK_PAPER_PLANE.get();
    };
    var stack = new net.minecraft.world.item.ItemStack(item, 3);
    when(inventory.getItem(0)).thenReturn(stack);
    var teleporter = new Object();
    TeleportPayments.begin(player, kind, teleporter);
    assertEquals(3, stack.getCount());
    // A different native teleporter must not spend this payment.
    assertEquals(1, TeleportPayments.teleport(player, new Object(), () -> 1, n -> n > 0, () -> 0));
    assertEquals(3, stack.getCount());
    assertTrue(TeleportPayments.pending(player.getUUID()));
    // Model the actual native completion result at the payment boundary.
    assertEquals(1, TeleportPayments.teleport(player, teleporter, () -> 1, n -> n > 0, () -> 0));
    assertEquals(kind.consumable() ? 2 : 3, stack.getCount());
    assertFalse(TeleportPayments.pending(player.getUUID()));
  }

  @Test
  void failedNativeMovementRefundsAndInterruptedWarmupChargesNothing() {
    var player = mock(ServerPlayer.class);
    when(player.getUUID()).thenReturn(UUID.randomUUID());
    when(player.getAbilities()).thenReturn(new net.minecraft.world.entity.player.Abilities());
    var inventory = mock(net.minecraft.world.entity.player.Inventory.class);
    when(player.getInventory()).thenReturn(inventory);
    when(inventory.getContainerSize()).thenReturn(1);
    var stack = new net.minecraft.world.item.ItemStack(dev.rinchan.paperplane.registry.PaperPlaneRegistries.BACK_PAPER_PLANE.get(), 3);
    when(inventory.getItem(0)).thenReturn(stack);
    // Inventory.add mutates the destination stack in a real inventory.
    when(inventory.add(any())).thenAnswer(call -> {
      var refund = (net.minecraft.world.item.ItemStack) call.getArgument(0);
      stack.grow(refund.getCount());
      refund.setCount(0);
      return true;
    });
    var teleporter = new Object();
    TeleportPayments.begin(player, PlaneKind.BACK, teleporter);
    assertEquals(0, TeleportPayments.teleport(player, teleporter, () -> 0, n -> n > 0, () -> 0));
    assertEquals(3, stack.getCount());
    assertFalse(TeleportPayments.pending(player.getUUID()));
    TeleportPayments.begin(player, PlaneKind.BACK, teleporter);
    TeleportPayments.reconcile(java.util.Set.of(), java.util.Set.of(player.getUUID()));
    assertTrue(TeleportPayments.pending(player.getUUID()));
    TeleportPayments.reconcile(java.util.Set.of(), java.util.Set.of());
    assertEquals(3, stack.getCount());
    assertFalse(TeleportPayments.pending(player.getUUID()));
  }

  @Test
  void unrelatedNativeWarmupCancellationCannotEraseAnUnacceptedPlaneRequest() throws Exception {
    var requester = UUID.randomUUID();
    var request = UUID.randomUUID();
    var field = PaperPlane.class.getDeclaredField("REQUESTS");
    field.setAccessible(true);
    var requests = (TeleportRequestLedger) field.get(null);
    requests.add(new TeleportRequestLedger.Pending(request, requester, UUID.randomUUID(), PlaneKind.ENDER));
    try {
      TeleportPayments.cancel(requester);
      assertTrue(requests.hasPending(requester), "Canceling another FTB ability must preserve an unaccepted paper-plane request");
    } finally {
      requests.remove(request);
    }
  }

}
