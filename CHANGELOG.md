# Changelog - Tiered Industries (formerly Tiered Power)

## 1.72.1 - Tree Farm fixes

### Fixed
- **Tall trees were only cut halfway** and **leaves were left behind**: the Tree Farm's search for the rest of the tree used up its limit on the air around the tree and only looked upwards, so it missed tree tops and leaves hanging below branches. It now follows only the tree itself, in every direction (never below the base), and takes the whole tree - up to 48 blocks tall - every log and every natural leaf. Leaves you placed yourself are left alone.
- **Lag when trees were felled**: a whole tree used to come down in one tick. Now it comes down 24 blocks per job - leaves first, then the logs from the top - with one chopping sound instead of a break effect for every block.
- **Trees grew too fast**: Fertilizer now grows at most one step every 2 seconds (about a tree every 4 seconds), so the farm can't flood the world with new trees.

## 1.72.0 - Tree Farm: all-around planting and Fertilizer

### Changed
- **Tree Farm plants all around itself** instead of a patch in front: every other block out to 4 blocks away, on all sides - 24 trees, each with room to grow. Put it in the middle of your tree field.
- **Saplings that drop go back into its sapling slots**, so a handful of saplings keeps it going without piping them back.

### Added
- **Fertilizer slot** on the Tree Farm (left side), like the Crop Farmer's. Fertilizer (32 growth steps, about 16 trees) or bone meal (8) makes the saplings grow - even at night or in the dark, where saplings don't grow on their own. It only spends fertilizer on saplings with room above them to grow.
- Next to a storage network the Tree Farm keeps itself stocked with Fertilizer (or bone meal) and more of the saplings it already has.
- The Tree Farm's window shows the growth charge left ("Grow N").

### Note
- 2x2 trees (dark oak, giant spruce and jungle) won't grow on the Tree Farm's spaced-out grid - use saplings that grow on their own.

## 1.71.0 - Quantum Chest filters and Receive mode

### Added
- **Send / Receive** button on linked Quantum Chests (top right of the window). **Send** is the old behaviour: items go to the main chest. **Receive** works the other way: the main chest sends this chest the items on its filter - food, fuel and torches out to a planet base, for example. A Receive chest needs at least one filter item (its filter row shows red until it has one).
- **Filter row** on linked chests: 9 ghost slots above the chest (click with an item to set it, empty hand to clear; nothing is taken from you). On a Send chest only the filtered items are sent and everything else stays put - leave it empty to send everything. On a Receive chest it picks what to receive.
- **Linked chest count**: the main chest's window (and Jade) shows how many chests are linked to it, and how many are sending and receiving.
- Items for a Receive chest that isn't loaded wait in transit (up to 54 stacks) and arrive when it loads, like items for a main chest.

### Changed
- Quantum Chests now use their own window (same 54 slots, nothing in them is lost). Switching a chest back to Send, or unlinking it, drops anything still in transit to it into the chest.
- The mod has its own GitHub repository now: **CPSDomain/tiered-industries**. Releases, the changelog and the update checker all live there (Tiered Arsenal has its own, CPSDomain/tiered-arsenal).

## 1.70.0 - Quantum Chests

### Added
- **Quantum Chest**: a 54-slot chest that moves items between worlds at a quantum level - no Stargate needed. Every Quantum Chest starts as a **main chest**. Link other Quantum Chests to it and anything put into a linked chest (by hand, hopper, pipe or machine) lands in the main chest about twice a second, from any distance and any world. Link as many chests as you like to one main chest - one at each planet's mine, all feeding your base. If the main chest's area isn't loaded, items wait in transit (up to 270 stacks) and arrive when it loads. A linked chest only sends while its own area is loaded (you nearby, or a Chunk Loader). Pipes and the storage network pull from it like any chest. Recipe: Quantum Alloy in the corners, Ender Pearls on the sides, a chest in the middle - makes 2.
- **Quantum Linker**: sneak + right-click your main chest to record it (the linker glows), then right-click other Quantum Chests to link them. It keeps the main chest, so one trip can link chests on several planets. Sneak + right-click a linked chest to unlink it. Recipe: Ender Pearl + Redstone + Quantum Alloy (shapeless).

### Removed
- **Gate Interface** - replaced by Quantum Chests. **Empty your Gate Interfaces before updating**: placed interfaces disappear when the world loads, and anything in their buffers is lost. Stargates and DHDs are unchanged and still carry players.

## 1.69.5 - Stargate "busy" fixes

