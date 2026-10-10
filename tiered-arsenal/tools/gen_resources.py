#!/usr/bin/env python3
"""
Generates every asset and data file for Tiered Arsenal from one material table:
textures, block/item models, blockstates, loot tables, tags, recipes, worldgen and lang.

Run from the project root:   python tools/gen_resources.py
Needs Pillow:                pip install pillow
"""
import json
import os
import random
from PIL import Image

MOD = "tieredarsenal"
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", MOD)
DATA = os.path.join(ROOT, "data")

# ---------------------------------------------------------------------------
# Material table
# ---------------------------------------------------------------------------
# id, display name, base colour, is_new_ore, handle item, ingot item/tag for recipes
MATERIALS = [
    dict(id="copper",    name="Copper",    color=(216, 116, 74),  new=False, handle="minecraft:stick",
         ingot={"item": "minecraft:copper_ingot"}),
    dict(id="emerald",   name="Emerald",   color=(36, 196, 96),   new=False, handle="minecraft:stick",
         ingot={"tag": "forge:gems/emerald"}),
    dict(id="cobalt", accent=(226, 226, 236),    name="Cobalt",    color=(52, 98, 214),   new=True,  handle="minecraft:stick",
         ingot={"tag": "forge:ingots/cobalt"}),
    dict(id="titanium", accent=(96, 206, 236),  name="Titanium",  color=(176, 188, 202), new=True,  handle="minecraft:stick",
         ingot={"tag": "forge:ingots/titanium"}),
    dict(id="infernium", accent=(255, 200, 64), name="Infernium", color=(222, 52, 22),   new=True,  handle="minecraft:blaze_rod",
         ingot={"tag": "forge:ingots/infernium"}),
    dict(id="aetherium", accent=(150, 255, 240), name="Aetherium", color=(150, 96, 236),  new=True,  handle="minecraft:blaze_rod",
         ingot={"tag": "forge:ingots/aetherium"}),
]
BY_ID = {m["id"]: m for m in MATERIALS}
NEW = [m for m in MATERIALS if m["new"]]

# ---------------------------------------------------------------------------
# Upgrade chain (smithing table):  Iron -> Cobalt -> Titanium -> Infernium -> Aetherium
#   previous-tier piece + <material> Upgrade Template + 1 ingot  =  next-tier piece
#   Enchantments, name and trim carry over, like netherite.
# Copper and Emerald stay as normal crafting-table gear (early side options).
#
# Template:   first one   = 8 ingots around the previous tier's storage block
#             duplicate   = template + 7 ingots + host stone  -> 2 templates
# ---------------------------------------------------------------------------
UPGRADES = {
    # material: (previous tier prefix, block in first-craft centre, duplication stone, template frame colour)
    "cobalt":    ("minecraft:iron",        "minecraft:iron_block",          "minecraft:cobbled_deepslate", (96, 96, 102)),
    "titanium":  (f"{MOD}:cobalt",         f"{MOD}:cobalt_block",           "minecraft:cobbled_deepslate", (66, 66, 72)),
    "infernium": (f"{MOD}:titanium",       f"{MOD}:titanium_block",         "minecraft:netherrack",        (104, 44, 42)),
    "aetherium": (f"{MOD}:infernium",      f"{MOD}:infernium_block",        "minecraft:end_stone",         (206, 206, 150)),
}
UPGRADE_FROM_NAME = {"cobalt": "Iron", "titanium": "Cobalt", "infernium": "Titanium", "aetherium": "Infernium"}

# Ore blocks: (block id, material, host stone, smelt xp)
ORES = [
    ("cobalt_ore", "cobalt", "stone", 0.7),
    ("deepslate_cobalt_ore", "cobalt", "deepslate", 0.7),
    ("titanium_ore", "titanium", "stone", 1.0),
    ("deepslate_titanium_ore", "titanium", "deepslate", 1.0),
    ("nether_infernium_ore", "infernium", "netherrack", 1.2),
    ("end_aetherium_ore", "aetherium", "end_stone", 2.0),
]

# Mining level tag for every block of a material
NEEDS_TOOL = {
    "cobalt": "minecraft:needs_iron_tool",
    "titanium": "minecraft:needs_diamond_tool",
    "infernium": "minecraft:needs_diamond_tool",
    "aetherium": "forge:needs_netherite_tool",
}

ARMOR_PIECES = ["helmet", "chestplate", "leggings", "boots"]

SET_BONUS_TEXT = {
    "emerald": "Hero of the Village",
    "cobalt": "Haste I",
    "titanium": "Speed I",
    "infernium": "Fire Resistance",
    "aetherium": "Resistance I, no fall damage",
}


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def save_png(img, *parts):
    path = os.path.join(ASSETS, "textures", *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def clamp(v):
    return max(0, min(255, int(v)))


def shade(color, factor):
    return tuple(clamp(c * factor) for c in color)


def lighten(color, amount):
    return tuple(clamp(c + (255 - c) * amount) for c in color)


def palette(color):
    """Shade letters used by the pixel templates."""
    return {
        "o": shade(color, 0.32),          # outline
        "d": shade(color, 0.66),          # dark
        "m": color,                       # mid
        "l": lighten(color, 0.28),        # light
        "h": lighten(color, 0.58),        # highlight
        "w": (74, 52, 24),                # handle dark
        "W": (112, 82, 40),               # handle light
        "y": (232, 186, 60),              # blaze-rod handle (gold)
        "Y": (255, 224, 110),
    }


def from_template(rows, pal, seed=0, jitter=6):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows[:16]):
        row = row.ljust(16, ".")[:16]
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            c = pal[ch]
            j = rnd.randint(-jitter, jitter) if ch in "dml" else 0
            px[x, y] = (clamp(c[0] + j), clamp(c[1] + j), clamp(c[2] + j), 255)
    return img


# ---------------------------------------------------------------------------
# Pixel templates (16x16). o=outline d=dark m=mid l=light h=highlight w/W=handle
# ---------------------------------------------------------------------------
T_SWORD = [
    ".............ooo",
    "............ohlo",
    "...........ohlmo",
    "..........ohlmo.",
    ".........ohlmo..",
    "........ohlmo...",
    ".......ohlmo....",
    "..oo..ohlmo.....",
    "..odoohlmo......",
    "...odhlmo.......",
    "....odmo........",
    "...owodoo.......",
    "..owWo.odo......",
    ".owWo...oo......",
    "oWwo............",
    "ooo.............",
]

T_AXE = [
    "............ooo.",
    "......oooooowWo.",
    ".....ohhhllwWo..",
    "....ohhllmwWo...",
    "....ohlmmwWo....",
    "....olmdwWo.....",
    "....oddwWo......",
    "....oowWo.......",
    "....owWo........",
    "...owWo.........",
    "..owWo..........",
    ".owWo...........",
    "owWo............",
    "ooo.............",
    "................",
    "................",
]

T_INGOT = [
    "................",
    "................",
    "................",
    "................",
    "........oooooo..",
    "......oohhhhlo..",
    "....oohhllllmo..",
    "..oohllllmmmdo..",
    ".ohllllmmmmddo..",
    ".olmmmmmmdddoo..",
    ".odmmmmdddddo...",
    ".odddddddddoo...",
    ".ooooooooooo....",
    "................",
    "................",
    "................",
]

T_RAW = [
    "................",
    "................",
    "................",
    "......oooo......",
    "....oohhlloo....",
    "...ohhllmmlloo..",
    "..ohllmmmmmlldo.",
    "..olmmmdmmmmldo.",
    ".olmmmddmmmmmdo.",
    ".odmmmmmmddmddo.",
    "..odddmmmmmddo..",
    "...oodddddddo...",
    ".....oooooo.....",
    "................",
    "................",
    "................",
]

T_HELMET = [
    "................",
    "................",
    "................",
    "...oooooooooo...",
    "..ohhhhllllllo..",
    ".ohhllllmmmmmdo.",
    ".ollmmmmmmmmmdo.",
    ".olmmoooooommdo.",
    ".olmo......omdo.",
    ".odmo......omdo.",
    ".oddo......oddo.",
    ".oooo......oooo.",
    "................",
    "................",
    "................",
    "................",
]

T_CHEST = [
    "................",
    "..ooo......ooo..",
    ".ohlloooooolmdo.",
    ".ohllllllllmmdo.",
    ".ohlllllmmmmmdo.",
    ".oolllmmmmmmdoo.",
    "..olllmmmmmmdo..",
    "..ollmmmmmmmdo..",
    "..ollmmmmmmddo..",
    "..olmmmmmmmddo..",
    "..olmmmmmmdddo..",
    "..olmmmmmddddo..",
    "..oddddddddddo..",
    "..oooooooooooo..",
    "................",
    "................",
]

T_LEGS = [
    "................",
    "................",
    "...oooooooooo...",
    "...ohhllllmdo...",
    "...ohllmmmmdo...",
    "...ohlmoomddo...",
    "...ohlmo.omdo...",
    "...ohlmo.omdo...",
    "...ohlmo.omdo...",
    "...olmdo.omdo...",
    "...olmdo.oddo...",
    "...olmdo.oddo...",
    "...oddo..oddo...",
    "...oooo..oooo...",
    "................",
    "................",
]

T_BOOTS = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "..oooo...oooo...",
    "..ohlo...ohlo...",
    "..ohlo...ohlo...",
    "..ohlo...ohlo...",
    "..ohlo...ohlo...",
    ".ohllmo.ohllmo..",
    ".olmmdo.olmmdo..",
    ".oooooo.oooooo..",
    "................",
    "................",
]

# Ore speckle layout drawn on top of the host stone
T_ORE_SPOTS = [
    "................",
    "..ml.......lm...",
    ".mhlm.....mhm...",
    "..md.......d....",
    "......lm........",
    ".....mhlm.......",
    "......mdd....m..",
    "............mhl.",
    "..lm.........d..",
    ".mhld...........",
    "..dd.....lm.....",
    "........mhlm....",
    ".........dd.....",
    "...m............",
    "..mhl......ml...",
    "...d......mhd...",
]

# Titanium: thin diagonal streaks
T_ORE_STREAKS = [
    "................",
    "..l.........m...",
    "...hm........l..",
    "....md.......hd.",
    "................",
    "......l.........",
    ".......hm.......",
    "........md......",
    "..m.............",
    "...hl.......l...",
    "....dm.......hm.",
    "..............d.",
    ".....l..........",
    "......hm....m...",
    ".......md....hl.",
    "..............d.",
]

# Infernium: glowing branching veins
T_ORE_VEINS = [
    "................",
    ".m..............",
    "..h.......lm....",
    "..lh.....h..m...",
    "...mh...lh......",
    "....dhllh.......",
    "......dh........",
    ".......mh.......",
    "........dh..m...",
    ".........mhh....",
    "..l.......dmh...",
    "...hm.......dh..",
    "....dmh......lm.",
    "......dh.....d..",
    ".......m........",
    "................",
]

# Aetherium: crystal shards
T_ORE_SHARDS = [
    "................",
    "...h.........l..",
    "..hlm.......hmd.",
    "..lmd.......md..",
    "..mmd...........",
    "...d.....h......",
    ".........hl.....",
    "........hlmd....",
    "........lmmd....",
    "........mmdd....",
    ".........md.....",
    "..h.............",
    ".hlm.........h..",
    ".lmd........hmd.",
    ".mdd........md..",
    "..d.............",
]

ORE_PATTERNS = {"titanium": T_ORE_STREAKS, "infernium": T_ORE_VEINS, "aetherium": T_ORE_SHARDS}

# Where the accent colour (studs, gems, trim) goes on each item, for the tiers that have one
ACCENT_SPOTS = {
    "sword": [(3, 8), (4, 9), (5, 10)],
    "helmet": [(7, 5), (8, 5)],
    "chestplate": [(7, 6), (8, 6), (7, 7), (8, 7)],
    "leggings": [(6, 3), (7, 3), (8, 3), (9, 3)],
    "boots": [(3, 11), (10, 11)],
    "greatsword": [(2, 9), (3, 10), (4, 11), (5, 12)],
    "dagger": [(4, 9), (5, 10), (6, 11)],
    "warhammer": [(7, 5), (8, 5), (9, 5), (10, 5), (11, 5), (12, 5)],
}


def add_accent(img, key, material):
    """Paint the tier's accent colour on the spots for this item type (no-op for plain tiers)."""
    accent = material.get("accent")
    if not accent or key not in ACCENT_SPOTS:
        return img
    px = img.load()
    for n, (x, y) in enumerate(ACCENT_SPOTS[key]):
        c = lighten(accent, 0.25) if n == 0 else accent
        px[x, y] = (*c, 255)
    return img


# Upgrade template: stone tablet (o/d/m/l/h from frame colour) with a material inlay (C/A/B)
T_TEMPLATE = [
    "................",
    "...oooooooooo...",
    "..ohhlllllllmo..",
    "..ohlmmmmmmmdo..",
    "..olmCCCCCCmdo..",
    "..olmCBBACmmdo..",
    "..olmCBAACmmdo..",
    "..olmCCCCCCmdo..",
    "..olmmmmmmmmdo..",
    "..olmmCCCmmmdo..",
    "..olmmCBCmmmdo..",
    "..olmmCCCmmmdo..",
    "..oldmmmmmmddo..",
    "..oddddddddddo..",
    "...oooooooooo...",
    "................",
]


def template_texture(material, frame_color, seed):
    pal = palette(frame_color)
    mat = palette(material["color"])
    pal.update({"C": mat["d"], "A": mat["m"], "B": mat["h"]})
    return from_template(T_TEMPLATE, pal, seed, jitter=5)


