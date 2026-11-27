# Cobblemon Majesty

**Cobblemon Majesty** introduces **Majestic** Pokémon variants for **Cobblemon 1.8.4** on **NeoForge 1.21.1**.

Majestic is an independent global variant flag (`majestic`) that stacks seamlessly with **Shiny**, **Alpha**, **Lucky**, Mega Evolution, Terastallization, and custom model/texture packs. Rather than replacing a Pokémon's skin, it renders an animated, dual-strand elemental aura in the Pokémon's own type colors, shader-blooming eyes, subtle aura-tinted ground lighting, and a configurable **1.1x final damage bonus** on moves matching its active natural types.

## Features

- **100% Variant Stacking**: Registered as a synchronized global `FlagSpeciesFeature` and aspect (`majestic`) with zero `ModelVariationSet` overrides—every Shiny, Lucky, Alpha, or resource-pack texture stays intact underneath.
- **Non-Averaged Dual-Type Auras**: Dual-type Pokémon weave both of their elemental type colors as distinct low silhouette-framing base wisps, rising comet-embers, 4-point star sparkles, and a smoothly merged steady polar gradient ground halo (for example, a Steel/Fighting Lucario radiates distinct Metallic Silver and Martial Red particles rather than a blended pink average). Mono-type Pokémon pair their primary aura hue with a harmonic accent shade.
- **Shader-Compatible Eye Glow & Area Light**: Renders emissive eye glow via `RenderType.eyes` (`gbuffers_spidereyes` HDR bloom in BSL and Complementary, respecting Cobblemon blink animations) and casts subtle aura-colored ground and area illumination onto surrounding terrain (including per-strand colored entity light tags for Iris and Complementary/Euphoria voxel lighting).
- **Server-Side Showdown Mastery**: Hooks Cobblemon's server-side Pokémon Showdown `ModifyDamage` stage (`1.1x` default, stacking multiplicatively after STAB: `1.5x -> 1.65x`, `2.0x -> 2.20x`) for moves matching active natural types (including Mega Evolution, form swaps, and Protean/Libero, while excluding Tera-added types and fixed-damage moves). Can also be toggled to purely aesthetic mode in the config.
- **Natural & Trainer Encounters**: Rolls `1 in 6,000` by default across wild spawns, fishing, PokéSnacks, fossils, starters, and NPC/Trainer teams (including Radical Cobblemon Trainers), granting `+2` additional `31` IVs on top of existing perfect IVs, a resonant wild encounter chime, and `✦` badges on world nameplates, battle HUD tiles, and the Summary screen.

## Commands & Properties

Majestic works anywhere Cobblemon `PokemonProperties` or `aspects` are accepted:

```text
/pokegive <player> lucario shiny majestic
/pokegive <player> charizard alpha majestic
/pokespawn gengar lvl=50 majestic=true
/majesty status
```

In trainer JSON definitions (such as `rctmod`), add `"majestic"` to a team member's `"aspects"` array to guarantee a Majestic boss or champion ace.

## Configuration

### `config/cobblemon_majesty-common.toml`

- `spawning.enabled` (`true`) — Master toggle for natural and trainer Majestic rolls.
- `spawning.oneIn` (`6000`) — Natural spawn odds (`1 in N`; `0` disables random natural rolls).
- `spawning.trainerOneIn` (`6000`) — NPC and trainer Pokémon roll odds (`1 in N`; `0` disables random trainer rolls).
- `spawning.additionalPerfectIvs` (`2`) — Number of non-`31` permanent IVs promoted to `31` when Majestic is rolled.
- `spawning.wildSpawns`, `fishingSpawns`, `snackSpawns`, `fossils`, `starters`, `trainerPokemon` (`true`) — Per-source roll toggles. Bred eggs never inherit or roll Majestic.
- `combat.damageBonusEnabled` (`true`) — Enables the Showdown final damage multiplier (`false` makes Majestic purely aesthetic in battle).
- `combat.damageMultiplier` (`1.1`) — Final damage multiplier applied after STAB on moves matching active natural types.

### `config/cobblemon_majesty-client.toml`

- `aura.enabled` (`true`) — Master client kill switch for all Majestic visuals and audio.
- `aura.groundHaloEnabled`, `starMotesEnabled`, `glowingEyesEnabled`, `auraLightEnabled`, `wildChimeEnabled`, `nameplateBadgeEnabled`, `summaryBadgeEnabled` (`true`).
- `aura.auraLightIntensity` (`1.0`) — Relative brightness scale for the subtle colored ground and area light.
- `aura.fullAuraDistance` (`32.0`), `maxFullAurasOnScreen` (`16`).
- `palette.<type>.primary` and `palette.<type>.accent` — Custom `#RRGGBB` hex colors for all 18 Pokémon types.

## Building

Requires JDK 21 and Node.js 20+:

```bash
node tools/assets/generate.mjs --check
./gradlew build
```

## License

Released under the [Mozilla Public License 2.0](LICENSE).
