# Architecture

## Module Overview

| Package | Side | Responsibility |
| :--- | :--- | :--- |
| `codes.honor.cobblemonmajesty` | Common | `@Mod` entrypoint (`CobblemonMajesty`), fault-isolating `Guard`, lifecycle wiring |
| `codes.honor.cobblemonmajesty.config` | Common | `MajestyConfig` (`cobblemon_majesty-common.toml`) for odds, IV promotion, source toggles, and combat toggle |
| `codes.honor.cobblemonmajesty.feature` | Common | `GlobalSpeciesFeatures` registration (`majestic` `FlagSpeciesFeatureProvider`), aspect sync, idempotent roll marker (`cobblemon_majesty_rolled`), `+N` additional 31 IV selection |
| `codes.honor.cobblemonmajesty.spawning` | Common | Event subscribers for wild spawns, fishing, PokéSnacks, fossils, starters, egg exclusion, and `ActorType.NPC` trainer team rolls |
| `codes.honor.cobblemonmajesty.battle` | Common | `MajestyShowdown` GraalJS bridge registering per-battle Majestic `Pokemon.uuid` sets and hooking Showdown `ModifyDamage` |
| `codes.honor.cobblemonmajesty.palette` | Common | 18-type emissive primary + accent defaults, non-averaging dual-strand resolver, symmetric pixel inversion math, and `EyeBoneClassifier` |
| `codes.honor.cobblemonmajesty.sound` | Common | `DeferredRegister` for `cobblemon_majesty:majestic_presence` |
| `codes.honor.cobblemonmajesty.client` | `Dist.CLIENT` | `MajestyClientConfig`, `MajestyInvertedTextureCache`, Iris-compatible `RenderType` shards, ground filament/spark + wisp/comet/star renderer, `PosableModel` shader eye glow, terrain-conforming HDR area light + Iris colored-entity bridge, wild notice chime, world/battle/Summary `✦` badges |
| `codes.honor.cobblemonmajesty.mixin` | Common & Client | `GraalShowdownServiceMixin` (server/common) and 6 client-only rendering/UI mixins isolated under `"client"` in `cobblemon_majesty.mixins.json` |

## Key Design Decisions

### 1. Variant Stacking + Runtime Base Color Inversion
Texture variants in Cobblemon compete for `VaryingModelRepository` texture slots. By registering `majestic` as a `FlagSpeciesFeatureProvider` (`isAspect = true`) in `GlobalSpeciesFeatures` with no `ModelVariationSet` overrides, Cobblemon resolves the underlying Normal, Shiny, Lucky, Alpha, Mega, or resource-pack texture first. `VaryingModelRepositoryMixin` then passes that resolved `ResourceLocation` through `MajestyInvertedTextureCache`, which generates and caches a nearest-neighbor RGB-inverted `DynamicTexture` (evicted on resource reload). Shiny Majestic and Normal Majestic therefore produce distinct inverted palettes across both world entities and 3D UI portraits.

### 2. Shader-Compatible Aura, Eye Glow, and Ringless Area Lighting
- **Ground Sweeps & Rising Particles**: `MajestyRenderTypes` builds static `RenderType`s on `RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER` with `TRANSLUCENT_TRANSPARENCY` and full-bright lightmaps (`15, 15`) for low 3D camera-aligned helical ground filaments, fine ground spark motes, silhouette-framing base wisps, rising comet-embers, and 4-point star sparkles.
- **Shader Eye Glow**: `MajestyEyeGlowRenderer` unwraps any inverted `DynamicTexture` back to its source `ResourceLocation`, builds a cached pre-multiplied grayscale eye mask, and renders open `pupil`/`iris`/`eye` `Bone` subtrees (with an `"eye"` locator fallback) through `RenderType.eyes` (`gbuffers_spidereyes` in Iris shaderpacks), depth-testing cleanly behind closed eyelids during blink animations.
- **Subtle Colored Ground/Area Light**: `MajestyAuraLightRenderer` projects a ringless, center-weighted, terrain-conforming HDR additive light mesh (`RenderType.eyes`) across nearby block surfaces, while `IrisShaderBridge` tags each aura strand with Complementary/Euphoria colored-entity IDs (`50300..50312`) when Iris is present.

### 3. Showdown `ModifyDamage` Pipeline
Cobblemon's `BattleRegistry.packTeam()` embeds every `BattlePokemon`'s `uuid` into Showdown's `Pokemon` object (`pokemon.uuid`). `MajestyShowdown` installs a single static handler into `Battle.prototype.findBattleEventHandlers` for `onModifyDamage`. When `source.uuid` is registered for an active battle and `source.getTypes(false, true).includes(move.type)`, Showdown applies `this.chainModify(multiplier)` (`1.1x` default):
- Runs after `ModifySTAB`, crits, and effectiveness (`1.5x * 1.1x = 1.65x`, `2.0x * 1.1x = 2.20x`).
- `source.getTypes(false, true)` (`preterastallized = true`) includes active natural types (Mega Evolution, form changes, `Protean`/`Libero`) while excluding Tera-added types.
- Fixed-damage moves (`Seismic Toss`, `Night Shade`, `Super Fang`, `Counter`, OHKOs) return in Showdown prior to `ModifyDamage` and remain unaffected.
- Avoids Held Item and Ability slots completely, and cleans up per-battle UUID maps on battle completion.
