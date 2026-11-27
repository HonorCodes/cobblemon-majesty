# Architecture

## Module Overview

| Package | Side | Responsibility |
| :--- | :--- | :--- |
| `codes.honor.cobblemonmajesty` | Common | `@Mod` entrypoint (`CobblemonMajesty`), fault-isolating `Guard`, lifecycle wiring |
| `codes.honor.cobblemonmajesty.config` | Common | `MajestyConfig` (`cobblemon_majesty-common.toml`) for odds, IV promotion, source toggles, and combat toggle |
| `codes.honor.cobblemonmajesty.feature` | Common | `GlobalSpeciesFeatures` registration (`majestic` `FlagSpeciesFeatureProvider`), aspect sync, idempotent roll marker (`cobblemon_majesty_rolled`), `+N` additional 31 IV selection |
| `codes.honor.cobblemonmajesty.spawning` | Common | Event subscribers for wild spawns, fishing, PokéSnacks, fossils, starters, egg exclusion, and `ActorType.NPC` trainer team rolls |
| `codes.honor.cobblemonmajesty.battle` | Common | `MajestyShowdown` GraalJS bridge registering per-battle Majestic `Pokemon.uuid` sets and hooking Showdown `ModifyDamage` |
| `codes.honor.cobblemonmajesty.palette` | Common | 18-type emissive primary + accent defaults, non-averaging dual-strand resolver, and `EyeBoneClassifier` |
| `codes.honor.cobblemonmajesty.sound` | Common | `DeferredRegister` for `cobblemon_majesty:majestic_presence` |
| `codes.honor.cobblemonmajesty.client` | `Dist.CLIENT` | `MajestyClientConfig`, Iris-compatible `RenderType` shards, base wisp + comet-ember + star + polar gradient halo renderer, `PosableModel` shader eye glow, terrain-conforming HDR area light + Iris colored-entity bridge, wild notice chime, world/battle/Summary `✦` badges |
| `codes.honor.cobblemonmajesty.mixin` | Common & Client | `GraalShowdownServiceMixin` (server/common) and 5 client-only rendering/UI mixins isolated under `"client"` in `cobblemon_majesty.mixins.json` |

## Key Design Decisions

### 1. Variant Stacking via `GlobalSpeciesFeatures`
Texture variants in Cobblemon compete for `VaryingModelRepository` texture slots. By registering `majestic` as a `FlagSpeciesFeatureProvider` (`isAspect = true`) in `GlobalSpeciesFeatures` with no `ModelVariationSet` overrides, `Pokemon` instances persist `majestic: Boolean` across NBT, JSON, PC, Party, Pastures, and network sync (`PokemonEntity.ASPECTS` and `RenderablePokemon.aspects`) while leaving Shiny, Lucky, Alpha, Mega, and resource-pack textures untouched. `MajestyFeature.isMajestic` checks both `FlagSpeciesFeature` and `forcedAspects`/`aspects` so static trainer definitions (`"aspects": ["majestic"]`) receive both client rendering and server Showdown bonuses.

### 2. Shader-Compatible Aura, Eye Glow, and Area Lighting
- **Particles & Steady Gradient Halo**: `MajestyRenderTypes` builds static billboard `RenderType`s on `RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER` with `TRANSLUCENT_TRANSPARENCY` and full-bright lightmaps (`15, 15`) so BSL and Complementary natively bloom wisps, comet-embers, stars, and the 28-segment polar-interpolated dual-type ground halo without washing dark types (`Dark`, `Ghost`) out to white.
- **Shader Eye Glow**: `MajestyEyeGlowRenderer` builds cached pre-multiplied grayscale eye masks from each Pokémon's resolved `ResourceLocation` and renders open `pupil`/`iris`/`eye` `ModelPart` cubes (with an `"eye"` locator fallback) through `RenderType.eyes`, which maps directly to `gbuffers_spidereyes` in Iris shaderpacks while depth-testing cleanly behind closed eyelids during blink animations.
- **Subtle Colored Ground/Area Light**: `MajestyAuraLightRenderer` projects a low-intensity, terrain-conforming pre-multiplied HDR additive light mesh (`RenderType.eyes`) across nearby block surfaces, while `IrisShaderBridge` tags each aura strand with Complementary/Euphoria colored-entity IDs (`50300..50312`) when Iris is present.

### 3. Showdown `ModifyDamage` Pipeline
Cobblemon's `BattleRegistry.packTeam()` embeds every `BattlePokemon`'s `uuid` into Showdown's `Pokemon` object (`pokemon.uuid`). `MajestyShowdown` installs a single static handler into `Battle.prototype.findBattleEventHandlers` for `onModifyDamage`. When `source.uuid` is registered for an active battle and `source.getTypes(false, true).includes(move.type)`, Showdown applies `this.chainModify(multiplier)` (`1.1x` default):
- Runs after `ModifySTAB`, crits, and effectiveness (`1.5x * 1.1x = 1.65x`, `2.0x * 1.1x = 2.20x`).
- `source.getTypes(false, true)` (`preterastallized = true`) includes active natural types (Mega Evolution, form changes, `Protean`/`Libero`) while excluding Tera-added types.
- Fixed-damage moves (`Seismic Toss`, `Night Shade`, `Super Fang`, `Counter`, OHKOs) return in Showdown prior to `ModifyDamage` and remain unaffected.
- Avoids Held Item and Ability slots completely, and cleans up per-battle UUID maps on battle completion.
