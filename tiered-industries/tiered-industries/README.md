# Tiered Industries

Tiered power generation, storage, transport and automation for **Minecraft 1.20.1 / Forge 47+**. Everything runs on Forge Energy.
See [docs/MODPAGE.md](docs/MODPAGE.md) for the overview, [CHANGELOG.md](CHANGELOG.md) for history, and the in-game Tiered Industries Guide for details.

## Building
```
./gradlew build          # jar in build/libs/
./gradlew runClient      # dev client with JEI
```
Licensed under MIT - see [LICENSE](LICENSE).

---
## Development notes by phase

> These notes cover the early phases (up to 1.3.0). Everything since is in [CHANGELOG.md](CHANGELOG.md), which is also shown on the web guide's Changelog section.
# Tiered Industries (Forge 1.20.1)

Stage 1: Coal Generator, Battery Box, Electric Furnace, and four tiers of cable.
Everything uses **Forge Energy (FE)**, the shared power standard on Forge (same unit as RF),
so the cables carry power to and from machines in Mekanism, Thermal, Immersive Engineering,
Powah, Ender IO, AE2, Create add-ons that use FE, and so on.

## Build
1. Install **JDK 17** (e.g. Eclipse Temurin 17).
2. In this folder run `gradlew build` (Windows) or `./gradlew build`.
   The first build downloads and sets up Minecraft, so it takes a while.
3. The mod jar is in `build/libs/`. Put it in your `.minecraft/mods` folder (Forge 1.20.1 profile).

To test without installing: open the folder in IntelliJ IDEA, let Gradle import, then run `gradlew genIntellijRuns` and use the `runClient` configuration.

## Cables
| Cable | FE per tick | Recipe |
|---|---|---|
| Copper | 1,000 | 3 copper ingots in a row -> 6 |
| Gold | 8,000 | 8 copper cables around a gold ingot -> 8 |
| Diamond | 32,000 | 8 gold cables around a diamond -> 8 |
| Netherite | 128,000 | 8 diamond cables around a netherite ingot -> 8 |

All connected cables form one **network**: every tick the network pools the power, pulls from generators that don't push on their own,
and shares it fairly to every block that accepts power anywhere on the network. Length doesn't matter. Each connection to a machine
or battery is limited by the tier of the cable touching it (e.g. Netherite = 128,000 FE/t per connection). Tiers can be mixed.
Add or rebalance tiers in `energy/CableTier.java`.

## Machines
| Block | Capacity | Rate |
|---|---|---|
| Coal Generator | 50,000 FE | makes 40 FE/t from any furnace fuel (vanilla furnace burn time) |
| Battery Box | 1,000,000 FE | 8,000 FE/t in on 5 faces, out of the marked front |
| Electric Furnace | 20,000 FE | uses 20 FE/t, 100 ticks per item |

## Stage 2: Steam
**Steel Ingot**: smelt an iron ingot in a vanilla Blast Furnace. Tagged `forge:ingots/steel`, so other mods' steel works in these recipes.

**Boiler** (steel, bucket, furnace): burns any furnace fuel to boil water into steam.
- Heat: solid fuel or lava in its lava tank = 100%; lava, magma, fire or a lit campfire directly underneath = 50% with no fuel.
- Lava tank (4,000 mB): pipe lava in, right-click lava buckets, or place any lava-providing block next to it. 50 mB of lava = 1,000 ticks of heat.
- Water: each adjacent water source block adds 25 mB/t; pulls from any adjacent block that offers water (sinks, tanks, infinite water blocks); or pipe it in / use buckets.
- Makes up to 100 mB steam/t using 10 mB water/t.

**Steam Turbine** (steel, gold, redstone block, gold cable): place directly ON TOP of the Boiler.
Spins up over ~5 seconds, turns 1 mB steam into 2 FE, up to 200 FE/t. Outputs on every side except the bottom.
One coal through Boiler + Turbine = ~320,000 FE, about 5x the Coal Generator.

