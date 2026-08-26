from __future__ import annotations

import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


class VersionProfilesTest(unittest.TestCase):
    def test_supported_profiles_pin_official_release_dependencies(self) -> None:
        profiles = json.loads(
            (ROOT / "gradle/version-profiles.json").read_text(encoding="utf-8")
        )
        self.assertEqual({"1.20.6", "1.21"}, set(profiles))
        self.assertEqual(
            {
                "ftb_library_version": "2006.1.2",
                "ftb_essentials_version": "2006.1.1",
            },
            {
                key: profiles["1.20.6"][key]
                for key in ("ftb_library_version", "ftb_essentials_version")
            },
        )
        self.assertEqual(
            {
                "ftb_library_version": "2100.1.4",
                "ftb_essentials_version": "2100.1.0",
            },
            {
                key: profiles["1.21"][key]
                for key in ("ftb_library_version", "ftb_essentials_version")
            },
        )
        for profile in profiles.values():
            self.assertNotIn("SNAPSHOT", json.dumps(profile))
            self.assertEqual(21, profile["java_version"])

    def test_builds_select_one_shared_source_profile(self) -> None:
        root_build = (ROOT / "build.gradle").read_text(encoding="utf-8")
        fabric_build = (ROOT / "fabric/build.gradle").read_text(encoding="utf-8")
        neoforge_build = (ROOT / "neoforge/build.gradle").read_text(encoding="utf-8")
        properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")

        self.assertIn("version-profiles.json", root_build)
        self.assertIn('findProperty("minecraft_version")', root_build)
        self.assertIn("versionProfile", fabric_build)
        self.assertIn("versionProfile", neoforge_build)
        self.assertIn("dev.ftb.mods:ftb-library-fabric", fabric_build)
        self.assertIn("dev.ftb.mods:ftb-essentials-fabric", fabric_build)
        self.assertIn("dev.ftb.mods:ftb-library-neoforge", neoforge_build)
        self.assertIn("dev.ftb.mods:ftb-essentials-neoforge", neoforge_build)
        self.assertNotIn("curse.maven:ftb-", neoforge_build)
        self.assertIn("minecraft_version=1.20.6", properties)

    def test_loader_metadata_and_recipe_layout_are_profile_driven(self) -> None:
        fabric_metadata = (
            ROOT / "fabric/src/main/resources/fabric.mod.json"
        ).read_text(encoding="utf-8")
        fabric_build = (ROOT / "fabric/build.gradle").read_text(encoding="utf-8")
        neoforge_build = (ROOT / "neoforge/build.gradle").read_text(encoding="utf-8")

        for token in (
            "${minecraft_version}",
            "${fabric_loader_version_range}",
            "${ftb_library_version_range}",
            "${ftb_essentials_version_range}",
        ):
            self.assertIn(token, fabric_metadata)
        self.assertIn('replace("/recipe/", "/recipes/")', fabric_build)
        self.assertIn("replace('/recipe/', '/recipes/')", neoforge_build)
        self.assertIn("includeEmptyDirs = false", neoforge_build)


if __name__ == "__main__":
    unittest.main()
