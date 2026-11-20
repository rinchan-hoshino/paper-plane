package dev.rinchan.paperplane.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.rinchan.paperplane.client.PaperPlaneConfigScreen;

public final class PaperPlaneModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PaperPlaneConfigScreen::new;
    }
}
