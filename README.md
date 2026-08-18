# Paper Plane

Paper Plane adds item-driven player-to-player teleport requests through an FTB Library player picker backed by FTB Essentials' TPA system.

## Features

- Craft a **Paper Plane** from five pieces of paper:

```text
x - -
x x x
x - -
```

- Right-click a Paper Plane to open a player list UI and send an FTB Essentials TPA request.
- The target player receives FTB Essentials' standard clickable accept/deny chat prompt.
- Paper Plane calls the TPA backend directly and owns the accept/deny entrypoints, so FTB Essentials' command feature switch can remain disabled.
- Shift-right-click throws a paper plane as a slow arrow-like projectile.
- Drop a normal Paper Plane into water to turn it into **Paper Plane That Can't Fly?** through a Lychee item-inside-liquid recipe.
- Craft an **Ender Paper Plane** with eight ender pearls around a Paper Plane; it can send requests without being consumed.

## Requirements

- Minecraft 1.21.1
- NeoForge
- FTB Essentials
- FTB Library
- Architectury API, required by FTB Library
- Lychee
- Kiwi, required by Lychee