### Fixed
- **"The gate at the other end is busy" after a reload**: a gate that was open when the world was saved (for example a planet's gate, left connected to home) came back still marked as open, so nothing could dial it until someone went there. Gates now always start closed after a reload, and a gate that nothing is keeping open no longer counts as busy.
- **Dialling a planet that is already connected to this gate** (for example one held open by redstone from the planet's side) now says "Already connected - the gate is open, walk through" instead of "busy". Items, fluids and power already flow through Gate Interfaces in both directions on that connection.

### Changed
- **Close gate works from either end**: on the receiving end of a connection, the DHD now shows "Incoming - gate open", and its Close gate button (or the dome) shuts the connection even though the other DHD opened it. If the other end is held open by redstone, it waits 10 seconds before dialling again.

## 1.69.4 - Irrigated Sand

### Added
- **Irrigated Sand**: sand for greenhouses. Sugar cane grows on it with no water channel, as long as a Sprinkler with water in it is overhead - within 4 blocks sideways and up to 8 above, the same area a Sprinkler waters. Water beside it works too, like normal sand, and cactus grows on it.
  - It doesn't fall like sand.
  - 8 sand around a water bucket makes 8 (you get the bucket back).
  - Lay it where the farmland would go, in the Crop Farmer's reach, put Sugar Cane Seeds in the farmer, and it plants and harvests the cane with the rest of the greenhouse.

## 1.69.3 - Sugar Cane Seeds, slower fertilizer, Crop Farmer restocking

### Added
- **Sugar Cane Seeds**: one sugar cane makes two. Plant them by hand or in the Crop Farmer, the same way as sugar cane: on sand, dirt or grass with water right beside that block.
- **Crop Farmer restocking**: place the farmer next to a storage cable (or any other block of your storage network). Every two seconds it tops up its fertilizer slot from storage - Fertilizer, or bone meal if there's none - and refills its seed slots with more of the seeds already in them.

### Changed
- **Fertilizer lasts much longer** in the Crop Farmer. Instead of using one per plant, the farmer now grows each plant one step as it passes. One Fertilizer is good for 32 steps and bone meal for 8, so a stack of Fertilizer covers 2,048 growth steps. The screen shows how many steps are left from the last one used.

### How sugar cane planting works
- The farmer works on its own level, so the sand (or dirt) must be one block below the farmer's level, with water in the block right beside it - for example a row of sand next to a water channel. Sugar cane can't be planted on sand without water beside it - that's how Minecraft works.

## 1.69.2 - Crop Farmer: sugar cane, cactus, stems and a fertilizer slot

### Changed
- The **Crop Farmer** now plants more than crop seeds. Its seed slots take:
  - **Sugar cane** (sugar cane is its own seed, so put a stack of sugar cane in). It's planted on sand or dirt next to water.
  - **Cactus**, planted on sand.
  - **Melon and pumpkin seeds**, planted on farmland.
- It now also harvests cactus (leaving the bottom block, like sugar cane).
- **Fertilizer slot**: a slot of its own on the far left, which also takes bone meal. The farmer uses one on each plant it passes that's still growing. That now includes melon and pumpkin stems, sugar cane, cactus and nether wart as well as crops. Fertilizer counts as about three bone meal.

### Fixed
- Fertilizer couldn't be put into the Crop Farmer by hand - only hoppers could add it. It now has its own slot (fertilizer already in a seed slot still gets used).

## 1.69.1 - GitHub releases and update checker

### Changed
- The mod now lives in the **CPSDomain/Modding** GitHub repository, in the `tiered-industries` folder. Every new version is built on GitHub and published on the Releases page with its jar.
- **Update checker**: Forge's Mods screen now shows when a newer version of Tiered Industries is out.
- The mod's homepage and issue links point to the new repository.

## 1.69.0 - Balance pass

Every recipe (458 of them) was traced back to vanilla materials to check the order things unlock in, and the numbers for everything added since the last balance pass were compared with the machines they sit next to.

### Changed
- **Digital Factory**: 4 operations at once per Factory Cell (was 8), up to 16 cells (was 32) - so a full factory runs 64 at once instead of 256. A Naquadah-tier machine runs 12, so the factory is still the bulk option.
  - It now pays the same power per item as the separate machines: 20 FE/t per operation to smelt, 40 to crush, 60 to wash and 80 for Ore to Ingots (was 12 for everything). Washing uses 250 mB of water per ore, like the Ore Purifier (was 100).
  - Factory Cells now need two Superconductor Ingots and make 1 per craft (was 2).
- **Energy upgrades**: power use can't drop below 15% any more. This only affects Mk II cards: 16 Energy levels used to cut power by 93%, now 85%.
- **Geothermal Generator**: back to 20 FE/t per lava source and 5 per magma block, 120 FE/t at most (1.57 had doubled it to 40 / 10 / 240). 1.57 doubled it to match a lava-heated Boiler, but a Boiler with lava under it only makes about 100 FE/t through a Steam Turbine, so 120 FE/t already matches it. 240 FE/t free from the very start was out-producing six Coal Generators.
- **Wind Turbine**: 128 FE/t at full height (was 256), still x1.5 in rain and x2 in thunderstorms. A free generator that early shouldn't beat a fuelled Steam Turbine.
- **Gate Interface**: 32 items, 8,000 mB and 8 million FE per tick (was 64, 16,000 and 16 million) - in line with the Quantum pipes and cables.

### Checked and left as they are
- Generation, from Water Wheel to Antimatter Reactor, climbs in clear steps, and each fuel is worth more in the bigger machines.
- Tier Installers, Mk II upgrades, the Naquadah tier, the Stellar Solar Panel (the same output as the four Quantum panels it's made from, in one block) and the Electric Pump's tiers all fit their place.
- Nothing you can make before the Stargate needs a planet material.
- Every item can be made or found. The only items with no recipe are ores, fluids, creative items, guardian loot, address tablets, and items machines make.

## 1.68.0 - Hidden-world materials and more upgradable machines

### Added
- Every hidden world now has a material of its own (in newly explored terrain), and six of them make new **Quantum Suit modules**:

  - **Eden**: **Lifebloom** flowers. Used for the **Regeneration Module** (chestplate): heals you while you're hurt.
  - **Karoo**: **Sunstone Ore**. Sunstone burns like 40 coal in any generator or furnace.
  - **Redwood**: **Amber Ore**. Used for the **Resistance Module** (leggings): a fifth off all damage.
  - **Murk**: **Witchroot**. Used for the **Antidote Module** (helmet): cures poison, wither, hunger, nausea, blindness, darkness, weakness and slowness.
  - **Titan**: **Gravitite Ore**. Used for the **Haste Module** (helmet): Haste II.
  - **Wraith**: **Soul Crystal Ore**. Used for the **Fire Immunity Module** (leggings): fire and lava don't hurt.
  - **Umbra**: **Umbral Ore**. Used for the **Stealth Module** (boots): invisible while sneaking.
  - **Void Reach**: **Voidstone Ore**. Eight Void Shards around an Eye of Ender make a **Void Pearl**: a reusable ender pearl with a 3 second cooldown.
- Each module is four of its material, four Quantum Alloy and a themed centre (golden apple, shield, milk, diamond pickaxe, magma cream, fermented spider eye).
- The new ores work in the Rock Crusher, have forge ore tags, and turn up in the ruins, outposts and guardian drops of their worlds.

### Changed
- **Tier Installers** now also work on the **Smithing Press**, **Oxygen Furnace** and **Freezer** (3/5/7/9/12 at once).
- The Dialler's descriptions of the hidden worlds now say what's found only there.

## 1.67.0 - Planet bases and guardians

### Added
- **Ruins and outposts** on all sixteen planets, built in each planet's own style (packed ice on Frost, sandstone on Dune, nether bricks on Inferno, prismarine under the Abyss, quartz in the Skylands, deepslate in the caves, purpur in the Void Reach and so on). They appear in newly explored terrain, roughly one every 40 chunks; on cave planets they sit on cave floors.
  - **Ruins**: a 7x7 broken-down building with a loot chest - iron, gold, redstone, diamonds, emeralds, the odd Naquadah Ingot, the planet's own material, and sometimes a Stargate Address.
  - **Outposts** (about one in four): an 11x11 two-room building with a fallen-in roof, two richer chests (Mk II upgrades, enchanted books, Naquadah, golden apples, a good chance of an address) and a **Guardian Altar**.
- **Guardians** (optional): sneak + right-click a Guardian Altar to call the planet's guardian - a souped-up mob with 300 health, heavy armour, a boss bar and a planet theme (a Stray on Frost, a Husk on Dune, a Vindicator on Verdant, Eden and Redwood, a Wither Skeleton on Inferno, Wraith and Umbra, a Drowned in the Abyss, a Blaze on Skylands, a Ravager on Mycelia, Karoo and Titan, a Piglin Brute in the Crystal Depths, a Witch in the Murk and an Evoker in the Void Reach). Each altar calls one guardian, then goes dark.
  - Guardians drop a **Guardian Heart**, 6-12 Naquadah Ingots, diamonds, emeralds, Mk II upgrades, a Stargate Address and a stack of the planet's material.
  - Nothing needs a Guardian Heart, but it's a shortcut: with a Quantum Installer it makes a Naquadah Installer, and with four Speed or Energy Upgrades it makes four Mk II upgrades.

## 1.66.0 - Digital Factory

### Added
- **Digital Factory**: a mid-game multiblock that processes ores in bulk.
  - Place the **Digital Factory** controller and build **Factory Cells** onto it - touching the controller or each other, any shape you like, up to 32 cells. Each cell runs 8 operations at once, so a full factory handles 256 items per cycle (5 seconds, faster with Speed upgrades).
  - Four modes, picked with the button in its screen:
    - **Smelt** - furnace recipes.
    - **Crush** - like the Pulverizer: 2 dust per ore.
    - **Wash** - like the Ore Purifier: 3 dust per ore, 100 mB water each.
    - **Ore to Ingots** - washes, then smelts the dust: 3 ingots per ore in one step.
  - Nine input and nine output slots, Speed and Energy upgrade slots, auto-output and side settings like any machine. 12 FE/t per operation running (about 3,000 FE/t flat out).
  - **Factory Ports** anywhere on the structure connect pipes, cables and hoppers: items into the inputs, items out of the outputs, water and power in.
  - Shows up on the Machine Status Display like other machines.

### Fixed
- Machine screens showed the wrong "at once" number for Naquadah-tier machines.

## 1.65.0 - Crafting terminal upgrades

### Added
- **Craft All Missing** in the Crafting Terminal: after JEI's + fills the grid, any ingredients storage didn't have leave the grid slots empty, and a **+** button appears under the grid's **x** with "N missing".
  - Click it to start crafting jobs for every missing ingredient the network has patterns for (one per slot, or a stack per slot if you used shift with JEI's +).
  - The grid fills itself in as the crafted ingredients arrive (for up to 5 minutes), so you can just wait and take the result.
  - Anything without a pattern is listed in chat.
- **Live crafting queue** beside every Storage and Crafting Terminal: while the network is crafting, a panel to the right shows each job's item, amount and crafts left (or what it's waiting for), refreshed every second. Hover a job for its steps; the Jobs button still has the full view and Cancel. JEI moves its item list out of the way.

## 1.64.0 - Wireless redstone and the Machine Status Display

### Added
- **Wireless Redstone Transmitter** and **Receiver**: a transmitter sends the redstone signal going into it on one of 64 channels; every receiver on that channel gives out the strongest signal being sent - any distance, any dimension, as long as the transmitter's chunk is loaded.
  - Right-click to pick the channel, sneak + right-click to go back. Jade shows the channel and signal.
  - Receivers power the blocks around them and through the block they sit on, so they can drive pistons, lamps, machines' redstone control, the Logic Controller or the Stargate DHD's redstone dialling.
- **Machine Status Display**: lists every machine within 24 blocks and what it's doing - Working, Idle, Blocked (has items but isn't running: output full or an ingredient missing), No power, or Off (paused by redstone). Problems come first; click the chips at the top to filter.
  - Its screen glows green when everything is fine, amber when something is blocked and red when something is out of power.
  - A comparator next to it gives the number of machines that need attention (up to 15) - put a Wireless Redstone Transmitter beside it for an alarm anywhere in your base.

### Changed
- The Electric Pump's screen now shows its tier as buckets per cycle.

## 1.63.1 - Upgradable Electric Pump and string

### Changed
- The **Electric Pump** now takes Tier Installers (it already took Speed and Energy upgrades):

  - Advanced: 3 buckets per cycle, 48,000 mB tank, 48-block reach.
  - Elite: 5, 80,000 mB, 64 blocks. Ultimate: 7, 112,000 mB, 80 blocks.
  - Quantum: 9, 144,000 mB, 96 blocks. Naquadah: 12, 192,000 mB, 128 blocks.
  - Power use and output speed to pipes scale with the tier too. A Naquadah pump moves about 3,000 mB per tick with 8 Speed Upgrades, or 5,400 with 8 Speed Upgrade Mk II.
  - Lakes of lava (or other fluids) now empty from the far edges inwards.

### Added
- **String** recipes: any wool makes 4 string (crafting table or Pulverizer), and a cobweb makes 3.

## 1.63.0 - Hidden worlds and Stargate addresses

### Added
- **Eight hidden worlds**, for sixteen in all. Their addresses aren't in the DHD's list until you find them:
  - **Eden** - gentle plains, flower meadows and birch woods, full of villages.
  - **Karoo** - dry savanna and high plateaus with savanna villages.
  - **Redwood** - giant old-growth spruce and pine forests.
  - **Murk** - swamps, mangroves and dark woods under an eternal dusk.
  - **Titan** - towering amplified mountains and deep valleys.
  - **Wraith** - a haunted underworld of soul sand valleys, warped forests and basalt, with fortresses and bastions.
  - **Umbra** - the deep dark everywhere: sculk caves and ancient cities (and the Warden).
  - **Void Reach** - the outer End islands: chorus, end cities and shulkers. You land on a platform over the void, so bring blocks.
- **Stargate Address** tablets: an old stone tablet carved with a hidden world's six glyphs (they're in its tooltip).
  - Found in chests around the world: desert and jungle temples, strongholds, ancient cities, shipwrecks, buried treasure, woodland mansions, bastions, end cities, dungeons, mineshafts, pillager outposts, ruined portals, igloos, ocean ruins and nether fortresses.
  - Every planet's landing platform now has a supply chest (torches, bread, cobblestone) with the address of another hidden world, so each world leads on to the next.
  - Right-click a tablet to learn the address: it joins your DHD's address list. Tablets aren't used up - pass them to friends.
  - Or just type the glyphs on the DHD and press the dome - dialling a world also teaches you its address.

### Changed
- The DHD's address list is now your personal address book: the first eight worlds, plus the hidden worlds you've found. It scrolls with the mouse wheel, and hovering a world shows its glyphs.
- The redstone selector only offers addresses you know.

## 1.62.0 - Gate logistics

### Added
- **Gate Interface**: carries items, fluids (and gases) and power through an open Stargate.
  - Put one within 8 blocks of each gate. Right-click with an empty hand to switch it between **Send** (arrow up) and **Receive** (arrow down).
  - Feed a Send interface with pipes, cables or a hopper. While the gate is open it passes up to 64 items, 16,000 mB and 16 million FE per tick to the Receive interfaces at the other end, which push everything into the blocks next to them (or let pipes pull it).
  - It works both ways at once, so one connection can bring planet ore home and send power out.
- **Redstone dialling**: the bottom of the DHD keypad has a "Redstone signal dials" selector. Pick an address, and a redstone signal into the DHD dials it and holds the gate open until the signal goes off - a lever for an outpost that's always connected, or a Logic Controller for one that opens when storage runs low.
- **Close gate**: a button on the keypad, or press the dome while the gate is open.

### Changed
- **Upkeep**: a gate opened from home now costs 1 million FE per tick while it's open, on top of the 50 billion FE to dial. Dialling from a planet is still free. If the DHD runs out, the gate shuts.
- **Both ends open**: the gate at the other end now spins up, locks its chevrons and bursts open too, and you can walk back through it - the connection works both ways for travel as well.
- A gate that's connected to another gate is busy: it can't dial out until the connection closes.
- The far gate's area stays loaded while the gate is open, so interfaces there keep working with nobody around. The dialling end still needs a player or a Chunk Loader nearby.
- A planet's Home address now leads to the home gate that last dialled that planet (when there's no player record to go by, such as redstone dialling).
- Dialling a planet builds its landing platform right away if nobody has been there yet, so interfaces can be set up from the first trip.
- Open gates now close cleanly when the DHD is broken, its chunk unloads, or the world is reloaded.

## 1.61.1 - The DHD and the kawoosh

