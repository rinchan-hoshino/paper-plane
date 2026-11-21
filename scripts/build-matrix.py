#!/usr/bin/env python3
"""Build, unit-test, and verify every Paper Plane target in matrix.json."""
from __future__ import annotations

import argparse
import glob
import json
import os
import struct
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MATRIX_PATH = ROOT / "matrix.json"

REQUIRED_LANGS = ("en_us", "zh_cn", "zh_tw", "ja_jp")
REQUIRED_ITEMS = ("paper_plane", "ender_paper_plane", "back_paper_plane", "ender_back_paper_plane")
FORBIDDEN_ENTRY_SUBSTRINGS = (
    "rinlib",
    "soggy",
    "flightmodel",
    "paperplaneentity",
    "itementitymixin",
    "nightconfig",
    "com/terraformersmc/modmenu/api/",
    "_edge.png",
)
TEXT_CHECK_SUFFIXES = (".json", ".toml")


def resolve_java_home(version: int) -> str | None:
    for env_key in (
        f"JAVA_{version}_HOME",
        f"JAVA_HOME_{version}_X64",
        f"JAVA_HOME_{version}_ARM64",
    ):
        env_val = os.environ.get(env_key)
        if env_val and os.path.isfile(os.path.join(env_val, "bin", "java")):
            return env_val

    candidates = [
        f"/opt/homebrew/opt/openjdk@{version}/libexec/openjdk.jdk/Contents/Home",
        f"/opt/homebrew/opt/openjdk@{version}",
        *sorted(glob.glob(f"/usr/lib/jvm/*{version}*")),
    ]
    gradle_jdks = sorted(glob.glob(os.path.expanduser(f"~/.gradle/jdks/*-{version}-*")))
    for p in gradle_jdks:
        if os.path.isdir(os.path.join(p, "Contents/Home")):
            candidates.append(os.path.join(p, "Contents/Home"))
        elif os.path.isdir(os.path.join(p, "bin")):
            candidates.append(p)
    for c in candidates:
        if os.path.isfile(os.path.join(c, "bin", "java")):
            return c
    if sys.platform == "darwin":
        res = subprocess.run(
            ["/usr/libexec/java_home", "-v", str(version)],
            capture_output=True,
            text=True,
        )
        if res.returncode == 0 and res.stdout.strip():
            return res.stdout.strip()
    return os.environ.get("JAVA_HOME")


def find_built_jar(profile_dir: Path, mc: str, loader: str) -> Path:
    libs_dir = profile_dir / loader / "build" / "libs"
    if not libs_dir.is_dir():
        raise FileNotFoundError(f"Missing libs directory: {libs_dir}")
    jars = sorted(
        p
        for p in libs_dir.glob("paper_plane-*.jar")
        if not p.name.endswith(("-sources.jar", "-dev.jar", "-javadoc.jar"))
    )
    if not jars:
        raise FileNotFoundError(f"No built JAR found for {mc} {loader} in {libs_dir}")
    # Forge 1.20.1 jarJar outputs *-all.jar alongside regular jar; prefer *-all.jar if present.
    all_jars = [p for p in jars if p.name.endswith("-all.jar")]
    return all_jars[-1] if all_jars else jars[-1]


