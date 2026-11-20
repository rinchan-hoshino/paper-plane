# Changelog

All notable changes to Paper Plane are documented in this file.

## [2.0.0] - 2026-11-20

### Added
- Rebuilt `TeleportPlayerScreen` using native FTB Library scrollable `Panel` + `PanelScrollBar`, fixing list overflow on servers with more than 7 online players.
- Added player skin head avatars (`FaceIcon`) beside each player entry in the teleport picker.
- Added a live case-insensitive search box at the top of the teleport picker with Enter-to-send when narrowed to a single target.
- Added concise usage tooltips for all 4 paper plane items.
- Added subtle right-click sound feedback (`BOOK_PAGE_TURN` for all planes, layered with `AMETHYST_BLOCK_CHIME` for Ender planes).
- Replaced NightConfig with a zero-dependency commented `config/paper_plane.toml` supporting automatic hot-reload without restarting.
- Added native in-game Config Screen (`PaperPlaneConfigScreen`) integrated with Mod Menu (Fabric), Forge, and NeoForge.
- Added Traditional Chinese (`zh_tw`) and Japanese (`ja_jp`) localizations alongside updated `en_us` and `zh_cn`.
- Replaced project icon with a new 64x64 pixel-art badge (`512x512` `icon.png`).
- Added `26.1.2` item model definitions (`assets/paper_plane/items/*.json`), `FEATURE_MAP.md`, CI workflow, and `scripts/build-matrix.py`.

### Changed
- Converged supported build matrix to 6 mainstream targets: `1.20.1` (Fabric/Forge), `1.21.1` (Fabric/NeoForge), and `26.1.2` (Fabric/NeoForge).
- Consolidated fragmented `shared/` directories into `shared/core`, `shared/modern`, `shared/resources`, `shared/resources-modern`, `shared/test`, and compile-only `shared/modmenu-stub`.

### Removed
- Removed the legacy Soggy Paper Plane (`paper_plane:soggy_paper_plane`) item, water-wetting `ItemEntityMixin`, models, and textures. Legacy worlds seamlessly migrate `paper_plane:soggy_paper_plane` to `paper_plane:paper_plane` via registry aliases / `MissingMappingsEvent`.
- Removed dead flight/entity remnants (`PaperPlaneFlightModel`, `PaperPlaneEntity`, `PaperPlaneEntityRenderer`, `PaperPlaneClientItemExtensions`, `entity.paper_plane.paper_plane`, and `docs/images/plane-in-flight.png`).
- Removed transitional version profiles (`1.19.2`, `1.20.4`, `1.20.6`, `1.21`, `1.21.11`) and unused `night-config` shading.

## [1.3.0] - 2026-10-27
- Added configurable native Back Paper Plane and Ender Back Paper Plane items.
- Settled single-use plane payments only after native teleport completion.

## [1.2.0] - 2026-10-24
- Fixed repeated teleport responses and removed throwing across all supported profiles.
