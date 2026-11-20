#!/usr/bin/env python3
"""Generate 4 crisp 16x16 diagonal origami paper plane item textures and a 16x preview sheet."""
from __future__ import annotations

from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
TEX_DIR = ROOT / "shared/resources/assets/paper_plane/textures/item"
PREVIEW_PATH = Path("/Users/rin/.rin/work/public-mods-overhaul/pp-items-preview.png")

# 16x16 contiguous pixel-art grids (zero detached pixels so item/generated hand extrusion is razor-clean).
# 2x2 visual grammar:
#   Material: White Paper (single-use) vs Vanilla Ender-Pearl Jade-Teal (reusable)
#   Function: Clean Wing (TPA forward) vs Gilded Crimson-Gold Wing Trim & Return Chevron (Back)

BASE_GRID = [
    "............OO..",  # y=0
    "..........OOHHO.",  # y=1
    "........OOHHLHO.",  # y=2
    "......OOHHLLCRO.",  # y=3
    "....OOHHLLlCRRO.",  # y=4
    "..OOHHLLllCRRRO.",  # y=5
    ".OHHLLLllCRRRrO.",  # y=6
    ".OLLLLllCKRRrrO.",  # y=7
    "..OOlllCKKRrrO..",  # y=8
    "....OOCKKRRrrO..",  # y=9
    ".....OCKORrrO...",  # y=10
    ".....OCKORrrO...",  # y=11
    ".....OOKORrO....",  # y=12
    "......OOORrO....",  # y=13
    ".........OO.....",  # y=14
    "................",  # y=15
]

# Ender TPA: Vanilla Ender-Pearl deep jade-teal origami with glowing seafoam-cyan pearl core & nose
ENDER_GRID = [
    "............OO..",  # y=0
    "..........OOGAO.",  # y=1
    "........OOAGLAO.",  # y=2
    "......OOHALLCRO.",  # y=3
    "....OOHLLLlCRRO.",  # y=4
    "..OOHLLLAlCRRRO.",  # y=5
    ".OHLLLLAGCRRRrO.",  # y=6
    ".OLLLLlACKRRrrO.",  # y=7
    "..OOlllCKKRrrO..",  # y=8
    "....OOCKKRRrrO..",  # y=9
    ".....OCKORrrO...",  # y=10
    ".....OCKORrrO...",  # y=11
    ".....OOKORrO....",  # y=12
    "......OOORrO....",  # y=13
    ".........OO.....",  # y=14
    "................",  # y=15
]

# Back Paper Plane: Warm bone-parchment origami with gilded amber-gold return chevron & trailing wing trim
BACK_GRID = [
    "............OO..",  # y=0
    "..........OOHHO.",  # y=1
    "........OOHHLHO.",  # y=2
    "......OOHHLLCRO.",  # y=3
    "....OOHHBAACRRO.",  # y=4
    "..OOHHLBLlCARRO.",  # y=5
    ".OHHLLALllCBArO.",  # y=6
    ".OAAABllCKRRArO.",  # y=7
    "..OOAAACKKRrAO..",  # y=8
    "....OOCKKRRAAO..",  # y=9
    ".....OCKORrAO...",  # y=10
    ".....OCKORAAO...",  # y=11
    ".....OOKORAO....",  # y=12
    "......OOOBO.....",  # y=13
    ".........OO.....",  # y=14
    "................",  # y=15
]

# Ender Back Paper Plane: Deep Ender-Pearl jade-teal body + bright gilded gold return chevron & trailing wing trim
ENDER_BACK_GRID = [
    "............OO..",  # y=0
    "..........OOGGO.",  # y=1
    "........OOGHLGO.",  # y=2
    "......OOHHLLCRO.",  # y=3
    "....OOHLBAACRRO.",  # y=4
    "..OOHLLBLlCARRO.",  # y=5
    ".OHLLLALllCBArO.",  # y=6
    ".OAAABllCKRRArO.",  # y=7
    "..OOAAACKKRrAO..",  # y=8
    "....OOCKKRRAAO..",  # y=9
    ".....OCKORrAO...",  # y=10
    ".....OCKORAAO...",  # y=11
    ".....OOKORAO....",  # y=12
    "......OOOBO.....",  # y=13
    ".........OO.....",  # y=14
    "................",  # y=15
]

