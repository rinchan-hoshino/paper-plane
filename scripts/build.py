"""Build every released Minecraft/loader combination or a selected subset."""
from pathlib import Path
import argparse, json, subprocess, os
root = Path(__file__).resolve().parents[1]
matrix = json.loads((root / 'matrix.json').read_text())
p = argparse.ArgumentParser()
p.add_argument('--minecraft', action='append', choices=matrix)
p.add_argument('--loader', action='append', choices=['fabric', 'forge', 'neoforge'])
a = p.parse_args()
for mc in a.minecraft or matrix:
    cfg = matrix[mc]
    loaders = [l for l in cfg['loaders'] if not a.loader or l in a.loader]
    if not loaders:
        continue
    profile = root / cfg['profile']
    wrapper = ['cmd', '/c', 'gradlew.bat'] if os.name == 'nt' else ['bash', 'gradlew']
    subprocess.run(wrapper + cfg['properties'] + [f':{l}:build' for l in loaders]
                   + ['--console=plain'], cwd=profile, check=True)