### Changed
- The **Stargate Dialler** now looks like the DHD from the TV series: a pedestal with a wide, sloping console of glyph keys and a red dome in the middle. The keys and dome glow while its gate is open.
- Right-clicking it opens a **DHD keypad** instead of a list of buttons: two rings of 19 glyph keys around the big red dome.
  - Every planet (and Home) has its own six-glyph address. Press the six glyphs, then the point of origin (the pyramid), then the dome to dial. Each key lights up as its chevron is encoded.
  - A wrong address gives "Dial program failed" and clears the keypad.
  - The **Addresses** list on the right types an address in for you, key by key - then press the dome. Hover a planet to see what it has.

### Added
- **The kawoosh**: when a gate opens, an unstable vortex bursts about five blocks out of the front of the ring (the side the DHD is on), then collapses back into the event horizon.
- Like on TV, it vaporises mobs standing in its path. Players, villagers and named or tamed animals are thrown clear instead of being hurt.

## 1.61.0 - Planet resources

### Added
- Every planet now has a material you can only find there, generated in newly explored terrain (chunks you've already visited don't change; the Overworld and Nether are untouched):
  - **Frost** - Cryonite Ore (2-4 Cryonite Shards). A Block of Cryonite inside a multiblock Fission Reactor counts as three Coolant Channels.
  - **Dune** - Solarite Sand, which smelts into glowing Solar Glass.
  - **Verdant** - Livingwood trees. Livingwood planks for building; a log with 4 bone meal makes 16 Fertilizer.
  - **Inferno** - Naquadah Ore (diamond pickaxe). Raw Naquadah, Naquadah Dust and Naquadah Ingots; doubles and triples in the ore machines like any ore.
  - **Abyss** - Abyssal Pearl Ore on the sea bed. Right-click a pearl for 10 minutes of water breathing, night vision and dolphin's grace.
  - **Skylands** - Aether Crystal Ore in the floating islands.
  - **Mycelia** - glowing Sporecaps. One makes 2,000 mB of methane in the Bio-Digester (ten times normal biomass).
  - **Crystal Depths** - Resonance Crystal Ore (diamond pickaxe).
- **Naquadah Installer**: a sixth machine tier. Right-click a Quantum machine to run 12 operations at once; a Naquadah Chunk Loader reaches 19x19 chunks.
- **Speed Upgrade Mk II** and **Energy Upgrade Mk II**: each card counts as two, so a full slot gives 16 levels (x9 speed, or about -93% power).
- **Stellar Solar Panel**: 36,000 FE/t in full sun, four times a Quantum Solar Panel.
- **Aether Stabilizer Module** for the Quantum Suit chestplate: flight uses a quarter of the power.
- Cheaper Stargate Frames: 4 Naquadah Ingots and 4 Steel Plates make 8 - so a second gate costs far less once you've reached the Inferno.
- The Dialler's planet descriptions now say what each planet is for.

## 1.60.2 - Machine patterns take any ore of the same kind

### Fixed
- A machine (processing) pattern only accepted the exact item it was encoded with. JEI's + button copies whichever item its slot is showing at that moment - often the stone ore while the slot cycles - so a pattern meant for Deepslate Iron Ore wouldn't take it (or the other way round), and jobs waited forever.
- Machine patterns now accept any item that shares the input's material tag: every Iron Ore (stone, deepslate, other mods'), every Raw Copper, every Iron Dust or Iron Ingot, and so on. This works for patterns you've already encoded too - no need to remake them.

## 1.60.1 - Ores into raw materials

### Added
- The **Rock Crusher** now breaks any ore block - normal or deepslate, from this mod or vanilla - into raw materials: 2 Raw Iron, Raw Gold, Raw Uranium or Raw Lithium, 5 Raw Copper, or the ore's gems, redstone, lapis, coal, quartz or netherite scrap (same amounts as the Pulverizer). Handy for silk-touched ores from the Digital Miner, Quarry or Laser Drill.

## 1.60.0 - A proper Stargate, and ores on every planet

### Added
- **Round Stargate**: build the 7x7 square of Stargate Frame as before, then right-click any frame block with an empty hand. The square becomes a round gate like the one on TV:
  - a thick ring with a recessed band of glyphs,
  - nine chevrons around the outside,
  - and, when you dial, the glyph band spins and the chevrons lock one by one (top first) before a rippling, round event horizon opens with a surge.
- The corner blocks of a formed gate can be walked through, so only the ring itself is solid. Breaking any piece turns the gate back into blocks.
- Planet landing platforms build their gate already formed. Existing planet gates can be formed with a right-click.
- An unformed square gate still works exactly as before.
- **Planet ores**: every planet now has its own extra, richer veins of Uranium and Lithium ore (more veins, and from deep down up to Y 128 so the Skylands get some too). They also form in netherrack, basalt, sandstone and terracotta, so the Inferno and Dune planets have them. These veins only appear on planets - the Overworld and Nether are unchanged.

## 1.59.0 - The Stargate

### Added
- **Stargate**: the end-game goal for an Energy Core. Build a standing 7x7 ring of **Stargate Frame** (24 blocks, empty inside, facing any way) and place a **Stargate Dialler** within 8 blocks.
  - Pick a planet in the Dialler and the ring fills with a shimmering event horizon for 30 seconds. Walk through to travel.
  - From home, every gate costs **50 billion FE** (the Dialler holds 100 billion) - feed it from an Energy Core's Output Pylons.
- **Eight planets**, each a whole world that generates as you explore:
  - **Frost** - ice plains, ice spikes and frozen peaks.
  - **Dune** - desert and red badlands.
  - **Verdant** - jungle, bamboo and mangrove swamp.
  - **Inferno** - a volcanic underworld of lava seas and basalt.
  - **Abyss** - a planet-wide warm ocean with reefs, shipwrecks, ruins and ocean monuments.
  - **Skylands** - floating islands of cherry groves and meadows.
  - **Mycelia** - mushroom fields and dark forests.
  - **Crystal Depths** - a sealed cave world of lush caves, dripstone and amethyst.
- The first visit to a planet builds a lit landing platform with its own gate and Dialler. Dialling from a planet is free: **Home** takes you back to where you left, or go on to another planet.
- Jade shows the Dialler's stored power, whether it found its ring, and how long the gate stays open. Hover a planet button for a description.

## 1.58.0 - Bigger disks and Elevators

### Added
- **Fluid Storage Disks (1M and 4M)**: 1,048,576 and 4,194,304 buckets of fluids and gases in one drive bay slot - steam, deuterium, oxygen, rocket fuel and the rest.
- **Storage Disks (1M and 4M)** for items, to match.
- Each is made from four of the size below: 1M with superconductor and a nether star, 4M with Quantum Alloy and a Quantum Power Core.
- **Elevator**: stack them in a shaft (up to 32 blocks apart, two blocks of room above each). Stand on one and jump to go up, sneak to go down. Right-click with a dye to colour it - elevators only link to their own colour, so several lifts can share a shaft. No power needed. 2 from 8 wool and an ender pearl.

## 1.57.0 - Item Magnets, Overclock Accelerator and a balance pass

### Added
- **Item Magnet**: pulls dropped items and XP orbs to you from 6 blocks while switched on (sneak + right-click). Works from the hotbar and needs no power.
- **Advanced Item Magnet**: 12 blocks, works from anywhere in your inventory, runs on FE (charge it in a Charger). Ignore list of up to 9 items: hold the item in your off hand and right-click with the magnet.
- Magnets leave items on conveyor belts and near a collecting Drone Station alone, and stop while you sneak.
- **Overclock Accelerator**: a Quantum Assembly Matrix part made from a Quantum Crafting Accelerator. Adds 16 crafts per cycle, and while the Matrix is running jobs, every machine it crafts with runs as if it had 2 more Speed upgrades (up to 8 more). The Matrix screen's status tooltip shows the overclock.

### Changed (balance)
- **Solar Panels**: 20 / 100 / 450 / 2,000 / 9,000 FE/t (were 12 / 60 / 300 / 1,500 / 7,500), so the first panel isn't weaker than a water wheel.
- **Geothermal Generator**: 40 FE/t per lava source and 10 per magma block, up to 240 FE/t (was 20 / 5 / 120) - closer to what a lava-heated boiler gives.
- **Compact Fusion Reactor**: 8,000 FE/t by default (was 4,000), so it clearly beats a single fission reactor.
- **Quantum Cable**: 2,048,000 FE/t per connection (was 512,000), enough for Quantum Battery Boxes, the Antimatter Reactor and Energy Core pylons.
- **Industrial Turbine**: up to 128 rotors count (about 102,000 FE/t at most), and it now follows the generation multiplier.
- **Energy Core**: 20B / 100B / 1T / 10T / 100T FE (was 1B to 10T), so a core always holds more than the cells used to build it.
- **Energy Cell** now needs an Advanced Battery Box, and the **Advanced Energy Cell** a netherite ingot instead of a diamond block, in line with batteries of the same size.
- **Isotope Separator**: 2 mB of deuterium a tick for 200 FE/t (was 1 mB for 400 FE/t) - gas fuel now costs about the same as Electrolyzer cells.
- **Bio-Digester**: 200 mB of methane per item (was 80).

### Fixed
- Particle Collider and Isotope Separator: Speed upgrades in odd numbers did nothing but still cost power. Progress now carries over between ticks, and the collider's energy per mB follows the same rules as other machines (so it stays 2.5x cheaper than what the antimatter gives back).
- Upgraded machines (high tiers plus Speed upgrades) could need more power per tick than they were allowed to take in, and ran only part of the time. A machine's power intake now grows with its tier, Speed upgrades and overclock.
- Laser Drill and other worker machines with many Speed upgrades could need more power per job than their buffer held, so they never ran. The buffer now grows to fit.
- Storm Caller with an energy multiplier above 1 could never call a storm.
- Power Transmitter now takes power in as fast as its configured rate.
- Spatial Projector follows the machine energy multiplier.
- Fusion Power Port text said 128,000 FE/t; it pushes 512,000. JEI text now lists every Quantum tier.
- The in-game guide book was stuck at 1.47.1; it now matches the web guide.

### Note
- New defaults (solar, geothermal and the rest are in code; compact fusion and Quantum Cable are config values) only apply to a fresh config. To pick up the new compact fusion and Quantum Cable defaults in an existing world, delete `compactFusionOutput` and the `quantum` cable line from `config/tieredpower-common.toml`.

## 1.56.0 - Spatial Capture

