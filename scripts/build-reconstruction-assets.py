#!/usr/bin/env python3
"""Build original, reproducible voxel reconstructions; no downloaded world bytes.

Visual approximation authorized 2026-10-17. Hidden faces, dimensions and stage
details are inferred. Gameplay numbers and the separately supplied track stay
authoritative. Python standard library only; generated assets are bundled in JAR.
"""
import argparse
from collections import Counter
import gzip
import hashlib
import json
import math
from pathlib import Path
import random
import re
import struct

FARM_REF = "https://www.cubecraft.net/threads/274681/"
TOWER_REF = "https://www.cubecraft.net/threads/96401/"
MATURE_REF = "https://www.cubecraft.net/threads/how-to-defend-in-tower-defense.283335/"
QUAKE_REF = "https://www.cubecraft.net/threads/113397/"
OFFSET = (-60, -3, -45)
SIZE = (245, 82, 285)


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    data = (json.dumps(value, separators=(",", ":"), ensure_ascii=False) + "\n").encode()
    path.write_bytes(data)
    return hashlib.sha256(data).hexdigest()


def nbt(tag, value):
    if tag == 2:
        return struct.pack(">h", value)
    if tag == 3:
        return struct.pack(">i", value)
    if tag == 7:
        return struct.pack(">i", len(value)) + value
    if tag == 8:
        data = value.encode()
        return struct.pack(">H", len(data)) + data
    if tag == 9:
        kind, items = value
        return bytes([kind]) + struct.pack(">i", len(items)) + b"".join(nbt(kind, v) for v in items)
    if tag == 10:
        return b"".join(bytes([kind]) + nbt(8, key) + nbt(kind, val)
                        for key, (kind, val) in value.items()) + b"\0"
    if tag == 11:
        return struct.pack(">i", len(value)) + b"".join(struct.pack(">i", v) for v in value)
    raise ValueError(tag)


class Voxels:
    def __init__(self):
        self.blocks = {}

    def put(self, x, y, z, state):
        self.blocks[x, y, z] = "minecraft:" + state if ":" not in state else state

    def box(self, x1, y1, z1, x2, y2, z2, state):
        for x in range(x1, x2 + 1):
            for y in range(y1, y2 + 1):
                for z in range(z1, z2 + 1):
                    self.put(x, y, z, state)

    def ellipsoid(self, cx, cy, cz, rx, ry, rz, state, rough=False):
        for x in range(math.floor(cx-rx), math.ceil(cx+rx)+1):
            for y in range(math.floor(cy-ry), math.ceil(cy+ry)+1):
                for z in range(math.floor(cz-rz), math.ceil(cz+rz)+1):
                    limit = 1.0 + (0.075 * math.sin(x*3.1+y*5.3+z*2.2) if rough else 0)
                    if ((x-cx)/rx)**2 + ((y-cy)/ry)**2 + ((z-cz)/rz)**2 <= limit:
                        self.put(x, y, z, state)

    def line(self, a, b, radius, state):
        steps = max(abs(b[i]-a[i]) for i in range(3)) * 2 + 1
        for t in range(steps+1):
            p = [round(a[i] + (b[i]-a[i])*t/steps) for i in range(3)]
            self.ellipsoid(*p, radius, radius, radius, state)


