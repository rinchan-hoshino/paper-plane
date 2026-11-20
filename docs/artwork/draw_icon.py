#!/usr/bin/env python3
"""Generate the 64x64 pixel-art Paper Plane badge icon upscaled to 512x512."""
from __future__ import annotations

import math
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT_PATH = ROOT / "shared/resources/icon.png"

SIZE = 64
SCALE = 8


def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * max(0.0, min(1.0, t))


def lerp_color(c1: tuple[int, int, int], c2: tuple[int, int, int], t: float) -> tuple[int, int, int, int]:
    return (
        int(round(lerp(c1[0], c2[0], t))),
        int(round(lerp(c1[1], c2[1], t))),
        int(round(lerp(c1[2], c2[2], t))),
        255,
    )


def in_rounded_rect(x: int, y: int, x0: int, y0: int, x1: int, y1: int, r: float) -> bool:
    cx = min(max(x + 0.5, x0 + r), x1 - r)
    cy = min(max(y + 0.5, y0 + r), y1 - r)
    dx = (x + 0.5) - cx
    dy = (y + 0.5) - cy
    return dx * dx + dy * dy <= r * r


def point_in_tri(px: float, py: float, a: tuple[float, float], b: tuple[float, float], c: tuple[float, float]) -> bool:
    def sign(p1, p2, p3):
        return (p1[0] - p3[0]) * (p2[1] - p3[1]) - (p2[0] - p3[0]) * (p1[1] - p3[1])

    d1 = sign((px, py), a, b)
    d2 = sign((px, py), b, c)
    d3 = sign((px, py), c, a)
    has_neg = (d1 < 0) or (d2 < 0) or (d3 < 0)
    has_pos = (d1 > 0) or (d2 > 0) or (d3 > 0)
    return not (has_neg and has_pos)