# ---------------------------------------------------------------------------
# Texture builders
# ---------------------------------------------------------------------------
HOSTS = {
    "stone": dict(base=(126, 126, 126), var=14, bands=False),
    "deepslate": dict(base=(78, 78, 84), var=10, bands=True),
    "netherrack": dict(base=(112, 52, 50), var=18, bands=False),
    "end_stone": dict(base=(220, 222, 160), var=12, bands=False),
}


def host_texture(kind, seed):
    spec = HOSTS[kind]
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        band = (-6 if y % 4 == 0 else 3) if spec["bands"] else 0
        for x in range(16):
            v = rnd.randint(-spec["var"], spec["var"]) + band
            b = spec["base"]
            px[x, y] = (clamp(b[0] + v), clamp(b[1] + v), clamp(b[2] + v), 255)
    return img


def ore_texture(material, host, seed):
    img = host_texture(host, seed)
    px = img.load()
    pal = palette(material["color"])
    rnd = random.Random(seed + 99)
    for y, row in enumerate(ORE_PATTERNS.get(material["id"], T_ORE_SPOTS)):
        for x, ch in enumerate(row):
            if ch != ".":
                c = pal[ch]
                j = rnd.randint(-8, 8)
                px[x, y] = (clamp(c[0] + j), clamp(c[1] + j), clamp(c[2] + j), 255)
    return img


def storage_block_texture(material, seed):
    pal = palette(material["color"])
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            j = rnd.randint(-5, 5)
            if x == 0 or y == 0:
                c = pal["h"]
            elif x == 15 or y == 15:
                c = pal["d"]
            elif x == 1 or y == 1:
                c = pal["l"]
            elif x == 14 or y == 14:
                c = shade(material["color"], 0.82)
            elif (x in (7, 8)) and 3 <= y <= 12 or (y in (7, 8)) and 3 <= x <= 12:
                c = shade(material["color"], 0.9)  # faint plate seams
            else:
                c = pal["m"]
            px[x, y] = (clamp(c[0] + j), clamp(c[1] + j), clamp(c[2] + j), 255)
    return img


def raw_block_texture(material, seed):
    pal = palette(material["color"])
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    choices = ["d", "d", "m", "m", "m", "l", "l", "h", "o"]
    for y in range(16):
        for x in range(16):
            c = pal[rnd.choice(choices)]
            px[x, y] = (*c, 255)
    return img


# --- Armour layer textures (64x32, vanilla humanoid UV layout) -------------
def box_faces(u, v, w, h, d):
    """Returns dict of face -> (x, y, width, height) for a model box's UV layout."""
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


SIDES = ("right", "front", "left", "back")


def paint(px, rect, pal, rnd, rows=None):
    x0, y0, w, h = rect
    r0, r1 = rows if rows else (0, h)
    for yy in range(r0, r1):
        for xx in range(w):
            if yy == r0 or xx == 0:
                c = pal["l"]
            elif yy == r1 - 1 or xx == w - 1:
                c = pal["d"]
            else:
                c = pal["m"] if rnd.random() > 0.15 else pal["l"]
            j = rnd.randint(-6, 6)
            px[x0 + xx, y0 + yy] = (clamp(c[0] + j), clamp(c[1] + j), clamp(c[2] + j), 255)


def armor_layers(material, seed):
    pal = palette(material["color"])
    rnd = random.Random(seed)

    head = box_faces(0, 0, 8, 8, 8)
    body = box_faces(16, 16, 8, 12, 4)
    arm = box_faces(40, 16, 4, 12, 4)
    leg = box_faces(0, 16, 4, 12, 4)

    # Layer 1: helmet, chestplate, boots
    l1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    p1 = l1.load()
    for face in head.values():
        paint(p1, face, pal, rnd)
    fx, fy, _, _ = head["front"]
    for xx in range(1, 7):          # visor slit
        p1[fx + xx, fy + 3] = (*pal["o"], 255)
    for face in body.values():
        paint(p1, face, pal, rnd)
    paint(p1, arm["top"], pal, rnd)
    for s in SIDES:                 # shoulder pads
        paint(p1, arm[s], pal, rnd, rows=(0, 5))
    paint(p1, leg["bottom"], pal, rnd)
    for s in SIDES:                 # boots
        paint(p1, leg[s], pal, rnd, rows=(8, 12))

    # Layer 2: leggings
    l2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    p2 = l2.load()
    for s in SIDES:                 # waist band
        paint(p2, body[s], pal, rnd, rows=(7, 12))
    paint(p2, body["bottom"], pal, rnd)
    paint(p2, leg["top"], pal, rnd)
    for s in SIDES:
        paint(p2, leg[s], pal, rnd, rows=(0, 9))

    accent = material.get("accent")
    if accent:
        hi = (*lighten(accent, 0.3), 255)
        ac = (*accent, 255)
        bx, by, _, _ = body["front"]          # chest emblem
        for (x, y) in ((3, 2), (4, 2), (3, 3), (4, 3)):
            p1[bx + x, by + y] = hi if y == 2 else ac
        hx, hy, _, _ = head["front"]          # helmet crest
        p1[hx + 3, hy] = hi
        p1[hx + 4, hy] = hi
        tx, ty, _, _ = head["top"]
        p1[tx + 3, ty + 3] = ac
        p1[tx + 4, ty + 3] = ac
        lx, ly, _, _ = leg["front"]           # boot caps
        p1[lx + 1, ly + 10] = ac
        p1[lx + 2, ly + 10] = ac
        p2[bx + 3, by + 8] = ac               # leggings belt buckle
        p2[bx + 4, by + 8] = ac
    return l1, l2


# ---------------------------------------------------------------------------
# Generation
# ---------------------------------------------------------------------------
def gen_textures():
    for i, m in enumerate(MATERIALS):
        pal = palette(m["color"])
        handle_pal = dict(pal)
        if m["handle"] == "minecraft:blaze_rod":
            handle_pal["w"], handle_pal["W"] = pal["y"], pal["Y"]
        s = i * 1000
        save_png(add_accent(from_template(T_SWORD, handle_pal, s + 1), "sword", m), "item", f"{m['id']}_sword.png")
        save_png(from_template(T_AXE, handle_pal, s + 2), "item", f"{m['id']}_axe.png")
        save_png(add_accent(from_template(T_HELMET, pal, s + 3), "helmet", m), "item", f"{m['id']}_helmet.png")
        save_png(add_accent(from_template(T_CHEST, pal, s + 4), "chestplate", m), "item", f"{m['id']}_chestplate.png")
        save_png(add_accent(from_template(T_LEGS, pal, s + 5), "leggings", m), "item", f"{m['id']}_leggings.png")
        save_png(add_accent(from_template(T_BOOTS, pal, s + 6), "boots", m), "item", f"{m['id']}_boots.png")
        l1, l2 = armor_layers(m, s + 7)
        save_png(l1, "models", "armor", f"{m['id']}_layer_1.png")
        save_png(l2, "models", "armor", f"{m['id']}_layer_2.png")
        if m["new"]:
            save_png(from_template(T_INGOT, pal, s + 8), "item", f"{m['id']}_ingot.png")
            save_png(from_template(T_RAW, pal, s + 9), "item", f"raw_{m['id']}.png")
            save_png(storage_block_texture(m, s + 10), "block", f"{m['id']}_block.png")
            save_png(raw_block_texture(m, s + 11), "block", f"raw_{m['id']}_block.png")
        if m["id"] in UPGRADES:
            save_png(template_texture(m, UPGRADES[m["id"]][3], s + 12),
                     "item", f"{m['id']}_upgrade_smithing_template.png")
    for j, (block, mat, host, _) in enumerate(ORES):
        save_png(ore_texture(BY_ID[mat], host, 5000 + j), "block", f"{block}.png")


def all_blocks():
    blocks = [o[0] for o in ORES]
    for m in NEW:
        blocks += [f"raw_{m['id']}_block", f"{m['id']}_block"]
    return blocks


def gen_models():
    for b in all_blocks():
        write_json(os.path.join(ASSETS, "blockstates", f"{b}.json"),
                   {"variants": {"": {"model": f"{MOD}:block/{b}"}}})
        write_json(os.path.join(ASSETS, "models", "block", f"{b}.json"),
                   {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{b}"}})
        write_json(os.path.join(ASSETS, "models", "item", f"{b}.json"),
                   {"parent": f"{MOD}:block/{b}"})

    def item_model(name, parent):
        write_json(os.path.join(ASSETS, "models", "item", f"{name}.json"),
                   {"parent": parent, "textures": {"layer0": f"{MOD}:item/{name}"}})

    for m in MATERIALS:
        for tool in ("sword", "axe"):
            item_model(f"{m['id']}_{tool}", "minecraft:item/handheld")
        for piece in ARMOR_PIECES:
            item_model(f"{m['id']}_{piece}", "minecraft:item/generated")
        if m["new"]:
            item_model(f"{m['id']}_ingot", "minecraft:item/generated")
            item_model(f"raw_{m['id']}", "minecraft:item/generated")
        if m["id"] in UPGRADES:
            item_model(f"{m['id']}_upgrade_smithing_template", "minecraft:item/generated")


def gen_loot():
    def path(b):
        return os.path.join(DATA, MOD, "loot_tables", "blocks", f"{b}.json")

    for block, mat, _, _ in ORES:
        write_json(path(block), {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1.0,
                "bonus_rolls": 0.0,
                "entries": [{
                    "type": "minecraft:alternatives",
                    "children": [
                        {
                            "type": "minecraft:item",
                            "name": f"{MOD}:{block}",
                            "conditions": [{
                                "condition": "minecraft:match_tool",
                                "predicate": {"enchantments": [{
                                    "enchantment": "minecraft:silk_touch",
                                    "levels": {"min": 1}}]}
                            }]
                        },
                        {
                            "type": "minecraft:item",
                            "name": f"{MOD}:raw_{mat}",
                            "functions": [
                                {"function": "minecraft:apply_bonus",
                                 "enchantment": "minecraft:fortune",
                                 "formula": "minecraft:ore_drops"},
                                {"function": "minecraft:explosion_decay"}
                            ]
                        }
                    ]
                }]
            }],
            "random_sequence": f"{MOD}:blocks/{block}"
        })
    for m in NEW:
        for b in (f"raw_{m['id']}_block", f"{m['id']}_block"):
            write_json(path(b), {
                "type": "minecraft:block",
                "pools": [{
                    "rolls": 1.0,
                    "bonus_rolls": 0.0,
                    "entries": [{"type": "minecraft:item", "name": f"{MOD}:{b}"}],
                    "conditions": [{"condition": "minecraft:survives_explosion"}]
                }],
                "random_sequence": f"{MOD}:blocks/{b}"
            })


def tag(ns, kind, name, values):
    write_json(os.path.join(DATA, ns, "tags", kind, f"{name}.json"),
               {"replace": False, "values": values})


def gen_tags():
    blocks = [f"{MOD}:{b}" for b in all_blocks()]
    tag("minecraft", "blocks", "mineable/pickaxe", blocks)

    by_level = {}
    for m in NEW:
        mid = m["id"]
        members = [f"{MOD}:{o[0]}" for o in ORES if o[1] == mid]
        members += [f"{MOD}:raw_{mid}_block", f"{MOD}:{mid}_block"]
        ns, name = NEEDS_TOOL[mid].split(":")
        by_level.setdefault((ns, name), []).extend(members)
    for (ns, name), members in by_level.items():
        tag(ns, "blocks", name, members)

    # Empty "needs_<tier>_tool" tags referenced by ModToolTiers
    for m in MATERIALS:
        tag(MOD, "blocks", f"needs_{m['id']}_tool", [])

    # Forge conventions so other mods (e.g. Tiered Industries machines) can use our materials
    ore_tags_blocks, ore_tags_items = [], []
    for m in NEW:
        mid = m["id"]
        ores = [f"{MOD}:{o[0]}" for o in ORES if o[1] == mid]
        tag("forge", "blocks", f"ores/{mid}", ores)
        tag("forge", "items", f"ores/{mid}", ores)
        tag("forge", "blocks", f"storage_blocks/{mid}", [f"{MOD}:{mid}_block"])
        tag("forge", "items", f"storage_blocks/{mid}", [f"{MOD}:{mid}_block"])
        tag("forge", "blocks", f"storage_blocks/raw_{mid}", [f"{MOD}:raw_{mid}_block"])
        tag("forge", "items", f"storage_blocks/raw_{mid}", [f"{MOD}:raw_{mid}_block"])
        tag("forge", "items", f"ingots/{mid}", [f"{MOD}:{mid}_ingot"])
        tag("forge", "items", f"raw_materials/{mid}", [f"{MOD}:raw_{mid}"])
        ore_tags_blocks.append(f"#forge:ores/{mid}")
        ore_tags_items.append(f"#forge:ores/{mid}")
    tag("forge", "blocks", "ores", ore_tags_blocks)
    tag("forge", "items", "ores", ore_tags_items)
    tag("forge", "items", "ingots", [f"#forge:ingots/{m['id']}" for m in NEW])
    tag("forge", "items", "raw_materials", [f"#forge:raw_materials/{m['id']}" for m in NEW])
    tag("forge", "blocks", "storage_blocks",
        [f"#forge:storage_blocks/{m['id']}" for m in NEW] +
        [f"#forge:storage_blocks/raw_{m['id']}" for m in NEW])
    tag("forge", "items", "storage_blocks",
        [f"#forge:storage_blocks/{m['id']}" for m in NEW] +
        [f"#forge:storage_blocks/raw_{m['id']}" for m in NEW])

    armor = [f"{MOD}:{m['id']}_{p}" for m in MATERIALS for p in ARMOR_PIECES]
    tag("minecraft", "items", "trimmable_armor", armor)
    tag("minecraft", "items", "axes", [f"{MOD}:{m['id']}_axe" for m in MATERIALS])
    tag("forge", "items", "tools/swords", [f"{MOD}:{m['id']}_sword" for m in MATERIALS])
    tag("forge", "items", "tools/axes", [f"{MOD}:{m['id']}_axe" for m in MATERIALS])
    tag("forge", "items", "armors/helmets", [f"{MOD}:{m['id']}_helmet" for m in MATERIALS])
    tag("forge", "items", "armors/chestplates", [f"{MOD}:{m['id']}_chestplate" for m in MATERIALS])
    tag("forge", "items", "armors/leggings", [f"{MOD}:{m['id']}_leggings" for m in MATERIALS])
    tag("forge", "items", "armors/boots", [f"{MOD}:{m['id']}_boots" for m in MATERIALS])


