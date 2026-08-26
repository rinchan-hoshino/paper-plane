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
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void clientCannotDeclarePlaneKindOrAnswerUntrackedCommands() throws Exception {
        String requestPacket = source("common/src/main/java/dev/rinchan/paperplane/RequestTeleportPacket.java");
        String client = source("common/src/main/java/dev/rinchan/paperplane/client/PaperPlaneClient.java");
        String clickMixin = source("common/src/main/java/dev/rinchan/paperplane/mixin/ScreenMixin.java");
        assertFalse(requestPacket.contains("boolean enderPlane"));
        assertTrue(requestPacket.contains("UUID sessionId"));
        assertTrue(client.contains("TRACKED_REQUESTS.contains(requestId)"));
        assertTrue(clickMixin.contains("handleComponentClicked"));
        assertTrue(clickMixin.contains("PaperPlaneClient.handleTeleportResponseCommand"));
        assertFalse(Files.exists(ROOT.resolve("common/src/main/java/dev/rinchan/paperplane/mixin/ClientPacketListenerMixin.java")));
    }

    @Test
    void serverOwnsFtbRequestsResponsesAndAtomicPayment() throws Exception {
        String mixin = source("common/src/main/java/dev/rinchan/paperplane/mixin/TPACommandMixin.java");
        String owner = source("common/src/main/java/dev/rinchan/paperplane/PaperPlane.java");
        String fabric = source("fabric/src/main/java/dev/rinchan/paperplane/PaperPlaneNetworking.java");
        String forge = source("forge/src/main/java/dev/rinchan/paperplane/PaperPlaneNetworking.java");

        assertTrue(mixin.contains("@WrapMethod(method = \"tpaccept\")"));
        assertTrue(mixin.contains("acceptTeleportRequest"));
        int paymentBranch = owner.indexOf("ItemStack payment = ItemStack.EMPTY");
        assertTrue(paymentBranch >= 0);
        assertTrue(owner.indexOf("takePlane(requester", paymentBranch) < owner.indexOf("operation.getAsInt()", paymentBranch));
        assertTrue(owner.contains("refund(requester, payment)"));
        assertTrue(owner.contains("TPACommands.tpa(requester, target, false)"));
        assertTrue(owner.contains("TPACommands.tpaccept(target, requestId)"));
        assertTrue(owner.contains("TPACommands.tpdeny(target, requestId)"));

        assertTrue(fabric.contains("PaperPlane.respondToTeleportRequest(player"));
        assertTrue(forge.contains("context.getSender()"));
        assertTrue(forge.contains("PaperPlane.respondToTeleportRequest(sender"));
        assertFalse(Files.exists(ROOT.resolve("common/src/main/java/dev/rinchan/paperplane/PaperPlaneCommands.java")));
    }

    @Test
    void droppedPlanesOwnTheirSingleWaterTransition() throws Exception {
        String wetting = source("common/src/main/java/dev/rinchan/paperplane/mixin/ItemEntityMixin.java");
        String mixins = source("common/src/main/resources/paper_plane.mixins.json");
        assertTrue(wetting.contains("stack.is(PaperPlaneRegistries.PAPER_PLANE.get())"));
        assertTrue(wetting.contains("SOGGY_PAPER_PLANE"));
        assertTrue(mixins.contains("ItemEntityMixin"));
        assertFalse(Files.exists(ROOT.resolve("common/src/main/resources/data/paper_plane/recipes/soggy_paper_plane.json")));
    }

    @Test
    void bothVersionsKeepMouthPoseAndVelocityOrientedWorldRendering() throws Exception {
        String item = source("common/src/main/java/dev/rinchan/paperplane/item/PaperPlaneItem.java");
        assertTrue(item.contains("UseAnim.TOOT_HORN"));
        assertTrue(item.contains("PaperPlaneFlightModel.launchSpeed"));

        for (String version : new String[] {"1.19.2", "1.20.1"}) {
            String renderer = source("versions/" + version + "/src/main/java/dev/rinchan/paperplane/client/PaperPlaneEntityRenderer.java");
            String pose = source("forge/src/" + version + "/java/dev/rinchan/paperplane/client/PaperPlaneClientItemExtensions.java");
            assertTrue(renderer.contains("plane.getYRot"));
            assertTrue(renderer.contains("plane.getXRot"));
            assertTrue(renderer.contains("plane.getItem()"));
            assertTrue(renderer.contains("TextureAtlas.LOCATION_BLOCKS"));
            assertFalse(renderer.contains("textures/entity/"));
            assertFalse(renderer.contains("cameraOrientation"));
            assertTrue(pose.contains("side > 0.0F ? -225.0F : -45.0F"));
            assertTrue(pose.contains("entity.getUsedItemHand() == hand"));
        }

        String model = source("common/src/main/resources/assets/paper_plane/models/item/paper_plane.json");
        assertTrue(model.contains("\"firstperson_righthand\": {\n      \"rotation\": [\n        0,\n        225"));
        assertTrue(model.contains("\"firstperson_lefthand\": {\n      \"rotation\": [\n        0,\n        45"));
    }

    @Test
    void loaderAndVersionSpecificOwnersAreCompleteWithoutFallbackCopies() {
        for (String version : new String[] {"1.19.2", "1.20.1"}) {
            assertTrue(Files.isRegularFile(ROOT.resolve("fabric/src/" + version + "/java/dev/rinchan/paperplane/registry/PaperPlaneRegistries.java")));
            assertTrue(Files.isRegularFile(ROOT.resolve("fabric/src/" + version + "/java/dev/rinchan/paperplane/fabric/PaperPlaneFabricCreativeTabs.java")));
            assertTrue(Files.isRegularFile(ROOT.resolve("forge/src/" + version + "/java/dev/rinchan/paperplane/forge/PaperPlaneForgeCreativeTabs.java")));
        }
        assertTrue(Files.isRegularFile(ROOT.resolve("forge/src/main/java/dev/rinchan/paperplane/registry/PaperPlaneRegistries.java")));
        assertFalse(Files.exists(ROOT.resolve("common/src/main/java/dev/rinchan/paperplane/PaperPlaneNetworking.java")));
        assertFalse(Files.exists(ROOT.resolve("common/src/main/java/dev/rinchan/paperplane/registry/PaperPlaneRegistries.java")));
    }
}