## Stage 3: Fusion
**Lithium Ore**: generates below Y=0 (mostly deepslate), about as common as gold. Needs an iron pickaxe.
Drops Raw Lithium (Fortune works; Silk Touch gives the ore). Smelt or blast into Lithium Ingots.
Tagged `forge:ingots/lithium`, so Mekanism's lithium works too.

**Empty Fuel Cell** (x4): steel + glass. Reusable.

**Electrolyzer** (steel, glass, gold cable, bucket, diamond): 100 FE/t.
- Deuterium Cell = empty cell + 1,000 mB water + 20,000 FE (10 s)
- Tritium Cell  = empty cell + lithium ingot + 40,000 FE (20 s). Lithium in its slot = makes tritium.
- Water: pulls from adjacent sinks/tanks, buckets, or pipes. Hoppers: cells top, lithium sides, output bottom.

**Fusion Reactor** (steel, netherite, diamond blocks, nether star, diamond cable):
1. Charge: feed it 1,000,000 FE while it's off (charges at up to 32,000 FE/t).
2. Load one Deuterium + one Tritium cell - it ignites automatically when fully charged.
3. Each pair burns 2 minutes; plasma heats over ~9 s; output up to 4,000 FE/t. Empty cells come back out.
4. Out of fuel: plasma cools over ~25 s. Refuel in time or it needs re-igniting.
Use Diamond or Netherite cable on the output: Copper (1,000 FE/t) can't carry it.
Hoppers: deuterium top, tritium sides, empty cells out the bottom.

## Phase 1: Machines that use power
Machine recipes are data files (`data/tieredpower/recipes/pulverizing/` and `alloying/`), so you, datapacks or KubeJS can add more.