def tower(tower_id, level, path):
    v = Voxels()
    b, put = v.box, v.put
    if tower_id == "archer":
        h = 2 + (level >= 3)
        for x in (-1, 1):
            for z in (-1, 1):
                b(x, 0, z, x, h-1, z, "oak_log[axis=y]")
        for y in (1, h):
            for x, z in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                put(x, y, z, "oak_fence[north=true,south=true,east=true,west=true]")
        b(-1, h, -1, 1, h, 1, "oak_planks")
        b(-1, h+1, -1, 1, h+1, 1, "oak_slab[type=bottom]")
        if level >= 2:
            for x in (-1, 1):
                for z in (-1, 1):
                    put(x, h+1, z, "spruce_slab[type=bottom]")
        if level >= 3:
            put(0, h+1, 0, "light_gray_terracotta" if path == "TOP" else "red_terracotta")
        if level == 4:
            for x in (-1, 1):
                put(x, h+1, 0, "oak_stairs[facing=" + ("west" if x == -1 else "east") + "]")
            if path == "BOTTOM":
                put(0, h, 0, "glowstone")
    elif tower_id == "artillery":
        stone = "stone_bricks"
        b(-1, 0, -1, 1, 0, 1, stone)
        for x in (-1, 1):
            for z in (-1, 1):
                b(x, 1, z, x, 2, z, stone)
                put(x, 3, z, "chiseled_stone_bricks")
        b(0, 1, 0, 0, 2, 0, "gray_terracotta")
        for x, z in ((0, -1), (0, 1), (-1, 0), (1, 0)):
            put(x, 3, z, "stone_brick_slab[type=bottom]")
        put(0, 3, 0, "tnt" if level >= 3 and path == "TOP" else "polished_andesite")
        if level >= 2:
            for x in (-1, 1):
                put(x, 1, 0, "cobblestone_wall")
        if level >= 3 and path == "BOTTOM":
            put(0, 3, 0, "dark_oak_pressure_plate")
        if level == 4:
            put(0, 4, 0, "tnt" if path == "TOP" else "coal_block")
    elif tower_id == "ice":
        b(-1, 0, -1, 1, 0, 1, "stone_bricks")
        for x in (-1, 1):
            for z in (-1, 1):
                put(x, 1, z, "cobblestone_wall")
                put(x, 2, z, "chiseled_stone_bricks")
        b(-1, 2, -1, 1, 2, 1, "stone_brick_slab[type=bottom]")
        b(0, 1, 0, 0, 2+level, 0, "packed_ice")
        if level >= 2:
            for x, z in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                put(x, 3, z, "packed_ice")
        if level >= 3:
            b(-1, 0, -1, 1, 0, 1, "snow_block")
        if level == 4:
            put(0, 6, 0, "light_blue_stained_glass")
    elif tower_id == "mage":
        b(-1, 0, -1, 1, 0, 1, "nether_bricks")
        b(-1, 3, -1, 1, 3, 1, "nether_bricks")
        for x in (-1, 1):
            for z in (-1, 1):
                b(x, 1, z, x, 2, z, "nether_brick_fence")
                put(x, 4, z, "nether_brick_slab[type=bottom]")
        b(0, 1, 0, 0, 2, 0, "orange_stained_glass")
        put(0, 1, 0, "glowstone")
        put(0, 4, 0, "magma_block" if level >= 3 else "orange_stained_glass")
        if level >= 2:
            put(0, 0, 0, "obsidian")
        if level == 4:
            put(0, 5, 0, "orange_stained_glass")
    elif tower_id == "poison":
        b(-1, 0, -1, 1, 0, 1, "spruce_planks")
        for x in (-1, 1):
            for z in (-1, 1):
                b(x, 0, z, x, 3, z, "green_terracotta")
        b(0, 0, 0, 0, 2, 0, "lime_stained_glass")
        put(0, 1, 0, "glowstone")
        for x, z in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            put(x, 3, z, "spruce_stairs[facing=" + ({(-1,0):"east",(1,0):"west",(0,-1):"south",(0,1):"north"}[x,z]) + "]")
        if level >= 2:
            put(0, 3, 0, "lime_stained_glass")
        if level >= 3:
            put(0, 3, 0, "emerald_block" if path == "TOP" else "purple_stained_glass")
        if level == 4:
            for x in (-1, 1):
                put(x, 4, 0, "lime_stained_glass_pane" if path == "TOP" else "purple_stained_glass_pane")
    elif tower_id == "quake":
        b(0, 0, 0, 0, 2, 0, "cobblestone")
        for y in (1, 3):
            b(-1, y, -1, 1, y, 1, "stone_brick_slab[type=bottom]")
        for x, z in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            put(x, 2, z, "stone_brick_stairs[facing=" + ({(-1,0):"east",(1,0):"west",(0,-1):"south",(0,1):"north"}[x,z]) + ",half=top]")
        put(0, 4, 0, "anvil[facing=north]")
        if level >= 2:
            put(0, 0, 0, "stone_bricks")
        if level >= 3:
            for x in (-1, 1):
                put(x, 0, 0, "stone_brick_slab[type=bottom]")
        if level == 4:
            b(-1, 3, -1, 1, 3, 1, "stone_bricks")
    elif tower_id == "sorcerer":
        b(-1, 0, -1, 1, 0, 1, "oak_planks")
        for z in (-1, 0, 1):
            put(-1, 1, z, "bookshelf")
        put(0, 1, 1, "bookshelf")
        b(0, 1, 0, 0, 2, 0, "oak_log[axis=y]")
        put(0, 3, 0, "enchanting_table")
        put(1, 1, 0, "anvil[facing=north]")
        if level >= 2:
            put(-1, 2, 0, "bookshelf")
        if level >= 3:
            put(1, 2, 1, "bookshelf")
            for x in (-1, 1):
                b(x, 3, -1, x, 4, -1, "purple_stained_glass_pane[north=true,south=true]")
        if level == 4:
            put(0, 4, -1, "magenta_stained_glass" if path == "TOP" else "purple_stained_glass")
    elif tower_id == "zeus":
        for x in (-1, 1):
            for z in (-1, 1):
                b(x, 0, z, x, 1, z, "blue_terracotta")
                put(x, 2, z, "quartz_slab[type=bottom]")
        b(0, 0, 0, 0, 0, 0, "quartz_block")
        b(0, 2, 0, 0, 4, 0, "quartz_pillar[axis=y]")
        for x, z in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            put(x, 3, z, "quartz_slab[type=bottom]")
        put(0, 5, 0, "light_blue_stained_glass_pane")
        if level >= 2:
            put(0, 1, 0, "glowstone")
        if level >= 3:
            put(0, 4, 0, "quartz_block" if path == "BOTTOM" else "diamond_block")
        if level == 4:
            put(0, 5, 0, "quartz_slab[type=bottom]" if path == "BOTTOM" else "light_blue_stained_glass")
    elif tower_id == "necromancer":
        b(-2, 0, -2, 2, 0, 2, "nether_bricks")
        b(-1, 0, -1, 1, 0, 1, "obsidian")
        for x in (-2, 2):
            for z in (-2, 2):
                b(x, 1, z, x, 3, z, "nether_bricks")
        for y in (1, 4):
            for x in range(-2, 3):
                for z in range(-2, 3):
                    if max(abs(x), abs(z)) == 2:
                        put(x, y, z, "nether_brick_stairs[facing=north]" if y == 1 else "nether_bricks")
        for x in (-2, 0, 2):
            for z in (-2, 2):
                put(x, 5, z, "nether_brick_slab[type=bottom]")
        if level >= 2:
            for x in (-1, 1):
                put(x, 0, 0, "bone_block[axis=y]")
        if level == 3:
            put(0, 1, 0, "purpur_block" if path == "TOP" else "snow_block")
    elif tower_id == "turret":
        b(-2, 0, -2, 2, 0, 2, "stone_bricks")
        for x in (-2, 2):
            for z in (-2, 2):
                b(x, 1, z, x, 3, z, "stone_bricks")
                put(x, 0, z, "chiseled_stone_bricks")
        for x in range(-2, 3):
            for z in range(-2, 3):
                if max(abs(x), abs(z)) == 2:
                    put(x, 3, z, "chiseled_stone_bricks" if (x+z)%2 == 0 else "stone_brick_slab[type=bottom]")
        b(-1, 4, -1, 1, 4, 1, "oak_planks")
        b(0, 4, -2, 0, 4, 0, "coal_block")
        put(0, 5, 0, "lever[face=floor,facing=north]")
        if level >= 2:
            put(0, 1, 0, "oak_log[axis=y]")
            put(1, 4, -1, "coal_block" if path == "TOP" else "lapis_block")
        if level == 3:
            put(-1, 4, -1, "coal_block" if path == "TOP" else "lapis_block")
    elif tower_id == "leach":
        b(-2, 0, -2, 2, 0, 2, "quartz_block")
        b(-1, 0, -1, 1, 0, 1, "obsidian")
        for x in (-2, 2):
            for z in (-2, 2):
                b(x, 1, z, x, 3, z, "quartz_pillar[axis=y]")
                put(x, 4, z, "quartz_slab[type=bottom]")
        b(0, 1, 0, 0, 3+level, 0, "purple_stained_glass")
        put(0, 2, 0, "glowstone")
        put(0, 4+level, 0, "beacon")
        for x, z in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            put(x, 3+level, z, "quartz_stairs[facing=north,half=top]")
    else:
        raise ValueError(tower_id)
    return v


