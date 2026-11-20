# Paper Plane — Feature & Architecture Map

## Supported Targets (6)

| Profile Directory | Minecraft | Loaders | Java |
|---|---|---|---|
| `1.20.1/` | `1.20.1` | `fabric`, `forge` | 17 |
| `1.21.1/` | `1.21.1` | `fabric`, `neoforge` | 21 |
| `26.1.2/` | `26.1.2` | `fabric`, `neoforge` | 25 |

## Shared Layout (`shared/`)

| Directory | Scope | Contents |
|---|---|---|
| `shared/common.gradle` | All profiles | Wires shared source/resource sets, license packaging, and Fabric compile-only Mod Menu stub. |
| `shared/core/` | All profiles | Pure/cross-version core: `PlaneKind`, `PaperPlaneConfig` (zero-dep commented TOML + hot-reload `Holder`), `PlayerFilter`, `SelectionSessions`, `TeleportPayments`, `TeleportResponseCommand`, `NativeWarmupMixin`. |
| `shared/modern/` | `1.21.1`, `26.1.2` | Shared modern server logic: `PaperPlane`, `TeleportRequestLedger`, `FtbBack`, `FtbPlayerData`, `PlayerEntry`, `TPACommandMixin`, `BackCommandInvoker`. |
| `shared/resources/` | All profiles | `icon.png` (512x512), 4-language `lang/*.json` (`en_us`, `zh_cn`, `zh_tw`, `ja_jp`), 4 3D origami item models (`models/item/*.json`), 8 item textures (`textures/item/*.png`). |
| `shared/resources-modern/` | `1.21.1`, `26.1.2` | 4 crafting recipes (`data/paper_plane/recipe/*.json`) and `paper_plane.mixins.json`. |
| `shared/test/` | All profiles | Unit & contract tests: `PaperPlaneConfigTest`, `PlayerFilterTest`, `SelectionSessionsTest`, `TeleportResponseCommandTest`, `PaperPlaneTextureRepairContractTest`. |
| `shared/modmenu-stub/` | Fabric compileOnly | Thin compile-time stub for `ModMenuApi` / `ConfigScreenFactory` (never packaged into output JARs). |

## Core Mechanics

1. **Server-Authoritative Selection & Ledger**
   - `SelectionSessions`: 30-second server-owned session tokens so clients cannot forge plane kinds or replay stale screens.
   - `TeleportRequestLedger`: Tracks active Paper Plane TPA requests tied to native FTB Essentials request IDs.
   - `TeleportPayments`: Wraps native FTB teleport execution (`NativeTeleportMixin` + `NativeWarmupMixin`) so single-use planes are consumed only after the teleport actually completes; cancellations/failures refund immediately.
2. **Legacy Save Migration**
   - `paper_plane:soggy_paper_plane` is aliased to `paper_plane:paper_plane` via `BuiltInRegistries.ITEM.addAlias` (Fabric 1.20.1 / 1.21.1 / 26.1.2), `DeferredRegister.addAlias` (NeoForge 1.21.1 / 26.1.2), and `MissingMappingsEvent` (Forge 1.20.1).
3. **UI & Config**
   - `TeleportPlayerScreen` (per MC version): FTB Library scrollable `Panel` + `PanelScrollBar`, `FaceIcon` player avatars, live case-insensitive `TextBox` search with Enter-to-send.
   - `PaperPlaneConfigScreen` (per MC version): Native vanilla config screen wired to Mod Menu (Fabric), `ConfigScreenHandler` (Forge), and `IConfigScreenFactory` (NeoForge).
