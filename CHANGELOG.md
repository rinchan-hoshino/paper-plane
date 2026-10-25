# 1.1.2 — Minecraft 1.21.1 NeoForge

Paper Plane 1.1.2 for Minecraft 1.21.1 NeoForge fixes spurious expired-selection errors when repeatedly using the same paper plane while the player picker is open. The original expiration deadline remains unchanged and selections can still only be submitted once, while changing plane types or disconnecting continues to cancel the previous selection.

# Changelog

## 1.1.1

- Require RinLib across all supported Minecraft versions and loaders.
- Share flight physics, selection sessions and modern FTB teleport handling across version profiles.
- Reject stale selection tokens without invalidating a newer selection screen.
- Validate the recipient before processing Paper Plane acceptance or denial packets.
- Correct Fabric dependency ranges on Minecraft 1.19.2 and 1.20.1.
- Fix item registration during startup on Minecraft 26.1.2.
- Separate loader-specific code and provide one build matrix for all 17 supported combinations.

## 1.1.0 — Minecraft 1.21.1

- Require RinLib 1.0.2 or newer on Fabric and NeoForge.
- Update the build toolchain for compatibility with RinLib.
- Run the shared unit tests on both loaders.
- Refresh the project icon, description and screenshots.


## 1.0.1

- Routes every syntactically valid FTB accept/deny button through the server-owned Paper Plane ledger, eliminating the client-tracking race that could leak a click into disabled FTB commands.
- Returns non-Paper Plane request buttons to their original FTB command explicitly, preserving compatibility without trusting client request ownership.

## 1.0.0

- Promotes the server-authoritative request, atomic payment, two-second charging pose, deterministic glide physics and corrected hand transforms to the stable public contract.
- Publishes all 17 stable FTB Library / FTB Essentials targets from Minecraft 1.19.2 through 26.1.2 across Fabric, Forge and NeoForge.
- Replaces the Lychee/Kiwi water recipe with an owned single-transition item-water boundary so auxiliary dependencies cannot truncate support.

## 0.1.9

- Corrects right- and left-hand model rotations so the charging transform consistently points the plane nose back toward the player.

## 0.1.8

- Adds a two-second sneak-use blowing pose and deterministic lift, drag, gravity, stall and glide physics.
- Replaces the camera-billboard projectile renderer with velocity-aligned world rendering.
- Makes plane kind server-authoritative through one-use selection sessions.
- Allows only one pending teleport target per requester.
- Atomically removes normal or soggy planes before successful FTB acceptance and refunds failed teleports.
- Limits accept/deny command interception to tracked Paper Plane request IDs.
- Makes the network channel mandatory and logs handler failures.
- Prevents double resolution when a projectile hits a block underwater.
- Declares the complete distribution dependency set for the formal release.