### Added
- **Spatial Projector** and **Spatial Cells** (3x3x3, 7x7x7 and 15x15x15): pick up a whole cube of your world and put it down somewhere else.
  - Insert a cell and a cube outline appears on top of the projector, centred over it.
  - **Capture** takes every block in the cube into the cell - chests, machines and everything inside them - and leaves air.
  - Take the cell to another projector (or move this one), clear the outlined area, and press **Deploy** to put it all back exactly as it was.
  - Costs 40 FE per block moved. Unbreakable blocks such as bedrock can't be captured; deploying needs the area to be empty.
  - A full cell shimmers and its tooltip says how many blocks it holds. The blocks are stored with the world, so cells stay light to carry.

## 1.55.0 - Industrial Turbine

### Added
- **Industrial Turbine**: a multiblock steam turbine that grows with its size. Build a hollow box from 3x3x3 up to 13x13x13:
  - **Turbine Casing** on the edges and corners.
  - **Turbine Casing**, **Turbine Glass** and **Turbine Valves** on the walls, with the **Turbine Controller** in one wall.
  - **Turbine Rotors** (and air) inside.
- Pipe steam into any valve. Each rotor passes 200 mB/t of steam, and more rotors make every mB worth more: x1.3 with one rotor, up to x2 with 15 or more. At the default 2 FE per mB that's up to 4 FE per mB.
- The turbine holds 2,000 mB of steam per block of inside space, so bigger shells buffer more.
- Steam condenses back into water (1 mB for every 10 mB), which the valves push out - pipe it back to your boilers for a closed loop.
- Valves push power into whatever touches them, and cables can pull from them too.
- The blades appear on the rotors once the turbine forms and spin faster the more steam goes through. Build with Turbine Glass to watch.
- The controller's screen shows steam, output, flow, efficiency and stored power. Right-click it to see what's missing when it won't form. Jade shows the same.

## 1.54.0 - Utility Drones

### Added
- **Drone Station**: launches up to 4 **Utility Drones** and gives them work. Click its mode button to choose:
  - **Harvest**: picks ripe crops within 12 blocks and replants them (wheat, carrots, potatoes, beetroot, nether wart, cocoa, melons, pumpkins, sugar cane).
  - **Collect**: picks up dropped items within 12 blocks.
  - **Fetch**: brings items from a linked inventory to the station.
  - **Deliver**: takes items from the station to a linked inventory.
- Whatever the drones bring back goes into the station's 15 slots, which pipes, hoppers, belts and buses can empty (or fill, for Deliver).
- **Drone Remote**: right-click a chest or machine, then right-click a Drone Station to link them (up to 64 blocks apart). Sneak + right-click a station to clear the link.
- Drones fly over the ground to their job, spin their four rotors, carry their load underneath, and can't be hurt. They fly through blocks.
- Power: 20 FE/t for each drone out working. Redstone control pauses the station and calls the drones home.
- Breaking the station, or its chunk unloading, brings the drones in with their cargo - nothing is lost.

## 1.53.0 - Conveyor belts

### Added
- **Conveyor Belts** in three speeds: **Conveyor Belt** (1.25 blocks/s), **Fast** (2.5) and **Express** (5).
  - Items ride along as real dropped items, so you can watch everything move. Mobs and players ride too; sneak to stand still.
  - A belt faces away from you when placed. At its end, items go into any chest, machine, pipe or other inventory in front of it.
  - Hoppers, pipes and Export Buses can put items onto a belt (up to 4 item stacks on one belt block at a time). A hopper under a belt pulls items off it.
  - **Slopes**: sneak + right-click a belt with an empty hand to make it go up a block, then down a block, then flat again.
  - Items on belts don't despawn.
- **Belt Splitter**: items reaching the middle go out left, straight on and right in turn - only to sides with a belt or an inventory. Good for feeding a row of machines from one belt.
- **Filter Belt**: items in its filter (up to 9, right-click with an item to add or remove) turn off to one side; everything else goes straight on. Sneak + right-click with an empty hand to switch between left and right; the arrow on top shows which.
- Jade shows each belt's speed, and a Filter Belt's filter.

## 1.52.0 - Connector tiers and bigger Chunk Loaders

### Added
- **Machine Connector tiers**: Crafting Upgrades now work on Machine Connectors too. A connector sends 4 operations per crafting cycle into its machine at Basic, 8 at Advanced, 16 at Elite and 32 at Quantum, so bulk smelting and pulverising keep up with the Matrix.
  - Upgraded connectors get the same glowing tier strip on their face, keep their tier when broken, and show it in Jade and on right-click.
- **Chunk Loader tiers**: Tier Installers now work on the Chunk Loader and raise the largest area it can load: 5x5 chunks (Basic), 7x7 (Advanced), 9x9 (Elite), 11x11 (Ultimate) and 15x15 (Quantum).
  - Click the size button for a bigger area, right-click it for a smaller one. The screen shows the largest size for its tier.
  - Power use is still per chunk, so a 15x15 area costs 225 times a single chunk.
  - Breaking the loader gives the Tier Installers back, like other machines.

## 1.51.0 - Crafting tiers

### Added
- **Crafting Upgrades** (Advanced, Elite, Quantum): right-click a Crafting CPU or Crafting Accelerator to raise its tier, one tier at a time.
  - **CPU** jobs at once: 1 Basic, 2 Advanced, 4 Elite, 8 Quantum.
  - **Accelerator** crafts per cycle: +2 Basic, +4 Advanced, +8 Elite, +16 Quantum.