def recipe(name, obj):
    write_json(os.path.join(DATA, MOD, "recipes", f"{name}.json"), obj)


def shaped(name, pattern, key, result, count=1, group=None):
    obj = {"type": "minecraft:crafting_shaped", "category": "equipment",
           "pattern": pattern, "key": key, "result": {"item": result, "count": count}}
    if group:
        obj["group"] = group
    recipe(name, obj)


def gen_recipes():
    for m in MATERIALS:
        mid, x, s = m["id"], m["ingot"], {"item": m["handle"]}
        if mid in UPGRADES:
            continue  # made in the smithing table, see below
        shaped(f"{mid}_sword", ["X", "X", "S"], {"X": x, "S": s}, f"{MOD}:{mid}_sword")
        shaped(f"{mid}_axe", ["XX", "XS", " S"], {"X": x, "S": s}, f"{MOD}:{mid}_axe")
        shaped(f"{mid}_helmet", ["XXX", "X X"], {"X": x}, f"{MOD}:{mid}_helmet")
        shaped(f"{mid}_chestplate", ["X X", "XXX", "XXX"], {"X": x}, f"{MOD}:{mid}_chestplate")
        shaped(f"{mid}_leggings", ["XXX", "X X", "X X"], {"X": x}, f"{MOD}:{mid}_leggings")
        shaped(f"{mid}_boots", ["X X", "X X"], {"X": x}, f"{MOD}:{mid}_boots")

    # ---- Tiered upgrade chain ----
    for mid, (prev, core_block, dupe_stone, _) in UPGRADES.items():
        x = BY_ID[mid]["ingot"]
        template = f"{MOD}:{mid}_upgrade_smithing_template"
        for piece in ["sword", "axe"] + ARMOR_PIECES:
            recipe(f"{mid}_{piece}_smithing", {
                "type": "minecraft:smithing_transform",
                "template": {"item": template},
                "base": {"item": f"{prev}_{piece}"},
                "addition": x,
                "result": {"item": f"{MOD}:{mid}_{piece}"}})
        # first template: 8 ingots around the previous tier's block
        # (with Tiered Industries installed the higher ones use TI alloys instead - see gen_tieredpower)
        first = {
            "type": "minecraft:crafting_shaped", "category": "misc",
            "pattern": ["XXX", "XBX", "XXX"],
            "key": {"X": x, "B": {"item": core_block}},
            "result": {"item": template}}
        if mid in TI_TEMPLATES:
            first = {"conditions": [WITHOUT_TI], **first}
        recipe(f"{mid}_upgrade_smithing_template", first)
        # duplicate: template + 7 ingots + stone -> 2
        recipe(f"{mid}_upgrade_smithing_template_duplicate", {
            "type": "minecraft:crafting_shaped", "category": "misc",
            "pattern": ["XTX", "XSX", "XXX"],
            "key": {"X": x, "T": {"item": template}, "S": {"item": dupe_stone}},
            "result": {"item": template, "count": 2}})

    xp = {o[1]: o[3] for o in ORES}
    for m in NEW:
        mid = m["id"]
        ingot = f"{MOD}:{mid}_ingot"
        sources = [f"{MOD}:raw_{mid}"] + [f"{MOD}:{o[0]}" for o in ORES if o[1] == mid]
        for src in sources:
            short = src.split(":")[1]
            for kind, time in (("smelting", 200), ("blasting", 100)):
                r = {"type": f"minecraft:{kind}", "category": "misc", "group": f"{mid}_ingot",
                     "ingredient": {"item": src}, "result": ingot,
                     "experience": xp[mid], "cookingtime": time}
                if mid in TI_ALLOYS:  # with Tiered Industries these need the Alloy Smelter
                    r = {"conditions": [WITHOUT_TI], **r}
                recipe(f"{mid}_ingot_from_{kind}_{short}", r)
        # 9 <-> 1 compression
        for small, big in ((ingot, f"{MOD}:{mid}_block"), (f"{MOD}:raw_{mid}", f"{MOD}:raw_{mid}_block")):
            sn, bn = small.split(":")[1], big.split(":")[1]
            recipe(f"{bn}", {"type": "minecraft:crafting_shaped", "category": "building",
                             "pattern": ["###", "###", "###"], "key": {"#": {"item": small}},
                             "result": {"item": big}})
            recipe(f"{sn}_from_{bn}", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                       "ingredients": [{"item": big}],
                                       "result": {"item": small, "count": 9}})


# Worldgen: (material, size, count, height provider, targets, biomes)
def stone_targets(stone_block, deep_block):
    return [
        {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"},
         "state": {"Name": f"{MOD}:{stone_block}"}},
        {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"},
         "state": {"Name": f"{MOD}:{deep_block}"}},
    ]


WORLDGEN = [
    dict(name="cobalt", size=8, count=7,
         height={"type": "minecraft:trapezoid",
                 "min_inclusive": {"absolute": -48}, "max_inclusive": {"absolute": 32}},
         targets=stone_targets("cobalt_ore", "deepslate_cobalt_ore"),
         biomes="#minecraft:is_overworld"),
    dict(name="titanium", size=6, count=4,
         height={"type": "minecraft:uniform",
                 "min_inclusive": {"absolute": -64}, "max_inclusive": {"absolute": -16}},
         targets=stone_targets("titanium_ore", "deepslate_titanium_ore"),
         biomes="#minecraft:is_overworld"),
    dict(name="infernium", size=5, count=6,
         height={"type": "minecraft:uniform",
                 "min_inclusive": {"above_bottom": 8}, "max_inclusive": {"below_top": 10}},
         targets=[{"target": {"predicate_type": "minecraft:block_match", "block": "minecraft:netherrack"},
                   "state": {"Name": f"{MOD}:nether_infernium_ore"}}],
         biomes="#minecraft:is_nether"),
    # Outer End only, so the dragon has to be beaten first
    dict(name="aetherium", size=4, count=6,
         height={"type": "minecraft:uniform",
                 "min_inclusive": {"absolute": 10}, "max_inclusive": {"absolute": 90}},
         targets=[{"target": {"predicate_type": "minecraft:block_match", "block": "minecraft:end_stone"},
                   "state": {"Name": f"{MOD}:end_aetherium_ore"}}],
         biomes=["minecraft:end_highlands", "minecraft:end_midlands",
                 "minecraft:end_barrens", "minecraft:small_end_islands"]),
]


def gen_worldgen():
    for w in WORLDGEN:
        feat = f"ore_{w['name']}"
        write_json(os.path.join(DATA, MOD, "worldgen", "configured_feature", f"{feat}.json"), {
            "type": "minecraft:ore",
            "config": {"size": w["size"], "discard_chance_on_air_exposure": 0.0, "targets": w["targets"]}})
        write_json(os.path.join(DATA, MOD, "worldgen", "placed_feature", f"{feat}.json"), {
            "feature": f"{MOD}:{feat}",
            "placement": [
                {"type": "minecraft:count", "count": w["count"]},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:height_range", "height": w["height"]},
                {"type": "minecraft:biome"}]})
        write_json(os.path.join(DATA, MOD, "forge", "biome_modifier", f"add_{feat}.json"), {
            "type": "forge:add_features",
            "biomes": w["biomes"],
            "features": f"{MOD}:{feat}",
            "step": "underground_ores"})


def gen_lang():
    lang = {f"itemGroup.{MOD}": "Tiered Arsenal",
            f"tooltip.{MOD}.set_bonus": "Full set bonus:"}
    for mid in UPGRADES:
        n, prev = BY_ID[mid]["name"], UPGRADE_FROM_NAME[mid]
        key = f"item.{MOD}.smithing_template.{mid}_upgrade"
        lang[f"item.{MOD}.{mid}_upgrade_smithing_template"] = f"{n} Upgrade Template"
        lang[f"upgrade.{MOD}.{mid}_upgrade"] = f"{n} Upgrade"
        lang[f"{key}.applies_to"] = f"{prev} Equipment"
        lang[f"{key}.ingredients"] = f"{n} Ingot"
        lang[f"{key}.base_slot_description"] = f"Add {prev.lower()} armour, sword or axe"
        lang[f"{key}.additions_slot_description"] = f"Add {n} Ingot"
    for m in MATERIALS:
        n = m["name"]
        lang[f"item.{MOD}.{m['id']}_sword"] = f"{n} Sword"
        lang[f"item.{MOD}.{m['id']}_axe"] = f"{n} Axe"
        for p in ARMOR_PIECES:
            lang[f"item.{MOD}.{m['id']}_{p}"] = f"{n} {p.capitalize()}"
        if m["id"] in SET_BONUS_TEXT:
            lang[f"tooltip.{MOD}.set_bonus.{m['id']}"] = "  " + SET_BONUS_TEXT[m["id"]]
        if m["new"]:
            lang[f"item.{MOD}.{m['id']}_ingot"] = f"{n} Ingot"
            lang[f"item.{MOD}.raw_{m['id']}"] = f"Raw {n}"
            lang[f"block.{MOD}.{m['id']}_block"] = f"Block of {n}"
            lang[f"block.{MOD}.raw_{m['id']}_block"] = f"Block of Raw {n}"
    ore_names = {
        "cobalt_ore": "Cobalt Ore", "deepslate_cobalt_ore": "Deepslate Cobalt Ore",
        "titanium_ore": "Titanium Ore", "deepslate_titanium_ore": "Deepslate Titanium Ore",
        "nether_infernium_ore": "Nether Infernium Ore", "end_aetherium_ore": "End Aetherium Ore",
    }
    for k, v in ore_names.items():
        lang[f"block.{MOD}.{k}"] = v
    write_json(os.path.join(ASSETS, "lang", "en_us.json"), lang)


# ===========================================================================
# Phase 4: weapon types (greatsword, dagger, spear, war hammer, bow)
# ===========================================================================
# Iron is added for these only, so the smithing chain Iron -> Cobalt -> ... has a start.
IRON = dict(id="iron", name="Iron", color=(206, 202, 196), new=False, handle="minecraft:stick",
            ingot={"item": "minecraft:iron_ingot"})
WEAPON_MATS = [BY_ID["copper"], BY_ID["emerald"], IRON, BY_ID["cobalt"], BY_ID["titanium"],
               BY_ID["infernium"], BY_ID["aetherium"]]
# For weapon types the chain starts at our own iron versions
WEAPON_PREV = {"cobalt": "iron", "titanium": "cobalt", "infernium": "titanium", "aetherium": "infernium"}

T_GREATSWORD = [
    "............oooo",
    "...........ohhlo",
    "..........ohhlmo",
    ".........ohhlmdo",
    "........ohhlmdo.",
    ".......ohhlmdo..",
    "......ohhlmdo...",
    ".....ohhlmdo....",
    ".oo.ohhlmdo.....",
    ".odoohlmdo......",
    "..odhlmdo.......",
    "...odmdo........",
    "..owodoo........",
    ".owWo.odo.......",
    "oWwo...oo.......",
    "ooo.............",
]

T_DAGGER = [
    "................",
    "................",
    "................",
    "................",
    "..........ooo...",
    ".........ohlo...",
    "........ohlmo...",
    ".......ohlmo....",
    "...oo.ohlmo.....",
    "...odohlmo......",
    "....odlmo.......",
    "....owdo........",
    "...owWodo.......",
    "..owWo.oo.......",
    "..ooo...........",
    "................",
]

T_SPEAR = [
    "...........oooo.",
    "..........ohhlo.",
    ".........ohhlmo.",
    "........ohlmmdo.",
    ".........olmddo.",
    "........owWoo...",
    ".......owWo.....",
    "......owWo......",
    ".....owWo.......",
    "....owWo........",
    "...owWo.........",
    "..owWo..........",
    ".owWo...........",
    "owWo............",
    "oWo.............",
    "oo..............",
]

T_WARHAMMER = [
    "......oooooooo..",
    ".....ohhhhlllmo.",
    ".....ohhllmmmdo.",
    ".....ohlmmmmddo.",
    ".....olmmmmdddo.",
    ".....oddddddddo.",
    "......ooowWoooo.",
    ".......owWo.....",
    "......owWo......",
    ".....owWo.......",
    "....owWo........",
    "...owWo.........",
    "..owWo..........",
    ".owWo...........",
    "owWo............",
    "ooo.............",
]

WEAPON_TYPES = [
    # id, display name, template, model parent
    ("greatsword", "Greatsword", T_GREATSWORD, "minecraft:item/handheld"),
    ("dagger", "Dagger", T_DAGGER, "minecraft:item/handheld"),
    ("spear", "Spear", T_SPEAR, "minecraft:item/handheld"),
    ("warhammer", "War Hammer", T_WARHAMMER, "minecraft:item/handheld"),
    ("bow", "Bow", None, None),
]

