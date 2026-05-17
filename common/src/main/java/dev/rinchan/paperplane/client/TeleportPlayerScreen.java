package dev.rinchan.paperplane.client;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.rinchan.paperplane.PaperPlaneNetworking;
import dev.rinchan.paperplane.PlayerEntry;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class TeleportPlayerScreen extends BaseScreen {
    private final List<PlayerEntry> players;
    private final boolean enderPlane;

    public TeleportPlayerScreen(List<PlayerEntry> players, boolean enderPlane) {
        this.players = players;
        this.enderPlane = enderPlane;
        setSize(240, Math.min(210, 42 + Math.max(1, players.size()) * 24));
    }

    @Override
    public void addWidgets() {
        if (players.isEmpty()) {
            SimpleButton empty = new SimpleButton(this, Component.translatable("screen.paper_plane.no_players"), Icon.empty(), (button, mouseButton) -> closeGui(false));
            empty.setPosAndSize(12, 32, width - 24, 20);
            add(empty);
            return;
        }

        int y = 32;
        for (PlayerEntry player : players) {
            SimpleButton button = new SimpleButton(this, Component.literal(player.name()), Icon.empty(), (b, mouseButton) -> {
                PaperPlaneNetworking.sendTeleportRequest(player::id, enderPlane);
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
    public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        super.drawBackground(graphics, theme, x, y, w, h);
        theme.drawGui(graphics, x, y, w, h, getWidgetType());
        theme.drawString(graphics, Component.translatable(enderPlane ? "screen.paper_plane.title_ender" : "screen.paper_plane.title"), x + 12, y + 12);
    }
}