def draw_icon() -> Image.Image:
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()

    # 1. Rounded badge background with beveled border and deep night-sky blue-purple gradient
    c_top = (19, 22, 54)
    c_mid = (34, 30, 82)
    c_bot = (56, 36, 108)

    for y in range(SIZE):
        for x in range(SIZE):
            if not in_rounded_rect(x, y, 2, 2, 62, 62, 12.0):
                continue
            if not in_rounded_rect(x, y, 4, 4, 60, 60, 10.0):
                # Outer crisp dark frame
                px[x, y] = (14, 13, 34, 255)
            elif not in_rounded_rect(x, y, 5, 5, 59, 59, 9.0):
                # Inner bevel highlight (top-left lighter, bottom-right darker)
                if x + y < 64:
                    px[x, y] = (92, 86, 168, 255)
                else:
                    px[x, y] = (38, 30, 78, 255)
            else:
                t = (0.35 * (x / SIZE) + 0.65 * (y / SIZE))
                if t < 0.5:
                    col = lerp_color(c_top, c_mid, t * 2.0)
                else:
                    col = lerp_color(c_mid, c_bot, (t - 0.5) * 2.0)
                # Subtle radial glow behind the plane
                dist = math.hypot(x - 36, y - 26)
                if dist < 24:
                    glow = (1.0 - dist / 24.0) * 0.28
                    col = (
                        min(255, int(col[0] + 45 * glow)),
                        min(255, int(col[1] + 55 * glow)),
                        min(255, int(col[2] + 95 * glow)),
                        255,
                    )
                px[x, y] = col

    # 2. Subtle night-sky pixel stars
    stars = [
        (13, 13, (210, 225, 255, 220), True),
        (24, 10, (170, 190, 245, 190), False),
        (11, 27, (165, 235, 255, 200), False),
        (51, 45, (215, 190, 255, 220), True),
        (46, 52, (160, 210, 255, 180), False),
        (29, 53, (190, 165, 255, 170), False),
    ]
    for sx, sy, scol, big in stars:
        px[sx, sy] = scol
        if big:
            dim = (scol[0], scol[1], scol[2], 110)
            for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
                px[sx + dx, sy + dy] = dim

    # 3. Sweeping cyan-to-violet ender teleport stardust arc from bottom-left to plane tail
    # Quadratic Bezier: P0=(11, 52), P1=(18, 36), P2=(33, 33)
    p0 = (11.0, 52.0)
    p1 = (17.0, 35.0)
    p2 = (33.5, 32.5)
    c_cyan = (62, 238, 215)
    c_sky = (92, 198, 255)
    c_violet = (178, 102, 255)

    for step in range(180):
        t = step / 179.0
        mt = 1.0 - t
        bx = mt * mt * p0[0] + 2 * mt * t * p1[0] + t * t * p2[0]
        by = mt * mt * p0[1] + 2 * mt * t * p1[1] + t * t * p2[1]
        # Width tapers from thin at start to fuller near tail
        radius = 0.8 + 1.45 * math.sin(t * math.pi * 0.85)
        for dy in range(-3, 4):
            for dx in range(-3, 4):
                ix, iy = int(round(bx)) + dx, int(round(by)) + dy
                if not in_rounded_rect(ix, iy, 6, 6, 58, 58, 8.0):
                    continue
                d = math.hypot(ix - bx, iy - by)
                if d <= radius:
                    if t < 0.5:
                        col = lerp_color(c_violet, c_sky, t * 2.0)
                    else:
                        col = lerp_color(c_sky, c_cyan, (t - 0.5) * 2.0)
                    # Bright core along center of arc
                    if d < radius * 0.55:
                        col = (
                            min(255, col[0] + 50),
                            min(255, col[1] + 45),
                            min(255, col[2] + 30),
                            255,
                        )
                    px[ix, iy] = col

    # Ender stardust sparkles around the trail
    sparkles = [
        (10, 49, (215, 130, 255, 255)),
        (15, 48, (140, 225, 255, 255)),
        (14, 39, (90, 245, 225, 255)),
        (22, 43, (195, 125, 255, 255)),
        (24, 34, (130, 255, 240, 255)),
        (28, 41, (115, 215, 255, 255)),
        (36, 42, (200, 145, 255, 255)),
        (18, 51, (165, 110, 245, 255)),
    ]
    for sx, sy, scol in sparkles:
        px[sx, sy] = scol
    # Two 3x3 diamond sparkles
    for sx, sy, scol in [(19, 39, (220, 255, 250, 255)), (30, 46, (235, 195, 255, 255))]:
        px[sx, sy] = (255, 255, 255, 255)
        for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            px[sx + dx, sy + dy] = scol

    # 4. Crisp 3D Origami Paper Plane soaring upward-right toward nose (54, 11)
    nose = (54.0, 11.0)
    left_tip = (13.0, 24.0)
    upper_fold = (28.0, 31.0)
    keel_tip = (27.0, 43.0)
    lower_fold = (35.0, 35.0)
    right_tip = (45.0, 49.0)

    # First compute plane mask & facets
    plane_map: dict[tuple[int, int], tuple[int, int, int, int]] = {}
    for y in range(6, 58):
        for x in range(6, 58):
            cx, cy = x + 0.5, y + 0.5
            # Facet priority: upper wing, right wing, inner fold, keel shadow
            if point_in_tri(cx, cy, nose, left_tip, upper_fold):
                # Bright sun-lit top-left wing
                t = max(0.0, min(1.0, (54.0 - cx) / 41.0))
                plane_map[(x, y)] = lerp_color((255, 255, 255), (232, 240, 255), t)
            elif point_in_tri(cx, cy, nose, lower_fold, right_tip):
                # Slightly cooler right wing
                t = max(0.0, min(1.0, (cy - 11.0) / 38.0))
                plane_map[(x, y)] = lerp_color((242, 247, 255), (206, 220, 245), t)
            elif point_in_tri(cx, cy, nose, upper_fold, keel_tip):
                # Deep inner crease under left wing
                t = max(0.0, min(1.0, (54.0 - cx) / 30.0))
                plane_map[(x, y)] = lerp_color((168, 184, 218), (118, 134, 176), t)
            elif point_in_tri(cx, cy, nose, keel_tip, lower_fold):
                # Underside keel facet (medium-dark shadow)
                plane_map[(x, y)] = (88, 102, 146, 255)

    # Draw 1px crisp dark-indigo outline around the origami silhouette
    outline = (22, 24, 54, 255)
    for (x, y) in list(plane_map.keys()):
        for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in plane_map and in_rounded_rect(nx, ny, 5, 5, 59, 59, 9.0):
                px[nx, ny] = outline

    # Fill plane facets on top of outline
    for (x, y), col in plane_map.items():
        px[x, y] = col

    # Add a crisp 1px cyan-white specular glint at the nose tip
    px[52, 12] = (255, 255, 255, 255)
    px[53, 11] = (225, 252, 255, 255)

    return img


def main() -> None:
    icon_64 = draw_icon()
    icon_512 = icon_64.resize((SIZE * SCALE, SIZE * SCALE), Image.Resampling.NEAREST)
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    icon_512.save(OUT_PATH, format="PNG", optimize=True)
    print(f"Wrote {OUT_PATH} ({icon_512.size[0]}x{icon_512.size[1]})")


if __name__ == "__main__":
    main()