**Pulverizer** (iron, flint, copper cable, piston, redstone): 40 FE/t.
Ores and raw ores -> 2 dust (ore doubling) for iron, gold, copper, lithium (and other mods' ores via `forge:ores/*` tags).
Ingots -> 1 dust. Cobblestone -> gravel -> sand, blaze rod -> 4 powder, bone -> 6 bone meal.
Dusts smelt or blast into ingots.

**Alloy Smelter** (iron, bricks, copper cable, furnace, redstone): 60 FE/t. Inputs go in either slot.
- Steel: 1 iron (ingot or dust) + 2 coal/charcoal
- Superconductor Ingot x2: 3 copper + 1 lithium (ingots or dusts). Used for the Phase 4 multiblock reactor.

**Charger** (iron, copper, gold, redstone block, gold cable): charges any Forge Energy item from any mod at up to 8,000 FE/t.
Full items move to the output slot (hopper underneath collects them).

## Phase 2: JEI + textured GUIs
**JEI** (optional - the mod works without it):
- Categories for the Pulverizer, Alloy Smelter, Electrolyzer and Fusion Reactor, showing inputs, outputs, time and total FE.
- Electric Furnace appears on vanilla Smelting pages; Coal Generator and Boiler on the Fuel page.
- Click the arrow (or flame) in a machine GUI to open its recipes.
- Info pages (press U in JEI) for the Coal Generator, Battery Box, Boiler, Steam Turbine, Charger, Fusion Reactor, Lithium Ore and every cable.
- JEI version is set in gradle.properties (`jei_version`) to match your modpack.

**GUIs** now use textures in `assets/tieredpower/textures/gui/`:
- `machine_base.png` - panel and player inventory
- `widgets.png` - slots, large output slot, progress arrow, flame, energy bar, tank frame and gauge marks
Edit these to restyle every machine at once. Hover the energy bar or a tank for exact numbers.

## Phase 3: Upgrades
Electric Furnace, Pulverizer, Alloy Smelter and Electrolyzer have two upgrade slots on the left (top = Speed, bottom = Efficiency), up to 4 each.
- **Speed Upgrade** (redstone, gold, sugar, superconductor): +50% speed each, +75% power per tick each.
- **Efficiency Upgrade** (redstone, lapis, steel): -15% power per tick each (compounding).
- 4 Speed = 3x speed. 4 Speed + 4 Efficiency = 3x speed at about 1.3x the power per tick.
The GUI shows the current speed multiplier and FE/t. Upgrades drop when the machine is broken.

## Phase 4: Multiblock Fusion Reactor
A 5x5x5 cube:
- Edges and corners: **Reactor Casing** (8 steel around obsidian -> 8)
- Faces: Casing, **Reactor Glass** (casing + glass), **Fuel Ports** (casing + hopper) and **Power Ports** (casing + netherite cable). At least one Fuel Port and one Power Port.
- **Fusion Controller** (compact Fusion Reactor + diamond cables + casing) built into one side face (anywhere except an edge), front facing out. Ports also replace casing blocks in the faces.
- Interior 3x3x3: **Magnet Coils** (superconductor, steel, redstone block) or air.

Output: 4,000 FE/t + 1,500 FE/t per coil (27 coils = 44,500 FE/t). Ignition: 5,000,000 FE via a Power Port.
Same Deuterium + Tritium cells, 2 minutes per pair. Right-click the controller to check the structure; it names the wrong block and its coordinates.
Fuel Ports: hoppers/pipes insert cells and pull empties. Power Ports: take ignition power in, push output out (use Netherite cable).

### Phase 4 update: any size
The reactor can be any rectangular box from 3x3x3 to 13x13x13 (outside size); it doesn't have to be a cube.
The controller goes anywhere in a side wall (not an edge). Every Magnet Coil inside adds 1,500 FE/t (base 4,000 FE/t).
Up to 27 coils a fuel pair lasts 2 minutes; bigger reactors burn fuel proportionally faster (same FE per pair, much more FE/t).
Each Power Port pushes up to 128,000 FE/t per side - use several ports and Netherite cable for big reactors.

## Phase 5a: Tiered batteries
| Battery | Capacity | Per-face rate | Recipe core |
|---|---|---|---|
| Battery Box | 1,000,000 FE | 8,000 FE/t | copper, iron, redstone block |
| Advanced Battery Box | 8,000,000 FE | 32,000 FE/t | Battery Box + steel, gold cable, redstone block |
| Elite Battery Box | 32,000,000 FE | 128,000 FE/t | Advanced + superconductor, diamond cable, diamond block |
| Ultimate Battery Box | 256,000,000 FE | 512,000 FE/t | Elite + netherite, netherite cable, nether star |

- Each face is **Input** (blue socket), **Output** (glowing core) or **Disabled** (grey X). Shift + right-click a face with an empty hand to cycle it.
- When placed, the face pointing at you is the Input; the other five faces are Outputs.
- Batteries keep their energy when mined; the item tooltip shows how much. (Crafting a battery into the next tier does not carry the energy over - use it up first.)
- The GUI shows stored energy plus live FE/t in and out.

## Phase 5b: Energy Bank (multiblock storage)
Any rectangular box from 3x3x3 to 13x13x13:
- Edges and corners: **Bank Casing** (8 steel around a redstone block -> 8)
- Walls: Bank Casing, **Bank Glass** (casing + glass), **Bank Ports** (casing + diamond cable) - at least one port
- **Bank Controller** (casing, diamond cable, Advanced Battery Box) built into a side wall, front facing out
- Inside: **Energy Cells** or air

| Cell | Capacity | Recipe |
|---|---|---|
| Energy Cell | 32M FE | steel, redstone blocks, Battery Box |
| Advanced Energy Cell | 256M FE | 4 Energy Cells, superconductor, diamond block |
| Ultimate Energy Cell | 2,048M FE | 4 Advanced Cells, netherite, nether star |

Bank Ports are Input (blue, arrow in) or Output (orange, arrow out); shift + right-click with an empty hand to switch.
Each port moves up to 1,000,000 FE/t. The controller GUI shows stored/capacity, %, cell count and live FE/t in/out.
Breaking the structure keeps the stored energy (nothing flows until it's rebuilt). Rebuilding it smaller loses anything above the new capacity.

## Phase 6: Quality of life
- **Wrench** (iron + copper): right-click rotates machines, cycles Battery Box faces, switches Bank Ports.
  Shift + right-click picks any Tiered Industries block up with its items, energy, fluids and settings kept.
- **Redstone control**: button in the top-right of every machine GUI - Ignore redstone / Run with signal / Run without signal.
- **Comparator output**: Battery Boxes and the Energy Bank controller give 0-15 depending on how full they are.
- **Config file**: `config/tieredpower-common.toml` - generator outputs, cable rates, battery sizes/rates, fusion output per coil,
  sink water rate, and on/off for machine sounds and particles.
- **Sounds and particles**: running machines crackle, grind, bubble or hum quietly; burning generators show smoke and flames.
- **Jade**: looking at a block shows running/idle, redstone mode, upgrades, battery in/out, reactor coils/output, bank fill and cell count, cable rate.
- **Sink** (iron + water bucket): endless water, pushes into neighbouring tanks/machines and fills buckets. No water source or power needed.

## Phase 7: Fluids and resources
- **Electric Pump** (steel, bucket, copper cable, piston, glass): place above a lake or lava pool. Pumps 1 bucket/s (Speed Upgrades make it faster)
  from the connected fluid within 32 blocks. Water is left in place (endless); lava is removed. Pushes fluid into tanks/machines beside or above it.
- **Fluid Tanks**: Basic 16 buckets (iron + glass), Advanced 64 (steel), Elite 256 (diamond), Ultimate 1,024 (netherite) - each upgrades the tier below.
  Fluid is visible through the glass; keeps contents when mined; comparator output; right-click with an empty hand to see the contents.
- **Rock Crusher** (iron, iron pickaxe, water + lava buckets, copper cable, redstone): now a Phase 8 processing machine - see below.
- **Freezer** (steel, packed ice, copper cable, bucket, redstone): 9 Ice -> Packed Ice, 9 Packed Ice -> Blue Ice; water -> Ice, lava -> Obsidian.
  Pulls water/lava from neighbouring tanks, sinks and pumps.
- All three machines have upgrade slots and redstone control.
- Wrench fix: it now works on machines and batteries (it acts before the block opens its GUI).

## Phase 8: Processing
Recipes are data files in `data/tieredpower/recipes/purifying|compressing|sawing/` - add your own, and all show in JEI.
- **Ore Purifier** (steel, water bucket, glass, Pulverizer, gold cable): ores and raw ores -> **3 dusts** (ore tripling), 250 mB water each.
  Also gravel -> flint, dirt -> 2 clay. Water from a Sink, Pump, tank, pipes or buckets.
- **Compressor** (steel, piston, copper cable, anvil, redstone): ingots -> plates (iron, gold, copper, steel), 8 coal blocks -> diamond,
  4 snowballs -> snow block, 4 sand -> sandstone, 4 sawdust -> paper.
- **Electric Sawmill** (iron, iron axe, copper cable, stonecutter, redstone): logs -> 6 planks + sawdust (bamboo block -> 3 planks), planks -> 3 sticks.
- **Sawdust** burns in furnaces, the Coal Generator and the Boiler (100 ticks, like a stick).
- All three have upgrade slots, redstone control and a secondary output slot; hoppers insert from the top/sides and extract from the bottom.

### Phase 8 (rebuilt): Rock Crusher, Fluid Pipes and Gas Pipes
- **Rock Crusher**: crushes the blocks you put in - stone -> cobblestone -> gravel -> sand, sandstone -> 4 sand, granite/diorite/andesite/tuff/calcite/
  deepslate/netherrack/blackstone/basalt -> gravel, end stone -> sand, glass -> sand, clay -> 4 clay balls, glowstone -> 4 dust. Data-driven (`recipes/crushing/`), JEI, upgrades, redstone.
- **Fluid Pipes** (liquids) and **Gas Pipes** (gases such as Steam): Basic 1,000 / Advanced 4,000 / Elite 16,000 / Ultimate 64,000 mB/t per connection.
  Recipes: Basic Fluid Pipe = iron-glass-iron (x6); Basic Gas Pipe = copper-glass-copper (x6); each higher tier = 8 pipes around steel / diamond / netherite (x8).
  Connected pipes form one network carrying one fluid at a time, shared fairly - length doesn't matter. Pipes push into tanks and machines.
  **Wrench a connection** to make it pull fluid OUT of that block instead (orange ring) - e.g. to empty a tank into the pipes.
- **Steam is now a real gas**: Boilers push steam into Gas Pipes on any side, and Steam Turbines accept it from pipes - so turbines can go anywhere,
  and one boiler can feed several turbines. A turbine directly on top of a boiler still works.
- The Electric Pump's tank can only be emptied from outside (pipes won't push fluid back into it).

## Phase 9: Automation
- **Powered Quarry** (steel, diamond pickaxe, gold cable, redstone block, hopper): mines a 17x17 area below itself (config `quarryRadius`)
  down to bedrock, as if with a diamond pickaxe. Skips fluids, unbreakable blocks and anything with an inventory. 9-slot buffer, pushed into
  neighbouring chests/inventories; pauses when full. 80 FE/t, 4 blocks/s (faster with Speed Upgrades).
- **Crop Farmer** (iron, iron hoe, copper cable, water bucket, redstone): 9x9 around itself at its own height (config `farmerRadius`).
  Harvests + replants wheat, carrots, potatoes, beetroot, nether wart; melons, pumpkins; sugar cane above the bottom block.
  Seeds in the 3 left slots plant empty farmland. Output pushed into neighbouring chests.
- **Auto-Crafter** (iron, crafting table, copper cable, piston, redstone): click items into the 3x3 pattern (ghost items). Pulls ingredients
  from neighbouring chests, crafts every second, output slot for hoppers/pipes; leftovers (empty buckets) go back into a neighbour.
- **Chunk Loader** (obsidian, ender pearls, gold cable, diamond block): keeps 1x1 / 3x3 / 5x5 chunks loaded while powered.
  20 FE/t per chunk (config), Efficiency Upgrades reduce it. Releases when unpowered, switched off by redstone, or broken.
- All four have redstone control and upgrade slots.

### Quarry settings (0.16)
The Quarry GUI has a settings panel: area size (3x3 to 65x65), Centred / Behind, Bottom Y (shift-click = 10), tool mode
(Normal / Silk Touch / Fortune III, 50% more power), a 6-slot Void filter (those drops are deleted), and Reset.
Changing size, position or depth restarts it from the top.

## Phase 10: Machine outputs, Item Pipes and Filters
- **Auto-output button** (green arrow, top-right of every machine GUI, next to redstone control): when ON, finished items are pushed
  every half second into any neighbouring inventory - chests, Item Pipes, Storage Drawers, AE2 / Refined Storage interfaces, etc.
- **Item Pipes**: Basic (iron + planks + iron -> 6), Advanced / Elite / Ultimate (8 pipes around steel / diamond / netherite -> 8).
  Pull rate per wrenched connection: 8 / 32 / 128 / 512 items per second. Items move instantly.
  - Anything pushed into a pipe (auto-output, hoppers) is delivered to the other inventories on the network, shared round-robin, never back into the sender.
  - Wrench a connection to make it PULL from that block (orange ring) - e.g. pull a chest's contents into your storage system.
- **Item Filter** (paper + hopper + redstone -> 2): right-click the air to set up to 9 items and Allow/Block. Right-click a pipe connection
  with it to install it on that connection; shift + right-click the connection with an empty hand to take it off.
  A delivering connection with a filter only accepts matching items; a pulling connection only pulls them.
- Works with any storage mod that uses Forge's standard item capability: Storage Drawers (drawers / controller), AE2 (ME Interface, or an
  Import Bus on the pipe/machine), Refined Storage (Interface / Importer), Sophisticated Storage, vanilla chests and barrels.
