from __future__ import annotations

import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
COMMON = ROOT / "common/src/main"


class TpaCommandOwnershipTest(unittest.TestCase):
    def test_server_registers_no_competing_tpa_commands(self) -> None:
        self.assertFalse(
            (COMMON / "java/dev/rinchan/paperplane/PaperPlaneCommands.java").exists()
        )
        for entrypoint in (
            ROOT / "fabric/src/main/java/dev/rinchan/paperplane/fabric/PaperPlaneFabric.java",
            ROOT / "forge/src/main/java/dev/rinchan/paperplane/forge/PaperPlaneForge.java",
        ):
            source = entrypoint.read_text(encoding="utf-8")
            self.assertNotIn("RegisterCommandsEvent", source)
            self.assertNotIn("Commands.literal", source)
            self.assertNotIn("PaperPlaneCommands", source)

    def test_ftb_chat_buttons_are_routed_through_authenticated_payloads(self) -> None:
        packet = (
            COMMON / "java/dev/rinchan/paperplane/RespondTeleportPacket.java"
        ).read_text(encoding="utf-8")
        self.assertIn("String requestId", packet)
        self.assertIn("boolean accept", packet)
        self.assertIn('"respond_teleport"', packet)

        client = (
            COMMON / "java/dev/rinchan/paperplane/client/PaperPlaneClient.java"
        ).read_text(encoding="utf-8")
        self.assertIn('"tpaccept "', client)
        self.assertIn('"tpdeny "', client)
        self.assertIn("PaperPlaneNetworking.sendTeleportResponse", client)
        self.assertIn("TRACKED_REQUESTS.contains(requestId)", client)

        mixin = (
            COMMON / "java/dev/rinchan/paperplane/mixin/ScreenMixin.java"
        ).read_text(encoding="utf-8")
        self.assertIn('method = "handleComponentClicked"', mixin)
        self.assertIn("PaperPlaneClient.handleTeleportResponseCommand", mixin)
        self.assertIn("cancellable = true", mixin)

        mixin_config = json.loads(
            (COMMON / "resources/paper_plane.mixins.json").read_text(encoding="utf-8")
        )
        self.assertIn("ScreenMixin", mixin_config["client"])

    def test_each_loader_uses_sender_owned_server_handlers(self) -> None:
        fabric = (
            ROOT / "fabric/src/main/java/dev/rinchan/paperplane/PaperPlaneNetworking.java"
        ).read_text(encoding="utf-8")
        forge = (
            ROOT / "forge/src/main/java/dev/rinchan/paperplane/PaperPlaneNetworking.java"
        ).read_text(encoding="utf-8")
        self.assertIn("PaperPlane.respondToTeleportRequest(player", fabric)
        self.assertIn("context.getSender()", forge)
        self.assertIn("PaperPlane.respondToTeleportRequest(sender", forge)

        owner = (COMMON / "java/dev/rinchan/paperplane/PaperPlane.java").read_text(
            encoding="utf-8"
        )
        self.assertIn("TPACommands.tpa(requester, target, false)", owner)
        self.assertIn("TPACommands.tpaccept(target, requestId)", owner)
        self.assertIn("TPACommands.tpdeny(target, requestId)", owner)

    def test_release_metadata_covers_both_legacy_targets(self) -> None:
        properties: dict[str, str] = {}
        for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                properties[key] = value
        self.assertEqual("1.0.0", properties["mod_version"])
        self.assertEqual("GPL-3.0-only", properties["mod_license"])

        build = (ROOT / "build.gradle").read_text(encoding="utf-8")
        self.assertIn("'1.19.2': [", build)
        self.assertIn("'1.20.1': [", build)
        self.assertIn("include 'fabric', 'forge'", (ROOT / "settings.gradle").read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
