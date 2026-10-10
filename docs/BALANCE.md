# Tiered Industries - balance reference (1.69.0)

Default config values. Two global knobs scale the whole mod: `balance.generationMultiplier` (everything generators
make) and `balance.machineEnergyMultiplier` (everything machines use), in `config/tieredpower-common.toml`.
Note: changing a default in a new version doesn't change an existing config file - delete the line (or the file) to
pick up a new default.

## Generation ladder
| Stage | Output | Energy per resource | Notes |
|---|---|---|---|
| Water Wheel | 8 FE/t per side with flowing water, max 32 | free | Cheap first step |
| Coal Generator | 40 FE/t | 64,000 FE per coal | Any furnace fuel |
| Solar Panel (6 tiers) | 20 / 100 / 450 / 2,000 / 9,000 / 36,000 (Stellar) FE/t | free | Day only, half in rain; each tier = 4 of the one below |
| Geothermal Generator | 20 FE/t per touching lava source, 5 per magma, max 120 | free | |
| Wind Turbine | 0 at Y 64 -> 128 FE/t at Y 256 | free | x1.5 rain, x2 storm, needs open space |
| Boiler + Steam Turbine | 200 FE/t per turbine | 320,000 FE per coal | Heat source underneath gives 50 mB/t steam free |
| Steam Engine | 40 FE/t | 1 FE per mB steam | |
| Gas Burner Generator | 280-600 FE/t | H2 7 FE/mB, methane 12 FE/mB (+O2 bonus) | Bio-Digester: 200 mB methane per item |
| Diesel Generator | 200 (biodiesel) / 300 (rocket fuel) / 40 (creosote) | 1 mB/t | |
| Industrial Turbine (multiblock) | 200 mB/t steam per rotor x 2 FE/mB x 1.3-2.0 | condenses 1 mB water per 10 mB steam | Up to 128 rotors count: max ~102,400 FE/t |
| Fission Reactor (single) | up to 3,200 FE/t (4 rods), MOX + cryo 7,200 | 4.8M FE per rod | |
| Fission Reactor (multiblock) | 1,000 FE/t per Fuel Assembly, +10% per neighbour | 6M+ FE per rod | |
| Compact Fusion Reactor | 8,000 FE/t (x1-x4 with Plasma Coils) | ~19M FE per fuel pair | 1M FE to ignite |
| Fusion Reactor (multiblock) | 4,000 + 1,500 per Magnet Coil (x1-x4 plasma) | ~100M FE per pair | 5M FE to ignite; ports push 512k/side |
| Lightning Collector | 2,500,000 FE per strike | Storm Caller: 4M FE per storm | |
| Receiver Dish (Solar Satellite) | 100,000 FE/t | one satellite | Day and night |
| Antimatter Reactor | up to 1,000,000 FE/t | 50,000 FE per mB | Collider spends 20,000 FE per mB (2.5x return) |

## Fuel makers
| Machine | Cost | Makes |
|---|---|---|
| Isotope Separator | 200 FE/t + 100 mB/t water | 2 mB/t deuterium (100 FE per mB) |
| Electrolyzer | 20,000 FE + 1,000 mB water | Deuterium cell (= 250 mB of gas) |
| Tritium Breeder | lithium + a running fission reactor | 250 mB tritium per lithium |
| Particle Collider | 20,000 FE + 10 mB D + 10 mB T | 1 mB antimatter |

## Storage
| | Basic | Advanced | Elite | Ultimate | Quantum |
|---|---|---|---|---|---|
| Battery Box | 1M (8k/t) | 8M (32k/t) | 32M (128k/t) | 256M (512k/t) | 2,000M (2,048k/t) |
| Energy Cell (Energy Bank) | 32M | 256M | - | 2,048M | 16,384M |

- Energy Bank: up to 1,331 cells, ~21.8 trillion FE with Quantum cells; 1M FE/t per port.
- Energy Core: 20B / 100B / 1T / 10T / 100T FE (tiers I-V); pylons move 1M / 4M / 16M / 64M / 256M FE/t each.

## Transport
| | Copper / Basic | Gold / Advanced | Diamond / Elite | Netherite / Ultimate | Quantum |
|---|---|---|---|---|---|
| Cable (FE/t per connection) | 1,000 | 8,000 | 32,000 | 128,000 | 2,048,000 |
| Fluid / Gas Pipe (mB/t) | 1,000 | 4,000 | 16,000 | 64,000 | 256,000 |
| Item Pipe (items per pull) | 8 | 32 | 128 | 512 | 2,048 |

