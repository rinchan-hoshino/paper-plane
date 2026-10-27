"""Exercise the actual client handlers with only UI/network boundaries replaced."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class ResponseRoutingTest(unittest.TestCase):
    def test_legacy_and_modern_buttons_always_reach_the_server(self):
        for profile, request_id, parameter_type in [
            ("1.19.2-1.20.1", "CAFEBABE", "String"),
            ("1.21.1", "d0c45834-82c8-4419-a8a5-192132858e40", "java.util.UUID"),
        ]:
            with self.subTest(profile=profile), tempfile.TemporaryDirectory() as temporary:
                directory = Path(temporary)
                package = directory / "src/dev/rinchan/paperplane"
                client = package / "client"
                client.mkdir(parents=True)
                source = ROOT / profile / "common/src/main/java/dev/rinchan/paperplane/client/PaperPlaneClient.java"
                (client / source.name).write_text(source.read_text())
                (package / "TeleportResponseCommand.java").write_text(
                    (ROOT / "shared/java/dev/rinchan/paperplane/TeleportResponseCommand.java").read_text()
                )
                (package / "OpenTeleportScreenPacket.java").write_text(
                    "package dev.rinchan.paperplane; public record OpenTeleportScreenPacket(java.util.UUID sessionId, java.util.List<Object> players, boolean enderPlane) {}"
                )
                (client / "TeleportPlayerScreen.java").write_text(
                    "package dev.rinchan.paperplane.client; public class TeleportPlayerScreen { public TeleportPlayerScreen(java.util.UUID id, java.util.List<Object> players, boolean ender) {} public void openGui() {} }"
                )
                (package / "PaperPlaneNetworking.java").write_text(
                    "package dev.rinchan.paperplane; public class PaperPlaneNetworking { public static int calls; public static void sendTeleportResponse("
                    + parameter_type + " id, boolean accept) { calls++; } }"
                )
                (package / "Probe.java").write_text(
                    """package dev.rinchan.paperplane;
import dev.rinchan.paperplane.client.PaperPlaneClient;
public class Probe {
    public static void main(String[] args) {
        String id = args[0];
        if (!PaperPlaneClient.handleTeleportResponseCommand("tpaccept " + id)) throw new AssertionError("Native accept was not routed");
        if (!PaperPlaneClient.handleTeleportResponseCommand("tpaccept " + id)) throw new AssertionError("Repeat accept fell through to chat");
        if (!PaperPlaneClient.handleTeleportResponseCommand("tpdeny " + id)) throw new AssertionError("Stale deny fell through to chat");
        if (PaperPlaneClient.handleTeleportResponseCommand("home")) throw new AssertionError("Unrelated command was intercepted");
        if (PaperPlaneClient.handleTeleportResponseCommand("tpaccept invalid")) throw new AssertionError("Malformed request was routed");
        if (PaperPlaneNetworking.calls != 3) throw new AssertionError("Unexpected packet count");
    }
}
"""
                )
                java_home = os.environ.get("JAVA_HOME")
                javac = str(Path(java_home) / "bin/javac") if java_home else "javac"
                java = str(Path(java_home) / "bin/java") if java_home else "java"
                output = directory / "classes"
                subprocess.run([javac, "--release", "17", "-d", str(output), *map(str, package.rglob("*.java"))], check=True, capture_output=True, text=True)
                subprocess.run([java, "-cp", str(output), "dev.rinchan.paperplane.Probe", request_id], check=True, capture_output=True, text=True)
