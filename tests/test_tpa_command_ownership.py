from __future__ import annotations

import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


class TpaCommandOwnershipTest(unittest.TestCase):
    def test_paper_plane_owns_accept_and_deny_entrypoints(self) -> None:
        commands_path = ROOT / "common/src/main/java/dev/rinchan/paperplane/PaperPlaneCommands.java"
        self.assertTrue(commands_path.is_file())
        source = commands_path.read_text(encoding="utf-8")
        self.assertIn('Commands.literal("tpaccept")', source)
        self.assertIn('Commands.literal("tpdeny")', source)
        self.assertIn("PaperPlane.acceptTeleportRequest", source)
        self.assertIn("PaperPlane.denyTeleportRequest", source)
        self.assertNotIn("FTBEConfig", source)

    def test_command_registration_is_unconditional(self) -> None:
        entrypoint = (
            ROOT / "common/src/main/java/dev/rinchan/paperplane/neoforge/PaperPlaneNeoForge.java"
        ).read_text(encoding="utf-8")
        self.assertIn("RegisterCommandsEvent", entrypoint)
        self.assertIn("PaperPlaneCommands.register(event.getDispatcher())", entrypoint)
        self.assertNotIn("FTBEConfig", entrypoint)

    def test_release_metadata_matches_current_source_license(self) -> None:
        properties = {}
        for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                properties[key] = value
        self.assertEqual("0.1.6", properties["mod_version"])
        self.assertEqual("GPL-3.0-only", properties["mod_license"])
        self.assertEqual("8442866", properties["ftb_essentials_file_id"])

    def test_request_backend_is_called_directly(self) -> None:
        source = (ROOT / "common/src/main/java/dev/rinchan/paperplane/PaperPlane.java").read_text(
            encoding="utf-8"
        )
        self.assertIn("FTB_TPA.tpa(requester, target, false)", source)
        self.assertNotIn("FTBEConfig", source)


if __name__ == "__main__":
    unittest.main()
