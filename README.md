# Paper Plane

Paper Plane turns folded paper into a server-authoritative TPA interface and a charged low-speed glider.

## Items

- **Paper Plane** — crafted from five paper, stacks to 16.
- **Soggy Paper Plane** — produced when a normal plane remains in water through Lychee, stacks to 16, and cannot fly.
- **Ender Paper Plane** — a normal plane surrounded by eight ender pearls, stacks to one, is uncommon, and is reusable.

## Teleport requests

Right-click a plane without sneaking to open the FTB Library player picker. Paper Plane asks the FTB Essentials TPA backend to create and resolve the request; it does not register a parallel teleport command or implement teleportation itself.

The server creates a one-use selection session bound to the actual plane used. The client sends only that session id and a target UUID, so it cannot claim a normal plane is an ender plane. One requester may own only one pending target. Acceptance, denial, either player disconnecting, or stale FTB state clears the lock.

Normal and soggy planes are removed immediately before FTB performs an accepted teleport. A failed teleport refunds the exact plane, and a missing plane prevents teleportation. Creative players keep their item. Ender planes remain reusable.

FTB chat accept/deny buttons are converted to packets only for request UUIDs explicitly tracked by Paper Plane. FTB TPA commands may therefore remain disabled without intercepting unrelated commands from other mods.

## Charged flight

Sneak and hold use with a normal or ender plane. The use pose raises the plane to the player's mouth with its nose facing back toward the player. Release after at least five ticks to throw it; charge reaches its cap after 40 ticks (two seconds).

- 5 ticks launches at 0.65 blocks/tick and travels roughly 5–8 blocks when thrown flat.
- 40 ticks launches at 1.65 blocks/tick and travels roughly 40–50 blocks when thrown flat.
- Lift scales with horizontal speed, while profile drag and gravity slow the plane into a stall and nose-down descent.
- The world renderer follows velocity yaw and pitch rather than camera-billboarding the item.
- A normal plane drops itself on impact and becomes soggy in water.
- An ender plane disappears on impact without consuming the held item.
- Planes do not home or deal custom damage.

## State

Paper Plane adds three items, one projectile entity, four payload types, and two focused Mixins. Selection and pending-request state are bounded in memory and are removed on completion or disconnect. The projectile persists only its ender-plane flag through normal entity NBT.

There is no Paper Plane config file, command, capability, attachment, player SavedData, or RinLib dependency.

## Required dependencies

- NeoForge
- FTB Library
- FTB Essentials
- Lychee
- Kiwi
- Architectury API (required by the FTB stack)

## Support

Paper Plane 1.0.0 targets Minecraft 1.21.1 NeoForge. Minecraft 26.1.2 has the required dependencies but needs a substantive entity, renderer, item-use and FTB GUI port; Minecraft 26.2 has no FTB Library or FTB Essentials release.
