# 1.1.1 validation — 2026-09-21

## Build and runtime coverage

| Minecraft | Build and unit tests | Packaged Fabric server: startup, flight and item drop |
| --- | --- | --- |
| 1.19.2 | Fabric, Forge passed | Passed |
| 1.20.1 | Fabric, Forge passed | Passed |
| 1.20.4 | Fabric, Forge, NeoForge passed | Passed |
| 1.20.6 | Fabric, NeoForge passed | Passed |
| 1.21 | Fabric, NeoForge passed | Passed |
| 1.21.1 | Fabric, NeoForge passed | Passed |
| 1.21.11 | Fabric, NeoForge passed | Passed |
| 26.1.2 | Fabric, NeoForge passed | Passed |

All 17 distribution artifacts contain the shared classes and declare RinLib. Forge distribution artifacts are the `-all.jar` files, which include MixinExtras.

The isolated Fabric server checks use the production jars and actual FTB Library, FTB Essentials, RinLib and loader dependencies. A spawned paper-plane entity must land and become an item. These checks do not exercise every version's client UI.

## Two-client teleport check

Minecraft 1.21.1, Paper Plane 1.1.1, NeoForge server 21.1.228 and clients 21.1.250:

- FTB `/tpa` disabled; request opened through the paper-plane item and player-selection screen.
- The recipient clicked FTB's accept button: the requester reached the recipient; normal paper planes decreased from 16 to 15.
- The recipient clicked deny on a subsequent request: the count remained 15 and the requester did not move.

This full client interaction check was performed on 1.21.1. Other Minecraft versions received the build/unit and packaged-server checks listed above.

## Regressions addressed

- Legacy Fabric dependency predicates now use Fabric syntax, allowing compatible FTB dependencies to load.
- Minecraft 26.1.2 item factories receive registry IDs before constructing items, fixing startup failure.
- Selection tokens are bound to the player, consumed once and expire on server time. A stale token cannot delete a newer session.
- Packet-driven responses validate the request recipient before calling FTB.

The existing Python ownership/metadata checks also pass after updating their paths for the unified repository.

An additional Minecraft 26.1.2 NeoForge development server check passed startup, flight and item drop after the registration fix.
