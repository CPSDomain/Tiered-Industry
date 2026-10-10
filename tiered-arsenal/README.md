# Tiered Arsenal

Weapons and armour companion mod to **Tiered Industries**. Forge 1.20.1, mod id `tieredarsenal`, author BobRoflza.

## Progression

| Tier | Material | Source | Sword dmg | Armour (H/C/L/B) | Toughness | Full-set bonus |
|---|---|---|---|---|---|---|
| Early | Copper | Vanilla copper ingots | 5.5 | 2/5/4/1 | 0 | - |
| Early-mid | Emerald | Vanilla emeralds | 6.5 | 2/6/5/2 | 0.5 | Hero of the Village |
| Mid | Cobalt | **New ore**, Overworld y -48 to 32 | 6.5 | 3/6/5/2 | 1 | Haste I |
| Mid-high | Titanium | **New ore**, Overworld y -64 to -16 | 7.5 | 3/8/6/3 | 2.5 | Speed I |
| High | Infernium | **New ore**, Nether (netherrack) | 8.5 | 3/8/7/3 | 3 | Fire Resistance |
| End game | Aetherium | **New ore**, outer End islands only | 9.5 | 4/9/7/4 | 4 | Resistance I + no fall damage |

For reference: iron sword 6, diamond 7, netherite 8. Diamond armour 3/8/6/3 toughness 2, netherite toughness 3.

## How gear is made (tiered upgrades)

Copper and Emerald gear is crafted normally at the crafting table.

Everything from Cobalt up is made in the **smithing table** by upgrading the tier below, the same way netherite works:

```
Iron  --[Cobalt Upgrade + cobalt ingot]-->  Cobalt
Cobalt  --[Titanium Upgrade + titanium ingot]-->  Titanium
Titanium  --[Infernium Upgrade + infernium ingot]-->  Infernium
Infernium  --[Aetherium Upgrade + aetherium ingot]-->  Aetherium
```

Enchantments, custom names and armour trims carry over on every upgrade.

Upgrade templates:
- **First one:** 8 ingots of the new tier around a storage block of the previous tier (iron block, cobalt block, titanium block, infernium block).
- **Copying:** template + 7 ingots + stone (cobbled deepslate for cobalt/titanium, netherrack for infernium, end stone for aetherium) gives 2 templates.

Mining: cobalt needs iron pickaxe, titanium and infernium need diamond, aetherium needs netherite.
Infernium and Aetherium items survive lava and fire like netherite.

Each material has: sword, axe, helmet, chestplate, leggings, boots. New ores also give raw ore, ingot, raw block and storage block, with smelting/blasting and silk touch/fortune support. Armour is trimmable.

## Weapon types

Besides swords and axes, every tier has five more weapons. They also come in **Iron**, which starts their upgrade chain (Iron to Cobalt to Titanium to Infernium to Aetherium, same templates).

| Weapon | Damage (Iron / Aetherium) | Attack speed | Special |
|---|---|---|---|
| Greatsword | 8 / 11.5 | 1.1 | Heavy, sweeping hits |
| Dagger | 4 / 7.5 | 2.2 | 1.5x damage when hitting from behind |
| Spear | 5 / 8.5 | 1.4 | +1.5 blocks reach, no sweep |
| War Hammer | 9 / 12.5 | 0.8 | Shockwave: 40% damage to mobs within 2.5 blocks of the target |
| Bow | arrows +0.5 / +3 | - | Durability 465 / 1500 (vanilla bow 384, arrows 2) |

Crafting (Copper, Emerald, Iron; X = ingot or emerald, S = stick, T = string):

```
Greatsword   Dagger   Spear    War Hammer   Bow
 X           X          X     XXX           XT
 X           S         S      XSX         S  T
XSX                   S        S            XT
```

## Gear modules

Modules add extra effects to Tiered Arsenal weapons and armour. Fit and remove them at the **Armoury Workbench**.

