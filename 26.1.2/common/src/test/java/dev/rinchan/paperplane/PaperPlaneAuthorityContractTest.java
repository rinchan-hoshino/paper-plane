package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PaperPlaneAuthorityContractTest {
    private static Path source(String relative) {
        for (String local : new String[]{"common/src/main/java", "../common/src/main/java", "neoforge/src/main/java", "../neoforge/src/main/java", "fabric/src/main/java", "../fabric/src/main/java"}) {
            Path candidate = Path.of(local).resolve(relative);
            if (Files.isRegularFile(candidate)) return candidate;
        }
        for (String sharedDir : new String[]{"shared/modern", "shared/core"}) {
            for (String prefix : new String[]{"../", "../../"}) {
                Path candidate = Path.of(prefix + sharedDir).resolve(relative);
                if (Files.isRegularFile(candidate)) return candidate;
            }
        }
        return Path.of("../common/src/main/java").resolve(relative).normalize();
    }

    private static Path resource(String relative) {
        for (String local : new String[]{"common/src/main/resources", "../common/src/main/resources"}) {
            Path candidate = Path.of(local).resolve(relative);
            if (Files.isRegularFile(candidate)) return candidate;
        }
        for (String sharedDir : new String[]{"shared/resources", "shared/resources-modern"}) {
            for (String prefix : new String[]{"../", "../../"}) {
                Path candidate = Path.of(prefix + sharedDir).resolve(relative);
                if (Files.isRegularFile(candidate)) return candidate;
            }
        }
        return Path.of("../common/src/main/resources").resolve(relative).normalize();
    }

    @Test
    void clientCannotDeclarePlaneKindAndServerOwnsTeleportResponses() throws Exception {
        String requestPacket = Files.readString(source("dev/rinchan/paperplane/RequestTeleportPacket.java"));
        String client = Files.readString(source("dev/rinchan/paperplane/client/PaperPlaneClient.java"));
        assertFalse(requestPacket.contains("boolean enderPlane"));
        assertTrue(requestPacket.contains("UUID sessionId"));
        assertTrue(client.contains("TeleportResponseCommand.parse(command)"));
        assertFalse(client.contains("TRACKED_REQUESTS"));
    }

    @Test
    void paymentWrapsFtbAcceptanceAndNetworkFailuresAreLogged() throws Exception {
        String mixin = Files.readString(source("dev/rinchan/paperplane/mixin/TPACommandMixin.java"));
        String platform = Files.readString(source("dev/rinchan/paperplane/neoforge/PaperPlaneNeoForge.java"));
        assertTrue(mixin.contains("@WrapMethod(method = \"tpaccept\")"));
        assertTrue(mixin.contains("acceptTeleportRequest"));
        assertFalse(platform.contains(".optional()"));
        assertTrue(platform.contains("LOGGER.error"));
        assertFalse(platform.contains("exceptionally(throwable -> null)"));
    }

    @Test
    void soggyVariantIsRemovedAndRemappedLosslessly() throws Exception {
        String mixins = Files.readString(resource("paper_plane.mixins.json"));
        assertFalse(mixins.contains("ItemEntityMixin"));
        assertFalse(Files.exists(resource("data/paper_plane/recipe/soggy_paper_plane.json")));
        assertTrue(Files.readString(source("dev/rinchan/paperplane/registry/PaperPlaneRegistries.java")).contains("addAlias"));
    }

    @Test
    void generatedItemModelsReferenceOwnTextures() throws Exception {
        for (String item : new String[]{"paper_plane", "ender_paper_plane", "back_paper_plane", "ender_back_paper_plane"}) {
            String model = Files.readString(resource("assets/paper_plane/models/item/" + item + ".json"));
            assertTrue(model.contains("\"parent\": \"minecraft:item/generated\""));
            assertTrue(model.contains("\"layer0\": \"paper_plane:item/" + item + "\""));
            assertFalse(model.contains("_edge"));
        }
    }
}