WEAPON_RECIPES = {
    # X = ingot, S = stick, T = string
    "greatsword": [" X ", " X ", "XSX"],
    "dagger": ["X", "S"],
    "spear": ["  X", " S ", "S  "],
    "warhammer": ["XXX", "XSX", " S "],
    "bow": [" XT", "S T", " XT"],
}


def bow_texture(material, stage, seed):
    """Procedural bow. stage 0 = resting, 1-3 = drawn further, with an arrow nocked."""
    import math
    pal = palette(material["color"])
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    arc = {}
    # Limb: quarter circle centred on the bottom-left corner, tips at (1,1) and (14,14)
    for t10 in range(0, 901, 5):
        t = math.radians(t10 / 10)
        for r, key in ((11.4, "d"), (12.0, "m"), (12.6, "l")):
            x, y = round(1 + r * math.sin(t)), round(14 - r * math.cos(t))
            if 0 <= x < 16 and 0 <= y < 16:
                grip = 38 <= t10 / 10 <= 52
                arc[(x, y)] = ("W" if key != "d" else "w") if grip else key
    for (x, y), k in arc.items():
        c = pal[k]
        j = rnd.randint(-6, 6) if k in "dml" else 0
        px[x, y] = (clamp(c[0] + j), clamp(c[1] + j), clamp(c[2] + j), 255)
    # Outline around the limb
    for (x, y) in list(arc):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < 16 and 0 <= ny < 16 and (nx, ny) not in arc and px[nx, ny][3] == 0:
                px[nx, ny] = (*pal["o"], 255)

    def line(a, b, color):
        (x0, y0), (x1, y1) = a, b
        n = max(abs(x1 - x0), abs(y1 - y0))
        for i in range(n + 1):
            x = round(x0 + (x1 - x0) * i / n)
            y = round(y0 + (y1 - y0) * i / n)
            if 0 <= x < 16 and 0 <= y < 16 and (x, y) not in arc:
                px[x, y] = (*color, 255)

    string = (226, 226, 222)
    tip1, tip2 = (2, 3), (12, 13)
    nock = (7 - stage, 8 + stage) if stage else None
    if nock is None:
        line(tip1, tip2, string)
    else:
        line(tip1, nock, string)
        line(nock, tip2, string)
        # Arrow from the nock towards the top-right
        shaft, head, fletch = (122, 92, 52), (150, 150, 156), (240, 240, 240)
        length = 9 + stage
        for i in range(length):
            x, y = nock[0] + i, nock[1] - i
            if 0 <= x < 16 and 0 <= y < 16:
                c = fletch if i < 2 else (head if i >= length - 2 else shaft)
                px[x, y] = (*c, 255)
    return img


def gen_weapons():
    lang_path = os.path.join(ASSETS, "lang", "en_us.json")
    with open(lang_path, encoding="utf-8") as f:
        lang = json.load(f)

    bow_display_parent = "minecraft:item/bow"
    for i, m in enumerate(WEAPON_MATS):
        mid = m["id"]
        pal = palette(m["color"])
        handle_pal = dict(pal)
        if m["handle"] == "minecraft:blaze_rod":
            handle_pal["w"], handle_pal["W"] = pal["y"], pal["Y"]
        s = 20000 + i * 100

        for j, (wid, wname, tpl, parent) in enumerate(WEAPON_TYPES):
            name = f"{mid}_{wid}"
            lang[f"item.{MOD}.{name}"] = f"{m['name']} {wname}"
            if wid == "bow":
                for stage in range(4):
                    suffix = "" if stage == 0 else f"_pulling_{stage - 1}"
                    save_png(bow_texture(m, stage, s + 50 + stage), "item", f"{name}{suffix}.png")
                overrides = [
                    {"predicate": {"pulling": 1}, "model": f"{MOD}:item/{name}_pulling_0"},
                    {"predicate": {"pulling": 1, "pull": 0.65}, "model": f"{MOD}:item/{name}_pulling_1"},
                    {"predicate": {"pulling": 1, "pull": 0.9}, "model": f"{MOD}:item/{name}_pulling_2"},
                ]
                write_json(os.path.join(ASSETS, "models", "item", f"{name}.json"),
                           {"parent": bow_display_parent, "textures": {"layer0": f"{MOD}:item/{name}"},
                            "overrides": overrides})
                for k in range(3):
                    write_json(os.path.join(ASSETS, "models", "item", f"{name}_pulling_{k}.json"),
                               {"parent": f"{MOD}:item/{name}",
                                "textures": {"layer0": f"{MOD}:item/{name}_pulling_{k}"}})
            else:
                save_png(add_accent(from_template(tpl, handle_pal, s + j), wid, m), "item", f"{name}.png")
                write_json(os.path.join(ASSETS, "models", "item", f"{name}.json"),
                           {"parent": parent, "textures": {"layer0": f"{MOD}:item/{name}"}})

            # Recipes
            if mid in WEAPON_PREV:
                recipe(f"{name}_smithing", {
                    "type": "minecraft:smithing_transform",
                    "template": {"item": f"{MOD}:{mid}_upgrade_smithing_template"},
                    "base": {"item": f"{MOD}:{WEAPON_PREV[mid]}_{wid}"},
                    "addition": m["ingot"],
                    "result": {"item": f"{MOD}:{name}"}})
            else:
                pattern = WEAPON_RECIPES[wid]
                key = {"X": m["ingot"], "S": {"item": "minecraft:stick"}, "T": {"item": "minecraft:string"}}
                used = {ch for row in pattern for ch in row if ch != " "}
                shaped(name, pattern, {k: v for k, v in key.items() if k in used}, f"{MOD}:{name}")

    # The upgrade templates now cover these weapons too
    for mid in UPGRADES:
        prev = UPGRADE_FROM_NAME[mid].lower()
        lang[f"item.{MOD}.smithing_template.{mid}_upgrade.base_slot_description"] = \
            f"Add {prev} armour or weapon"

    lang[f"tooltip.{MOD}.dagger"] = "Backstab: 1.5x damage from behind"
    lang[f"tooltip.{MOD}.spear"] = "+1.5 block reach, no sweep"
    lang[f"tooltip.{MOD}.warhammer"] = "Shockwave: 40% damage to mobs around the target"
    lang[f"tooltip.{MOD}.bow"] = "Arrows +%s damage"
    write_json(lang_path, lang)

    # Tags
    def ids(wid):
        return [f"{MOD}:{m['id']}_{wid}" for m in WEAPON_MATS]
    tag("forge", "items", "tools/swords",
        [f"{MOD}:{m['id']}_sword" for m in MATERIALS] + ids("greatsword") + ids("dagger"))
    tag("forge", "items", "tools/bows", ids("bow"))
    tag("forge", "items", "tools/spears", ids("spear"))
    tag("forge", "items", "tools/hammers", ids("warhammer"))


# ===========================================================================
# Tiered Industries integration (mod id "tieredpower"). Every recipe here has a
# forge:mod_loaded condition, so none of it loads unless TI is installed.
# ===========================================================================
TI = "tieredpower"
WITH_TI = {"type": "forge:mod_loaded", "modid": TI}
WITHOUT_TI = {"type": "forge:not", "value": WITH_TI}

PULVERIZE_TIME = {"cobalt": 100, "titanium": 140, "infernium": 180, "aetherium": 240}
# Alloy Smelter recipes: 2 raw ore or dust + extra -> 2 ingots. With TI installed these
# are the ONLY way to make the ingot; furnace smelting of these three is switched off.
TI_ALLOYS = {
    "titanium": ({"tag": "forge:ingots/steel"}, 1, 200),
    "infernium": ({"item": "minecraft:blaze_powder"}, 2, 300),
    "aetherium": ({"tag": "forge:ingots/superconductor"}, 1, 400),
}
# Upgrade templates with TI: pattern + extra keys (X = ingot, B = previous tier's block)
TI_TEMPLATES = {
    "titanium": (["XPX", "PBP", "XPX"], {"P": {"tag": "forge:plates/steel"}}),
    "infernium": (["XPX", "PBP", "XPX"], {"P": {"tag": "forge:ingots/superconductor"}}),
    "aetherium": (["XQX", "PBP", "XPX"], {"P": {"tag": "forge:ingots/superconductor"},
                                         "Q": {"tag": "forge:ingots/quantum_alloy"}}),
}

T_DUST = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".......oo.......",
    ".....oohlo......",
    "....ohllmmo.....",
    "...ohlmmmmdo....",
    "..olmmdmmmmdo...",
    ".olmmmmmdmmmdo..",
    ".oddmmmmmmmddo..",
    "..ooddddddddo...",
    "....oooooooo....",
    "................",
]