**Slots per piece:** Copper, Emerald and Iron have 1. Cobalt has 2, Titanium 3, Infernium 4 and Aetherium 5. Modules stay on through smithing upgrades, so moving up a tier gives you more free slots. Each module fits once per piece.

| Module | Fits | Effect | Recipe (Blank Module +) |
|---|---|---|---|
| Lifesteal | Weapons | Heals 15% of damage dealt | Ghast Tear, Glistering Melon Slice |
| Fire | Weapons | Sets the target on fire for 4 s | Blaze Powder, Fire Charge |
| Knockback | Weapons | +1 knockback | Piston, Slime Ball |
| Looting | Weapons | +1 Looting | Rabbit's Foot, Emerald |
| Vitality | Armour | +2 hearts per piece | Golden Apple |
| Bulwark | Armour | +10% knockback resistance per piece | Obsidian, Shield |
| Step Assist | Boots | Walk up full blocks | Feather, any stairs |
| Aqua Lung | Helmets | Water breathing under water | Pufferfish, Prismarine Crystals |
| Thorns | Armour | Melee attackers take 10% of the damage back, per piece | Cactus, Sweet Berries |
| Regeneration | Chestplates | Regeneration I while hurt | Ghast Tear, Golden Carrot |
| Swiftness | Leggings | +10% walking speed | Sugar, Rabbit Hide |
| Auto-Smelt | Tools | Mined blocks drop their smelted result | Blast Furnace, Blaze Rod |
| Vein Miner | Tools | Sneak-mine an ore or log to break up to 32 connected blocks | Iron Pickaxe, Observer |
| Celestial | Weapons and armour | Weapons +2 damage; armour +1 armour and +1 toughness | Guardian Core (boss drop) |

**Blank Module (×4):** gold ingots in the corners, redstone on the edges, a diamond in the middle.

**Armoury Workbench:**
- It runs on Forge Energy, which goes in through the **bottom face only**. It stores 100,000 FE and takes up to 2,000 FE/t.
- Fitting a module costs 10,000 FE. Removing one is free and gives the module back.
- Put the gear in the top slot and a module in the slot below, then press **Install**. To remove a fitted module, press the **x** next to it.
- **Recipe:** with Tiered Industries, steel plates, an anvil, a smithing table, copper cables and a redstone block. Without TI, iron, redstone, an anvil, a smithing table and a redstone block, powered by any mod's FE.

## Shields

Every tier has a shield, which blocks like the vanilla shield (336 durability).

- **Copper (250 durability) and Emerald (420):** crafted from 5 ingots or emeralds plus a plank: `X W X / X X X / _ X _`.
- **Cobalt (600), Titanium (900), Infernium (1,200) and Aetherium (1,800):** upgraded in the smithing table, starting from a vanilla shield.

The top three do something to a melee attacker when you block:

| Shield | When you block a hit |
|---|---|
| Titanium | The attacker is knocked back |
| Infernium | The attacker is set on fire for 4 s |
| Aetherium | 25% of the blocked damage is reflected |

Banner patterns can't be put on tiered shields.

## Throwing spears

Hold right-click for at least half a second with a spear and let go to throw it, like a trident.

- It hits for 1.25x its melee damage, and Sharpness and Smite count.
- It then drops and can be picked up again; spears you can pick up never despawn.
- Each throw costs 1 durability.

## Config

`config/tieredarsenal-common.toml` holds the balance settings. Changes need a restart.

| Setting | Default |
|---|---|
| `setBonuses` (full-set bonuses on or off) | true |
| `daggerBackstabMultiplier` | 1.5 |
| `warHammerSplash` | 0.4 |
| `spearThrowMultiplier` | 1.25 |
| `aetheriumShieldReflect` | 0.25 |
| `lifesteal` | 0.15 |
| `chestLoot` | true |
| `mobGearChance` | 0.03 |

## Tools

There's a pickaxe, shovel and hoe for every tier, alongside the axes.