def build_towers(root, source):
    # Read actual stage declarations instead of maintaining another gameplay table.
    sections = re.split(r'        TowerDefinition\(\s*\n', source)[1:]
    entries, summaries = [], []
    for section in sections:
        tower_id = re.match(r'\s*"([a-z]+)"', section)[1]
        stages = {(p, int(l)) for p, l in re.findall(r'd\(TowerPathOption\.(NONE|TOP|BOTTOM),(\d+),', section)}
        for path in ("TOP", "BOTTOM"):
            for level in range(1, 5):
                if ("NONE", level) not in stages and (path, level) not in stages:
                    continue
                effective_path = "SHARED" if ("NONE", level) in stages else path
                voxels = tower(tower_id, level, effective_path)
                radius = 2 if tower_id in {"leach", "necromancer", "turret"} else 1
                assert 16 <= len(voxels.blocks) <= 1600
                assert all(-radius <= x <= radius and 0 <= y <= 63 and -radius <= z <= radius for x,y,z in voxels.blocks)
                name = f"{tower_id}-{path.lower()}-{level}.json"
                model = {"format-version": 1, "tower-id": tower_id, "level": level, "path": path,
                         "evidence-status": "COMMUNITY_RECONSTRUCTION",
                         "evidence-refs": [TOWER_REF, MATURE_REF] + ([QUAKE_REF] if tower_id == "quake" else []),
                         "reconstruction": "Historical silhouette; unseen surfaces, materials and tier details inferred. Not original extraction.",
                         "original-client-fidelity-certified": False,
                         "blocks": [{"x": x, "y": y, "z": z, "block-data": state}
                                    for (x,y,z), state in sorted(voxels.blocks.items())]}
                sha = write_json(root / "tower-models" / name, model)
                entries.append({"file": name, "sha256": sha})
                summaries.append({"tower": tower_id, "path": path, "level": level, "voxels": len(voxels.blocks)})
    assert len(entries) == 82 and len({v["tower"] for v in summaries}) == 11
    write_json(root / "tower-models/catalog.yml", {"format-version": 1, "models": entries})
    return summaries


