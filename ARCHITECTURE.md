# Architecture

## Module Overview

| Package | Side | Responsibility |
| :--- | :--- | :--- |
| `codes.honor.cobblemonmajesty` | Common | `@Mod` entrypoint (`CobblemonMajesty`), fault-isolating `Guard`, lifecycle wiring |
| `codes.honor.cobblemonmajesty.config` | Common | `MajestyConfig` (`cobblemon_majesty-common.toml`) for odds, IV promotion, source toggles, and combat toggle |
| `codes.honor.cobblemonmajesty.feature` | Common | `GlobalSpeciesFeatures` registration (`majestic` `FlagSpeciesFeatureProvider`), aspect sync, idempotent roll marker (`cobblemon_majesty_rolled`), `+N` additional 31 IV selection |
| `codes.honor.cobblemonmajesty.spawning` | Common | Event subscribers for wild spawns, fishing, PokéSnacks, fossils, starters, egg exclusion, and `ActorType.NPC` trainer team rolls |
| `codes.honor.cobblemonmajesty.battle` | Common | `MajestyShowdown` GraalJS bridge registering per-battle Majestic `Pokemon.uuid` sets and hooking Showdown `ModifyDamage` |
| `codes.honor.cobblemonmajesty.palette` | Common | 18-type emissive primary + accent defaults and non-averaging dual-strand resolver |
| `codes.honor.cobblemonmajesty.sound` | Common | `DeferredRegister` for `cobblemon_majesty:majestic_presence` |
| `codes.honor.cobblemonmajesty.client` | `Dist.CLIENT` | `MajestyClientConfig`, Iris-compatible `RenderType` shards, silhouette wisp/star/halo renderer, `PosableModel` dual-vein sheen, wild notice chime, world/battle/Summary `✦` badges |
| `codes.honor.cobblemonmajesty.mixin` | Common & Client | `GraalShowdownServiceMixin` (server/common) and 5 client-only rendering/UI mixins isolated under `"client"` in `cobblemon_majesty.mixins.json` |

## Key Design Decisions

### 1. Variant Stacking via `GlobalSpeciesFeatures`
Texture variants in Cobblemon compete for `VaryingModelRepository` texture slots. By registering `majestic` as a `FlagSpeciesFeatureProvider` (`isAspect = true`) in `GlobalSpeciesFeatures` with no `ModelVariationSet` overrides, `Pokemon` instances persist `majestic: Boolean` across NBT, JSON, PC, Party, Pastures, and network sync (`PokemonEntity.ASPECTS` and `RenderablePokemon.aspects`) while leaving Shiny, Lucky, Alpha, Mega, and resource-pack textures untouched. `MajestyFeature.isMajestic` checks both `FlagSpeciesFeature` and `forcedAspects`/`aspects` so static trainer definitions (`"aspects": ["majestic"]`) receive both client rendering and server Showdown bonuses.

### 2. Iris-Compatible Dual-Strand Rendering
Iris overrides custom vanilla GLSL core `ShaderInstance`s with fallback entity programs unless shaderpacks explicitly bind them. `MajestyRenderTypes` instead builds static `RenderType`s on `RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER` with `TRANSLUCENT_TRANSPARENCY` (`SRC_ALPHA, ONE_MINUS_SRC_ALPHA`) and full-bright lightmaps (`15, 15`), which Complementary and BSL natively bloom without washing overlapping strands out to white or hiding dark-toned types (`Dark`, `Ghost`). Dynamic `TexturingStateShard` matrices scroll the 3D surface veins via `RenderSystem.setTextureMatrix` without allocating per-frame `RenderType` instances.

### 3. Showdown `ModifyDamage` Pipeline
 Cobblemon's `BattleRegistry.packTeam()` embeds every `BattlePokemon`'s `uuid` into Showdown's `Pokemon` object (`pokemon.uuid`). `MajestyShowdown` installs a single static handler into `Battle.prototype.findBattleEventHandlers` for `onModifyDamage`. When `source.uuid` is registered for an active battle and `source.getTypes(false, true).includes(move.type)`, Showdown applies `this.chainModify(multiplier)` (`1.1x` default):
- Runs after `ModifySTAB`, crits, and effectiveness (`1.5x * 1.1x = 1.65x`, `2.0x * 1.1x = 2.20x`).
- `source.getTypes(false, true)` (`preterastallized = true`) includes active natural types (Mega Evolution, form changes, `Protean`/`Libero`) while excluding Tera-added types.
- Fixed-damage moves (`Seismic Toss`, `Night Shade`, `Super Fang`, `Counter`, OHKOs) return in Showdown prior to `ModifyDamage` and remain unaffected.
- Avoids Held Item and Ability slots completely, and cleans up per-battle UUID maps on battle completion.