def verify_jar(jar_path: Path, mc: str, loader: str) -> None:
    with zipfile.ZipFile(jar_path, "r") as zf:
        names = set(zf.namelist())
        errors: list[str] = []

        if "icon.png" not in names:
            errors.append("missing icon.png")

        if mc == "1.20.1" and loader == "forge":
            if "pack.mcmeta" not in names:
                errors.append("missing pack.mcmeta")
            else:
                try:
                    pack_json = json.loads(zf.read("pack.mcmeta").decode("utf-8"))
                    if pack_json.get("pack", {}).get("pack_format") != 15:
                        errors.append(
                            f"pack.mcmeta pack.pack_format must be 15, got {pack_json.get('pack', {}).get('pack_format')!r}"
                        )
                except Exception as exc:
                    errors.append(f"invalid JSON in pack.mcmeta: {exc}")
        elif "pack.mcmeta" in names:
            try:
                pack_json = json.loads(zf.read("pack.mcmeta").decode("utf-8"))
                if pack_json.get("pack", {}).get("pack_format") == 15:
                    errors.append("unexpected pack.mcmeta with pack_format == 15")
            except Exception as exc:
                errors.append(f"invalid JSON in pack.mcmeta: {exc}")

        for lang in REQUIRED_LANGS:
            path = f"assets/paper_plane/lang/{lang}.json"
            if path not in names:
                errors.append(f"missing {path}")

        for item in REQUIRED_ITEMS:
            model = f"assets/paper_plane/models/item/{item}.json"
            tex = f"assets/paper_plane/textures/item/{item}.png"
            for req in (model, tex):
                if req not in names:
                    errors.append(f"missing {req}")
            if model in names:
                try:
                    model_json = json.loads(zf.read(model).decode("utf-8"))
                    if model_json.get("parent") != "minecraft:item/generated":
                        errors.append(f"{model} parent must be minecraft:item/generated")
                    if model_json.get("textures", {}).get("layer0") != f"paper_plane:item/{item}":
                        errors.append(f"{model} layer0 must be paper_plane:item/{item}")
                except Exception as exc:
                    errors.append(f"invalid JSON in {model}: {exc}")
            if tex in names:
                png_bytes = zf.read(tex)
                if len(png_bytes) < 24 or png_bytes[:8] != b"\x89PNG\r\n\x1a\n":
                    errors.append(f"{tex} is not a valid PNG")
                else:
                    w, h = struct.unpack(">II", png_bytes[16:24])
                    if (w, h) != (16, 16):
                        errors.append(f"{tex} must be 16x16, got {w}x{h}")
            if mc == "26.1.2":
                item_def = f"assets/paper_plane/items/{item}.json"
                if item_def not in names:
                    errors.append(f"missing {item_def}")

        for name in sorted(names):
            lower = name.lower()
            for forbidden in FORBIDDEN_ENTRY_SUBSTRINGS:
                if forbidden in lower:
                    errors.append(f"forbidden entry '{forbidden}' in {name}")
            if lower.endswith(TEXT_CHECK_SUFFIXES) and not lower.startswith("meta-inf/jarjar/"):
                raw = zf.read(name).decode("utf-8", errors="ignore").lower()
                for forbidden in ("rinlib", "soggy", "flightmodel", "itementitymixin"):
                    if forbidden in raw:
                        errors.append(f"forbidden token '{forbidden}' in {name}")

        if errors:
            raise AssertionError(
                f"JAR verification failed for {mc}/{loader} ({jar_path.name}):\n  - "
                + "\n  - ".join(errors)
            )
    print(f"  [OK] Verified {mc}/{loader}: {jar_path.relative_to(ROOT)} ({jar_path.stat().st_size:,} bytes)")


def main() -> None:
    matrix = json.loads(MATRIX_PATH.read_text(encoding="utf-8"))
    parser = argparse.ArgumentParser(description="Build and verify Paper Plane matrix.")
    parser.add_argument("--minecraft", action="append", choices=list(matrix.keys()))
    parser.add_argument("--loader", action="append", choices=["fabric", "forge", "neoforge"])
    parser.add_argument("--skip-py-tests", action="store_true")
    parser.add_argument("--clean", action="store_true")
    args = parser.parse_args()

    if not args.skip_py_tests:
        print("==> Running Python contract tests...")
        env = os.environ.copy()
        j21 = resolve_java_home(21)
        if j21:
            env["JAVA_HOME"] = j21
        subprocess.run(
            [sys.executable, "-m", "unittest", "discover", "-s", str(ROOT / "tests"), "-v"],
            cwd=ROOT,
            env=env,
            check=True,
        )

    targets = args.minecraft or list(matrix.keys())
    verified_count = 0

    for mc in targets:
        cfg = matrix[mc]
        loaders = [l for l in cfg["loaders"] if not args.loader or l in args.loader]
        if not loaders:
            continue
        profile_dir = ROOT / cfg["profile"]
        env = os.environ.copy()
        gradle_java = max(21, cfg["java"])
        java_home = resolve_java_home(gradle_java)
        if java_home:
            env["JAVA_HOME"] = java_home
            env["PATH"] = f"{java_home}/bin:{env.get('PATH', '')}"

        wrapper = ["cmd", "/c", "gradlew.bat"] if os.name == "nt" else ["bash", "gradlew"]
        tasks: list[str] = []
        if args.clean:
            tasks.extend(f":{l}:clean" for l in loaders)
        tasks.extend(f":{l}:build" for l in loaders)

        print(f"\n==> Building {mc} ({', '.join(loaders)}) with Java {cfg['java']} ({java_home or 'default'})...")
        subprocess.run(
            wrapper + cfg.get("properties", []) + tasks + ["--console=plain"],
            cwd=profile_dir,
            env=env,
            check=True,
        )

        for loader in loaders:
            jar_path = find_built_jar(profile_dir, mc, loader)
            verify_jar(jar_path, mc, loader)
            verified_count += 1

    print(f"\nALL CHECKS PASSED ({verified_count} target JAR(s) built & verified).")


if __name__ == "__main__":
    main()