def build_scene():
    v = Voxels()
    b, p, e = v.box, v.put, v.ellipsoid
    rng = random.Random(274681)
    # Complete raised farm floor; oversized props around the retained two tracks.
    b(-44, -2, -34, 168, 0, 228, "dirt")
    b(-44, 1, -34, 168, 1, 228, "coarse_dirt")
    for x in range(-44,169):
        for z in range(-34,229):
            val = math.sin(x*.13)*math.cos(z*.11) + math.sin((x+z)*.08)
            p(x, 2, z, "grass_block[snowy=false]" if val > .2 else "podzol[snowy=false]" if val < -.6 else "coarse_dirt")
    # Both end faces of the giant barn; all four walls, hollow interior and roof.
    for x in range(43, 82):
        wall = "red_terracotta" if x%4 == 0 else "red_wool"
        b(x, 3, 72, x, 29, 72, wall)
        b(x, 3, 118, x, 29, 118, wall)
    for z in range(72, 119):
        b(43, 3, z, 43, 29, z, "red_terracotta" if z%4 == 0 else "red_wool")
        b(81, 3, z, 81, 29, z, "red_terracotta" if z%4 == 0 else "red_wool")
    b(44, 2, 73, 80, 2, 117, "spruce_planks")
    for z in (71,119):
        for x in (43,81): b(x, 3, z, x, 29, z, "white_terracotta")
        for y in (3,19,29): b(43,y,z,81,y,z,"smooth_stone")
        b(52,3,z,72,17,z,"red_wool")
        for x in (52,62,72): b(x,3,z,x,18,z,"quartz_block")
        b(52,18,z,72,18,z,"quartz_block")
        for t in range(14):
            for dx in (0,1):
                p(53+round(t*8/13)+dx,4+t,z,"quartz_block")
                p(71-round(t*8/13)-dx,4+t,z,"quartz_block")
        for cx in (51,73):
            b(cx-3,22,z,cx+3,27,z,"quartz_block")
            b(cx-2,23,z,cx+2,26,z,"red_wool")
            b(cx,23,z,cx,26,z,"white_terracotta")
            b(cx-2,24,z,cx+2,24,z,"white_terracotta")
    # Rounded gambrel-like silhouette, grey ribs and closed gables.
    for x in range(41,84):
        dx=abs(x-62)
        roof_y = 46 - round(17*(dx/21)**1.65)
        b(x,roof_y,70,x,roof_y,120,"stone_bricks")
        for z in (70,120):
            b(x,30,z,x,roof_y-1,z,"red_terracotta" if x%4 == 0 else "red_wool")
        for z in range(70,121,5): p(x,roof_y+1,z,"stone_brick_slab[type=bottom]")
    for z in (69,121):
        b(54,33,z,70,39,z,"quartz_block")
        b(55,34,z,69,38,z,"red_wool")
        for t in range(15):
            p(55+t,34+round(4*t/14),z,"quartz_block")
            p(55+t,38-round(4*t/14),z,"quartz_block")

    # Fit the barn between both playable tracks, rather than cutting its side
    # walls away with the route-clearance mask. Only this component exists above
    # ground at this point; compress its X span from 39 to 31 blocks.
    barn={(x,y,z):state for (x,y,z),state in v.blocks.items() if y>=3}
    for key in barn:del v.blocks[key]
    for (x,y,z),state in barn.items():p(62+round((x-62)*.8),y,z,state)

    def silo(cx,cz,height):
        for y in range(3, height+3):
            for x in range(cx-6,cx+7):
                for z in range(cz-6,cz+7):
                    r=math.hypot(x-cx,z-cz)
                    if 4.9 <= r <= 6.5:
                        p(x,y,z,"stone_bricks" if (y-3)%11 == 0 else "white_wool" if (x+z)%3 else "white_terracotta")
        for y in range(height+3,height+10):
            radius=max(1,6-(y-height-3)*.8)
            e(cx,y,cz,radius,.55,radius,"stone_bricks")
        for y in (12,29,45):
            if y < height:
                b(cx-1,y,cz-6,cx+1,y+5,cz-6,"black_terracotta")
    silo(145,100,56)
    silo(-23,180,56)

    def hay(cx,y,cz,w,h,d,axis="y"):
        b(cx-w//2,y,cz-d//2,cx+w//2,y+h,cz+d//2,"hay_block[axis="+axis+"]")
        for yy in (y+1,y+h-1):
            for x in range(cx-w//2,cx+w//2+1):
                for z in range(cz-d//2,cz+d//2+1):
                    if x in (cx-w//2,cx+w//2) or z in (cz-d//2,cz+d//2):p(x,yy,z,"red_wool")
        for _ in range(w*d):
            x=cx+rng.randint(-w//2,w//2);z=cz+rng.randint(-d//2,d//2)
            p(x,y+h+1,z,"yellow_wool")
    for x,z in ((139,72),(-14,98),(145,24),(-19,167),(80,211),(12,-17)):
        hay(x,3,z,15,13,15)
        hay(x+4,17,z+2,12,12,12)
    for x,z in ((48,66),(76,66),(48,124),(76,124),(136,153),(-14,37)):
        hay(x,3,z,5,7,5)

    def apple_tree(cx,cz,scale):
        h=round(35*scale)
        v.line((cx,3,cz),(cx+round(6*scale),h,cz),4*scale,"oak_log[axis=y]")
        for dx,dz in ((-17,-9),(17,-11),(-14,13),(15,12)):
            bx=cx+round(dx*scale);bz=cz+round(dz*scale)
            v.line((cx+4,h-13,cz),(bx,h+2,bz),2.6*scale,"oak_log[axis=y]")
            e(bx,h+5,bz,14*scale,6*scale,12*scale,"oak_leaves[persistent=true]",True)
            for ax,az in ((bx-5,bz),(bx+5,bz+3)):
                e(ax,h-1,az,2.5*scale,3.5*scale,2.5*scale,"red_wool")
                p(ax,h+3,az,"oak_log[axis=y]")
        e(cx,h+6,cz,16*scale,7*scale,15*scale,"oak_leaves[persistent=true]",True)
    for x,z,s in ((-19,18,1.05),(143,184,1.05),(-20,192,.9),(143,4,.9),(64,146,.75),(63,45,.75)):
        apple_tree(x,z,s)

    def chicken(cx,cz,s):
        e(cx,round(10*s)+3,cz,6*s,7*s,8*s,"white_wool")
        e(cx,round(17*s)+3,cz-round(5*s),4*s,5*s,4*s,"white_wool")
        for dx in (-4,4):e(cx+round(dx*s),round(10*s)+3,cz,2*s,4*s,5*s,"white_terracotta")
        for dx in (-3,3):
            b(cx+round(dx*s),3,cz,cx+round(dx*s),round(5*s)+3,cz,"orange_terracotta")
            b(cx+round(dx*s)-1,3,cz-3,cx+round(dx*s)+1,3,cz+1,"orange_wool")
        b(cx-round(2*s),round(16*s)+3,cz-round(10*s),cx+round(2*s),round(18*s)+3,cz-round(8*s),"yellow_terracotta")
        b(cx,round(21*s)+3,cz-round(7*s),cx,round(24*s)+3,cz-round(3*s),"red_wool")
        b(cx-1,round(13*s)+3,cz-round(8*s),cx+1,round(15*s)+3,cz-round(7*s),"red_wool")
        for dx in (-3,3):p(cx+round(dx*s),round(19*s)+3,cz-round(8*s),"black_wool")
        v.line((cx,round(11*s)+3,cz+round(5*s)),(cx,round(17*s)+3,cz+round(11*s)),2*s,"white_wool")
    for x,z,s in ((-25,58,1),(-23,90,.65),(-24,123,.8),(147,49,.85)):
        chicken(x,z,s)

    def pig(cx,cz,s):
        e(cx,round(9*s)+3,cz,7*s,6*s,10*s,"pink_wool")
        b(cx-round(5*s),round(5*s)+3,cz-round(13*s),cx+round(5*s),round(14*s)+3,cz-round(7*s),"pink_wool")
        b(cx-round(3*s),round(7*s)+3,cz-round(15*s),cx+round(3*s),round(10*s)+3,cz-round(14*s),"pink_terracotta")
        for dx in (-4,4):
            for dz in (-6,6):b(cx+round(dx*s)-1,3,cz+round(dz*s)-1,cx+round(dx*s)+1,round(5*s)+3,cz+round(dz*s)+1,"pink_terracotta")
            p(cx+round(dx*s),round(12*s)+3,cz-round(13*s),"black_wool")
            b(cx+round(dx*s),round(15*s)+3,cz-round(10*s),cx+round(dx*s),round(18*s)+3,cz-round(9*s),"pink_wool")
    for x,z,s in ((145,137,1),(145,171,.7),(-22,152,.7)):pig(x,z,s)

    def sheep(cx,cz):
        e(cx,12,cz,7,7,10,"white_wool",True)
        for dx in (-4,4):
            for dz in (-6,6):
                b(cx+dx,3,cz+dz,cx+dx+1,7,cz+dz+1,"gray_terracotta")
        b(cx-3,12,cz-13,cx+3,21,cz-8,"white_wool")
        b(cx-2,15,cz-15,cx+2,19,cz-13,"brown_terracotta")
        for dx in (-3,3):
            p(cx+dx,20,cz-13,"black_wool")
            b(cx+dx,21,cz-10,cx+dx+1,24,cz-9,"white_terracotta")
    sheep(146,206)
    sheep(-23,-10)
    hay(139,31,72,12,13,12)

    # Monumental perimeter fence and smaller pen dividers.
    for x in range(-40,166,10):
        for z in (-30,224):b(x,3,z,x+2,21,z+2,"spruce_log[axis=y]")
    for z in range(-30,225,10):
        for x in (-40,164):b(x,3,z,x+2,21,z+2,"spruce_log[axis=y]")
    for y in (7,13,19):
        for z in (-30,224):b(-40,y,z,166,y+1,z,"stone_bricks")
        for x in (-40,164):b(x,y,-30,x,y+1,224,"stone_bricks")
    for z in range(20,196,8):
        for x in (-10,132):b(x,3,z,x+1,13,z+1,"spruce_log[axis=y]")
    for y in (6,10):
        for x in (-10,132):b(x,y,20,x,y,196,"stone_bricks")
    # Giant wheat, leafy crops and fallen apples, kept outside the track width.
    for _ in range(150):
        x=rng.choice((rng.randint(-8,3),rng.randint(121,130)))
        z=rng.randint(-15,210);h=rng.randint(8,15)
        b(x,3,z,x,3+h,z,"green_terracotta")
        for y in range(h-3,h+3,2):
            for dx,dz in ((-1,0),(1,0),(0,-1),(0,1)):
                p(x+dx,3+y,z+dz,"yellow_wool")
        p(x,4+h,z,"yellow_terracotta")
    for _ in range(160):
        x=rng.randint(-35,160);z=rng.randint(-23,217)
        if 4 <= x <= 120 and 0 <= z <= 190:continue
        e(x,3,z,2,1,2,"oak_leaves[persistent=true]")
    # White/tan ladder from the primary release views; central viewing platform.
    for x in (56,68):b(x,3,60,x+1,28,61,"white_terracotta")
    for y in range(5,28,4):b(56,y,60,69,y,61,"birch_planks")
    b(43,23,-24,81,23,-8,"spruce_planks")
    b(42,22,-25,82,22,-7,"stone_bricks")
    for x in range(42,83):
        for z in (-25,-7):p(x,24,z,"oak_fence")
    for z in range(-25,-6):
        for x in (42,82):p(x,24,z,"oak_fence")
    for x in range(57,68):p(x,24,-7,"air")
    for x in (47,77):
        b(x,24,-20,x,28,-20,"oak_log[axis=y]")
        p(x,29,-20,"glowstone")
    return v


def compose_track(voxels, track):
    """Same track-column clearance as ReconstructedFarmAssets.compose."""
    w,h,l=(track[k] for k in ("Width","Height","Length"))
    inverse={i:s for s,i in track["Palette"].items()}
    assert all(i<128 for i in track["BlockData"])
    top={}
    source={}
    for i,block_id in enumerate(track["BlockData"]):
        state=inverse[block_id]
        if state in {"minecraft:air","minecraft:cave_air","minecraft:void_air"}:continue
        x=i%w;z=(i//w)%l;y=i//(w*l)
        top[x,z]=y
        source[x,y,z]=state
    for z in range(l):
        for x in range(w):
            ceiling=max(top.get((x+dx,z+dz),-1) for dx in (-1,0,1) for dz in (-1,0,1))
            if ceiling>=0:
                for y in range(ceiling+19):voxels.blocks.pop((x,y,z),None)
    voxels.blocks.update(source)
    assert all(voxels.blocks[pos]==state for pos,state in source.items())
    return voxels


def write_schematic(path, voxels, offset=OFFSET, size=SIZE, block_entities=()):
    w,h,l=size
    states=sorted(set(voxels.blocks.values())-{"minecraft:air"})
    palette={"minecraft:air":0,**{s:i+1 for i,s in enumerate(states)}}
    assert len(palette)<128
    data=bytearray(w*h*l)
    for (x,y,z),state in voxels.blocks.items():
        x-=offset[0];y-=offset[1];z-=offset[2]
        if 0<=x<w and 0<=y<h and 0<=z<l:data[x+z*w+y*w*l]=palette[state]
        else:raise ValueError(f"Scene voxel out of bounds: {x},{y},{z}")
    root={"Version":(3,2),"DataVersion":(3,2580),"Width":(2,w),"Height":(2,h),"Length":(2,l),
          "Offset":(11,list(offset)),"PaletteMax":(3,len(palette)),
          "Palette":(10,{s:(3,i) for s,i in palette.items()}),"BlockData":(7,bytes(data)),
          "BlockEntities":(9,(10,[{"Id":(8,be["Id"]),
               "Pos":(11,[be["Pos"][i]-offset[i] for i in range(3)]),
               "Primary":(3,0),"Secondary":(3,0),"Levels":(3,1)} for be in block_entities])),"Entities":(9,(10,[])),
          "Metadata":(10,{"Name":(8,"Farm visual reconstruction"),"Author":(8,"CubeCraftTD community reconstruction"),
                            "EvidenceStatus":(8,"COMMUNITY_RECONSTRUCTION"),"Source":(8,FARM_REF)})}
    raw=b"\x0a"+nbt(8,"Schematic")+nbt(10,root)
    path.parent.mkdir(parents=True,exist_ok=True)
    encoded=gzip.compress(raw,compresslevel=9,mtime=0)
    path.write_bytes(encoded)
    return {"sha256":hashlib.sha256(encoded).hexdigest(),"dimensions":list(size),"offset":list(offset),
            "non-air":sum(v!=0 for v in data),"palette":len(palette)}


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output",type=Path,required=True)
    args=parser.parse_args()
    repo=Path(__file__).resolve().parent.parent
    root=args.output/"reconstruction"
    models=build_towers(root,(repo/"src/main/kotlin/dev/cubecrafttd/tower/TowerDefinitionRepository.kt").read_text())
    scene=build_scene()
    scene_report=write_schematic(root/"FarmScenery.schem",scene)
    write_json(root/"manifest.json",{"format-version":1,"classification":"COMMUNITY_RECONSTRUCTION",
              "original-extraction":False,"scene":scene_report,"models":models,
              "families":11,"catalog-stage-keys":len(models),"sources":[FARM_REF,TOWER_REF,MATURE_REF,QUAKE_REF],
              "limits":["Hidden surfaces and dimensions inferred","2022 adjusted track supplied separately",
                        "Stage and branch details reconstructed; not original block-for-block certification"]})
    print(json.dumps({"scene":scene_report,"tower-families":11,"catalog-stage-keys":len(models)}))


if __name__=="__main__":main()