def gen_tieredpower():
    lang_path = os.path.join(ASSETS, "lang", "en_us.json")
    with open(lang_path, encoding="utf-8") as f:
        lang = json.load(f)

    for i, m in enumerate(NEW):
        mid = m["id"]
        dust = f"{MOD}:{mid}_dust"
        save_png(from_template(T_DUST, palette(m["color"]), 30000 + i), "item", f"{mid}_dust.png")
        write_json(os.path.join(ASSETS, "models", "item", f"{mid}_dust.json"),
                   {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{mid}_dust"}})
        lang[f"item.{MOD}.{mid}_dust"] = f"{m['name']} Dust"
        tag("forge", "items", f"dusts/{mid}", [dust])

        ore_tag, raw_tag = {"tag": f"forge:ores/{mid}"}, {"tag": f"forge:raw_materials/{mid}"}
        # Pulverizer: 2 dust.  Ore Purifier (and Chemical Washer on top of it): 3 dust.
        for src_name, ing in (("ore", ore_tag), ("raw", raw_tag)):
            recipe(f"tieredpower/pulverizing/{mid}_{src_name}", {
                "conditions": [WITH_TI], "type": f"{TI}:pulverizing", "ingredient": ing,
                "result": {"item": dust, "count": 2}, "time": PULVERIZE_TIME[mid]})
            recipe(f"tieredpower/purifying/{mid}_{src_name}", {
                "conditions": [WITH_TI], "type": f"{TI}:purifying", "ingredient": ing,
                "result": {"item": dust, "count": 3}})
        # Ingots back to dust, like TI does for its own metals
        recipe(f"tieredpower/pulverizing/{mid}_ingot", {
            "conditions": [WITH_TI], "type": f"{TI}:pulverizing",
            "ingredient": {"tag": f"forge:ingots/{mid}"}, "result": {"item": dust, "count": 1},
            "time": PULVERIZE_TIME[mid] // 2})

        if mid in TI_ALLOYS:
            extra, extra_count, time = TI_ALLOYS[mid]
            recipe(f"tieredpower/alloying/{mid}_ingot", {
                "conditions": [WITH_TI], "type": f"{TI}:alloying",
                "inputs": [
                    {"ingredient": [{"tag": f"forge:dusts/{mid}"}, raw_tag], "count": 2},
                    {"ingredient": extra, "count": extra_count}],
                "result": {"item": f"{MOD}:{mid}_ingot", "count": 2}, "time": time})
        else:
            # Cobalt dust just smelts
            for kind, t in (("smelting", 200), ("blasting", 100)):
                recipe(f"{mid}_ingot_from_{kind}_dust", {
                    "type": f"minecraft:{kind}", "category": "misc", "group": f"{mid}_ingot",
                    "ingredient": {"item": dust}, "result": f"{MOD}:{mid}_ingot",
                    "experience": 0.5, "cookingtime": t})

    for mid, (pattern, extra_keys) in TI_TEMPLATES.items():
        _, core_block, _, _ = UPGRADES[mid]
        recipe(f"tieredpower/{mid}_upgrade_smithing_template", {
            "conditions": [WITH_TI], "type": "minecraft:crafting_shaped", "category": "misc",
            "pattern": pattern,
            "key": {"X": BY_ID[mid]["ingot"], "B": {"item": core_block}, **extra_keys},
            "result": {"item": f"{MOD}:{mid}_upgrade_smithing_template"}})

    tag("forge", "items", "dusts", [f"#forge:dusts/{m['id']}" for m in NEW])
    write_json(lang_path, lang)


# ===========================================================================
# Phase 5: gear modules and the Armoury Workbench
# ===========================================================================
MODULES = [
    # id, name, colour, recipe items (with the blank module), description, fits
    ("lifesteal", "Lifesteal", (176, 24, 44), [{"item": "minecraft:ghast_tear"},
                                               {"item": "minecraft:glistering_melon_slice"}],
     "Heals you for 15% of the damage you deal", "weapon"),
    ("fire", "Fire", (250, 122, 24), [{"item": "minecraft:blaze_powder"}, {"item": "minecraft:fire_charge"}],
     "Sets what you hit on fire for 4 seconds", "weapon"),
    ("knockback", "Knockback", (120, 210, 96), [{"item": "minecraft:piston"}, {"item": "minecraft:slime_ball"}],
     "Knocks what you hit further back", "weapon"),
    ("looting", "Looting", (232, 200, 60), [{"item": "minecraft:rabbit_foot"}, {"item": "minecraft:emerald"}],
     "+1 Looting on mob drops", "weapon"),
    ("vitality", "Vitality", (240, 92, 132), [{"item": "minecraft:golden_apple"}],
     "+2 hearts of max health", "armour"),
    ("bulwark", "Bulwark", (150, 150, 172), [{"item": "minecraft:obsidian"}, {"item": "minecraft:shield"}],
     "+10% knockback resistance", "armour"),
    ("step_assist", "Step Assist", (222, 182, 96), [{"item": "minecraft:feather"}, {"tag": "minecraft:stairs"}],
     "Walk up full blocks without jumping", "boots"),
    ("aqua_lung", "Aqua Lung", (44, 124, 222), [{"item": "minecraft:pufferfish"},
                                                {"item": "minecraft:prismarine_crystals"}],
     "Breathe under water", "helmet"),
]
FITS_TEXT = {"weapon": "Fits: any Tiered Arsenal weapon", "armour": "Fits: any Tiered Arsenal armour",
             "boots": "Fits: Tiered Arsenal boots", "helmet": "Fits: Tiered Arsenal helmets"}

T_MODULE = [
    "................",
    "................",
    "....y.y..y.y....",
    "...oooooooooo...",
    "..yoBBBBBBBBoy..",
    "...oBccccccBo...",
    "..yoBcCCCCcBoy..",
    "...oBcCHHCcBo...",
    "..yoBcCHHCcBoy..",
    "...oBcCCCCcBo...",
    "..yoBccccccBoy..",
    "...oBBBBBBBBo...",
    "...oooooooooo...",
    "....y.y..y.y....",
    "................",
    "................",
]


def module_texture(color, seed):
    pal = {"o": (30, 30, 34), "B": (72, 76, 84), "y": (232, 186, 60),
           "c": shade(color, 0.55), "C": color, "H": lighten(color, 0.55)}
    return from_template(T_MODULE, pal, seed, jitter=0)


def workbench_textures():
    rnd = random.Random(777)
    steel = (118, 122, 130)

    def plate():
        img = Image.new("RGBA", (16, 16))
        px = img.load()
        for y in range(16):
            for x in range(16):
                j = rnd.randint(-6, 6)
                if x == 0 or y == 0:
                    c = lighten(steel, 0.35)
                elif x == 15 or y == 15:
                    c = shade(steel, 0.55)
                else:
                    c = steel
                px[x, y] = (clamp(c[0] + j), clamp(c[1] + j), clamp(c[2] + j), 255)
        for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):  # rivets
            px[x, y] = (*lighten(steel, 0.5), 255)
            px[x + 1, y + 1] = (*shade(steel, 0.5), 255)
        return img

    side = plate()
    front = plate()
    px = front.load()
    for y in range(4, 11):  # screen
        for x in range(3, 13):
            edge = y in (4, 10) or x in (3, 12)
            px[x, y] = (20, 22, 26, 255) if edge else (36, 40, 46, 255)
    for x in range(5, 11):  # energy line in the mod's red
        px[x, 7] = (214, 58, 46, 255)
    for x in range(5, 8):
        px[x, 9] = (232, 186, 60, 255)

    top = plate()
    px = top.load()
    anvil = [
        "................",
        "................",
        "................",
        "................",
        "...oooooooooo...",
        "...oddddddddo...",
        "....ooddddoo....",
        "......oddo......",
        "......oddo......",
        ".....oddddo.....",
        "....oddddddo....",
        "....oooooooo....",
    ]
    for y, row in enumerate(anvil):
        for x, ch in enumerate(row):
            if ch == "o":
                px[x, y + 1] = (34, 36, 40, 255)
            elif ch == "d":
                px[x, y + 1] = (62, 66, 72, 255)
    return side, front, top


def gui_texture():
    W, H = 176, 186
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    px = img.load()
    bg, hi, lo, edge = (198, 198, 198), (255, 255, 255), (85, 85, 85), (0, 0, 0)
    # Panel with the vanilla bevel and rounded corners
    for y in range(H):
        for x in range(W):
            corner = (x < 2 or x > W - 3) and (y < 2 or y > H - 3)
            if corner and not ((x in (1, W - 2)) and (y in (1, H - 2))):
                continue
            if x == 0 or y == 0 or x == W - 1 or y == H - 1:
                c = edge
            elif x <= 2 or y <= 2:
                c = hi
            elif x >= W - 3 or y >= H - 3:
                c = lo
            else:
                c = bg
            px[x, y] = (*c, 255)
    for (x, y) in ((1, 1), (W - 2, H - 2)):
        px[x, y] = (*edge, 255)
    px[2, 2] = (*hi, 255)
    px[W - 3, H - 3] = (*lo, 255)

    def sunken(x0, y0, w, h, fill=(139, 139, 139)):
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                if x == x0 or y == y0:
                    c = (55, 55, 55)
                elif x == x0 + w - 1 or y == y0 + h - 1:
                    c = hi
                else:
                    c = fill
                px[x, y] = (*c, 255)

    sunken(25, 23, 18, 18)          # gear slot
    sunken(25, 53, 18, 18)          # module slot
    sunken(64, 15, 94, 74, (54, 58, 66))  # module list
    sunken(160, 17, 8, 72, (40, 16, 14))  # energy bar
    for r in range(3):
        for c in range(9):
            sunken(7 + c * 18, 103 + r * 18, 18, 18)
    for c in range(9):
        sunken(7 + c * 18, 161, 18, 18)
    return img


def gen_modules():
    lang_path = os.path.join(ASSETS, "lang", "en_us.json")
    with open(lang_path, encoding="utf-8") as f:
        lang = json.load(f)

    # Module items
    blank = f"{MOD}:blank_module"
    save_png(module_texture((120, 124, 130), 40000), "item", "blank_module.png")
    write_json(os.path.join(ASSETS, "models", "item", "blank_module.json"),
               {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/blank_module"}})
    lang[f"item.{MOD}.blank_module"] = "Blank Module"
    recipe("blank_module", {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["GRG", "RDR", "GRG"],
        "key": {"G": {"tag": "forge:ingots/gold"}, "R": {"tag": "forge:dusts/redstone"},
                "D": {"tag": "forge:gems/diamond"}},
        "result": {"item": blank, "count": 4}})

    for i, (mid, name, color, extras, desc, fits) in enumerate(MODULES):
        item = f"{mid}_module"
        save_png(module_texture(color, 40001 + i), "item", f"{item}.png")
        write_json(os.path.join(ASSETS, "models", "item", f"{item}.json"),
                   {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{item}"}})
        lang[f"item.{MOD}.{item}"] = f"{name} Module"
        lang[f"module.{MOD}.{mid}"] = name
        lang[f"tooltip.{MOD}.module.{mid}"] = desc
        recipe(item, {
            "type": "minecraft:crafting_shapeless", "category": "misc",
            "ingredients": [{"item": blank}] + extras,
            "result": {"item": f"{MOD}:{item}"}})
    for k, v in FITS_TEXT.items():
        lang[f"tooltip.{MOD}.module.fits.{k}"] = v

    # Workbench block
    wb = "armoury_workbench"
    side, front, top = workbench_textures()
    save_png(side, "block", f"{wb}_side.png")
    save_png(front, "block", f"{wb}_front.png")
    save_png(top, "block", f"{wb}_top.png")
    write_json(os.path.join(ASSETS, "models", "block", f"{wb}.json"), {
        "parent": "minecraft:block/orientable_with_bottom",
        "textures": {"top": f"{MOD}:block/{wb}_top", "front": f"{MOD}:block/{wb}_front",
                     "side": f"{MOD}:block/{wb}_side", "bottom": f"{MOD}:block/{wb}_side"}})
    write_json(os.path.join(ASSETS, "models", "item", f"{wb}.json"), {"parent": f"{MOD}:block/{wb}"})
    write_json(os.path.join(ASSETS, "blockstates", f"{wb}.json"), {"variants": {
        "facing=north": {"model": f"{MOD}:block/{wb}"},
        "facing=east": {"model": f"{MOD}:block/{wb}", "y": 90},
        "facing=south": {"model": f"{MOD}:block/{wb}", "y": 180},
        "facing=west": {"model": f"{MOD}:block/{wb}", "y": 270}}})
    write_json(os.path.join(DATA, MOD, "loot_tables", "blocks", f"{wb}.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
                   "entries": [{"type": "minecraft:item", "name": f"{MOD}:{wb}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"{MOD}:blocks/{wb}"})
    tag("minecraft", "blocks", "mineable/pickaxe", [f"{MOD}:{b}" for b in all_blocks()] + [f"{MOD}:{wb}"])
    os.makedirs(os.path.join(ASSETS, "textures", "gui"), exist_ok=True)
    gui_texture().save(os.path.join(ASSETS, "textures", "gui", f"{wb}.png"))

    # With Tiered Industries: built from its parts. Without: plain iron and redstone.
    recipe(f"tieredpower/{wb}", {
        "conditions": [WITH_TI], "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["PAP", "CSC", "PBP"],
        "key": {"P": {"tag": "forge:plates/steel"}, "A": {"item": "minecraft:anvil"},
                "S": {"item": "minecraft:smithing_table"}, "C": {"item": f"{TI}:copper_cable"},
                "B": {"item": "minecraft:redstone_block"}},
        "result": {"item": f"{MOD}:{wb}"}})
    recipe(wb, {
        "conditions": [WITHOUT_TI], "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["IAI", "RSR", "IBI"],
        "key": {"I": {"tag": "forge:ingots/iron"}, "A": {"item": "minecraft:anvil"},
                "S": {"item": "minecraft:smithing_table"}, "R": {"tag": "forge:dusts/redstone"},
                "B": {"item": "minecraft:redstone_block"}},
        "result": {"item": f"{MOD}:{wb}"}})

    g = f"gui.{MOD}.armoury."
    lang.update({
        f"block.{MOD}.{wb}": "Armoury Workbench",
        f"tooltip.{MOD}.modules": "Modules %s/%s",
        g + "install": "Install",
        g + "install_tooltip": "Fit the module to the gear (uses %s FE)",
        g + "remove_tooltip": "Remove - the module comes back",
        g + "insert_gear": "Put gear in",
        g + "slots": "%s/%s slots",
        g + "empty_slot": "- empty -",
        f"message.{MOD}.doesnt_fit": "That module doesn't fit this gear",
        f"message.{MOD}.already_fitted": "That module is already fitted",
        f"message.{MOD}.no_slot": "No free module slots",
        f"message.{MOD}.no_energy": "Not enough power - needs 10,000 FE",
    })
    write_json(lang_path, lang)


# ===========================================================================
# Phase 6: shields, throwable spears, advancements
# ===========================================================================
SHIELD_MATS = ["copper", "emerald", "cobalt", "titanium", "infernium", "aetherium"]
SHIELD_PREV = {"cobalt": "minecraft:shield", "titanium": f"{MOD}:cobalt_shield",
               "infernium": f"{MOD}:titanium_shield", "aetherium": f"{MOD}:infernium_shield"}
SHIELD_PERKS = {"titanium": "Blocking a hit knocks the attacker back",
                "infernium": "Blocking a hit sets the attacker on fire",
                "aetherium": "Blocking a hit reflects 25% of the damage"}


def shield_texture(material, seed):
    """64x64 texture in the vanilla shield model's UV layout."""
    rnd = random.Random(seed)
    pal = palette(material["color"])
    wood, wood_dark = (122, 88, 52), (86, 60, 34)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()

    def put(x, y, c, j=6):
        v = rnd.randint(-j, j)
        px[x, y] = (clamp(c[0] + v), clamp(c[1] + v), clamp(c[2] + v), 255)

    def rect(x0, y0, w, h, fn):
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                fn(x, y, x - x0, y - y0, w, h)

    def face(x, y, i, j, w, h):  # shield front: rim, body, emblem
        if i == 0 or j == 0:
            put(x, y, pal["l"])
        elif i == w - 1 or j == h - 1:
            put(x, y, pal["o"], 2)
        elif i == 1 or j == 1 or i == w - 2 or j == h - 2:
            put(x, y, pal["d"])
        elif abs(i - (w - 1) / 2) + abs(j - (h - 1) / 2) * 0.55 <= 3.2:
            put(x, y, pal["h"], 3)
        else:
            put(x, y, pal["m"])

    def back(x, y, i, j, w, h):  # wooden back with a metal rim
        if i in (0, w - 1) or j in (0, h - 1):
            put(x, y, pal["d"])
        else:
            put(x, y, wood_dark if j % 5 == 0 else wood)

    def edge(x, y, i, j, w, h):
        put(x, y, pal["d"])

    def handle(x, y, i, j, w, h):
        put(x, y, wood_dark if (i + j) % 3 == 0 else wood)

    # Plate: texOffs(0,0), 12 x 22 x 1
    rect(1, 0, 12, 1, edge)
    rect(13, 0, 12, 1, edge)
    rect(0, 1, 1, 22, edge)
    rect(1, 1, 12, 22, face)
    rect(13, 1, 1, 22, edge)
    rect(14, 1, 12, 22, back)
    # Handle: texOffs(26,0), 2 x 6 x 6
    rect(32, 0, 2, 6, handle)
    rect(34, 0, 2, 6, handle)
    rect(26, 6, 6, 6, handle)
    rect(32, 6, 2, 6, handle)
    rect(34, 6, 6, 6, handle)
    rect(40, 6, 2, 6, handle)
    return img


ADVANCEMENTS = [
    # id, parent, icon, title, description, items (any of), frame, all_required
    ("root", None, "cobalt_sword", "Tiered Arsenal", "Find one of the new ores",
     ["raw_cobalt", "raw_titanium", "raw_infernium", "raw_aetherium"], "task", False),
    ("cobalt", "root", "cobalt_ingot", "Blue Steel", "Make a Cobalt Ingot", ["cobalt_ingot"], "task", False),
    ("first_upgrade", "cobalt", "cobalt_upgrade_smithing_template", "Upgrade Path",
     "Upgrade a piece of iron gear to Cobalt in a smithing table",
     [f"cobalt_{p}" for p in ["sword", "axe", "helmet", "chestplate", "leggings", "boots", "greatsword",
                             "dagger", "spear", "warhammer", "bow", "shield"]], "task", False),
    ("titanium", "first_upgrade", "titanium_ingot", "Light but Strong", "Make a Titanium Ingot",
     ["titanium_ingot"], "task", False),
    ("infernium", "titanium", "infernium_ingot", "Forged in Fire", "Make an Infernium Ingot",
     ["infernium_ingot"], "task", False),
    ("aetherium", "infernium", "aetherium_ingot", "Beyond the Dragon", "Make an Aetherium Ingot",
     ["aetherium_ingot"], "goal", False),
    ("full_aetherium", "aetherium", "aetherium_chestplate", "Peak Arsenal",
     "Have a full set of Aetherium armour", [f"aetherium_{p}" for p in ARMOR_PIECES], "challenge", True),
    ("workbench", "cobalt", "armoury_workbench", "Tinkerer", "Build an Armoury Workbench",
     ["armoury_workbench"], "task", False),
    ("module", "workbench", "lifesteal_module", "Plugged In", "Make a gear module",
     [f"{m[0]}_module" for m in MODULES], "task", False),
    ("shield", "root", "cobalt_shield", "Hold the Line", "Get a tiered shield",
     [f"{m}_shield" for m in SHIELD_MATS], "task", False),
]


def gen_phase6():
    lang_path = os.path.join(ASSETS, "lang", "en_us.json")
    with open(lang_path, encoding="utf-8") as f:
        lang = json.load(f)

    # Shields
    for i, mid in enumerate(SHIELD_MATS):
        m = BY_ID[mid]
        name = f"{mid}_shield"
        shield_texture(m, 50000 + i).save(_ensure(os.path.join(ASSETS, "textures", "entity", "shield", f"{mid}.png")))
        write_json(os.path.join(ASSETS, "models", "item", f"{name}.json"), {
            "parent": "minecraft:item/shield",
            "overrides": [{"predicate": {"blocking": 1}, "model": f"{MOD}:item/{name}_blocking"}]})
        write_json(os.path.join(ASSETS, "models", "item", f"{name}_blocking.json"),
                   {"parent": "minecraft:item/shield_blocking"})
        lang[f"item.{MOD}.{name}"] = f"{m['name']} Shield"
        if mid in SHIELD_PERKS:
            lang[f"tooltip.{MOD}.shield.{mid}"] = SHIELD_PERKS[mid]
        if mid in SHIELD_PREV:
            recipe(f"{name}_smithing", {
                "type": "minecraft:smithing_transform",
                "template": {"item": f"{MOD}:{mid}_upgrade_smithing_template"},
                "base": {"item": SHIELD_PREV[mid]},
                "addition": m["ingot"],
                "result": {"item": f"{MOD}:{name}"}})
        else:
            shaped(name, ["XWX", "XXX", " X "], {"X": m["ingot"], "W": {"tag": "minecraft:planks"}},
                   f"{MOD}:{name}")
    tag("forge", "items", "tools/shields", [f"{MOD}:{m}_shield" for m in SHIELD_MATS])

    # Spears can be thrown now
    lang[f"tooltip.{MOD}.spear"] = "+1.5 block reach. Hold right-click and let go to throw"
    lang[f"entity.{MOD}.thrown_spear"] = "Thrown Spear"

    # Advancements
    for aid, parent, icon, title, desc, items, frame, all_required in ADVANCEMENTS:
        display = {"icon": {"item": f"{MOD}:{icon}"},
                   "title": {"translate": f"advancements.{MOD}.{aid}.title"},
                   "description": {"translate": f"advancements.{MOD}.{aid}.description"},
                   "frame": frame, "show_toast": parent is not None,
                   "announce_to_chat": parent is not None, "hidden": False}
        if parent is None:
            display["background"] = "minecraft:textures/block/deepslate.png"
        ids = [f"{MOD}:{x}" for x in items]
        if all_required:
            criteria = {"has_all": {"trigger": "minecraft:inventory_changed",
                                    "conditions": {"items": [{"items": [i]} for i in ids]}}}
        else:
            criteria = {"has_any": {"trigger": "minecraft:inventory_changed",
                                    "conditions": {"items": [{"items": ids}]}}}
        adv = {"display": display, "criteria": criteria, "requirements": [list(criteria)]}
        if parent:
            adv["parent"] = f"{MOD}:{parent}"
        write_json(os.path.join(DATA, MOD, "advancements", f"{aid}.json"), adv)
        lang[f"advancements.{MOD}.{aid}.title"] = title
        lang[f"advancements.{MOD}.{aid}.description"] = desc

    write_json(lang_path, lang)


JEI_INFO = {
    "cobalt_ore": "Found in the Overworld from Y 32 down to Y -48, most common around Y -8. Needs an iron "
                  "pickaxe. Smelt the raw ore into Cobalt Ingots - with Tiered Industries, pulverize it first "
                  "for 2 dust (3 in the Ore Purifier) and smelt the dust.",
    "titanium_ore": "Found deep in the Overworld, Y -16 and below, mostly in deepslate. Needs a diamond pickaxe. "
                    "Smelts into Titanium Ingots - but with Tiered Industries installed it must go through the "
                    "Alloy Smelter: 2 dust or raw ore + 1 Steel Ingot = 2 ingots.",
    "infernium_ore": "Found all through the Nether in netherrack; glows faintly and survives lava. Needs a diamond "
                     "pickaxe. With Tiered Industries: Alloy Smelter only, 2 dust or raw ore + 2 Blaze Powder = "
                     "2 ingots.",
    "aetherium_ore": "Found only on the outer End islands (not the main island), in end stone. Needs a netherite "
                     "pickaxe. With Tiered Industries: Alloy Smelter only, 2 dust or raw ore + 1 Superconductor "
                     "Ingot = 2 ingots.",
    "dust": "Made from ores and raw ores in the Tiered Industries Pulverizer (2) or Ore Purifier (3). Cobalt Dust "
            "smelts in a furnace; the others go into the Alloy Smelter.",
    "gated_ingot": "With Tiered Industries installed this ingot can only be made in the Alloy Smelter - the "
                   "furnace won't take the ore. Without Tiered Industries it smelts normally.",
    "upgrade_template": "Upgrades the tier below in a smithing table: Iron to Cobalt to Titanium to Infernium to "
                        "Aetherium. Use the template, the piece to upgrade and one ingot. Enchantments, names, "
                        "trims and fitted modules are kept. Copy a template with 7 ingots and a stone block for 2.",
    "greatsword": "Heavy two-handed blade: slow (attack speed 1.1) but hits hard, with the normal sweep. "
                  "Copper, Emerald and Iron are crafted; Cobalt and up are upgraded from the tier below.",
    "dagger": "Fast (attack speed 2.2) and light. Hitting a mob from behind does 1.5x damage.",
    "spear": "Reaches 1.5 blocks further than a sword and doesn't sweep. Hold right-click and let go to throw it "
             "like a trident (1.25x damage), then pick it back up.",
    "warhammer": "Very slow (attack speed 0.8). Every hit sends a shockwave: 40% of the damage to mobs within 2.5 "
                 "blocks of the target, knocking them back. Never hits you or your pets.",
    "bow": "Works like the vanilla bow with more durability and extra arrow damage at each tier, from +0 (Copper) "
           "to +3 (Aetherium).",
    "shield": "Blocks like the vanilla shield with more durability. When blocking a melee hit, Titanium knocks the "
              "attacker back, Infernium sets it on fire and Aetherium reflects 25% of the damage. Cobalt and up "
              "are upgraded from a vanilla shield.",
    "armoury_workbench": "Fits and removes gear modules. Power (Forge Energy) goes in through the BOTTOM only. "
                         "Gear in the top slot, module below, press Install (10,000 FE). Press x to take a module "
                         "off - free, and the module comes back.",
    "blank_module": "The base for every gear module. 4 from gold, redstone and a diamond.",
}


def gen_misc_recipes():
    # Gunpowder without hunting creepers: any coal or charcoal + flint + bone meal -> 2 gunpowder
    recipe("gunpowder", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"tag": "minecraft:coals"}, {"item": "minecraft:flint"}, {"item": "minecraft:bone_meal"}],
        "result": {"item": "minecraft:gunpowder", "count": 2}})


def gen_jei_lang():
    lang_path = os.path.join(ASSETS, "lang", "en_us.json")
    with open(lang_path, encoding="utf-8") as f:
        lang = json.load(f)
    for k, v in JEI_INFO.items():
        lang[f"jei.{MOD}.info.{k}"] = v
    slots = "Fits once per piece. Slots: Copper, Emerald, Iron 1; Cobalt 2; Titanium 3; Infernium 4; Aetherium 5."
    for mid, name, _, _, desc, fits in MODULES:
        where = {"weapon": "any Tiered Arsenal weapon", "armour": "any Tiered Arsenal armour piece",
                 "boots": "Tiered Arsenal boots", "helmet": "Tiered Arsenal helmets",
                 "chestplate": "Tiered Arsenal chestplates", "leggings": "Tiered Arsenal leggings",
                 "tool": "Tiered Arsenal pickaxes, shovels, axes and hoes",
                 "gear": "any Tiered Arsenal weapon or armour piece"}[fits]
        lang[f"jei.{MOD}.info.module.{mid}"] = f"{desc}. Fit it to {where} at the Armoury Workbench. {slots}"
    write_json(lang_path, lang)


# ===========================================================================
# 0.9.0: trims, enchantments, quiver and tool belt, Aetherium Guardian boss
# ===========================================================================
MODULES.append(("celestial", "Celestial", (186, 236, 255), [{"item": f"{MOD}:guardian_core"}],
                "Weapons: +2 damage. Armour: +1 armour and +1 toughness", "gear"))
FITS_TEXT["gear"] = "Fits: any Tiered Arsenal weapon or armour piece"

TRIM_MATS = [("cobalt", 0.01), ("titanium", 0.02), ("infernium", 0.03), ("aetherium", 0.04)]
TRIM_PATTERNS = ["coast", "sentry", "dune", "wild", "ward", "eye", "vex", "tide", "snout", "rib", "spire",
                 "wayfinder", "shaper", "silence", "raiser", "host"]

ENCHANTS = {
    "shockwave": ("Shockwave", "War hammer: the shockwave reaches 0.5 blocks further and hits 10% harder per level."),
    "assassin": ("Assassin", "Dagger: backstabs do an extra 0.25x damage per level."),
    "reach": ("Reach", "Spear: +0.5 blocks reach per level."),
    "counter": ("Counter", "Any shield: blocking a melee hit deals 2 damage per level to the attacker."),
}

T_QUIVER = [
    "................",
    "...........f.f..",
    "..........fs.s..",
    ".........oosoo..",
    "........oBBBBo..",
    ".......oBbbbbo..",
    "......oBbbbbo...",
    ".....oBbbbbo....",
    "....oBbyybo.....",
    "...oBbbbbo......",
    "..oBbbbbo.......",
    "..obbbbo........",
    "..oobbo.........",
    "...ooo..........",
    "................",
    "................",
]

T_TOOL_BELT = [
    "................",
    "................",
    "................",
    "....p.....k.....",
    "...ppp...kkk....",
    "....w.....w.....",
    "oooowoooooowoooo",
    "bBbbbbbbbbbbbbBb",
    "bbbbbbyyyybbbbbb",
    "bbbbbbyoyybbbbbb",
    "bbbbbbyyyybbbbbb",
    "oooooooooooooooo",
    "................",
    "................",
    "................",
    "................",
]

T_SIGIL = [
    "................",
    "......oooo......",
    "....ooPPPPoo....",
    "...oPPppppPPo...",
    "..oPpkkkkkkpPo..",
    "..oPpkkkkkkpPo..",
    ".oPpkkcCCckkpPo.",
    ".oPpkkCHHCkkpPo.",
    ".oPpkkCHHCkkpPo.",
    ".oPpkkcCCckkpPo.",
    "..oPpkkkkkkpPo..",
    "..oPpkkkkkkpPo..",
    "...oPPppppPPo...",
    "....ooPPPPoo....",
    "......oooo......",
    "................",
]

T_CORE = [
    "................",
    ".......oo.......",
    "......oHHo......",
    ".....oHCCco.....",
    "....oHCCCcco....",
    "...oHCCPCCcco...",
    "..oHCCPPPCccco..",
    "..oCCPPHPPCcco..",
    "..ocCCPPPCCcco..",
    "...occCCPCcco...",
    "....occCCcco....",
    ".....occcco.....",
    "......occo......",
    ".......oo.......",
    "................",
    "................",
]


def guardian_textures():
    """64x32 skeleton-layout texture and an eyes overlay for the boss."""
    bone, bone_l, bone_d = (150, 104, 226), (196, 162, 252), (88, 56, 150)
    rnd = random.Random(90000)
    tex = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    eyes = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px, ex = tex.load(), eyes.load()

    def fill(rect, base=bone, ribs=False):
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                c = bone_l if (x == 0 or y == 0) else bone_d if (x == w - 1 or y == h - 1) else base
                if ribs and y % 2 == 1 and 0 < x < w - 1:
                    c = bone_d
                j = rnd.randint(-8, 8)
                px[x0 + x, y0 + y] = (clamp(c[0] + j), clamp(c[1] + j), clamp(c[2] + j), 255)

    head = box_faces(0, 0, 8, 8, 8)
    for f in head.values():
        fill(f)
    fx, fy, _, _ = head["front"]
    for (x, y) in ((1, 3), (2, 3), (1, 4), (2, 4), (5, 3), (6, 3), (5, 4), (6, 4)):
        px[fx + x, fy + y] = (16, 8, 28, 255)                 # eye sockets
        ex[fx + x, fy + y] = (120, 255, 240, 255)             # glow
    for x in range(2, 6):
        px[fx + x, fy + 6] = (40, 24, 70, 255)                # jaw line
    body = box_faces(16, 16, 8, 12, 4)
    for name, f in body.items():
        fill(f, ribs=name in ("front", "back"))
    for u, v in ((40, 16), (0, 16)):                          # arms and legs (2x12x2)
        for f in box_faces(u, v, 2, 12, 2).values():
            fill(f)
    return tex, eyes


def gen_phase9():
    lang_path = os.path.join(ASSETS, "lang", "en_us.json")
    with open(lang_path, encoding="utf-8") as f:
        lang = json.load(f)

    # ---- Armour trim materials ----
    perms = {}
    for mid, index in TRIM_MATS:
        m = BY_ID[mid]
        asset = f"{MOD}_{mid}"
        r, g, b = m["color"]
        write_json(os.path.join(DATA, MOD, "trim_material", f"{mid}.json"), {
            "asset_name": asset,
            "description": {"color": f"#{r:02X}{g:02X}{b:02X}", "translate": f"trim_material.{MOD}.{mid}"},
            "ingredient": f"{MOD}:{mid}_ingot",
            "item_model_index": index})
        lang[f"trim_material.{MOD}.{mid}"] = f"{m['name']} Material"
        pal = Image.new("RGBA", (8, 1))
        for i in range(8):  # light to dark, like the vanilla palettes
            f = 1.45 - i * 0.12
            pal.putpixel((i, 0), (*(lighten(m["color"], f - 1) if f > 1 else shade(m["color"], f)), 255))
        save_png(pal, "trims", "color_palettes", f"{mid}.png")
        perms[asset] = f"{MOD}:trims/color_palettes/{mid}"
    tag("minecraft", "items", "trim_materials", [f"{MOD}:{mid}_ingot" for mid, _ in TRIM_MATS])
    textures = []
    for p in TRIM_PATTERNS:
        textures += [f"trims/models/armor/{p}", f"trims/models/armor/{p}_leggings"]
    write_json(os.path.join(ROOT, "assets", "minecraft", "atlases", "armor_trims.json"), {
        "sources": [{"type": "paletted_permutations", "textures": textures,
                     "palette_key": "trims/color_palettes/trim_palette", "permutations": perms}]})

    # ---- Enchantments ----
    for eid, (name, desc) in ENCHANTS.items():
        lang[f"enchantment.{MOD}.{eid}"] = name
        lang[f"enchantment.{MOD}.{eid}.desc"] = desc

    # ---- Quiver and tool belt ----
    leather = {"o": (52, 32, 16), "b": (139, 90, 43), "B": (176, 120, 64), "y": (232, 186, 60),
               "s": (122, 92, 52), "f": (240, 240, 240), "p": (150, 150, 156), "k": (150, 150, 156),
               "w": (112, 82, 40)}
    for name, tpl, title in (("quiver", T_QUIVER, "Quiver"), ("tool_belt", T_TOOL_BELT, "Tool Belt")):
        save_png(from_template(tpl, leather, 91000, jitter=0), "item", f"{name}.png")
        write_json(os.path.join(ASSETS, "models", "item", f"{name}.json"),
                   {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})
        lang[f"item.{MOD}.{name}"] = title
    recipe("quiver", {"type": "minecraft:crafting_shaped", "category": "equipment",
                      "pattern": ["LSL", "LAL", "LLL"],
                      "key": {"L": {"item": "minecraft:leather"}, "S": {"item": "minecraft:string"},
                              "A": {"item": "minecraft:arrow"}},
                      "result": {"item": f"{MOD}:quiver"}})
    recipe("tool_belt", {"type": "minecraft:crafting_shaped", "category": "equipment",
                         "pattern": ["LCL", "LIL"],
                         "key": {"L": {"item": "minecraft:leather"}, "C": {"tag": "forge:chests/wooden"},
                                 "I": {"tag": "forge:ingots/iron"}},
                         "result": {"item": f"{MOD}:tool_belt"}})
    lang.update({
        f"tooltip.{MOD}.storage": "Holds %s/%s stacks",
        f"tooltip.{MOD}.quiver": "Right-click to pack your arrows in. Feeds your bow when you run out",
        f"tooltip.{MOD}.tool_belt": "Right-click tools onto it to store them. Press the Tool Belt key (G) to swap",
        f"key.{MOD}.tool_belt": "Tool Belt: swap held tool",
        f"key.categories.{MOD}": "Tiered Arsenal",
    })

    # ---- Aetherium Guardian ----
    tex, eyes = guardian_textures()
    tex.save(_ensure(os.path.join(ASSETS, "textures", "entity", "aetherium_guardian.png")))
    eyes.save(os.path.join(ASSETS, "textures", "entity", "aetherium_guardian_eyes.png"))
    sig = {"o": (20, 10, 34), "P": (150, 96, 236), "p": (100, 60, 170), "k": (24, 16, 40),
           "c": (60, 180, 170), "C": (120, 255, 240), "H": (230, 255, 252)}
    core = {"o": (30, 20, 60), "H": (240, 255, 255), "C": (150, 240, 255), "c": (70, 150, 200),
            "P": (190, 140, 255)}
    for name, tpl, pal, title in (("void_sigil", T_SIGIL, sig, "Void Sigil"),
                                  ("guardian_core", T_CORE, core, "Guardian Core")):
        save_png(from_template(tpl, pal, 92000, jitter=0), "item", f"{name}.png")
        write_json(os.path.join(ASSETS, "models", "item", f"{name}.json"),
                   {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})
        lang[f"item.{MOD}.{name}"] = title
    write_json(os.path.join(ASSETS, "models", "item", "aetherium_guardian_spawn_egg.json"),
               {"parent": "minecraft:item/template_spawn_egg"})
    lang[f"item.{MOD}.aetherium_guardian_spawn_egg"] = "Aetherium Guardian Spawn Egg"
    lang[f"entity.{MOD}.aetherium_guardian"] = "Aetherium Guardian"
    lang[f"tooltip.{MOD}.void_sigil"] = "Use on a block in the End to summon the Aetherium Guardian"
    lang[f"message.{MOD}.sigil_end_only"] = "The sigil only works in the End"
    recipe("void_sigil", {"type": "minecraft:crafting_shaped", "category": "misc",
                          "pattern": ["EAE", "ACA", "EAE"],
                          "key": {"E": {"item": "minecraft:ender_eye"}, "A": {"tag": "forge:ingots/aetherium"},
                                  "C": {"item": "minecraft:end_crystal"}},
                          "result": {"item": f"{MOD}:void_sigil"}})

    def uniform(lo, hi):
        return [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    write_json(os.path.join(DATA, MOD, "loot_tables", "entities", "aetherium_guardian.json"), {
        "type": "minecraft:entity",
        "pools": [
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:guardian_core"}]},
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:aetherium_ingot",
                                      "functions": uniform(6, 10)}]},
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:aetherium_upgrade_smithing_template",
                                      "functions": uniform(1, 2)}]}]})

    write_json(os.path.join(DATA, MOD, "advancements", "guardian_slain.json"), {
        "parent": f"{MOD}:aetherium",
        "display": {"icon": {"item": f"{MOD}:guardian_core"},
                    "title": {"translate": f"advancements.{MOD}.guardian_slain.title"},
                    "description": {"translate": f"advancements.{MOD}.guardian_slain.description"},
                    "frame": "challenge", "show_toast": True, "announce_to_chat": True, "hidden": False},
        "criteria": {"killed": {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": [
            {"condition": "minecraft:entity_properties", "entity": "this",
             "predicate": {"type": f"{MOD}:aetherium_guardian"}}]}}},
        "requirements": [["killed"]]})
    lang[f"advancements.{MOD}.guardian_slain.title"] = "Guardian Slain"
    lang[f"advancements.{MOD}.guardian_slain.description"] = "Defeat the Aetherium Guardian"

    JEI_INFO.update({
        "quiver": "Holds 4 stacks of any arrows. Right-click it in the air to pack every arrow in your inventory "
                  "into it, or right-click arrows onto it like a bundle. When your bow or crossbow finds no arrows "
                  "in your inventory, the quiver hands you its next stack.",
        "tool_belt": "Holds 4 tools or weapons. Right-click them onto it like a bundle. Press the Tool Belt key (G by "
                     "default, see Controls) to swap your held item for the next one on the belt.",
        "void_sigil": "Use on a block in the End to summon the Aetherium Guardian. 300 health, a shockwave up close, "
                      "homing shulker bullets at range, and it teleports after you if you run.",
        "guardian_core": "Dropped by the Aetherium Guardian. The only way to make a Celestial Module.",
    })
    write_json(lang_path, lang)


def _ensure(path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    return path


# ===========================================================================
# 0.8.0: tools, more modules, crossbows, special arrows, chest loot
# ===========================================================================
T_PICKAXE = [
    "................",
    "....oooooo......",
    "...ohhhllmoo....",
    "....ooooolmmo...",
    "........owWldo..",
    ".......owWo.mdo.",
    "......owWo..odo.",
    ".....owWo....mo.",
    "....owWo.....ldo",
    "...owWo......odo",
    "..owWo........o.",
    ".owWo...........",
    "owWo............",
    "oo..............",
    "................",
    "................",
]

T_SHOVEL = [
    "................",
    "..........ooo...",
    ".........ohhlo..",
    "........ohllmdo.",
    "........olmmmdo.",
    ".........ommdo..",
    "........owWo....",
    ".......owWo.....",
    "......owWo......",
    ".....owWo.......",
    "....owWo........",
    "...owWo.........",
    "..owWo..........",
    ".owWo...........",
    ".oWo............",
    ".oo.............",
]

T_HOE = [
    "................",
    ".....oooooooo...",
    "....ohhllmmdWo..",
    "....oddddowWo...",
    ".....oooowWo....",
    ".......owWo.....",
    "......owWo......",
    ".....owWo.......",
    "....owWo........",
    "...owWo.........",
    "..owWo..........",
    ".owWo...........",
    "owWo............",
    "ooo.............",
    "................",
    "................",
]

TOOLS = [("pickaxe", "Pickaxe", T_PICKAXE, ["XXX", " S ", " S "]),
         ("shovel", "Shovel", T_SHOVEL, ["X", "S", "S"]),
         ("hoe", "Hoe", T_HOE, ["XX", " S", " S"])]

CROSSBOW_MATS = ["copper", "emerald", "cobalt", "titanium", "infernium", "aetherium"]
CROSSBOW_PREV = {"cobalt": "minecraft:crossbow", "titanium": f"{MOD}:cobalt_crossbow",
                 "infernium": f"{MOD}:titanium_crossbow", "aetherium": f"{MOD}:infernium_crossbow"}

MODULES.extend([
    ("thorns", "Thorns", (64, 140, 64), [{"item": "minecraft:cactus"}, {"item": "minecraft:sweet_berries"}],
     "Melee attackers take 10% of the damage back, per piece", "armour"),
    ("regeneration", "Regeneration", (230, 110, 170), [{"item": "minecraft:ghast_tear"},
                                                       {"item": "minecraft:golden_carrot"}],
     "Regeneration I while you are hurt", "chestplate"),
    ("swiftness", "Swiftness", (130, 200, 255), [{"item": "minecraft:sugar"}, {"item": "minecraft:rabbit_hide"}],
     "+10% walking speed", "leggings"),
    ("auto_smelt", "Auto-Smelt", (150, 104, 84), [{"item": "minecraft:blast_furnace"}, {"item": "minecraft:blaze_rod"}],
     "Mined blocks drop their smelted result", "tool"),
    ("vein_miner", "Vein Miner", (90, 220, 200), [{"item": "minecraft:iron_pickaxe"}, {"item": "minecraft:observer"}],
     "Sneak while mining an ore or log to break up to 32 connected blocks", "tool"),
])
FITS_TEXT.update({"chestplate": "Fits: Tiered Arsenal chestplates", "leggings": "Fits: Tiered Arsenal leggings",
                  "tool": "Fits: Tiered Arsenal pickaxes, shovels, axes and hoes"})

T_ARROW = [
    "................",
    "................",
    "..........HHHH..",
    "...........HHH..",
    "..........sHH...",
    ".........sg.H...",
    "........sg......",
    ".......s........",
    "......s.........",
    ".....s..........",
    "..f.s...........",
    "..ffs...........",
    "...fff..........",
    "................",
    "................",
    "................",
]
ARROWS = [
    ("explosive_arrow", "Explosive Arrow", (196, 40, 36), (80, 80, 84),
     "Explodes where it lands (breaks blocks only if mobGriefing is on)"),
    ("fire_arrow", "Fire Arrow", (255, 140, 24), (255, 210, 80), "Flies burning and sets what it hits on fire"),
    ("heavy_arrow", "Heavy Arrow", (84, 84, 96), (150, 150, 160), "+1.5 damage and extra knockback"),
]

LOOT = [
    # loot table (minecraft:chests/...), item, min, max, chance
    ("simple_dungeon", "cobalt_ingot", 1, 3, 0.35),
    ("simple_dungeon", "cobalt_upgrade_smithing_template", 1, 1, 0.10),
    ("abandoned_mineshaft", "raw_cobalt", 2, 5, 0.30),
    ("abandoned_mineshaft", "copper_pickaxe", 1, 1, 0.10),
    ("desert_pyramid", "emerald_sword", 1, 1, 0.12),
    ("jungle_temple", "emerald_dagger", 1, 1, 0.15),
    ("pillager_outpost", "explosive_arrow", 2, 6, 0.40),
    ("pillager_outpost", "cobalt_crossbow", 1, 1, 0.08),
    ("stronghold_corridor", "titanium_ingot", 1, 2, 0.20),
    ("stronghold_corridor", "titanium_upgrade_smithing_template", 1, 1, 0.10),
    ("ancient_city", "blank_module", 1, 3, 0.30),
    ("ancient_city", "titanium_upgrade_smithing_template", 1, 1, 0.12),
    ("nether_bridge", "raw_infernium", 1, 3, 0.30),
    ("bastion_treasure", "infernium_ingot", 1, 2, 0.30),
    ("bastion_treasure", "infernium_upgrade_smithing_template", 1, 1, 0.25),
    ("end_city_treasure", "aetherium_ingot", 1, 1, 0.10),
    ("end_city_treasure", "aetherium_upgrade_smithing_template", 1, 1, 0.15),
]


def crossbow_texture(material, stage, seed):
    """stage 0 resting, 1-3 drawing, 4 loaded with an arrow, 5 loaded with a firework."""
    pal = palette(material["color"])
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    wood, wood_l = (92, 64, 32), (128, 92, 48)

    def put(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = (*c, 255)

    def line(a, b, c, skip=()):
        (x0, y0), (x1, y1) = a, b
        n = max(abs(x1 - x0), abs(y1 - y0), 1)
        for i in range(n + 1):
            p = (round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n))
            if p not in skip:
                put(*p, c)

    # stock
    for i in range(9):
        put(2 + i, 13 - i, wood)
        put(3 + i, 13 - i, wood_l)
    put(4, 12, (60, 60, 64))  # trigger
    # limbs: two strokes bulging forward (towards top-right)
    limb = set()
    for a, b in (((7, 1), (12, 3)), ((12, 3), (14, 8))):
        (x0, y0), (x1, y1) = a, b
        n = max(abs(x1 - x0), abs(y1 - y0))
        for i in range(n + 1):
            limb.add((round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n)))
    for (x, y) in limb:
        c = pal["l"] if y < 4 else pal["m"]
        j = rnd.randint(-6, 6)
        put(x, y, tuple(clamp(v + j) for v in c))
    for (x, y) in list(limb):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, y + dy)
            if n not in limb and 0 <= n[0] < 16 and 0 <= n[1] < 16 and px[n][3] == 0:
                put(*n, pal["o"])
    # string
    nock = {0: (10, 5), 1: (9, 6), 2: (8, 7), 3: (7, 8), 4: (7, 8), 5: (7, 8)}[stage]
    string = (224, 224, 220)
    line((7, 1), nock, string, skip=limb)
    line(nock, (14, 8), string, skip=limb)
    # loaded projectile along the stock
    if stage >= 4:
        for i in range(9):
            x, y = nock[0] + i, nock[1] - i
            if stage == 4:
                c = (150, 150, 156) if i >= 7 else (122, 92, 52)
            else:
                c = (240, 240, 240) if i >= 7 else (200, 44, 40)
            put(x, y, c)
    return img