## Machines
- Energy per tick = base x (1 + 0.75 x Speed) x max(0.15, 0.85^Energy) x lanes. Progress per tick = 1 + 0.5 x Speed.
- Up to 8 Speed and 8 Energy upgrade cards; Mk II cards count double (max 16 levels). Tier Installers: 1 / 3 / 5 / 7 / 9 / 12 lanes (Basic to Naquadah).
- A machine's power intake grows with its tier, Speed upgrades and overclock, so upgraded machines never starve.
- Assembly Matrix Overclock Accelerators: +2 Speed levels per accelerator (max +8) for machines the Matrix crafts with.
- Chunk Loader: 20 FE/t per chunk; 5x5 (Basic) up to 19x19 (Naquadah).
- Spatial Projector: 40 FE per block. Drone Station: 20 FE/t per working drone.

## Planet materials (1.61)
| Planet | Material | Use |
|---|---|---|
| Frost | Cryonite | Block = 3 fission coolant channels; Aether Stabilizer |
| Dune | Solarite Sand -> Solar Glass | Stellar Solar Panel |
| Verdant | Livingwood | Planks; 16 Fertilizer per log |
| Inferno | Naquadah | Naquadah Installer (12 lanes), Mk II upgrades, Stellar panel, 8 gate frames per 4 ingots |
| Abyss | Abyssal Pearl | 10 min water breathing + night vision + dolphin's grace |
| Skylands | Aether Crystal | Aether Stabilizer (flight 25 FE/t instead of 100) |
| Mycelia | Sporecap | 2,000 mB methane each in the Bio-Digester |
| Crystal Depths | Resonance Crystal | Mk II upgrades, Naquadah Installer |

## Stargate (1.62)
- Dial from home: 50 billion FE, then 1 million FE/t while open (a hand-dialled gate: 30 s = 600M FE). From a planet: free.
- DHD holds 100 billion FE.
- Gate Interface: 32 items, 8,000 mB and 8M FE per tick per Send interface; buffers 9 slots, 64,000 mB, 32M FE.
- Address tablet chances per chest: stronghold library / mansion / bastion treasure 50%, end city 40%, ancient city / buried treasure 35%, jungle temple / igloo 30%, desert temple 25%, shipwreck treasure 20%, outpost / corridor / ocean ruins 15%, dungeon 12%, ruined portal / fortress 10%, mineshaft 6%. Plus one in every new landing platform's chest.

## Digital Factory (1.69)
- 4 operations at once per Factory Cell, up to 16 cells (64 at once). 100-tick cycle, faster with Speed upgrades.
- FE/t per operation matches the single machines: Smelt 20, Crush 40, Wash 60, Ore to Ingots 80. Wash uses 250 mB water per ore (as the Ore Purifier).
- Compare: a Naquadah-tier Pulverizer does 12 at once. The factory is the bulk option, not a cheaper one.

## Progression path (1.69 audit)
Crafting depth = steps from vanilla materials, checked for every item.
1. Start (depth 1-2): Coal Generator, Coke Oven -> Industrial Blast Furnace (steel), Copper Cable, Battery Box, basic pipes and tanks, Item Magnet, Storage disks.
2. Early machines (depth 3): Pulverizer, Electric Furnace, Alloy Smelter, Compressor, Sawmill, Rock Crusher, Electric Pump, Boiler + Steam Engine, Geothermal, Water Wheel, basic Solar, Wireless Redstone, Machine Status Display, Elevator.
3. Mid (depth 4): Ore Purifier, Steam Turbine, Wind Turbine, Storage Controller and Terminal, Autocrafting (CPU, Assembler, Pattern Encoder), Quarry, Chunk Loader, Advanced and Elite Installers, Speed/Energy upgrades, Factory Cells and Ports, Fission parts.
4. Mid-late (depth 5): Digital Factory, Chemical Washer, Fission Reactor, Industrial Turbine, Matrix, Teleporter, Digital Miner, Ultimate Installer, Quantum Alloy.
5. Late (depth 6-8): Quantum tier (cables, pipes, installers, cells), Fusion, Rocket program, Laser Drill, Spatial Projector, Wireless terminals, Quantum Power Core.
6. End (depth 9+): Antimatter, Energy Core, Quantum Suit, Particle Collider, Stargate -> planets (Naquadah tier, Mk II upgrades, Stellar panels, suit modules) -> Gate Interfaces, hidden worlds, guardians.
- No pre-planet item needs a planet material; planet items only need materials from planets you can already reach.
