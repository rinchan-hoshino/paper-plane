package dev.rinchan.paperplane.e2e;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod(PaperPlaneE2eMod.MOD_ID)
public final class PaperPlaneE2eMod {
    public static final String MOD_ID = "paper_plane_e2e";

    public PaperPlaneE2eMod(IEventBus modBus) {
        modBus.addListener(PaperPlaneE2eMod::onRegisterGameTests);
    }

    private static void onRegisterGameTests(RegisterGameTestsEvent event) {
        event.register(PaperPlaneE2eGameTests.class);
    }
}
