package dev.rinchan.paperplane.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.mojang.blaze3d.platform.Window;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.PanelScrollBar;
import dev.ftb.mods.ftblibrary.ui.TextBox;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.rinchan.paperplane.PlayerEntry;
import dev.rinchan.paperplane.client.TeleportPlayerScreen;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Headless black-box E2E test for {@link TeleportPlayerScreen} loaded directly from the
 * packaged deliverable {@code paper_plane-neoforge-*.jar} alongside real FTB Library JAR.
 *
 * <p>Exercises full FTB Library {@code BaseScreen} lifecycle with 12 players:
 * {@code initGui} -> {@code addWidgets} -> {@code alignWidgets} -> mouse-wheel scrolling ->
 * live {@code TextBox} {@code updateFilter} -> empty filter -> restore, ensuring regressions
 * like {@code PanelScrollBar.setMaxValue} {@link UnsupportedOperationException} can never ship.
 */
final class TeleportPlayerScreenBlackBoxE2eTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), java.util.Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void teleportPlayerScreenLoadedFromPackagedJarHandles12PlayersInitFilterAndScroll() {
        URL location = TeleportPlayerScreen.class.getProtectionDomain().getCodeSource().getLocation();
        String locationStr = location == null ? "" : location.toString();
        assertTrue(
            locationStr.endsWith(".jar") || locationStr.contains(".jar!"),
            "Expected TeleportPlayerScreen to be loaded from packaged deliverable .jar, got: " + locationStr
        );