- **Copper and Emerald:** crafted like iron tools.
- **Cobalt and up:** upgraded in the smithing table, starting from vanilla iron tools.

| Tier | Mines like | Durability |
|---|---|---|
| Copper | Stone | 200 |
| Emerald, Cobalt | Iron | 900, 1,100 |
| Titanium | Diamond | 2,000 |
| Infernium | Netherite (can mine Aetherium Ore) | 2,400 |
| Aetherium | Above netherite | 3,200 |

## Crossbows and special arrows

**Crossbows** come in six tiers and work like the vanilla crossbow, with more durability (350 to 1,600) and extra arrow damage (+0 to +3).

- **Copper and Emerald:** `S X S / T J T / _ S _` (stick, ingot or emerald, string, tripwire hook).
- **Cobalt and up:** upgraded starting from a vanilla crossbow.

**Arrows** work in any bow or crossbow:

| Arrow | Recipe | Effect |
|---|---|---|
| Explosive | 2 arrows + gunpowder = 2 | Small explosion where it lands; breaks blocks only if mobGriefing is on |
| Fire | 8 arrows around Blaze Powder = 8 | Flies burning, sets targets on fire |
| Heavy | 2 arrows + iron ingot = 2 | +1.5 damage, extra knockback |

## Gunpowder

Any coal or charcoal, a flint and a bone meal (shapeless) make 2 gunpowder.

## Found in the world

**Structure chests** can hold Tiered Arsenal loot:
- Dungeons and mineshafts: Cobalt.
- Strongholds and ancient cities: Titanium and Blank Modules.
- Nether fortresses and bastions: Infernium.
- End cities: Aetherium.
- Pillager outposts: explosive arrows and a Cobalt crossbow.

**Mobs:**
- About 3% of Overworld zombies and skeletons spawn with a Copper, Cobalt or (rarely) Titanium armour piece, and zombies may carry a sword or dagger.
- Wither skeletons sometimes carry an Infernium Sword.

Config: `chestLoot` (on or off) and `mobGearChance` (0 turns it off).

## Enchantments

These come from the enchanting table and on books:

| Enchantment | Goes on | Max | Effect per level |
|---|---|---|---|
| Shockwave | War hammers | III | Shockwave +0.5 blocks radius and +10% damage |
| Assassin | Daggers | III | Backstab +0.25x damage |
| Reach | Spears | II | +0.5 blocks reach |
| Counter | Any shield | III | Blocking a melee hit deals 2 damage to the attacker |

## Armour trim materials

Cobalt, Titanium, Infernium and Aetherium ingots work as trim materials in the smithing table, on any armour, vanilla or Tiered Arsenal. The trim shows in the material's colour when worn. Item icons of vanilla armour don't show these new trim colours; that's a limit of how vanilla draws trimmed icons.

## Quiver and tool belt

Both store items like a bundle: right-click items onto them, or right-click them onto an empty slot to take the first stack out.

- **Quiver** (leather, string, arrow): holds 4 stacks of any arrows.
  - Right-click it in the air to pack every arrow in your inventory into it.
  - When a bow or crossbow finds no arrows in your inventory, the quiver hands over its next stack.
- **Tool belt** (leather, a chest, an iron ingot): holds 4 tools or weapons.
  - Press **G** (Controls > Tiered Arsenal) to swap your held item for the next one on the belt.

## Aetherium Guardian (boss)

**Summoning:** make a **Void Sigil** (4 Eyes of Ender, 4 Aetherium Ingots, an End Crystal) and use it on a block in the End. There's also a spawn egg for creative.

**Stats and attacks:**
- 300 health, 12 armour; immune to fire, fall damage and knockback.
- A shockwave up close (8 damage + knockback every 6 s).
- Homing shulker bullets at range.
- It teleports next to you if you run more than 16 blocks away.

