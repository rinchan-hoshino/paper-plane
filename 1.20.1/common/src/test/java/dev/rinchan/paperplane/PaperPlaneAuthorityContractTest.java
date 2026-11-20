package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PaperPlaneAuthorityContractTest {
    private static final Path ROOT = Files.isRegularFile(Path.of("settings.gradle"))
        ? Path.of("")
        : Path.of("..").normalize();

    private static String source(String relative) throws Exception {
        Path direct = ROOT.resolve(relative);
        if (Files.isRegularFile(direct)) {
            return Files.readString(direct);
        }
        String resRel = relative.replaceFirst("^common/src/main/resources/", "");
        Path sharedRes = ROOT.resolve("../shared/resources").resolve(resRel).normalize();
        if (Files.isRegularFile(sharedRes)) {
            return Files.readString(sharedRes);
        }
        return Files.readString(direct);
    }

    @Test
    void requestPacketsKeepPlaneSelectionOnTheServer() throws Exception {
        String requestPacket = source("common/src/main/java/dev/rinchan/paperplane/RequestTeleportPacket.java");
        String client = source("common/src/main/java/dev/rinchan/paperplane/client/PaperPlaneClient.java");
        String clickMixin = source("common/src/main/java/dev/rinchan/paperplane/mixin/ScreenMixin.java");
        assertFalse(requestPacket.contains("boolean enderPlane"));
        assertTrue(requestPacket.contains("UUID sessionId"));
        assertTrue(clickMixin.contains("handleComponentClicked"));
        assertTrue(clickMixin.contains("PaperPlaneClient.handleTeleportResponseCommand"));
        assertFalse(Files.exists(ROOT.resolve("common/src/main/java/dev/rinchan/paperplane/mixin/ClientPacketListenerMixin.java")));
    }

    @Test
    void serverOwnsFtbRequestsResponsesAndAtomicPayment() throws Exception {
        String mixin = source("common/src/main/java/dev/rinchan/paperplane/mixin/TPACommandMixin.java");
        assertTrue(mixin.contains("@WrapMethod(method = \"tpaccept\")"));
        assertTrue(mixin.contains("acceptTeleportRequest"));
    }

    @Test
    void soggyVariantIsRemovedAndRemappedLosslessly() throws Exception {
        String mixins = source("common/src/main/resources/paper_plane.mixins.json");
        assertFalse(mixins.contains("ItemEntityMixin"));
        assertFalse(Files.exists(ROOT.resolve("common/src/main/java/dev/rinchan/paperplane/mixin/ItemEntityMixin.java")));
        assertFalse(Files.exists(ROOT.resolve("common/src/main/java/dev/rinchan/paperplane/item/SoggyPaperPlaneItem.java")));
        assertTrue(source("fabric/src/main/java/dev/rinchan/paperplane/registry/PaperPlaneRegistries.java").contains("addAlias"));
        assertTrue(source("forge/src/main/java/dev/rinchan/paperplane/forge/PaperPlaneForge.java").contains("MissingMappingsEvent"));
    }

    @Test
    void generatedItemModelsReferenceOwnTextures() throws Exception {
        for (String item : new String[]{"paper_plane", "ender_paper_plane", "back_paper_plane", "ender_back_paper_plane"}) {
            String model = source("common/src/main/resources/assets/paper_plane/models/item/" + item + ".json");
            assertTrue(model.contains("\"parent\": \"minecraft:item/generated\""));
            assertTrue(model.contains("\"layer0\": \"paper_plane:item/" + item + "\""));
            assertFalse(model.contains("_edge"));
        }
    }
}