- Upgraded blocks get a glowing light strip in the tier's colour (blue, purple, pink) with tier pips, and keep their tier when broken.
- The tier shows in Jade, in the item tooltip, and when you right-click a CPU (with the network's total jobs at once).
- Matrix screen: the status line now shows crafts per cycle; hover it for the accelerator count.

### Changed
- An Assembly Matrix can now reach 256 crafts per cycle (was 64).
- The Jobs screen shows how many jobs the network runs at once instead of the CPU count.
- Crafting CPU power use is 10 FE/t per job slot, so higher tiers draw more.

## 1.50.3 - Matrix crafting without assemblers

### Fixed
- Starting a craft said "Add a Molecular Assembler with patterns to the network" even when an Assembly Matrix held the patterns. A network with only a Matrix (plus Machine Connectors or buses on the machines) now crafts as it should; Molecular Assemblers are optional.

## 1.50.2 - Buses work with the Matrix

### Fixed
- Machines with only an **Import Bus** or **Export Bus** on them didn't count as connected, so the Assembly Matrix couldn't use them. Buses now work as machine interfaces too: they appear in **Connected machines** and the Matrix sends pattern inputs through them.
- Results an Import Bus pulls out of a machine into storage now count towards the running craft, so the job doesn't wait forever for output the bus already took.
- A bus only reaches the block it faces, so a bus between two machines doesn't pick up the wrong one.

## 1.50.1 - Matrix fixes and no mobs in multiblocks

### Fixed
- Big storage networks: the network scan stopped after 2,048 blocks, so machines connected far from the controller (for example Alloy Smelters) never showed in the Matrix and couldn't be used for crafting. The limit is now 32,768.
- Mobs no longer spawn on machines, Matrix parts, Pattern Banks, Accelerators, reactor and bank casings, or solar/wind panels, so they can't spawn inside a multiblock.
- The Assembly Matrix interior now ignores grass, snow layers and other replaceable blocks instead of refusing to form.

### Added
- Matrix **Connected machines** panel: warns when the network is too big, and lists any Machine Connector that isn't touching a machine, with its position.
- Right-click a **Machine Connector** with an empty hand to see whether it's linked to the network and which machine it serves.

## 1.50.0 - Logic Controller

### Added
- **Logic Controller**: joins the storage network and runs up to 4 rules: IF a condition THEN a redstone signal out of a chosen side (or all sides).
  - **Item**: how many of an item storage holds, below or above a number.
  - **Power**: how full the batteries touching the controller are, in percent.
  - **Redstone**: the controller is receiving a signal.
- Each rule shows a light (lit when true) and its current reading on hover. Rules are checked twice a second.

## 1.49.0 - Energy Core

### Added
- **Energy Core**: a steel cage with a glowing orb inside that grows, brightens and spins faster as it fills, coloured by tier.
  - Five tiers: 1 billion FE (I), 10 billion, 100 billion, 1 trillion, 10 trillion FE (V).
  - **Core Upgrades** II-V: right-click the core, in order.
  - Keeps its tier and energy when broken. Comparator output by fill.
- **Input Pylon** and **Output Pylon**: link to the nearest core within 8 blocks by themselves. Each moves 1M FE/t at tier I up to 256M FE/t at tier V, with sparks travelling along the link. Output Pylons push into anything touching them.
- Jade and right-click readouts for the core and pylons.

## 1.48.0 - Liquid Experience

### Added
- **Liquid Experience** (20 mB = 1 XP point), with a bucket. It glows faintly when poured out.
- The **Mob Grinder** pushes the XP it collects into tanks and machines next to it as Liquid Experience, and pipes can pull it out. Right-clicking to collect XP still works.
- The **Enchanting Machine** has an XP tank (16,000 mB). With 40 mB per enchantment level in the tank, it enchants on XP instead of power, twice as fast. Without XP it runs on power as before.

## 1.47.1 - 3D gas cylinders

### Changed
- The **Gas Cylinder** is now a 3D canister: rounded steel body, a shoulder coloured by the gas inside, and a brass valve and handle. It shows up that way in hand, on the ground, in item frames and in the inventory.

## 1.47.0 - Biodiesel

### Added
- **Bio Refinery**: seeds (sunflowers best) are pressed into plant oil, and sugary crops are fermented into ethanol. They blend 1:1 into **Biodiesel**, which is pushed out to Fluid Pipes. 20 FE/t.
- **Diesel Generator**: burns 1 mB a tick. Biodiesel 200 FE/t, Rocket Fuel 300 FE/t, Creosote Oil 40 FE/t. Holds 200,000 FE and pushes out of every side.
- **Biodiesel** liquid and bucket.

## 1.46.0 - Gas containers, 3D Digital Miner

### Added
- **Gas Cylinder**: a handheld bottle for 8,000 mB of one gas. Right-click a machine or tank to fill or empty it. Its shoulder is coloured by the gas. The creative tab has a full one of every gas.
- **Gas Tank**: a pressure cylinder for 64,000 mB of one gas. Pipes connect on any side, and it keeps its contents when broken.

### Changed
- The **Digital Miner** is a 3D drilling rig: base plate, corner struts, radar screen, antenna and drill.

## 1.45.0 - Coke Oven, Industrial Blast Furnace, flat buses

### Added
- **Coke Oven** (no power): coal -> **Coal Coke** + 250 mB **Creosote Oil** (30 s); logs -> charcoal + 125 mB Creosote (15 s). Coal Coke burns twice as long as coal.
- **Industrial Blast Furnace** (no power): iron ingot + Coal Coke -> steel ingot (20 s).
- **Creosote Oil** (pipes or buckets) and **Treated Planks** (8 planks + a Creosote bucket).
- JEI pages for both machines.

### Changed
- **Import Bus** and **Export Bus** are now flat plates against the inventory, with cable stubs, so Storage Cable joins up neatly like the other panels.

## 1.44.1 - Build fix

### Fixed
- Compile error in 1.44.0: the Digital Miner used `ItemStack.hashItemAndTag`, which doesn't exist in Minecraft 1.20.1. It now fingerprints its slots from each item and its data.

## 1.44.0 - Digital Miner

### Added
- **Digital Miner**: a quarry that mines only the ores you choose.
  - Item Filters in its slots pick the ores (none = every ore).
  - An Enchanted Book of Silk Touch or Fortune I-III sets how they're mined (not used up).
  - It scans a 33x33 area under itself, layer by layer down to the bottom of the world, mining only wanted ores (800 FE each, up to 4 a second) and leaving everything else in place.
  - Changing the filters or book rescans from the top. The front's radar animates while mining. Jade shows its progress.

## 1.43.0 - Greenhouse system

### Added
- **Sprinkler**: hangs above crops and runs on water (pipe or bucket, 20 mB a second). It keeps farmland in a 9x9 area up to 6 below wet and gives a 15% chance of extra growth each second, with a shower of water.
- **Grow Lamp**: hanging lamp, 40 FE/t. Full light (crops grow indoors and at night) and a 10% chance of extra growth each second. The bulb glows when powered.
- **Fertilizer** (2 bone meal + rotten flesh + dirt -> 4): three bone meal in one. The **Crop Farmer** now takes it in its seed slots and fertilises unripe crops automatically.
- **Greenhouse Glass**: green-framed glass for building the greenhouse.

## 1.42.0 - Plenisher, Block Mover, Ore Scanner, drill filters

### Added
- **Fluidic Plenisher**: fills the space below and around it with piped-in fluid. One source block every half second (1,000 mB + 100 FE), up to 32 blocks, never above itself.
- **Block Mover**: picks up a chest, machine or tank with its contents and puts it down elsewhere, contents intact.
- **Ore Scanner**: powered handheld. Lists ores within 16 blocks with counts and the direction and distance to the nearest of each (5,000 FE per scan).
- **Laser Drill filters**: Item Filters in the drill's input slots block (or allow) ores.
- **Item Filter "Name" mode**: matches items whose id starts with the same word, so one deepslate ore blocks every `deepslate_...` ore. This works on pipes too.

## 1.41.0 - Automated brewing, gem ore processing

### Added
- **Brewing Machine**: 3 bottle slots, an ingredient slot and 3 outputs.
  - Fills glass bottles from its own water tank (piped or bucket), then brews all three with one ingredient: 5 s, 20 FE/t, no blaze powder.
  - Every vanilla potion and modded brewing.
  - JEI's + on a brewing recipe makes a 3-bottle pattern for autocrafting.
- **Pulverizer and Ore Purifier recipes** for redstone, lapis, coal, diamond, emerald, nether quartz and ancient debris ores. They use ore tags, so **deepslate** and other mods' variants work too, and the Chemical Washer picks them up automatically.

### Fixed
- Deepslate redstone ore (and the other gem/dust ores) couldn't be pulverized, purified or washed: those ores had no recipes at all.

## 1.40.0 - Radiation and Hazmat Suit

### Added
- **Radiation**, off by default: set `[radiation] enabled = true` in the config, with `strength` as a percentage.
  - Running fission reactors within 16 blocks and carried nuclear materials (depleted rods, plutonium, MOX, uranium) build up a dose that fades when you're clear.
  - Effects: nausea at 100 rad, weakness and hunger at 300, poison at 600, damage from 1,000. Dying resets it.
- **Hazmat Suit** (4 pieces, 25% protection each).
- **Radiation Shielding Module** for the Quantum Chestplate (100% protection).
- **Geiger Counter**: hold it for exposure and dose readings, with clicks.
- **Iodine Tablets**: -300 rad each.

### Fixed
- The **Pattern Encoder**'s Machine slot sat under the Encode button and couldn't be clicked. It's now bottom-left (labelled "Machine"), and its tooltip wraps.

## 1.39.0 - Orbital Mining Laser

### Added
- **Mining Satellite**: a second Rocket payload.
- **Laser Drill**: place it under open sky and it claims one of your Mining Satellites. While powered, a laser beam comes down from orbit and it drills one ore a second (2,000,000 FE each, about 100,000 FE/t). Ores come from the `forge:ores` tag, other mods' ores included, with rare ores rarer.
- **Laser Lenses** (iron, copper, gold, redstone, diamond, uranium): in the drill's input slots, each makes its ore x10 as likely. They stack.
- Satellites now have a type: Receiver Dishes claim Solar Satellites and Laser Drills claim Mining Satellites. Existing satellites stay solar.

## 1.38.1 - Rocket liquid buckets

### Added
- **Buckets** of **Rocket Fuel**, **Liquid Methane** and **Liquid Oxygen**, in the creative tab and filled from the machines' tanks like any bucket. The liquids can also be poured into the world.
- The **Launch Controller**, **Cryogenic Condenser** and **Fuel Refinery** accept buckets (and other fluid containers) on right-click. A rocket needs 16 buckets of Rocket Fuel.

## 1.38.0 - Rocket program, part 2: launch and orbit

### Added
- The **launch site**:
  - **Launch Pad**: hazard-striped deck plates, 3x3.
  - **Launch Tower**: steel lattice with diagonal bracing.
  - **Launch Controller**: desk console with an angled screen.
- Put a **Rocket** and a **Solar Satellite** in the controller and pipe in 16,000 mB of Rocket Fuel. The rocket stands on the pad.
- Press **LAUNCH**: a 10-second countdown, then lift-off with flame and smoke. The satellite reaches orbit and the server is told. A checklist on the screen shows what's missing.
- **Receiver Dish** (tilted dish on a post): right-click to claim one of your satellites for about 100,000 FE/t, day and night. Sneak + right-click releases it. Holds 10M FE and pushes out of every side.
- The **Rocket** has a 3D model (body, nose cone, fins, engine bell), used for the item and on the pad.
- Config: `satelliteOutput`. Jade and the Multimeter show the dish's output.

## 1.37.0 - Rocket program, part 1: fuel and parts

### Added
- **Liquid Methane**, **Liquid Oxygen** and **Rocket Fuel**: liquids, so they go in Fluid Pipes.
- **Cryogenic Condenser**: 40 mB methane or oxygen + 4 mB nitrogen + 200 FE -> 20 mB of the liquid.
- **Fuel Refinery**: 10 mB Liquid Methane + 20 mB Liquid Oxygen + 100 FE -> 30 mB Rocket Fuel.
- Rocket parts from existing machines:
  - **Hull Plate**: Compressor, 4 steel plates.
  - **Rocket Engine**: Smithing Press, Rocket Nozzle + netherite template + Quantum Alloy.
  - **Fuel Tank Section**, **Guidance Computer** and **Nose Cone**.
- The **Rocket** and the **Solar Satellite** payload.
- JEI pages for both new machines.

The Launch Pad, launch and Receiver Dish come in part 2.

## 1.36.0 - Quantum Suit modules

### Added
- **Quantum Suit modules**: wear the piece and right-click the module to fit it.
  - **Magnet** (chestplate): pulls items and XP orbs from 8 blocks. Sneak to pause it.
  - **Flight** (chestplate): creative-style flight while charged, 100 FE/t while flying.
  - **Auto-Feed** (helmet): eats the most filling safe food when you're hungry.
  - **Jump Boost** (leggings): about three-block jumps.
  - **Water Walking** (boots): walk on water. Sneak to go under.
- **Suit Modules key** (default **V**): a screen listing fitted modules with On/Off and Remove (the module is returned).

## 1.35.0 - New logistics

### Added
- **Wireless Sender** and **Wireless Receiver**: item and fluid transport at any distance, across dimensions. The Sender holds 9 item slots and a 16,000 mB tank. Each linked Receiver takes up to 32 items and 2,000 mB twice a second and pushes them into its neighbours. Link them with the Wireless Linker.
- **Item Buffer** (27 slots) and **Fluid Buffer** (32,000 mB): fill from any side except the front, and push out of the front (16 items / 4 ticks, or 1,000 mB / tick).
- Jade shows each new block's state.

### Changed
- The **Power Linker** is now the **Wireless Linker**: it links Power Transmitters to Receivers and Wireless Senders to Receivers. Existing linkers keep working.

## 1.34.0 - Polish and usability

### Added
- **JEI recipe pages** for the Chemical Washer, Oxygen Furnace, Salt Evaporator, Brine Electrolyzer, Bio-Digester, Isotope Separator, Air Separator, Tritium Breeder, Particle Collider and Antimatter Reactor. The Smithing Press is listed on vanilla smithing. JEI's + in the Pattern Encoder fills in the machine from these pages.
- **Power history graph**: right-click a Power Monitor for generated / used / battery fill over the last 10 minutes or 2 hours, with hover values and auto-refresh. Sneak + right-click shows the old readout.
- **Job steps**: in a terminal's Jobs screen, click a job to see each step: how many are left, how many are running, and on which machine.
- **Config [endgame]** section: antimatter FE per mB, Particle Collider cost, Power Transmitter rate, Quantum Drill FE per block and Lightning Collector FE per strike.

## 1.33.0 - Antimatter, fluid filters, Quantum Drill

### Added
- **Antimatter** gas, made by the **Particle Collider**: 10 mB deuterium + 10 mB tritium + 20,000 FE -> 1 mB antimatter, one per tick (more with Speed Upgrades).
- **Antimatter Reactor**: burns antimatter at 50,000 FE per mB, up to 20 mB/t (1,000,000 FE/t). Holds 50M FE and pushes out of every side. That's 2.5x the power used to make the antimatter.
- **Fluid Filter**: up to 9 fluids, Allow or Block, for fluid and gas pipe connections. Right-click a connection to install; sneak + right-click with an empty hand to remove. On Pull connections, an allow-list pulls just those fluids from tanks holding several.
- **Quantum Drill**: drill + shovel + axe + hoe, very fast, mines 1x1 / 3x3 / 5x5 / 7x7 (shift + right-click to switch). 50M FE, 400 FE per block.

### Changed
- Drill area mining is generalised to any radius. The Advanced Drill still switches between 1x1 and 3x3.

## 1.32.0 - Fusion upgrades

### Added
- **Plasma Coils Mk I, II, III**: right-click a Fusion Reactor or Fusion Controller, fitting them in order, for x2 / x3 / x4 output. Fuel burns x1.5 / x2 / x2.5 as fast, so you get more power per fuel pair at every tier. Coils drop back out when the reactor is broken. Jade shows the tier.
- The **Reactor Gauge** now works on **Fusion Reactors**, single-block and multiblock (including built into the multiblock's walls). It shows plasma temperature, output and plasma tier.

### Changed
- The Fusion Controller's output limit is raised to 512,000 FE/t so a Mk III reactor can deliver its power.

## 1.31.5 - Panels plug into cables, wavier power line

### Fixed
- **Panels and Storage Cable left a gap**: the panel sits flat against its machine, while the cable ends at the middle of the panel's block space. Panels (both terminal panels, the Assembler Panel and the Machine Connector) now draw a short cable piece from each side with Storage Cable back to the plate, so the cable visibly plugs in.

### Changed
- The power cables' glowing line is **thinner** (1 pixel) and animates as a smooth **travelling wave**: a bright pulse rolling along the cable.

## 1.31.4 - Assembler Panel, see-through cables

### Added
- **Assembler Panel**: a flat Molecular Assembler (9 patterns, upgrades, same screen) that sticks onto a machine's face. Sneak + right-click to place it on a machine. It converts to and from a Molecular Assembler in the crafting grid.

### Changed
- **Power cables are see-through** (every tier). A **glowing yellow line** flows through them while power is moving, and goes dark when the network is idle. It updates about once a second.

## 1.31.3 - Set a pattern's machine in the Matrix

### Added
- In the **Matrix Controller**, click a processing pattern while holding a machine's item to set which machine it runs on. There's no need to re-encode older patterns. Hovering a processing pattern with no machine shows a reminder.

### Fixed
- JEI's item list drew over the Matrix Controller's Connected machines panel.
- The page arrows overlapped the page number.

## 1.31.2 - Easier Matrix building

### Changed
- **Pattern Banks and Crafting Accelerators can take the place of any wall block** of the Assembly Matrix, as well as going inside. There's no need to open the box up to add them.

## 1.31.1 - Matrix fixes, flat Machine Connector

### Fixed
- **Patterns could vanish** when put into a Matrix Controller with no Pattern Banks inside. The slots now refuse patterns until there's a Bank, and the screen says "Formed - put Pattern Banks inside".
- The Matrix Controller's status line overlapped the "Inventory" label. It has its own row now.

### Changed
- The **Machine Connector** is now a **flat panel**, like the terminal panels. Sneak + right-click to stick it onto a machine's face, then connect Storage Cable to it.

### Added
- The **Matrix Controller** screen shows a **Connected machines** panel: every machine touching a Molecular Assembler or Machine Connector on the network, counted by type.

## 1.31.0 - Assembly Matrix and Machine Connector

### Added
- **Assembly Matrix** multiblock: a hollow 3x3x3-7x7x7 box of **Matrix Casing** / **Matrix Glass** with a **Matrix Controller** in a wall, holding **Pattern Banks** (27 patterns each) and **Crafting Accelerators**. The controller's screen pages through the banks.
  - Crafting patterns are crafted inside the Matrix: 2 per cycle, +2 per accelerator.
  - Processing patterns run on any machine of the right type that a Molecular Assembler or Machine Connector touches, anywhere on the network, in parallel across machines.
- **Machine Connector**: joins the machine it touches to the network for the Matrix's patterns. No slots, no screen, 4 operations per cycle.
- Patterns can name their **Machine**. The Pattern Encoder (processing mode) has a Machine slot, and JEI's + fills it in for this mod's machines, furnace recipes and smithing. Pattern tooltips show it.

### Changed
- **Molecular Assemblers are back to 9 pattern slots.** Patterns from the extra slots added in 1.28.1 drop out beside the assembler the first time it loads, so nothing is lost.
- Results from machines are now credited per job step (each step only takes what it's owed), so two jobs making the same item no longer grab each other's results.

## 1.30.1 - Build fix

### Fixed
- Compile error in 1.30.0: the four automation machines' menu registrations referred to themselves by simple name inside their own initializer, which Java doesn't allow. They now refer to themselves through the class name.

## 1.30.0 - Storage Monitor

### Added
- **Storage Monitor**: right-click it with an item to show that item and its live count in the storage network on its face (green when stocked, red at zero). Sneak + right-click with an empty hand to clear it. Comparator output: 0 at none, +1 per doubling up to 15, for low-stock alarms or redstone-triggered production. Respects storage security.

## 1.29.0 - More automation

### Added
- **Block Breaker**: breaks the block in front once a second (diamond-pickaxe drops), 400 FE per block.
- **Block Placer**: places blocks from its inputs in front, twice a second, 100 FE per block.
- **Tree Farm**: plants saplings on a 9x9 patch in front, uses bone meal, and fells whole grown trees (logs and leaves) keeping everything. 200 FE per job.
- **Animal Ranch**: on a 9x9 area in front, it breeds pairs with feed (up to 24 animals), shears sheep, milks cows into empty buckets, and collects eggs and feathers. 200 FE per job.
- All four have 3 input slots, a 3x3 output buffer pushed into neighbouring inventories, Speed/Energy upgrades and redstone control.
- The **Molecular Assembler**'s core is now animated: a swirl while working, a slow pulse when idle.

## 1.28.1 - Bigger, see-through assemblers

### Changed
- **Molecular Assemblers hold 27 patterns** (3 rows of 9) instead of 9. Existing assemblers keep their patterns and upgrades where they were.
- **Assemblers work from any face** of this mod's machines: the machine's Sides settings no longer apply to assemblers. Items still go into the right slots (ingredients in, results out). Other mods' machines still use their own sided rules.
- **Transparent assemblers**: a glass case with a crafting core inside that glows while working.

### Notes
- Furnace recipes don't need patterns at all: an assembler touching an Electric Furnace gives the network every furnace recipe (since 1.27.3). Several assemblers can also share one machine.

## 1.28.0 - Wireless power

### Added
- **Power Transmitter** and **Power Receiver**: link receivers to a transmitter with the **Power Linker** (right-click the transmitter, then each receiver). Receivers draw from their transmitter and push power into everything around them. Each transmitter supplies up to 64,000 FE/t in total, shared between its receivers, and both hold 1M FE. Works at any distance and **across dimensions** (10% loss). Both ends' chunks must be loaded. Jade shows each end's status.

## 1.27.3 - Smarter autocrafting

### Added
- **Built-in smelting**: if any Molecular Assembler touches an Electric Furnace, the planner knows every furnace recipe without patterns. A missing Iron Ingot is smelted from raw iron, iron ore or iron dust in storage, whichever is most plentiful. Encoded patterns always take priority.
- **Exact job status**: a stuck job names the machine it's waiting on (name and position), the ingredient it needs ("Needs 2 Raw Iron in storage"), a machine that won't take its inputs, or that no assembler holds the pattern. Shown after a couple of seconds in the Jobs screen.

### Fixed
- **Ore Purifier** (and any pattern made with JEI's + for a machine that uses water) never received anything from an assembler: the pattern listed water as an input and storage had none. Water now never counts as missing; it's sent if storage has some, otherwise the machine's own Sink or pipe supplies it.
- Processing patterns now send whichever accepted ingredient is in stock, instead of only the first one encoded.

## 1.27.2 - Charger actually charges

### Fixed
- The **Charger** accepted items but never charged them: its energy buffer is closed for output (like every machine's), so the transfer it used always moved 0 FE. It now gives the item energy straight from its own buffer, up to 8,000 FE/t. The Wireless Charger was already doing this correctly.

## 1.27.1 - Charger fix

### Fixed
- The **Charger** refused an empty jetpack (and possibly other FE items). It now recognises this mod's powered items (jetpacks, Quantum Suit, drills, chainsaws, Portable Battery) directly, and accepts any item with an energy store that has room. The **Wireless Charger** uses the same lookup.

### Added
- Holding an item over the Charger's input slot that it won't accept now shows why in a red tooltip.

## 1.27.0 - Smithing and charging in autocrafting

### Added
- **Smithing Press**: a powered smithing table (netherite upgrades, armour trims, other mods' smithing recipes). Each input only goes into the slot a recipe uses it in, so assemblers and pipes can feed it. Use it with processing patterns, for example from JEI's + on a smithing recipe.
- **Charging patterns**: in Processing mode the Pattern Encoder's ⚡ button fills energy items in the outputs to full. With the same item as input, an assembler touching a Charger sends the item in and takes it back fully charged.
- **Quantum Power Core**: 4 Quantum Alloy + 4 Superconductor + an Ultimate Battery Box.

### Changed
- The **Quantum Suit** uses a Quantum Power Core instead of a Quantum Energy Cell. That removes the 20 Nether Stars and about 64 netherite from the full suit, and the whole suit can now be autocrafted.
- **Crafting patterns ignore charge**: a charged copy of an energy item (jetpack, tool, battery box or energy cell that kept its charge) in storage counts like an empty one, just as at a crafting table. Enchanted or renamed items are never used this way. Processing patterns stay exact, so charging patterns still work.
- The web guide has a **Changelog** section built from this file.

## 1.26.0 - Quantum Suit

### Added
- **Quantum Suit** (helmet, chestplate, leggings, boots): powered armour tougher than netherite (20 armour, 16 toughness for the full set). Each piece stores FE (10M, chestplate 20M) and charges in a Charger or near a Wireless Charger.
  - Helmet: night vision and water breathing (20 FE/t).
  - Chestplate: built-in jetpack, the fastest in the mod (120 FE/t while thrusting).
  - Leggings: Speed II (10 FE/t).
  - Boots: no fall damage, plus step assist (walk up full blocks).
  - **Energy shield**: each charged piece absorbs 22.5% of damage (90% for the full suit) at 1,000 FE per point absorbed. Damage that bypasses invulnerability (/kill, the void) isn't shielded.
- The creative tab has both empty and fully charged suit pieces.

## 1.25.0 - Nuclear fuel cycle

### Added
- **Plutonium**: the Chemical Washer reprocesses a Depleted Fuel Rod (with chlorine and water) into 1 Plutonium Dust. Smelt it into Plutonium Ingots.
- **MOX Fuel Rod**: steel + 2 uranium + 1 plutonium makes 2. Works in both Fission Reactors and their Fuel Ports. Lasts twice as long, with +50% power and +50% heat. The multiblock burns MOX first. Depletes into a normal depleted rod.

### Fixed
- The **craft request screen**'s plan list ran under the Back/Start buttons and overlapped the "scroll for more" hint. The list now sits in its own clipped box above the buttons (7 rows), with a scrollbar and an "x-y of n" counter.

## 1.24.1 - Reactor Gauge in reactor walls

### Changed
- The **Reactor Gauge** is now a multiblock part: build it into a Fission Reactor's wall in place of any Casing or Glass block (not edges or corners), and the reactor still forms. The controller links it, so it shows the reactor's status from anywhere on the walls. Placing it against a single-block reactor still works.

## 1.24.0 - Chemical ore processing

### Added
- **Chlorine** gas and **Salt** (tagged forge:dusts/salt).
- **Salt Evaporator**: 1,000 mB water -> 1 salt (5 s, 40 FE/t).
- **Brine Electrolyzer**: 1 salt + 500 mB water -> 250 mB chlorine + 250 mB hydrogen (2 s, 200 FE/t).
- **Chemical Washer**: 1 ore or raw ore + 100 mB chlorine + 500 mB water -> 4 dust (6 s, 300 FE/t). It works on every ore the Ore Purifier handles, including other mods' ores, and takes Tier Installers.

## 1.23.0 - Reactor extras and more tiered machines

### Added
- **Reactor Gauge**: placed against a Fission Reactor or Fission Controller, its face shows heat %, FE/t, status (Running / Idle / SCRAM) and fitted add-ons. Its comparator output follows heat (0-15).
- **Neutron Reflector**: right-click a fission reactor so its fuel rods last 50% longer. Shown in Jade; drops back out when the reactor is broken.
- **Tier Installers** now also work on the **Alloy Smelter** and **Fluid Mixer** (3 / 5 / 7 / 9 at once).

## 1.22.0 - Disk partitioning and storage security

### Added
- **Disk Workbench**: partition a Storage or Fluid Disk to up to 18 chosen items or fluids (empty = anything), with Fill-from-contents and Clear. Set a priority from -9 to +9. Settings are stored on the disk.
- Storing now fills **higher-priority disks first**. At the same priority, disks already holding the item come first, then disks partitioned for it, then the fullest.
- **Security Terminal**: locks a storage network to its owner (the placer) and trusted players, added by name. Others can't open its terminals, Drive Bays, buses, assemblers or Stock Keeper; can't use or link wireless terminals; can't break its blocks; and can't place storage parts, pipes or inventories against it. Only the owner can break the terminal, and server operators always have access. Networks without one are unchanged.

## 1.21.2 - Tiered Industries

### Changed
- The mod is now called **Tiered Industries**: mod list, guidebook, creative tabs, key bindings and the web guide. The internal id stays `tieredpower`, so existing worlds, items, configs and the jar's file name are unaffected.
- The web guide has a **Roadmap** section: what's done, what's next, and ideas.

## 1.21.1 - Quarry control and autocrafting loop fix

### Added
- **Quarry Planner**: mark two corners, then right-click a Quarry to dig exactly that rectangle (up to 65 x 65) from the higher corner down.

### Fixed
- **Autocrafting loops**: the planner no longer uses the item it's making as an ingredient further down the same chain. For example, it won't pulverize stored gold ingots just to smelt them back into gold ingots, which a Stock Keeper would repeat forever. Such requests now show as "can't craft".
- The **Quarry** now only pushes mined items out through faces set to Out (or In + Out) in its Sides window.

### Changed
- Author is now BobRoflza.

## 1.21.0 - Machine upgrades

### Added
- **Upgrades window** on every machine with upgrades (the green arrow button). It holds Speed and Energy cards and shows the Muffler and the machine's tier.
- **Tier Installers** (Advanced, Elite, Ultimate, Quantum): right-click to upgrade the Electric Furnace, Pulverizer, Compressor, Electric Sawmill, Ore Purifier or Rock Crusher in place, so it runs 3 / 5 / 7 / 9 operations at once. Install them in order. Power scales with how many run. Installers drop back out when the machine is broken.
- **Muffler**: right-click any machine to silence it, and remove it from the Upgrades window.
- Jade shows a machine's tier and whether it's muffled.

### Changed
- Upgrade cards stack to **8** per slot (was 4).
- **Efficiency Upgrade** is renamed **Energy Upgrade**. It's the same item and effect (-15% power each).
- Upgrade slots moved from the left of machine screens into the Upgrades window. Existing upgrades stay in place.

## 1.20.0 - Gas system part 2: oxygen steel and biogas

### Added
- **Methane** gas (biogas).
- **Oxygen Furnace**: 1 iron ingot or dust + 100 mB oxygen -> 1 steel ingot in 3 s at 80 FE/t, with no coal.
- **Bio-Digester**: crops, seeds, saplings, leaves, flowers, rotten flesh and more -> 80 mB methane each, with no power needed. What it accepts is set by the `tieredpower:biomass` item tag.

### Changed
- The **Gas Burner Generator** also burns methane: 12 FE/mB, or 15 with the same amount of oxygen. Hydrogen is still burned first.

## 1.19.0 - Gas system: fusion fuel, air separation, nitrogen cooling

### Added
- New gases: **Deuterium**, **Tritium** and **Nitrogen**.
- **Isotope Separator**: water -> deuterium (50:1), 400 FE/t.
- **Tritium Breeder**: lithium ingot -> 250 mB tritium, next to a running fission reactor. No power needed.
- **Air Separator**: power -> nitrogen (16 mB/t) + oxygen (4 mB/t). Needs open air.
- **Cryo Injector**: feeds nitrogen (20 mB/t) to a fission reactor for +50% power.

### Changed
- **Fusion Reactors** (single-block and multiblock) take deuterium and tritium by pipe: 250 mB of each equals one pair of cells. Gas is burned before cells, and cells still work.

## 1.18.0 - Storm Caller and quality of life

### Added
- **Storm Caller**: uses 4,000,000 FE and a **Storm Charge** to start a 5-minute thunderstorm. It follows its redstone setting, so it can be triggered by a comparator on a Lightning Collector.
- **Jade info**: Storage Controller (items, fluids, jobs), Drive Bay (disks, fill), Molecular Assembler (patterns, speed), Stock Keeper, Lightning Collector (stored, strikes), Storm Caller (status), Geothermal Generator and Water Wheel (output or what's missing).

### Changed
- The **creative tab** is split into five: Power, Machines, Pipes & Cables, Storage & Crafting, Tools & Materials.
- The **Wrench** now rotates six-way blocks (Import/Export Buses, terminal panels) through every direction, as well as machines.

## 1.17.0 - Lightning, geothermal and water power

### Added
- **Lightning Collector**: put a vanilla Lightning Rod on it. Every strike on the rod adds 2,500,000 FE. It stores 25M FE and outputs up to 20k FE/t. It glows after a strike, has a comparator output, and the Multimeter shows strikes caught.
- **Geothermal Generator**: 20 FE/t per touching lava source (up to 120 FE/t) and 5 FE/t per magma block. No fuel.
- **Water Wheel**: 8 FE/t per side with flowing water (up to 32 FE/t). Animated while turning.

## 1.16.1 - Fluids no longer break pipes and cables

### Fixed
- Water and lava washed away (broke and dropped) energy cables, item/fluid/gas pipes, Storage Cable, Import/Export Buses, terminal panels and other thin blocks. They now count as solid for fluids, so a pipe or cable blocks the flow like any other block.

## 1.16.0 - Fluids in autocrafting

### Added
- **Fluid and gas inputs/outputs in processing patterns** (3 each). Set them with a bucket or tank in the Encoder, or with JEI's + on machine recipes. Assemblers fill the machine from storage and drain fluid/gas results back into storage. Fluid-only patterns (e.g. water -> hydrogen) are allowed.
- The **planner** uses stored fluids, runs fluid-making patterns when a fluid is short, and counts byproducts as available for later steps. Plans show fluid lines.
- **Speed Upgrades in Molecular Assemblers**: +1 craft or machine operation per cycle each, up to 4.

## 1.15.0 - Stock Keeper and Jobs screen

### Added
- **Stock Keeper**: keeps up to 9 items at a target amount. It starts crafting jobs automatically when stock runs low, and shows per-item status (in stock / crafting / can't craft).
- **Jobs screen**: a Jobs button in every terminal lists all crafting jobs with live progress and a Cancel button for each.

## 1.14.0 - Autocrafting (part 2: processing and substitutes)

### Added
- **Processing patterns** (machine recipes): up to 9 inputs and 3 outputs with amounts. A Molecular Assembler holding one pushes the inputs into the machine it touches and pulls the results back into storage. The planner chains machine and crafting steps together.
- **Substitutes** for crafting patterns (on by default): any item the recipe accepts in a slot can be used, such as any planks for a piston. It uses whatever you have most of. Toggle with the S button.
- Pattern Encoder **C/P mode button**. JEI's + now works on machine recipes (Pulverizer, Alloy Smelter, furnace, other mods) to fill a processing pattern.

### Changed
- Pattern tooltips show inputs/outputs, and how many substitutes each slot accepts.
- 1.13.0 crafting patterns still work, but without substitutes. Re-encode them to get substitutes.

## 1.13.0 - Autocrafting (part 1: crafting recipes)

### Added
- **Pattern Encoder**: records crafting recipes onto **Blank Patterns**. Click items into its grid (copies only) or use JEI's + button.
- **Molecular Assembler**: holds 9 Crafting Patterns and does one craft every 8 ticks. More assemblers craft faster.
- **Crafting CPU**: runs one crafting job at a time. Right-click to see job progress; sneak + right-click to cancel.
- **Crafting from the terminals**: items your patterns can make appear marked "Craft" (or with a green + if some are already stored). Click, pick an amount, see the plan (from storage / to craft / missing) and press Start. Sub-parts are crafted automatically. Jobs survive a restart, and you get a chat message when a job finishes or is stuck waiting for ingredients.

## 1.12.3 - Item flow diagnostics

### Added
- **Multimeter on an item pipe**: lists every connection with its mode, the block on each side, what each Pull connection can take out, whether each Push destination would accept those items, and whether the network is stuck.
- **Multimeter on a machine**: shows each face's item setting, what the Out faces have ready, the power face and whether auto-output is on.

### Changed
- The **Sides** window now names faces as you see the machine standing in front of it: Front, Back, Left, Right and Top, with the bottom as the power face. The world compass direction shows when you hover a face. Existing settings are kept.

## 1.12.2 - Two-ingredient automation

### Fixed
- **Alloy Smelter with pipes, buses and hoppers:** each ingredient now goes into its own input slot. Before, one ingredient could fill both slots and block the recipe. Items that don't make a recipe with what's already loaded are refused, so the machine can't get stuck with an impossible pair. Players can still place items by hand as before.

## 1.12.1 - Machine sides rework

### Fixed
- An **Out** face now hands over finished items to pipes, Import Buses and hoppers on any side. Before, output slots were only reachable from the bottom, whatever the side setting said.

### Changed
- **Powered machines take power through the bottom face only**, leaving the other five faces for items. The Electric Pump and Quarry still take power on any side. To go back to any-side power, set `[machines] powerFromBottomOnly = false` in tieredpower-common.toml.
- The **Sides** window now opens to the right of the machine screen. Faces are shown by compass direction (N/E/S/W and Up) with the machine's front marked, plus the bottom shown as the power face and a colour legend. JEI's item list keeps clear of it.

## 1.12.0 - Tier 5 (Quantum)

### Added
- **Quantum Alloy Ingot**: made in the Alloy Smelter from 1 netherite and 4 superconductor ingots (makes 2).
- **Quantum tier** blocks:
  - Cable (512k FE/t)
  - Battery Box (2B FE, 2M FE/t)
  - Energy Cell (16.4B FE in an Energy Bank)
  - Fluid and Gas Pipes (256k mB/t)
  - Item Pipe (2,048 items/s)
  - Fluid Tank (4,096 buckets)
  - Solar Panel (7,500 FE/t)
- Config entries for the Quantum cable rate and battery capacity/rate.

### Changed
- Cleared all deprecation warnings: the supported replacements for `new ResourceLocation(...)`, JEI's `getBackground()` and `ModLoadingContext.get()`.

## 1.11.0 - Access points and creative tools

### Added
- **Wireless Access Point**: wireless terminals work within 128 blocks of any access point on their network (+10 FE/t each on the controller).
- **Creative Energy Cell**: unlimited power for testing.
- **Creative Storage Disk** and **Creative Fluid Disk**: practically unlimited capacity (63 types each).

### Changed
- Very large numbers show as T (trillions) or infinity instead of long digit strings.

## 1.10.0 - Wireless terminals

### Added
- **Wireless Terminal** and **Wireless Crafting Terminal**: link one to a Storage Controller (sneak + right-click) and open your storage from up to 64 blocks away. It runs on FE (500k, 5 FE/t while open), and the JEI + button works on the crafting version.
- **Open Wireless Terminal** key (unbound by default; set it under Controls > Key Binds > Tiered Power).

## 1.9.0 - Fluid and gas storage

### Added
- **Fluid Storage Disks** (4K-256K buckets, 63 types) for fluids and gases, in the same Drive Bays.
- **Storage Interface**: pipes, machines and reactors can push into, or pull out of, storage from anywhere on the cable.
- **Storage / Crafting Terminal Panels**: flat, AE2-style terminals for walls, floors and ceilings. Craft them from the blocks and back.
- The terminals show fluids and gases: fill a held bucket or tank from storage, or pour one in. There's a new All / Items / Fluids view button.
- The Import and Export Buses now move fluids and gases too. Filter a fluid with a bucket or tank of it.

### Changed
- The bus screens now spell out the direction (block -> storage or storage -> block) and show whether the bus is connected to a powered network.

## 1.8.0 - Storage expansion

### Added
- **Crafting Terminal**: craft straight from storage. Grid slots refill from storage, so shift-clicking the result crafts in bulk. There's a clear button, and the grid empties into storage when closed.
- **JEI "+" button** for the Crafting Terminal: fills the grid from storage and your inventory, with missing items shown in red. The Crafting Terminal is also listed as a JEI crafting station.
- **Import Bus**: pulls items from the inventory it faces into the network, with an optional filter.
- **Export Bus**: pushes filtered items from the network into the inventory it faces.

## 1.7.0 - Item storage network

### Added
- **Storage Controller**: the powered heart of the network (20 FE/t + 5 per Drive Bay). It works as one big inventory for Item Pipes and hoppers.
- **Storage Cable**: links network parts together.
- **Drive Bay**: holds 8 Storage Disks.
- **Storage Disks**: 4K / 16K / 64K / 256K items, 63 types each. Contents stay on the disk.
- **Storage Terminal**: a searchable grid of everything stored. Take, store, shift-click, search by name or @mod, and sort by count or name.

## 1.6.0 - Monitoring and control

### Added
- **Multimeter**: right-click a cable for the whole network's power (in, to machines, into storage, stored), or any block for its energy, output and tanks.
- **Energy Meter**: an in-line meter. Power goes in the left side and out the right, and the screen shows FE/t and a total. A redstone signal turns the flow off, and a comparator reads how much is flowing.
- **Power Monitor**: shows a whole cable network on its screen. A comparator reads how full the batteries are.
- Both Fission Reactors give comparators a signal for their heat.
- Cable networks now measure their own power flow.

## 1.5.0 - Gases

### Added
- **Hydrogen and Oxygen** gases. They travel in Gas Pipes and can be kept in Fluid Tanks.
- **Gas Electrolyzer**: splits water into hydrogen and oxygen (10 mB water -> 20 mB H2 + 10 mB O2 per tick, 200 FE/t). It refuels Hydrogen Jetpacks.
- **Gas Burner Generator**: burns hydrogen at 7 FE/mB (280 FE/t), or 9 FE/mB (360 FE/t) with oxygen. It idles when full.
- **Steam Engine**: a cheap early steam generator, 1 FE per mB of steam, up to 40 FE/t.
- **Steam Hammer**: runs Pulverizer recipes on steam (20 mB/t, 7.5 s per item).
- **Hydrogen Jetpack**: a 64,000 mB tank, 2 mB/t while thrusting.

## 1.4.0 - Fixes and quality of life
- Teleporters work (crouching on a pad now triggers it), and pads can be named: right-click a pad to rename it and pick a destination from every pad in the world.
- Machine side configuration: a Sides button in every machine GUI sets each face to In / Out / In+Out / Off for hoppers, pipes and auto-output.
- Fuel rods stack to 16, depleted rods to 64.
- Coal Generator, Boiler and both Fission Reactors pause instead of wasting fuel when their output is full.
- New Condenser: steam back into water (10:1), for a closed water loop on fission reactors.

## 1.3.0 - Polish and balance
- Balance review of every tier (see docs/BALANCE.md). Enchanting Machine now 400 FE/t.
- New config knobs: generationMultiplier and machineEnergyMultiplier scale the whole mod.
- Wind Turbine has a real spinning three-blade rotor.
- Coal Generator has a brick chimney that smokes while it burns.
- Every item: "Hold Shift for details" shows its guide description in the tooltip.

## 1.2.1
- Pipe connections now have four Wrench modes: Push, Pull, Push + Pull and Disabled (Item, Fluid and Gas Pipes). Pipe-to-pipe: connect / disconnect.
- Wrench picks the right connection more reliably (clicking the centre uses the face you clicked).
- Jade shows each pipe connection's mode and warns when a pipe network has nothing to deliver to.

## 1.2.0 - More machines
- Enchanting Machine: enchant items and books at level 10/20/30 with power and lapis instead of XP.
- Mob Grinder: kills mobs in front of it with player drops; stores XP (take it from the GUI); hostile-only or all mobs.
- Spawner Controller: powered vanilla spawners work without a player nearby and spawn faster.
- Fluid Mixer: concrete powder -> concrete, dirt -> mud, lava + water -> obsidian.
- Teleporter pads + Teleporter Linker: crouch to travel between linked pads, even across dimensions.

## 1.1.2
- Fission Fuel Port now only takes fuel in - right-click it with rods to load them (or use hoppers/pipes); empty hand shows what's loaded.
- New Fission Waste Port pushes depleted rods out into pipes, chests or hoppers.

## 1.1.1
- Multiblock Fission Reactor now floods its empty interior with water from the Coolant Ports - the rods sit underwater, and every water block adds 2 cooling. Coolant Channels are now optional.
- Clearer messages: the ports only work once the reactor has formed (it needs at least one Fuel Assembly).

## 1.1.0 - Multiblock Fission Reactor
- Any box from 3x3x3 to 13x13x13: Fission Casing, Fission Glass, Fuel / Coolant / Power Ports and the Fission Controller.
- Inside: Fuel Assemblies (1,000 FE/t each, +10% per touching assembly) and Coolant Channels (+10 heat/t cooling).
- Water cooling makes steam, pushed out of Coolant Ports. SCRAM at 100% heat (or meltdown if enabled).
- Fuel Assemblies glow and pulse while running. The single-block Fission Reactor remains as the starter version.

## 1.0.0 - first public release
- **In-game guidebook**: every player gets the Tiered Power Guide on first join (or craft a book + copper ingot).
  Categories, search, item pages with key numbers, live crafting recipes and everything each machine can process.
- Mod logo and full metadata; MIT license.

## 0.19 - Pipe upgrades
- Items visibly travel through Item Pipes (glass tubes); filter match modes (Item / Tag / Mod); connection priorities 0-9.

## 0.18 - Solar, wind and fission
- Solar Panels (4 tiers), Wind Turbine (height and weather based), uranium ore and the Fission Reactor (heat, water cooling, steam, SCRAM).

## 0.17 - Powered items
- Electric and Advanced Drills (3x3), Electric Chainsaw (tree felling), Jetpacks, Portable Batteries, Wireless Charger.
- See-through reactor, bank and tank glass; Magnet Coils animate while the reactor runs.

## 0.16 - Machine outputs, Item Pipes, filters
- Auto-output button on every machine; Item Pipes (4 tiers) with pull connections; Item Filters; Quarry settings.

## 0.15 - Automation
- Powered Quarry, Crop Farmer, Auto-Crafter, Chunk Loader.

## 0.14 - Processing and fluid/gas pipes
- Ore Purifier (x3), Compressor and plates, Electric Sawmill, Rock Crusher; Fluid and Gas Pipes; steam as a real gas.

## 0.12 - Fluids
- Electric Pump, Fluid Tanks, Freezer.

## 0.11 - Quality of life
- Wrench, redstone control, comparator output, config file, sounds and particles, Jade tooltips, Sink.

## 0.10 - Storage and networks
- Tiered Battery Boxes with configurable faces, the Energy Bank multiblock, network-based cables.

## 0.4 - 0.8 - Machines, JEI, upgrades, fusion multiblock
- Pulverizer, Alloy Smelter, Charger; JEI support and textured GUIs; Speed and Efficiency upgrades; any-size multiblock Fusion Reactor.

## 0.1 - 0.3 - Coal, steam, fusion
- Coal Generator, cables, Battery Box, Electric Furnace; Boiler and Steam Turbine; Electrolyzer and the compact Fusion Reactor.