- Tip: if two machines both auto-output onto the same pipe network, use filters so items don't bounce between them.

## Phase 11: Powered items, clear glass, live coils
- Reactor Glass, Bank Glass and Fluid Tank glass are now properly see-through (clear centre, like vanilla glass).
- Magnet Coils light up and ripple with plasma while the Fusion Reactor is running (dim when it's off).
- **Electric Drill** (100k FE, 100 FE/block, diamond level) and **Advanced Drill** (1M FE, netherite level, shift + right-click for 3x3).
- **Electric Chainsaw** (200k FE): axe + leaves; fells whole trees (sneak for one log).
- **Jetpack** (400k FE, 40 FE/t flying) and **Advanced Jetpack** (4M FE, 80 FE/t, faster): chest slot, hold jump. No fall damage.
  Dedicated servers need allow-flight=true.
- **Portable Battery** (2M FE, 2k FE/t) and **Advanced** (32M, 16k FE/t): right-click to switch on; charges your other items.
- **Wireless Charger**: charges powered items of every player within 16 blocks, 16k FE/t each.
- All powered items work with any FE charger and take enchantments (Efficiency, Unbreaking isn't needed).

## Phase 12: Solar, wind and fission
- **Solar Panels** (12 / 60 / 300 / 1,500 FE/t in sun): need open sky; half in rain; each tier from 4 of the one below.
- **Wind Turbine**: 0 FE/t at Y 64 rising to 256 FE/t at Y 256; rain x1.5, thunder x2; blocks within 3 blocks reduce output.
- **Uranium**: ore from Y 24 down (most common deep); raw, ingot, dust; Pulverizer x2, Ore Purifier x3; works with other mods' uranium.
- **Fission Reactor**: 4 Uranium Fuel Rods (2 per 3 uranium + 6 steel), 5 min each, 800 FE/t per rod at 50%+ heat.
  Water cooling above 50% heat turns into steam (pushed to Gas Pipes / turbines). No water -> SCRAM at 100%.
  Config `fission.meltdownExplodes` (default false) makes it explode instead.

## Phase 13: Pipe upgrades
- **Visible items**: items glide along Item Pipes from sender to receiver (Basic 4 / Advanced 8 / Elite 16 / Ultimate 32 blocks/s).
  Visual only - delivery is instant so nothing is ever lost. Config `effects.pipeItemVisuals` turns it off.
- **Filter match modes**: the Item Filter's new Match button - Item (exact), Tag (shares any tag), Mod (same mod).
- **Priorities**: right-click an Item Pipe connection with an empty hand to cycle 0-9. Higher fills first; equal priorities round-robin.
- **Jade**: looking at an Item Pipe lists each connection's filter and priority.
- 0.19.1: Item Pipes are now glass tubes (coloured frame, clear window) so you can actually see the items travelling inside.

## Phase 14: Guidebook and release (1.0.0)
- **Tiered Industries Guide** item: given once on first join, or crafted from a book + copper ingot. Categories, search, pages with stats,
  live crafting/furnace recipes and machine recipe lists. Content in `assets/tieredpower/guide/guide.json` (resource packs can replace it).
- Logo, MIT license, CHANGELOG, mod page text (docs/MODPAGE.md), .gitignore and a GitHub Actions build (tags `v*` create a Release).

## Phase 15: Multiblock Fission Reactor (1.1.0)
- Fission Casing (8 steel plates around uranium -> 8), Fission Glass, Fuel Assembly, Coolant Channel, Fuel/Coolant/Power Ports, Fission Controller.
- Output 1,000 FE/t per Fuel Assembly at 50%+ heat, x(1 + 10% x average touching assemblies). Cooling 5 + 10 per Coolant Channel heat/t,
  5 mB water per heat, 50 mB steam per heat. A rod = 6,000 assembly-ticks. Controller stores up to 64 rods.

## Phase 16: More machines (1.2.0)
Enchanting Machine, Mob Grinder, Spawner Controller, Fluid Mixer, Teleporter pads (+ Linker). See the in-game guide or CHANGELOG.md.

## Phase 18: Polish and balance (1.3.0)
Balance review (docs/BALANCE.md), global generation/machine multipliers in the config, spinning Wind Turbine rotor,
Coal Generator chimney with smoke, Shift tooltips with each item's guide text.
