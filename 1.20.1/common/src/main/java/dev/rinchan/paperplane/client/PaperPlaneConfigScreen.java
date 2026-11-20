package dev.rinchan.paperplane.client;

import dev.rinchan.paperplane.PaperPlane;
import dev.rinchan.paperplane.PaperPlaneConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class PaperPlaneConfigScreen extends Screen {
    private final Screen parent;
    private boolean enableTpa;
    private boolean enableBack;

    public PaperPlaneConfigScreen(Screen parent) {
        super(Component.translatable("config.paper_plane.title"));
        this.parent = parent;
        PaperPlaneConfig current = PaperPlane.currentConfig();
        this.enableTpa = current.enableTpa();
        this.enableBack = current.enableBack();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int buttonWidth = 220;
        int startY = this.height / 2 - 44;

        addRenderableWidget(
            CycleButton.onOffBuilder(this.enableTpa)
                .create(
                    centerX - buttonWidth / 2,
                    startY,
                    buttonWidth,
                    20,
                    Component.translatable("config.paper_plane.enable_tpa"),
                    (button, value) -> this.enableTpa = value
                )
        );

        addRenderableWidget(
            CycleButton.onOffBuilder(this.enableBack)
                .create(
                    centerX - buttonWidth / 2,
                    startY + 26,
                    buttonWidth,
                    20,
                    Component.translatable("config.paper_plane.enable_back"),
                    (button, value) -> this.enableBack = value
                )
        );

        int footerY = startY + 76;
        addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE, button -> {
                PaperPlane.saveConfig(new PaperPlaneConfig(this.enableTpa, this.enableBack));
                onClose();
            }).bounds(centerX - 105, footerY, 100, 20).build()
        );
        addRenderableWidget(
            Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(centerX + 5, footerY, 100, 20)
                .build()
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 72, 0xFFFFFF);
        graphics.drawCenteredString(
            this.font,
            Component.translatable("config.paper_plane.ftb_note"),
            this.width / 2,
            this.height / 2 + 12,
            0xA0A0A0
        );
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
