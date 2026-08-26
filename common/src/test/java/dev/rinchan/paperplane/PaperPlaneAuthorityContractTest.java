package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PaperPlaneAuthorityContractTest {
    private static Path source(String relative) {
        Path direct = Path.of("common/src/main/java").resolve(relative);
        return Files.isRegularFile(direct) ? direct : Path.of("../common/src/main/java").resolve(relative).normalize();
    }

    @Test
    void clientCannotDeclarePlaneKindOrAnswerUntrackedCommands() throws Exception {
        String requestPacket = Files.readString(source("dev/rinchan/paperplane/RequestTeleportPacket.java"));
        String client = Files.readString(source("dev/rinchan/paperplane/client/PaperPlaneClient.java"));
        assertFalse(requestPacket.contains("boolean enderPlane"));
        assertTrue(requestPacket.contains("UUID sessionId"));
        assertTrue(client.contains("TRACKED_REQUESTS.contains(id)"));
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
        int paymentBranch = owner.indexOf("ItemStack payment = ItemStack.EMPTY");
        assertTrue(owner.indexOf("takePlane(requester", paymentBranch) < owner.indexOf("operation.getAsInt()", paymentBranch));
        assertTrue(owner.contains("refund(requester, payment)"));
    }

    @Test
    void chargingUsesMouthPoseAndWorldEntityIsNotCameraBillboarded() throws Exception {
        String item = Files.readString(source("dev/rinchan/paperplane/item/PaperPlaneItem.java"));
        String renderer = Files.readString(source("dev/rinchan/paperplane/client/PaperPlaneEntityRenderer.java"));
        assertTrue(item.contains("UseAnim.TOOT_HORN"));
        assertTrue(item.contains("PaperPlaneFlightModel.launchSpeed"));
        assertTrue(renderer.contains("plane.getYRot()"));
        assertFalse(renderer.contains("cameraOrientation"));
    }
}
