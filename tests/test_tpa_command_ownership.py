from __future__ import annotations

import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
COMMON = ROOT / "common/src/main"


class TpaCommandOwnershipTest(unittest.TestCase):
    def test_server_registers_no_tpa_commands(self) -> None:
        self.assertFalse(
            (COMMON / "java/dev/rinchan/paperplane/PaperPlaneCommands.java").exists()
        )
        entrypoint = (
            COMMON / "java/dev/rinchan/paperplane/neoforge/PaperPlaneNeoForge.java"
        ).read_text(encoding="utf-8")
        self.assertNotIn("RegisterCommandsEvent", entrypoint)
        self.assertNotIn("Commands.literal", entrypoint)
        self.assertNotIn("PaperPlaneCommands", entrypoint)

    def test_ftb_chat_buttons_are_routed_through_a_payload(self) -> None:
        packet = (
            COMMON / "java/dev/rinchan/paperplane/RespondTeleportPacket.java"
        ).read_text(encoding="utf-8")
        self.assertIn("UUID requestId", packet)
        self.assertIn("boolean accept", packet)
        self.assertIn('"respond_teleport"', packet)

        client = (
            COMMON / "java/dev/rinchan/paperplane/client/PaperPlaneClient.java"
        ).read_text(encoding="utf-8")
        self.assertIn('"tpaccept "', client)
        self.assertIn('"tpdeny "', client)
        self.assertIn("PaperPlaneNetworking.sendTeleportResponse", client)
        networking = (
            COMMON / "java/dev/rinchan/paperplane/PaperPlaneNetworking.java"
        ).read_text(encoding="utf-8")
        self.assertIn("new RespondTeleportPacket", networking)
        self.assertIn("TRACKED_REQUESTS.contains(id)", client)

        mixin = (
            COMMON / "java/dev/rinchan/paperplane/mixin/ClientPacketListenerMixin.java"
        ).read_text(encoding="utf-8")
        self.assertIn('method = "sendCommand"', mixin)
        self.assertIn("PaperPlaneClient.handleTeleportResponseCommand", mixin)
        self.assertIn("cancellable = true", mixin)

        mixin_config = json.loads(
            (COMMON / "resources/paper_plane.mixins.json").read_text(encoding="utf-8")
        )
        self.assertIn("ClientPacketListenerMixin", mixin_config["client"])

    def test_server_payload_calls_ftb_backend_directly(self) -> None:
        entrypoint = (
            COMMON / "java/dev/rinchan/paperplane/neoforge/PaperPlaneNeoForge.java"
        ).read_text(encoding="utf-8")
        self.assertIn("RespondTeleportPacket.TYPE", entrypoint)
        self.assertIn("PaperPlane.respondToTeleportRequest", entrypoint)

        source = (COMMON / "java/dev/rinchan/paperplane/PaperPlane.java").read_text(
            encoding="utf-8"
        )
        self.assertIn("FTB_TPA.tpa(requester, target, false)", source)
        self.assertIn("FTB_TPA.tpaccept(target, requestId.toString())", source)
        self.assertIn("FTB_TPA.tpdeny(target, requestId.toString())", source)
        self.assertNotIn("FTBEConfig", source)

    def test_release_metadata_matches_current_source_license(self) -> None:
        properties = {}
        for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                properties[key] = value
        self.assertEqual("1.0.0", properties["mod_version"])
        self.assertEqual("GPL-3.0-only", properties["mod_license"])
        self.assertEqual("1.21.11", properties["minecraft_version"])
        self.assertEqual("2111.1.1", properties["ftb_library_version"])
        self.assertEqual("2111.1.1", properties["ftb_essentials_version"])


if __name__ == "__main__":
    unittest.main()