        List<PlayerEntry> twelvePlayers = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            twelvePlayers.add(new PlayerEntry(new UUID(0L, i), "Player" + i));
        }

        try (MockedStatic<Minecraft> mcStatic = mockMinecraftWindow(854, 480);
             MockedStatic<com.mojang.blaze3d.platform.InputConstants> inputStatic = mockStatic(com.mojang.blaze3d.platform.InputConstants.class)) {
            inputStatic.when(() -> com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt()
            )).thenReturn(false);
            TeleportPlayerScreen screen = new TeleportPlayerScreen(UUID.randomUUID(), twelvePlayers, false);
            screen.initGui();

            TextBox searchBox = findChild(screen, TextBox.class);
            Panel listPanel = findChild(screen, Panel.class);
            PanelScrollBar scrollBar = findChild(screen, PanelScrollBar.class);

            assertNotNull(searchBox, "TeleportPlayerScreen must add a TextBox");
            assertNotNull(listPanel, "TeleportPlayerScreen must add a PlayerListPanel");
            assertNotNull(scrollBar, "TeleportPlayerScreen must add a PanelScrollBar");

            // 12 rows * 22px = 264px > LIST_HEIGHT (154px) -> scrollbar MUST be active and positive
            assertEquals(12, listPanel.getWidgets().size(), "All 12 player buttons must be populated");
            assertTrue(scrollBar.getMaxValue() > 0.0, "12 players must overflow listPanel and enable scrolling");
            assertTrue(scrollBar.shouldDraw(), "ScrollBar must be visible for 12 players");
            assertTrue(scrollBar.isEnabled(), "ScrollBar must be enabled for 12 players");

            // Simulate mouse over listPanel + mouse wheel scroll down
            int panelCenterX = listPanel.getX() + listPanel.getWidth() / 2;
            int panelCenterY = listPanel.getY() + listPanel.getHeight() / 2;
            screen.updateMouseOver(panelCenterX, panelCenterY);
            assertTrue(screen.mouseScrolled(-1.0), "Mouse wheel scroll down should be handled");
            double scrolledOnce = listPanel.getScrollY();
            assertTrue(scrolledOnce > 0.0, "Scrolling down must advance listPanel.scrollY, got " + scrolledOnce);

            // Scroll further down and verify clamping does not crash
            screen.mouseScrolled(-20.0);
            assertTrue(listPanel.getScrollY() >= scrolledOnce, "Further scroll down must stay >= first scroll");

            // Live search filter via TextBox.setText -> onTextChanged -> updateFilter
            searchBox.setText("Player1");
            assertEquals(4, listPanel.getWidgets().size(), "Query 'Player1' must match Player1, Player10, Player11, Player12");
            assertEquals(0.0, listPanel.getScrollY(), 0.001, "Filtering must reset listPanel.scrollY to 0");

            // Narrow to unique match
            searchBox.setText("player12");
            assertEquals(1, listPanel.getWidgets().size(), "Case-insensitive query 'player12' must match 1 player");
            assertFalse(scrollBar.shouldDraw(), "Single player should not require scrollbar");

            // Zero-match query must not crash on filter or scroll
            searchBox.setText("nonexistent_player_xyz");
            assertEquals(0, listPanel.getWidgets().size(), "Non-matching query must yield 0 buttons");
            screen.mouseScrolled(-1.0);

            // Clearing query restores all 12 players and scrolling
            searchBox.setText("");
            assertEquals(12, listPanel.getWidgets().size(), "Clearing query must restore all 12 players");
            assertTrue(scrollBar.getMaxValue() > 0.0, "ScrollBar must re-enable after restoring 12 players");
            screen.updateMouseOver(panelCenterX, panelCenterY);
            assertTrue(screen.mouseScrolled(-1.0), "Scrolling must still work after clearing filter");
            assertTrue(listPanel.getScrollY() > 0.0, "ScrollY must advance after clearing filter");

            // Ender Paper Plane variant and empty player list must also initialize cleanly
            TeleportPlayerScreen enderScreen = new TeleportPlayerScreen(UUID.randomUUID(), twelvePlayers, true);
            enderScreen.initGui();
            assertEquals(12, findChild(enderScreen, Panel.class).getWidgets().size());

            TeleportPlayerScreen emptyScreen = new TeleportPlayerScreen(UUID.randomUUID(), List.of(), false);
            emptyScreen.initGui();
            assertEquals(0, findChild(emptyScreen, Panel.class).getWidgets().size());
            assertFalse(findChild(emptyScreen, PanelScrollBar.class).shouldDraw());
        }
    }

    private static MockedStatic<Minecraft> mockMinecraftWindow(int guiWidth, int guiHeight) {
        Minecraft mc = mock(Minecraft.class);
        Window window = mock(Window.class);
        when(window.getGuiScaledWidth()).thenReturn(guiWidth);
        when(window.getGuiScaledHeight()).thenReturn(guiHeight);
        when(mc.getWindow()).thenReturn(window);

        net.minecraft.client.gui.Font font = mock(net.minecraft.client.gui.Font.class);
        when(font.width(org.mockito.ArgumentMatchers.anyString())).thenAnswer(inv -> {
            String s = inv.getArgument(0);
            return s == null ? 0 : s.length() * 6;
        });
        when(font.width(org.mockito.ArgumentMatchers.any(net.minecraft.network.chat.FormattedText.class))).thenAnswer(inv -> {
            net.minecraft.network.chat.FormattedText t = inv.getArgument(0);
            return t == null ? 0 : t.getString().length() * 6;
        });
        when(font.plainSubstrByWidth(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt()))
            .thenAnswer(inv -> inv.getArgument(0));
        when(font.plainSubstrByWidth(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyBoolean()))
            .thenAnswer(inv -> inv.getArgument(0));
        try {
            var fontField = Minecraft.class.getField("font");
            fontField.setAccessible(true);
            fontField.set(mc, font);
            var lineHeightField = net.minecraft.client.gui.Font.class.getField("lineHeight");
            lineHeightField.setAccessible(true);
            lineHeightField.setInt(font, 9);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set mock Minecraft.font", e);
        }

        MockedStatic<Minecraft> mocked = mockStatic(Minecraft.class);
        mocked.when(Minecraft::getInstance).thenReturn(mc);
        return mocked;
    }

    private static <T extends Widget> T findChild(Panel parent, Class<T> type) {
        for (Widget w : parent.getWidgets()) {
            if (type.isInstance(w)) {
                return assertInstanceOf(type, w);
            }
        }
        return null;
    }
}
