package dev.rinchan.paperplane.client;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftblibrary.icon.FaceIcon;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.PanelScrollBar;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.TextBox;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.WidgetType;
import dev.rinchan.paperplane.PaperPlaneNetworking;
import dev.rinchan.paperplane.PlayerEntry;
import dev.rinchan.paperplane.PlayerFilter;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class TeleportPlayerScreen extends BaseScreen {
    private static final int WIDTH = 240;
    private static final int HEIGHT = 214;
    private static final int PADDING = 10;
    private static final int SEARCH_Y = 26;
    private static final int SEARCH_HEIGHT = 18;
    private static final int LIST_Y = 50;
    private static final int LIST_HEIGHT = HEIGHT - LIST_Y - PADDING;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 2;

    private final UUID sessionId;
    private final List<PlayerEntry> allPlayers;
    private final boolean enderPlane;
    private List<PlayerEntry> filteredPlayers;
    private TextBox searchBox;
    private PlayerListPanel listPanel;
    private PanelScrollBar scrollBar;
    private boolean sent;

    public TeleportPlayerScreen(UUID sessionId, List<PlayerEntry> players, boolean enderPlane) {
        this.sessionId = sessionId;
        this.allPlayers = List.copyOf(players);
        this.filteredPlayers = this.allPlayers;
        this.enderPlane = enderPlane;
        setSize(WIDTH, HEIGHT);
    }

    @Override
    public void addWidgets() {
        searchBox = new TextBox(this) {
            @Override
            public void onTextChanged() {
                updateFilter(getText());
            }

            @Override
            public void onEnterPressed() {
                PlayerFilter.uniqueMatch(allPlayers, PlayerEntry::name, getText())
                    .ifPresent(match -> select(match.id()));
            }
        };
        searchBox.ghostText = Component.translatable("screen.paper_plane.search").getString();
        searchBox.setPosAndSize(PADDING, SEARCH_Y, WIDTH - PADDING * 2, SEARCH_HEIGHT);
        searchBox.setFocused(true);
        add(searchBox);

        listPanel = new PlayerListPanel(this);
        listPanel.setPosAndSize(PADDING, LIST_Y, WIDTH - PADDING * 2 - SCROLLBAR_WIDTH - 4, LIST_HEIGHT);
        add(listPanel);

        scrollBar = new PanelScrollBar(this, listPanel);
        scrollBar.setPosAndSize(WIDTH - PADDING - SCROLLBAR_WIDTH, LIST_Y, SCROLLBAR_WIDTH, LIST_HEIGHT);
        scrollBar.setCanAlwaysScroll(true);
        scrollBar.setScrollStep(ROW_HEIGHT + ROW_GAP);
        add(scrollBar);
    }

    @Override
    public void alignWidgets() {
        setPos((getScreen().getGuiScaledWidth() - width) / 2, (getScreen().getGuiScaledHeight() - height) / 2);
    }

    @Override
    public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        theme.drawGui(graphics, x, y, w, h, WidgetType.NORMAL);
        theme.drawString(
            graphics,
            Component.translatable(enderPlane ? "screen.paper_plane.title_ender" : "screen.paper_plane.title"),
            x + PADDING,
            y + 9
        );
    }

    private void updateFilter(String query) {
        filteredPlayers = PlayerFilter.filter(allPlayers, PlayerEntry::name, query);
        if (listPanel != null) {
            listPanel.setScrollY(0);
            listPanel.refreshWidgets();
        }
    }

    private void select(UUID targetId) {
        if (sent) {
            return;
        }
        sent = true;
        PaperPlaneNetworking.sendTeleportRequest(sessionId, targetId);
        closeGui(false);
    }

    private static Icon avatarIcon(PlayerEntry player) {
        try {
            return FaceIcon.getFace(new GameProfile(player.id(), player.name()));
        } catch (RuntimeException ignored) {
            return Icons.PLAYER;
        }
    }

    private final class PlayerListPanel extends Panel {
        PlayerListPanel(Panel parent) {
            super(parent);
            setOnlyRenderWidgetsInside(true);
            setOnlyInteractWithWidgetsInside(true);
            setScrollStep(ROW_HEIGHT + ROW_GAP);
        }

        @Override
        public void addWidgets() {
            int rowY = 2;
            for (PlayerEntry player : filteredPlayers) {
                SimpleTextButton button = SimpleTextButton.create(
                    this,
                    Component.literal(player.name()),
                    avatarIcon(player),
                    mouseButton -> select(player.id())
                );
                button.setPosAndSize(2, rowY, width - 4, ROW_HEIGHT);
                add(button);
                rowY += ROW_HEIGHT + ROW_GAP;
            }
        }

        @Override
        public void alignWidgets() {
        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            theme.drawPanelBackground(graphics, x, y, w, h);
            if (allPlayers.isEmpty()) {
                theme.drawString(graphics, Component.translatable("screen.paper_plane.no_players"), x + 6, y + 8);
            } else if (filteredPlayers.isEmpty()) {
                theme.drawString(graphics, Component.translatable("screen.paper_plane.no_matches"), x + 6, y + 8);
            }
        }
    }
}