**Drops:** 6 to 10 Aetherium Ingots, 1 to 2 Aetherium Upgrade Templates and a **Guardian Core**. The Core makes the **Celestial** module (fits any weapon or armour): +2 damage on weapons, +1 armour and +1 toughness on armour.

## Annihilator (creative testing tool)

For testing spawns and high-health mobs. It's in the creative tab only and has no recipe.

- **Hit:** 1,000,000 damage, no attack cooldown. Kills count as yours, so drops and XP work.
- **Right-click:** kills every monster within 32 blocks.
- **Sneak + right-click:** kills every mob within 32 blocks except players and tamed pets.
- It never breaks and doesn't break blocks if you miss.

## Advancements

A Tiered Arsenal advancement tab walks players up the ladder:
- **Main line:** first new ore, Cobalt ingot, first smithing upgrade, Titanium, Infernium, Aetherium, and a full Aetherium set as a challenge.
- **Side branches:** the Armoury Workbench, then a gear module; and a tiered shield.

## JEI

With JEI installed, every Tiered Arsenal item has an info page, like the Tiered Industries ones. Press U on an item to see it. The pages cover where each ore spawns, how the upgrade templates work, what each weapon type and module does, and the workbench.

JEI is an optional dependency: the mod compiles against JEI's API (`jei_version` in `gradle.properties`) and runs fine without it. `gradlew runClient` starts with JEI loaded.

## Working with Tiered Industries

Tiered Arsenal works on its own. When Tiered Industries (mod id `tieredpower`) is installed, these recipes switch on automatically (all use Forge's `mod_loaded` condition):

| Machine | Input | Output |
|---|---|---|
| Pulverizer | Ore or raw ore | 2 dust |
| Pulverizer | Ingot | 1 dust |
| Ore Purifier / Chemical Washer | Ore or raw ore | 3 dust (Washer adds its usual extra) |
| Furnace / Blast furnace | Cobalt Dust | Cobalt Ingot |
| Alloy Smelter | 2 Titanium dust or raw + 1 Steel Ingot | 2 Titanium Ingots |
| Alloy Smelter | 2 Infernium dust or raw + 2 Blaze Powder | 2 Infernium Ingots |
| Alloy Smelter | 2 Aetherium dust or raw + 1 Superconductor Ingot | 2 Aetherium Ingots |

With TI installed, Titanium, Infernium and Aetherium **cannot be smelted in a furnace** - the Alloy Smelter is the only way. Cobalt stays furnace-smeltable as the entry tier.

Upgrade templates change with TI too (X = the tier's ingot, B = previous tier's block):

```
Titanium       Infernium            Aetherium
X P X          X S X                X Q X
P B P          S B S                S B S
X P X          X S X                X S X
P = Steel Plate   S = Superconductor Ingot   Q = Quantum Alloy Ingot
```

All materials also carry the standard Forge tags (`forge:ores/...`, `forge:raw_materials/...`, `forge:dusts/...`, `forge:ingots/...`), so other mods' machines can process them too.

## Building

Needs **JDK 17**.

```
gradlew build            (Windows)
./gradlew build          (Linux)
```

The jar lands in `build/libs/tieredarsenal-1.0.1.jar`. Drop it in `mods/` next to the Tiered Industries jar.
`gradlew runClient` starts a test client.

Set `forge_version` in `gradle.properties` to the same Forge version as Tiered Industries.

## Changing materials or textures

Stats live in Java:
- `material/ModToolTiers.java` - weapon damage, durability, enchantability
- `material/ModArmorMaterials.java` - armour values, toughness, durability
- `event/SetBonusHandler.java` - full-set bonuses

Everything under `src/main/resources` (textures, models, recipes, loot, tags, worldgen, lang) is generated by `tools/gen_resources.py` from one material table. Edit it and run `python tools/gen_resources.py` (needs `pip install pillow`). The textures are generated pixel art: each ore has its own pattern, and Cobalt and up get studs, gems or gold trim. Swap any PNG for hand-drawn art.
