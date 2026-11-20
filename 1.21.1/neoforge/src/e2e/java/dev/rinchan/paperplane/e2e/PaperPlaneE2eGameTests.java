package dev.rinchan.paperplane.e2e;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbessentials.api.records.TPARequest;
import dev.ftb.mods.ftbessentials.commands.impl.teleporting.TPACommand;
import dev.ftb.mods.ftbessentials.util.WarmupCooldownTeleporter;
import dev.rinchan.paperplane.OpenTeleportScreenPacket;
import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.TeleportResponseCommand;
import io.netty.channel.embedded.EmbeddedChannel;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Real Minecraft 1.21.1 NeoForge packaged-JAR black-box GameTests.
 *
 * <p>Loads the actual built {@code paper_plane-neoforge-*.jar} from {@code mods/} alongside
 * real FTB Library and FTB Essentials JARs, verifying Mixin application, dual-player item right-click,
 * server session creation, {@code /tpaccept} command & response routing, warmup/teleport completion,
 * single-use item deduction, reusable ender planes, and Back Paper Plane return.
 */
@GameTestHolder(PaperPlaneE2eMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PaperPlaneE2eGameTests {
    private PaperPlaneE2eGameTests() {}

    @GameTest(template = "empty", templateNamespace = PaperPlaneE2eMod.MOD_ID, timeoutTicks = 400)
    public static void packagedJarAndMixinsLoadedWithRealFtb(GameTestHelper helper) {
        // 1. Verify all real mods are loaded by FML
        if (!ModList.get().isLoaded("paper_plane")) {
            helper.fail("paper_plane mod is not loaded");
        }
        if (!ModList.get().isLoaded("ftblibrary")) {
            helper.fail("ftblibrary mod is not loaded");
        }
        if (!ModList.get().isLoaded("ftbessentials")) {
            helper.fail("ftbessentials mod is not loaded");
        }

        // 2. Prove paper_plane is loaded from the REAL packaged deliverable .jar (not loose build/classes)
        URL codeLocation = PaperPlane.class.getProtectionDomain().getCodeSource().getLocation();
        String locationStr = codeLocation == null ? "" : codeLocation.toString();
        if (!locationStr.contains(".jar")) {
            helper.fail("Expected PaperPlane to be loaded from packaged .jar, got: " + locationStr);
        }

        // 3. Prove Mixins from paper_plane.mixins.json actually transformed FTB Essentials classes
        boolean warmupTransformed = Arrays.stream(WarmupCooldownTeleporter.class.getDeclaredMethods())
            .anyMatch(m -> m.getName().contains("paperPlane$"));
        boolean tpaTransformed = Arrays.stream(TPACommand.class.getDeclaredMethods())
            .anyMatch(m -> m.getName().contains("paperPlane$"));
        if (!warmupTransformed || !tpaTransformed) {
            helper.fail(
                "Paper Plane Mixins did not apply to FTB Essentials (warmup="
                    + warmupTransformed + ", tpa=" + tpaTransformed + ")"
            );
        }

        // 4. Verify all 4 items are registered in BuiltInRegistries.ITEM
        for (String id : List.of("paper_plane", "ender_paper_plane", "back_paper_plane", "ender_back_paper_plane")) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("paper_plane", id);
            if (!BuiltInRegistries.ITEM.containsKey(rl)) {
                helper.fail("Missing registered item: " + rl);
            }
        }

        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = PaperPlaneE2eMod.MOD_ID, timeoutTicks = 400)
    public static void normalPaperPlaneTpaAndBackReturnJourney(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        BlockPos originPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos targetPos = helper.absolutePos(new BlockPos(25, 2, 25));
        ensureSafePlatform(level, originPos);
        ensureSafePlatform(level, targetPos);

        TestPlayer alice = spawnRealPlayer(server, level, "E2eAlice", originPos);
        TestPlayer bob = spawnRealPlayer(server, level, "E2eBob", targetPos);

        Item paperPlane = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("paper_plane", "paper_plane"));
        Item backPlane = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("paper_plane", "back_paper_plane"));

        alice.player.getInventory().setItem(0, new ItemStack(paperPlane, 2));
        alice.player.getInventory().setItem(1, new ItemStack(backPlane, 2));
        alice.player.getInventory().selected = 0;

        // Step 1: Alice right-clicks PaperPlaneItem -> server creates session & sends OpenTeleportScreenPacket
        InteractionResultHolder<ItemStack> useResult = alice.player.getMainHandItem()
            .use(level, alice.player, InteractionHand.MAIN_HAND);
        if (!useResult.getResult().consumesAction()) {
            cleanupPlayers(server, alice, bob);
            helper.fail("PaperPlaneItem.use did not succeed: " + useResult.getResult());
        }

        OpenTeleportScreenPacket screenPacket = alice.lastOpenScreenPacket()
            .orElseThrow(() -> new AssertionError("Server did not send OpenTeleportScreenPacket to Alice"));
        if (screenPacket.enderPlane()) {
            cleanupPlayers(server, alice, bob);
            helper.fail("Normal PaperPlane sent enderPlane=true");
        }
        boolean seesBob = screenPacket.players().stream().anyMatch(p -> p.id().equals(bob.player.getUUID()));
        if (!seesBob) {
            cleanupPlayers(server, alice, bob);
            helper.fail("OpenTeleportScreenPacket player list did not include Bob: " + screenPacket.players());
        }

        // Step 2: Client sends selection (RequestTeleportPacket -> PaperPlane.requestTeleport)
        PaperPlane.requestTeleport(alice.player, screenPacket.sessionId(), bob.player.getUUID());
        TPARequest ftbRequest = findFtbRequest(alice.player.getUUID(), bob.player.getUUID())
            .orElseThrow(() -> new AssertionError("FTB Essentials did not record TPA request for Alice -> Bob"));

        // Verify TeleportResponseCommand parses the exact FTB accept command format
        Optional<TeleportResponseCommand> parsedCmd = TeleportResponseCommand.parse("tpaccept " + ftbRequest.id());
        if (parsedCmd.isEmpty() || !parsedCmd.get().accept() || !parsedCmd.get().requestId().equals(ftbRequest.id())) {
            cleanupPlayers(server, alice, bob);
            helper.fail("TeleportResponseCommand failed to parse tpaccept command for " + ftbRequest.id());
        }

        // Step 3: Bob executes real server command /tpaccept <requestId>
        server.getCommands().performPrefixedCommand(
            bob.player.createCommandSourceStack(),
            "tpaccept " + ftbRequest.id()
        );

        // Step 4: Wait for warmup (if any) to complete, verify teleport + single item deduction, then use Back Paper Plane
        final int[] stage = {0};
        helper.onEachTick(() -> {
            if (stage[0] == 0) {
                double distToBob = alice.player.position().distanceTo(bob.player.position());
                if (distToBob < 3.0) {
                    int remainingPlanes = countItem(alice.player, paperPlane);
                    if (remainingPlanes != 1) {
                        cleanupPlayers(server, alice, bob);
                        helper.fail("Expected 1 remaining paper_plane after TPA, got " + remainingPlanes);
                    }
                    if (findFtbRequest(alice.player.getUUID(), bob.player.getUUID()).isPresent()) {
                        cleanupPlayers(server, alice, bob);
                        helper.fail("FTB TPA request was not cleared after completion");
                    }

                    // Step 5: Alice right-clicks Back Paper Plane to return to originPos
                    alice.player.getInventory().selected = 1;
                    alice.player.getMainHandItem().use(level, alice.player, InteractionHand.MAIN_HAND);
                    stage[0] = 1;
                }
            } else if (stage[0] == 1) {
                double distToOrigin = alice.player.position().distanceTo(originPos.getBottomCenter());
                if (distToOrigin < 3.0) {
                    int remainingBack = countItem(alice.player, backPlane);
                    cleanupPlayers(server, alice, bob);
                    if (remainingBack != 1) {
                        helper.fail("Expected 1 remaining back_paper_plane after return, got " + remainingBack);
                    }
                    helper.succeed();
                }
            }
        });
    }

    @GameTest(template = "empty", templateNamespace = PaperPlaneE2eMod.MOD_ID, timeoutTicks = 400)
    public static void enderPaperPlaneAndEnderBackPreserveItemCount(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        BlockPos originPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos targetPos = helper.absolutePos(new BlockPos(25, 2, 25));
        ensureSafePlatform(level, originPos);
        ensureSafePlatform(level, targetPos);

        TestPlayer charlie = spawnRealPlayer(server, level, "E2eCharlie", originPos);
        TestPlayer dana = spawnRealPlayer(server, level, "E2eDana", targetPos);

        Item enderPlane = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("paper_plane", "ender_paper_plane"));
        Item enderBack = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("paper_plane", "ender_back_paper_plane"));

        charlie.player.getInventory().setItem(0, new ItemStack(enderPlane, 1));
        charlie.player.getInventory().setItem(1, new ItemStack(enderBack, 1));
        charlie.player.getInventory().selected = 0;

        // Right-click Ender Paper Plane
        charlie.player.getMainHandItem().use(level, charlie.player, InteractionHand.MAIN_HAND);
        OpenTeleportScreenPacket screenPacket = charlie.lastOpenScreenPacket()
            .orElseThrow(() -> new AssertionError("Server did not send OpenTeleportScreenPacket for Ender Paper Plane"));
        if (!screenPacket.enderPlane()) {
            cleanupPlayers(server, charlie, dana);
            helper.fail("Ender PaperPlane sent enderPlane=false");
        }

        PaperPlane.requestTeleport(charlie.player, screenPacket.sessionId(), dana.player.getUUID());
        TPARequest ftbRequest = findFtbRequest(charlie.player.getUUID(), dana.player.getUUID())
            .orElseThrow(() -> new AssertionError("FTB Essentials did not record Ender TPA request"));

        // Accept via RespondTeleportPacket server entry
        PaperPlane.respondToTeleportRequest(dana.player, ftbRequest.id(), true);

        final int[] stage = {0};
        helper.onEachTick(() -> {
            if (stage[0] == 0) {
                if (charlie.player.position().distanceTo(dana.player.position()) < 3.0) {
                    int remainingEnder = countItem(charlie.player, enderPlane);
                    if (remainingEnder != 1) {
                        cleanupPlayers(server, charlie, dana);
                        helper.fail("Reusable ender_paper_plane must not be consumed, got " + remainingEnder);
                    }
                    // Use Ender Back Paper Plane
                    charlie.player.getInventory().selected = 1;
                    charlie.player.getMainHandItem().use(level, charlie.player, InteractionHand.MAIN_HAND);
                    stage[0] = 1;
                }
            } else if (stage[0] == 1) {
                if (charlie.player.position().distanceTo(originPos.getBottomCenter()) < 3.0) {
                    int remainingEnderBack = countItem(charlie.player, enderBack);
                    cleanupPlayers(server, charlie, dana);
                    if (remainingEnderBack != 1) {
                        helper.fail("Reusable ender_back_paper_plane must not be consumed, got " + remainingEnderBack);
                    }
                    helper.succeed();
                }
            }
        });
    }

    private static void ensureSafePlatform(ServerLevel level, BlockPos standPos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlockAndUpdate(standPos.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(standPos.offset(dx, 0, dz), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(standPos.offset(dx, 1, dz), Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static TestPlayer spawnRealPlayer(MinecraftServer server, ServerLevel level, String name, BlockPos pos) {
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        GameProfile profile = new GameProfile(uuid, name);
        CapturingConnection conn = new CapturingConnection();
        ServerPlayer player = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        server.getPlayerList().placeNewPlayer(conn, player, cookie);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        return new TestPlayer(player, conn);
    }

    private static void cleanupPlayers(MinecraftServer server, TestPlayer... players) {
        for (TestPlayer tp : players) {
            try {
                PaperPlane.clearPlayer(tp.player);
                server.getPlayerList().remove(tp.player);
            } catch (Exception ignored) {
            }
        }
    }

    private static Optional<TPARequest> findFtbRequest(UUID requesterId, UUID targetId) {
        return TPACommand.requests().values().stream()
            .filter(r -> r.source().getUuid().equals(requesterId) && r.target().getUuid().equals(targetId))
            .findFirst();
    }

    private static int countItem(ServerPlayer player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private record TestPlayer(ServerPlayer player, CapturingConnection connection) {
        Optional<OpenTeleportScreenPacket> lastOpenScreenPacket() {
            for (int i = connection.payloads.size() - 1; i >= 0; i--) {
                if (connection.payloads.get(i) instanceof OpenTeleportScreenPacket p) {
                    return Optional.of(p);
                }
            }
            return Optional.empty();
        }
    }

    private static final class CapturingConnection extends Connection {
        final List<CustomPacketPayload> payloads = new ArrayList<>();

        CapturingConnection() {
            super(PacketFlow.SERVERBOUND);
            EmbeddedChannel channel = new EmbeddedChannel();
            try {
                var channelField = Connection.class.getDeclaredField("channel");
                channelField.setAccessible(true);
                channelField.set(this, channel);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to initialize EmbeddedChannel on CapturingConnection", e);
            }
            net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(this);
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public void send(Packet<?> packet, @Nullable PacketSendListener listener, boolean flush) {
            if (packet instanceof ClientboundCustomPayloadPacket custom) {
                payloads.add(custom.payload());
            }
            if (listener != null) {
                listener.onSuccess();
            }
        }
    }
}
