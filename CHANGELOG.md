# Changelog

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
