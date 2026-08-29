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

    def test_ftb_chat_buttons_always_route_to_server_authority(self) -> None:
        packet = (
            COMMON / "java/dev/rinchan/paperplane/RespondTeleportPacket.java"
        ).read_text(encoding="utf-8")
        self.assertIn("UUID requestId", packet)
        self.assertIn("boolean accept", packet)
        self.assertIn('"respond_teleport"', packet)

        fallback = (
            COMMON / "java/dev/rinchan/paperplane/FallbackTeleportResponsePacket.java"
        ).read_text(encoding="utf-8")
        self.assertIn("UUID requestId", fallback)
        self.assertIn("boolean accept", fallback)
        self.assertIn('"fallback_teleport_response"', fallback)

        client = (
            COMMON / "java/dev/rinchan/paperplane/client/PaperPlaneClient.java"
        ).read_text(encoding="utf-8")
        self.assertIn("TeleportResponseCommand.parse(command)", client)
        self.assertIn("PaperPlaneNetworking.sendTeleportResponse", client)
        self.assertIn("fallbackTeleportResponse", client)
        self.assertNotIn("TRACKED_REQUESTS", client)
        self.assertNotIn("TrackTeleportRequestPacket", client)

        mixin = (
            COMMON / "java/dev/rinchan/paperplane/mixin/ClientPacketListenerMixin.java"
        ).read_text(encoding="utf-8")
        self.assertIn('method = "sendUnsignedCommand"', mixin)
        self.assertIn("PaperPlaneClient.handleTeleportResponseCommand", mixin)
        self.assertIn("cancellable = true", mixin)

        mixin_config = json.loads(
            (COMMON / "resources/paper_plane.mixins.json").read_text(encoding="utf-8")
        )
        self.assertIn("ClientPacketListenerMixin", mixin_config["client"])

    def test_server_ledger_decides_handle_or_explicit_fallback(self) -> None:
        entrypoint = (
            COMMON / "java/dev/rinchan/paperplane/neoforge/PaperPlaneNeoForge.java"
        ).read_text(encoding="utf-8")
        self.assertIn("RespondTeleportPacket.TYPE", entrypoint)
        self.assertIn("FallbackTeleportResponsePacket", entrypoint)
        self.assertIn("PaperPlane.respondToTeleportRequest", entrypoint)

        source = (COMMON / "java/dev/rinchan/paperplane/PaperPlane.java").read_text(
            encoding="utf-8"
        )
        self.assertIn("REQUESTS.isOwnedByTarget(requestId, target.getUUID())", source)
        self.assertIn("FTB_TPA.tpa(requester, target, false)", source)
        self.assertIn("FTB_TPA.tpaccept(target, requestId.toString())", source)
        self.assertIn("FTB_TPA.tpdeny(target, requestId.toString())", source)
        self.assertNotIn("FTBEConfig", source)

    def test_tracking_packet_is_removed_from_both_loader_protocols(self) -> None:
        self.assertFalse(
            (COMMON / "java/dev/rinchan/paperplane/TrackTeleportRequestPacket.java").exists()
        )
        neoforge = (
            COMMON / "java/dev/rinchan/paperplane/neoforge/PaperPlaneNeoForge.java"
        ).read_text(encoding="utf-8")
        fabric = (
            ROOT / "fabric/src/main/java/dev/rinchan/paperplane/fabric/PaperPlaneFabric.java"
        ).read_text(encoding="utf-8")
        fabric_client = (
            ROOT
            / "fabric/src/main/java/dev/rinchan/paperplane/fabric/PaperPlaneFabricClient.java"
        ).read_text(encoding="utf-8")
        for source in (neoforge, fabric, fabric_client):
            self.assertNotIn("TrackTeleportRequestPacket", source)
        self.assertIn("FallbackTeleportResponsePacket", neoforge)
        self.assertIn("FallbackTeleportResponsePacket", fabric)
        self.assertIn("FallbackTeleportResponsePacket", fabric_client)

    def test_release_metadata_matches_current_source_license(self) -> None:
        properties = {}
        for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                properties[key] = value
        self.assertEqual("1.0.1", properties["mod_version"])
        self.assertEqual("GPL-3.0-only", properties["mod_license"])
        self.assertEqual("8442866", properties["ftb_essentials_file_id"])


if __name__ == "__main__":
    unittest.main()