def gen_phase8():
    lang_path = os.path.join(ASSETS, "lang", "en_us.json")
    with open(lang_path, encoding="utf-8") as f:
        lang = json.load(f)

    # ---- Tools ----
    for i, m in enumerate(MATERIALS):
        mid = m["id"]
        pal = palette(m["color"])
        if m["handle"] == "minecraft:blaze_rod":
            pal["w"], pal["W"] = pal["y"], pal["Y"]
        for j, (tid, tname, tpl, pattern) in enumerate(TOOLS):
            name = f"{mid}_{tid}"
            save_png(from_template(tpl, pal, 60000 + i * 10 + j), "item", f"{name}.png")
            write_json(os.path.join(ASSETS, "models", "item", f"{name}.json"),
                       {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MOD}:item/{name}"}})
            lang[f"item.{MOD}.{name}"] = f"{m['name']} {tname}"
            if mid in UPGRADES:
                prev = UPGRADES[mid][0]
                recipe(f"{name}_smithing", {
                    "type": "minecraft:smithing_transform",
                    "template": {"item": f"{MOD}:{mid}_upgrade_smithing_template"},
                    "base": {"item": f"{prev}_{tid}"}, "addition": m["ingot"],
                    "result": {"item": f"{MOD}:{name}"}})
            else:
                shaped(name, pattern, {"X": m["ingot"], "S": {"item": "minecraft:stick"}}, f"{MOD}:{name}")
    for tid, _, _, _ in TOOLS:
        ids = [f"{MOD}:{m['id']}_{tid}" for m in MATERIALS]
        tag("minecraft", "items", f"{tid}s", ids)
        tag("forge", "items", f"tools/{tid}s", ids)

    # ---- Crossbows ----
    for i, mid in enumerate(CROSSBOW_MATS):
        m = BY_ID[mid]
        name = f"{mid}_crossbow"
        frames = {"": 0, "_pulling_0": 1, "_pulling_1": 2, "_pulling_2": 3, "_arrow": 4, "_firework": 5}
        for suffix, stage in frames.items():
            save_png(crossbow_texture(m, stage, 70000 + i * 10 + stage), "item", f"{name}{suffix}.png")
        write_json(os.path.join(ASSETS, "models", "item", f"{name}.json"), {
            "parent": "minecraft:item/crossbow", "textures": {"layer0": f"{MOD}:item/{name}"},
            "overrides": [
                {"predicate": {"pulling": 1}, "model": f"{MOD}:item/{name}_pulling_0"},
                {"predicate": {"pulling": 1, "pull": 0.58}, "model": f"{MOD}:item/{name}_pulling_1"},
                {"predicate": {"pulling": 1, "pull": 1.0}, "model": f"{MOD}:item/{name}_pulling_2"},
                {"predicate": {"charged": 1}, "model": f"{MOD}:item/{name}_arrow"},
                {"predicate": {"charged": 1, "firework": 1}, "model": f"{MOD}:item/{name}_firework"}]})
        for suffix in list(frames)[1:]:
            write_json(os.path.join(ASSETS, "models", "item", f"{name}{suffix}.json"),
                       {"parent": f"{MOD}:item/{name}", "textures": {"layer0": f"{MOD}:item/{name}{suffix}"}})
        lang[f"item.{MOD}.{name}"] = f"{m['name']} Crossbow"
        if mid in CROSSBOW_PREV:
            recipe(f"{name}_smithing", {
                "type": "minecraft:smithing_transform",
                "template": {"item": f"{MOD}:{mid}_upgrade_smithing_template"},
                "base": {"item": CROSSBOW_PREV[mid]}, "addition": m["ingot"],
                "result": {"item": f"{MOD}:{name}"}})
        else:
            shaped(name, ["SXS", "TJT", " S "], {"X": m["ingot"], "S": {"item": "minecraft:stick"},
                                                 "T": {"item": "minecraft:string"},
                                                 "J": {"item": "minecraft:tripwire_hook"}}, f"{MOD}:{name}")
    tag("forge", "items", "tools/crossbows", [f"{MOD}:{m}_crossbow" for m in CROSSBOW_MATS])

    # ---- Arrows ----
    for i, (aid, aname, head, extra, tip) in enumerate(ARROWS):
        pal = {"H": head, "g": extra, "s": (122, 92, 52), "f": (240, 240, 240)}
        save_png(from_template(T_ARROW, pal, 80000 + i, jitter=0), "item", f"{aid}.png")
        write_json(os.path.join(ASSETS, "models", "item", f"{aid}.json"),
                   {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{aid}"}})
        lang[f"item.{MOD}.{aid}"] = aname
        lang[f"tooltip.{MOD}.arrow.{aid.replace('_arrow', '')}"] = tip
    recipe("explosive_arrow", {"type": "minecraft:crafting_shapeless", "category": "combat",
                               "ingredients": [{"item": "minecraft:arrow"}, {"item": "minecraft:arrow"},
                                               {"item": "minecraft:gunpowder"}],
                               "result": {"item": f"{MOD}:explosive_arrow", "count": 2}})
    recipe("fire_arrow", {"type": "minecraft:crafting_shaped", "category": "combat",
                          "pattern": ["AAA", "ABA", "AAA"],
                          "key": {"A": {"item": "minecraft:arrow"}, "B": {"item": "minecraft:blaze_powder"}},
                          "result": {"item": f"{MOD}:fire_arrow", "count": 8}})
    recipe("heavy_arrow", {"type": "minecraft:crafting_shapeless", "category": "combat",
                           "ingredients": [{"item": "minecraft:arrow"}, {"item": "minecraft:arrow"},
                                           {"tag": "forge:ingots/iron"}],
                           "result": {"item": f"{MOD}:heavy_arrow", "count": 2}})
    tag("minecraft", "items", "arrows", [f"{MOD}:{a[0]}" for a in ARROWS])
    lang[f"entity.{MOD}.explosive_arrow"] = "Explosive Arrow"

    # ---- Loot modifiers ----
    entries = ["tieredarsenal:auto_smelt"]
    write_json(os.path.join(DATA, MOD, "loot_modifiers", "auto_smelt.json"),
               {"type": f"{MOD}:auto_smelt", "conditions": []})
    for table, item, lo, hi, chance in LOOT:
        name = f"chests/{table}_{item}"
        write_json(os.path.join(DATA, MOD, "loot_modifiers", f"{name}.json"), {
            "type": f"{MOD}:add_item",
            "conditions": [
                {"condition": "forge:loot_table_id", "loot_table_id": f"minecraft:chests/{table}"},
                {"condition": "minecraft:random_chance", "chance": chance}],
            "item": f"{MOD}:{item}", "min": lo, "max": hi})
        entries.append(f"{MOD}:{name}")
    write_json(os.path.join(DATA, "forge", "loot_modifiers", "global_loot_modifiers.json"),
               {"replace": False, "entries": entries})

    # Lang for this release's tooltips/JEI
    JEI_INFO.update({
        "tools": "Pickaxe, shovel and hoe for every tier. Copper and Emerald are crafted; Cobalt and up are "
                 "upgraded from vanilla iron tools in the smithing table. Tools take the Auto-Smelt and Vein "
                 "Miner modules.",
        "crossbow": "Works like the vanilla crossbow with more durability and extra arrow damage, from +0 (Copper) "
                    "to +3 (Aetherium). Cobalt and up are upgraded from a vanilla crossbow.",
        "explosive_arrow": "Explodes where it lands. Breaks blocks only when the mobGriefing game rule is on. "
                           "Used up on impact. 2 arrows + 1 gunpowder = 2.",
        "fire_arrow": "Flies burning and sets what it hits on fire. 8 arrows around Blaze Powder = 8. Picks up as "
                      "a normal arrow.",
        "heavy_arrow": "+1.5 damage and extra knockback. 2 arrows + 1 iron ingot = 2. Picks up as a normal arrow.",
    })
    write_json(lang_path, lang)



# ===========================================================================
# 1.0.1: Annihilator, a creative-only test weapon (no recipe)
# ===========================================================================
def gen_testing():
    lang_path = os.path.join(ASSETS, "lang", "en_us.json")
    with open(lang_path, encoding="utf-8") as f:
        lang = json.load(f)
    # Black blade with a red glowing edge and a gold hilt, so it can't be mistaken for real gear
    pal = {"o": (8, 2, 4), "d": (18, 6, 10), "m": (34, 12, 18), "l": (58, 20, 28), "h": (255, 64, 48),
           "w": (180, 130, 30), "W": (255, 214, 80)}
    save_png(from_template(T_GREATSWORD, pal, 93000, jitter=0), "item", "annihilator.png")
    write_json(os.path.join(ASSETS, "models", "item", "annihilator.json"),
               {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MOD}:item/annihilator"}})
    lang.update({
        f"item.{MOD}.annihilator": "Annihilator",
        f"tooltip.{MOD}.annihilator.hit": "Hit: deals 1,000,000 damage, no cooldown",
        f"tooltip.{MOD}.annihilator.use": "Right-click: kills every monster within 32 blocks",
        f"tooltip.{MOD}.annihilator.sneak": "Sneak + right-click: kills every mob within 32 blocks (not players or pets)",
        f"tooltip.{MOD}.annihilator.note": "Creative testing tool - no recipe",
        f"message.{MOD}.annihilator_cleared": "Annihilator: killed %s",
    })
    write_json(lang_path, lang)
    JEI_INFO["annihilator"] = (
        "Creative testing tool with no recipe. A hit deals 1,000,000 damage with no attack cooldown, and kills are "
        "credited to you, so drops and XP work as normal. Right-click to kill every monster within 32 blocks; "
        "sneak and right-click to kill every mob within 32 blocks except players and tamed pets. Never breaks.")


if __name__ == "__main__":
    # Start clean so removed recipes/textures don't linger
    import shutil
    shutil.rmtree(ASSETS, ignore_errors=True)
    shutil.rmtree(DATA, ignore_errors=True)
    gen_textures()
    gen_models()
    gen_loot()
    gen_tags()
    gen_recipes()
    gen_worldgen()
    gen_lang()
    gen_weapons()
    gen_tieredpower()
    gen_modules()
    gen_phase6()
    gen_phase8()
    gen_phase9()
    gen_testing()
    gen_jei_lang()
    gen_misc_recipes()
    print("Resources generated in", os.path.normpath(ROOT))
