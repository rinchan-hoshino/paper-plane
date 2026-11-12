# Changelog

## 1.3.0

- Adds Back Paper Plane (4 Bone Meal around a Paper Plane) and Ender Back Paper Plane (8 Ender Pearls around a Back Paper Plane) that return you to your previous position through FTB Essentials' history.
- Single-use Back Paper Planes are consumed only after the teleport completes; cancelled or failed teleports keep the item.
- Adds `enableTpa` and `enableBack` in `config/paper_plane.toml` to switch each plane family on or off independently of FTB commands.
- Paper planes stay non-throwable and use right-click on all 17 supported combinations.

## 1.2.0

- Right-click or sneak-right-click opens the player list directly; charging and throwing are removed.
- Accept requests are checked against the recipient before FTB Essentials handles them; rapid clicks and expired requests end cleanly.
- Reopening the player list with the same plane type keeps the original 30-second window.
- Requires both client and server to be updated to 1.2.0 (network protocol change).

## 1.1.2

- Fixes spurious expired-selection errors when repeatedly using the same paper plane while the player list is open.

## 1.1.1

- Requires RinLib on all supported Minecraft versions and loaders.
- Rejects stale selection tokens without invalidating a newer selection screen.
- Validates the recipient before processing accept or deny packets.
- Corrects Fabric dependency ranges on 1.19.2 and 1.20.1, and item registration on 26.1.2.

## 1.1.0

- Requires RinLib 1.0.2 or newer on Fabric and NeoForge.
- Refreshes the project icon, description and screenshots.

## 1.0.1

- Routes every valid FTB accept/deny button through the server-owned Paper Plane ledger.
- Returns non-Paper Plane request buttons to their original FTB command.

## 1.0.0

- Stable release: server-authoritative requests, atomic payment, deterministic glide physics and corrected hand transforms.
- Publishes all 17 stable FTB Library / FTB Essentials targets from Minecraft 1.19.2 through 26.1.2 across Fabric, Forge and NeoForge.
- Replaces the Lychee/Kiwi water recipe with an owned item-water boundary.

## 0.1.9

- Corrects right- and left-hand model rotations so the charging pose points the nose back toward the player.

## 0.1.8

- Adds the sneak-use blowing pose and deterministic lift, drag, gravity, stall and glide physics.
- Makes plane kind server-authoritative and allows one pending teleport target per requester.
- Removes the plane before a successful FTB acceptance and refunds failed teleports.
- Limits accept/deny interception to tracked Paper Plane request IDs.
