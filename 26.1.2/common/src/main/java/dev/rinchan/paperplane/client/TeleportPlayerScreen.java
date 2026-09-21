package dev.rinchan.paperplane.client;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.client.gui.theme.Theme;
import dev.ftb.mods.ftblibrary.client.gui.widget.BaseScreen;
import dev.ftb.mods.ftblibrary.client.gui.widget.SimpleTextButton;
import dev.rinchan.paperplane.PaperPlaneNetworking;
import dev.rinchan.paperplane.PlayerEntry;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class TeleportPlayerScreen extends BaseScreen {
    private final UUID sessionId;
    private final List<PlayerEntry> players;
    private final boolean enderPlane;

    public TeleportPlayerScreen(UUID sessionId, List<PlayerEntry> players, boolean enderPlane) {
        this.sessionId = sessionId;
        this.players = players;
        this.enderPlane = enderPlane;
        setSize(240, Math.min(210, 42 + Math.max(1, players.size()) * 24));
    }

    @Override
    public void addWidgets() {
        if (players.isEmpty()) {
            SimpleTextButton empty = SimpleTextButton.create(this, Component.translatable("screen.paper_plane.no_players"), Icon.empty(), mouseButton -> closeGui(false));
            empty.setPosAndSize(12, 32, width - 24, 20);
            add(empty);
            return;
        }

        int y = 32;
        for (PlayerEntry player : players) {
            SimpleTextButton button = SimpleTextButton.create(this, Component.literal(player.name()), Icon.empty(), mouseButton -> {
                PaperPlaneNetworking.sendTeleportRequest(sessionId, player.id());
                closeGui(false);
            });
            button.setPosAndSize(12, y, width - 24, 20);
            add(button);
            y += 24;
        }
    }

    @Override
    public void alignWidgets() {
        setPos((getScreen().getGuiScaledWidth() - width) / 2, (getScreen().getGuiScaledHeight() - height) / 2);
    }

    @Override
    public void drawBackground(GuiGraphicsExtractor graphics, Theme theme, int x, int y, int w, int h) {
        super.drawBackground(graphics, theme, x, y, w, h);
        theme.drawGui(graphics, x, y, w, h, getWidgetType());
        theme.drawString(graphics, Component.translatable(enderPlane ? "screen.paper_plane.title_ender" : "screen.paper_plane.title"), x + 12, y + 12);
    }
}
