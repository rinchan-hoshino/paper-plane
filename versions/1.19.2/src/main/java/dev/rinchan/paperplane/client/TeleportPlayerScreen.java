package dev.rinchan.paperplane.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.WidgetType;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.rinchan.paperplane.PaperPlaneNetworking;
import dev.rinchan.paperplane.PlayerEntry;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

public final class TeleportPlayerScreen extends BaseScreen {
    private static final int WIDTH = 220;
    private static final int ROW_HEIGHT = 22;
    private final UUID sessionId;
    private final List<PlayerEntry> players;
    private final boolean enderPlane;
    private boolean sent;

    public TeleportPlayerScreen(UUID sessionId, List<PlayerEntry> players, boolean enderPlane) {
        this.sessionId = sessionId;
        this.players = List.copyOf(players);
        this.enderPlane = enderPlane;
        setSize(WIDTH, Math.max(62, 36 + players.size() * ROW_HEIGHT));
    }

    @Override
    public void addWidgets() {
        for (int i = 0; i < players.size(); i++) {
            PlayerEntry player = players.get(i);
            SimpleTextButton button = new SimpleTextButton(
                this,
                Component.literal(player.name()),
                enderPlane ? ItemIcon.getItemIcon(Items.ENDER_PEARL) : Icons.CHECK
            ) {
                @Override
                public void onClicked(MouseButton mouseButton) {
                    select(player.id());
                }
            };
            button.setPosAndSize(10, 28 + i * ROW_HEIGHT, WIDTH - 20, 18);
            add(button);
        }
    }

    @Override
    public void alignWidgets() {
        setPos((getScreen().getGuiScaledWidth() - width) / 2, (getScreen().getGuiScaledHeight() - height) / 2);
    }

    @Override
    public void drawBackground(PoseStack poseStack, Theme theme, int x, int y, int width, int height) {
        theme.drawGui(poseStack, x, y, width, height, WidgetType.NORMAL);
        theme.drawString(poseStack, Component.translatable("screen.paper_plane.choose_player"), x + 10, y + 9);
        if (players.isEmpty()) {
            theme.drawString(poseStack, Component.translatable("screen.paper_plane.no_players"), x + 10, y + 32);
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
}
