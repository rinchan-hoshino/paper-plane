package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.integration.PermissionsHelper;
import dev.ftb.mods.ftbessentials.integration.PermissionsProvider;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.WarmupCooldownTeleporter;
import dev.ftb.mods.ftblibrary.snbt.config.IntValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NativeTeleportSettingsTest {
    @BeforeAll static void bootstrap() {
        net.neoforged.fml.loading.LoadingModList.of(java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @ParameterizedTest @ValueSource(strings = {"tpa", "back"})
    void nativeTeleporterReadsConfiguredCooldownWarmupAndPermissionOverride(String ability) throws Exception {
        var group = (SNBTConfig) FTBEConfig.TELEPORTATION.get().stream().filter(v -> v.getKey().equals(ability)).findFirst().orElseThrow();
        var cooldown = (IntValue) group.get().stream().filter(v -> v.getKey().equals("cooldown")).findFirst().orElseThrow();
        var warmup = (IntValue) group.get().stream().filter(v -> v.getKey().equals("warmup")).findFirst().orElseThrow();
        var previousCooldown = cooldown.get();
        var previousWarmup = warmup.get();
        var player = mock(ServerPlayer.class);
        try (var permissions = mockStatic(PermissionsHelper.class)) {
            permissions.when(PermissionsHelper::getInstance).thenReturn(new PermissionsProvider() {});
            cooldown.set(47);
            warmup.set(9);
            var data = new FTBEPlayerData(UUID.randomUUID(), "settings-test");
            var teleporter = ability.equals("tpa") ? data.tpaTeleporter : data.backTeleporter;
            var setting = ability.equals("tpa") ? FTBEConfig.TPA : FTBEConfig.BACK;
            assertEquals(47, setting.getCooldown(player));
            assertEquals(9, setting.getWarmup(player));
            var lastRun = WarmupCooldownTeleporter.class.getDeclaredField("lastRun");
            lastRun.setAccessible(true);
            lastRun.setLong(teleporter, System.currentTimeMillis());
            assertFalse(teleporter.checkCooldown(player).isSuccess(), "The native non-zero cooldown must block reuse");
            // Runtime changes are read through FTB, not cached by a Paper Plane timer.
            cooldown.set(0);
            assertTrue(teleporter.checkCooldown(player).isSuccess());
            cooldown.set(47);
            permissions.when(PermissionsHelper::getInstance).thenReturn(new PermissionsProvider() {
                @Override public int getInt(ServerPlayer p, int fallback, String node) {
                    return node.endsWith(".cooldown") ? 0 : fallback;
                }
            });
            assertTrue(teleporter.checkCooldown(player).isSuccess(), "FTB permission overrides retain authority");
        } finally {
            cooldown.set(previousCooldown);
            warmup.set(previousWarmup);
        }
    }
}