PALETTES = {
    "paper_plane": (
        BASE_GRID,
        {
            "O": (54, 65, 92, 255),
            "H": (255, 255, 255, 255),
            "L": (241, 246, 255, 255),
            "l": (214, 226, 245, 255),
            "C": (146, 165, 198, 255),
            "K": (104, 121, 156, 255),
            "R": (222, 232, 248, 255),
            "r": (188, 204, 230, 255),
        },
    ),
    "ender_paper_plane": (
        ENDER_GRID,
        {
            "O": (8, 34, 32, 255),
            "H": (72, 184, 164, 255),
            "L": (37, 130, 115, 255),
            "l": (24, 98, 86, 255),
            "C": (14, 64, 58, 255),
            "K": (10, 46, 42, 255),
            "R": (28, 108, 96, 255),
            "r": (18, 78, 70, 255),
            "A": (140, 244, 226, 255),
            "G": (214, 255, 248, 255),
        },
    ),
    "back_paper_plane": (
        BACK_GRID,
        {
            "O": (76, 54, 36, 255),
            "H": (255, 251, 238, 255),
            "L": (246, 235, 210, 255),
            "l": (224, 206, 174, 255),
            "C": (168, 142, 106, 255),
            "K": (126, 100, 70, 255),
            "R": (234, 218, 186, 255),
            "r": (204, 184, 146, 255),
            "A": (214, 116, 36, 255),
            "B": (255, 198, 68, 255),
        },
    ),
    "ender_back_paper_plane": (
        ENDER_BACK_GRID,
        {
            "O": (8, 34, 32, 255),
            "H": (72, 184, 164, 255),
            "L": (37, 130, 115, 255),
            "l": (24, 98, 86, 255),
            "C": (14, 64, 58, 255),
            "K": (10, 46, 42, 255),
            "R": (28, 108, 96, 255),
            "r": (18, 78, 70, 255),
            "A": (232, 152, 38, 255),
            "B": (255, 224, 96, 255),
            "G": (168, 252, 236, 255),
        },
    ),
}


def render_grid(grid: list[str], palette: dict[str, tuple[int, int, int, int]]) -> Image.Image:
    assert len(grid) == 16 and all(len(row) == 16 for row in grid)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = palette[ch]
    return img


def main() -> None:
    TEX_DIR.mkdir(parents=True, exist_ok=True)
    for old in TEX_DIR.glob("*_edge.png"):
        old.unlink()

    rendered: list[tuple[str, Image.Image]] = []
    for name, (grid, palette) in PALETTES.items():
        img = render_grid(grid, palette)
        out = TEX_DIR / f"{name}.png"
        img.save(out, format="PNG", optimize=True)
        rendered.append((name, img))
        print(f"Wrote {out}")

    scale = 16
    tile = 16 * scale
    pad = 24
    header = 32
    sheet_w = pad + len(rendered) * (tile + pad)
    sheet_h = header + tile + pad * 2
    sheet = Image.new("RGBA", (sheet_w, sheet_h), (32, 34, 44, 255))
    draw = ImageDraw.Draw(sheet)

    for idx, (name, img) in enumerate(rendered):
        x0 = pad + idx * (tile + pad)
        y0 = header + pad
        draw.rectangle([x0 - 4, y0 - 4, x0 + tile + 3, y0 + tile + 3], fill=(139, 139, 139, 255), outline=(55, 55, 55, 255))
        up = img.resize((tile, tile), Image.Resampling.NEAREST)
        sheet.alpha_composite(up, (x0, y0))
        draw.text((x0, 14), name, fill=(235, 240, 255, 255))

    PREVIEW_PATH.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(PREVIEW_PATH, format="PNG", optimize=True)
    print(f"Wrote preview {PREVIEW_PATH} ({sheet.size[0]}x{sheet.size[1]})")


if __name__ == "__main__":
    main()
