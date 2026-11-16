package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PaperPlaneAuthorityContractTest {
    private static Path source(String relative) {
        for (String local : new String[]{"common/src/main/java", "../common/src/main/java", "neoforge/src/main/java", "../neoforge/src/main/java"}) {
            Path candidate = Path.of(local).resolve(relative);
            if (Files.isRegularFile(candidate)) return candidate;
        }
        for (String sharedDir : new String[]{
            "shared/modern", "shared/java", "shared/java-1.20.4-plus", "shared/java-1.20.6-plus",
            "shared/java-1.20.4-1.21.1", "shared/java-1.20.6-1.21.1",
            "shared/neoforge-1.20.6-plus", "shared/neoforge-1.20.6-1.21.1"
        }) {
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
        String owner = Files.readString(source("dev/rinchan/paperplane/PaperPlane.java"));
        String platform = Files.readString(source("dev/rinchan/paperplane/neoforge/PaperPlaneNeoForge.java"));
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
        String owner = Files.readString(source("dev/rinchan/paperplane/PaperPlane.java"));

    }

    @Test
    void droppedPlanesOwnTheirSingleWaterTransition() throws Exception {
        String wetting = Files.readString(source("dev/rinchan/paperplane/mixin/ItemEntityMixin.java"));
        String mixins = Files.readString(resource("paper_plane.mixins.json"));
        assertTrue(wetting.contains("stack.is(PaperPlaneRegistries.PAPER_PLANE.get())"));
        assertTrue(wetting.contains("SOGGY_PAPER_PLANE"));
        assertTrue(mixins.contains("ItemEntityMixin"));
        assertFalse(Files.exists(resource("data/paper_plane/recipe/soggy_paper_plane.json")));
    }

    @Test
    void worldEntityHeadingAndHandTransformsRemainUnchanged() throws Exception {
        String renderer = Files.readString(source("dev/rinchan/paperplane/client/PaperPlaneEntityRenderer.java"));
        assertTrue(renderer.contains("plane.getYRot()"));
        assertFalse(renderer.contains("cameraOrientation"));
        String pose = Files.readString(source("dev/rinchan/paperplane/client/PaperPlaneClientItemExtensions.java"));
        assertTrue(pose.contains("side > 0.0F ? -225.0F : -45.0F"));
        String model = Files.readString(resource("assets/paper_plane/models/item/paper_plane.json"));
        assertTrue(model.contains("\"firstperson_righthand\": {\n      \"rotation\": [\n        0,\n        225"));
        assertTrue(model.contains("\"firstperson_lefthand\": {\n      \"rotation\": [\n        0,\n        45"));
    }
}
