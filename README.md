# Cobblemon Majesty

**Cobblemon Majesty** introduces **Majestic** Pokémon variants for **Cobblemon 1.8.4** on **NeoForge 1.21.1**.

Majestic is an independent global variant flag (`majestic`) that stacks seamlessly with **Shiny**, **Alpha**, **Lucky**, Mega Evolution, Terastallization, and custom model/texture packs. It inverts a Pokémon's resolved base palette at runtime, surrounds it with a dual-strand elemental aura in its own type colors, adds shader-blooming eyes, subtle aura-tinted ground lighting, a custom Majestic sparkle SFX, a **Majesty Orb** item for awakening captured Pokémon, and a configurable **1.1x final damage bonus** on moves matching its active natural types.

## Features

- **100% Variant Stacking + Runtime Color Inversion**: Registered as a synchronized global `FlagSpeciesFeature` and aspect (`majestic`) with zero static `ModelVariationSet` overrides. On the client, `VaryingModelRepository` dynamically caches an RGB-inverted `DynamicTexture` of whatever skin Cobblemon resolved—so Shiny, Lucky, Alpha, Mega, and custom resource-pack skins all invert into unique palettes (toggleable via `aura.invertBaseColors`).
- **Majesty Orb (`cobblemon_majesty:majesty_orb`)**: A pearlescent orb usable on any captured Pokémon you own—either by right-clicking your sent-out Pokémon directly or right-clicking in the air to pick from Cobblemon's party selector—awakening its Majestic aura and promoting `+2` additional `31` IVs.
- **Custom Majestic Sparkle & Discovery SFX**: Plays a custom crystalline bell-cascade SFX (`majestic_sparkle`) whenever a Majestic Pokémon emerges from a Poké Ball or awakens via a Majesty Orb, plus a resonant discovery chime (`majestic_presence`) when spotting a wild Majestic Pokémon.
- **Non-Averaged Dual-Type Auras & Ground Sweeps**: Dual-type Pokémon weave both of their elemental type colors as low ground-skimming helical filaments, fine ground spark motes, silhouette-framing wisps, rising comet-embers, and 4-point star sparkles (for example, a Steel/Fighting Lucario radiates distinct Metallic Silver and Martial Red strands rather than a blended pink average). Mono-type Pokémon pair their primary aura hue with a harmonic accent shade.
- **Shader-Compatible Eye Glow & Ringless Area Light**: Renders emissive eye glow via `RenderType.eyes` (`gbuffers_spidereyes` HDR bloom in BSL and Complementary, respecting Cobblemon blink animations) and casts a soft, ringless aura-colored ground light pool onto surrounding terrain (including per-strand colored entity light tags for Iris and Complementary/Euphoria voxel lighting).
- **Server-Side Showdown Mastery**: Hooks Cobblemon's server-side Pokémon Showdown `ModifyDamage` stage (`1.1x` default, stacking multiplicatively after STAB: `1.5x -> 1.65x`, `2.0x -> 2.20x`) for moves matching active natural types (including Mega Evolution, form swaps, and Protean/Libero, while excluding Tera-added types and fixed-damage moves). Can also be toggled to purely aesthetic mode in the config.
- **Natural & Trainer Encounters**: Rolls `1 in 6,000` by default across wild spawns, fishing, PokéSnacks, fossils, starters, and NPC/Trainer teams (including Radical Cobblemon Trainers), granting `+2` additional `31` IVs on top of existing perfect IVs and displaying `✦` badges on world nameplates, battle HUD tiles, and the Summary screen.

## Commands & Items

Majestic works anywhere Cobblemon `PokemonProperties` or `aspects` are accepted, or via the **Majesty Orb**:

```text
/give <player> cobblemon_majesty:majesty_orb
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
- `spawning.additionalPerfectIvs` (`2`) — Number of non-`31` permanent IVs promoted to `31` when Majestic is rolled or awakened via a Majesty Orb.
- `spawning.wildSpawns`, `fishingSpawns`, `snackSpawns`, `fossils`, `starters`, `trainerPokemon` (`true`) — Per-source roll toggles. Bred eggs never inherit or roll Majestic.
- `combat.damageBonusEnabled` (`true`) — Enables the Showdown final damage multiplier (`false` makes Majestic purely aesthetic in battle).
- `combat.damageMultiplier` (`1.1`) — Final damage multiplier applied after STAB on moves matching active natural types.

### `config/cobblemon_majesty-client.toml`

- `aura.enabled` (`true`) — Master client switch for Majestic aura visuals and local presence/send-out chimes.
- `aura.invertBaseColors` (`true`) — Inverts the resolved base model texture of Majestic Pokémon in the world and 3D portraits.
- `aura.groundParticlesEnabled`, `starMotesEnabled`, `glowingEyesEnabled`, `auraLightEnabled`, `wildChimeEnabled`, `sendOutSfxEnabled`, `nameplateBadgeEnabled`, `summaryBadgeEnabled` (`true`).
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
