#!/usr/bin/env python3
"""Render actual generated block geometry for visual QA (not client screenshots)."""
import argparse
import gzip
import json
from pathlib import Path
import runpy
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parent.parent
build=runpy.run_path(str(ROOT/"scripts/build-reconstruction-assets.py"))
read_nbt=runpy.run_path(str(ROOT/"scripts/package-playtest.py"))["read_nbt"]
COLORS={"air":(20,29,42),"dirt":(117,83,52),"coarse_dirt":(119,86,56),"podzol":(100,79,43),
        "grass_block":(91,125,51),"oak_leaves":(57,101,35),"oak_log":(102,76,44),
        "spruce_log":(74,52,32),"oak_planks":(167,133,76),"spruce_planks":(106,79,47),
        "dark_oak_planks":(67,48,29),"birch_planks":(218,207,147),"jungle_planks":(166,119,76),
        "hay_block":(199,167,32),"yellow_wool":(239,192,36),"yellow_terracotta":(185,135,40),
        "red_wool":(170,39,33),"red_terracotta":(134,62,45),"white_terracotta":(208,192,172),
        "white_wool":(227,229,222),"pink_wool":(222,145,168),"pink_terracotta":(157,83,81),
        "orange_terracotta":(167,86,34),"orange_wool":(230,125,29),"black_wool":(32,31,36),
        "stone_bricks":(126,129,127),"smooth_stone":(154,156,153),"polished_andesite":(147,151,150),
        "quartz_block":(231,227,215),"quartz_pillar":(229,225,213),"glowstone":(216,184,96),
        "nether_bricks":(57,29,36),"nether_brick_fence":(57,29,36),"magma_block":(181,87,30),
        "orange_stained_glass":(240,139,35),"packed_ice":(141,183,227),"snow_block":(232,239,239),
        "green_terracotta":(80,97,45),"lime_stained_glass":(131,184,51),"spruce_stairs":(106,79,47),
        "emerald_block":(51,167,90),"purple_stained_glass":(125,67,159),"bookshelf":(142,108,53),
        "enchanting_table":(40,70,64),"blue_terracotta":(76,77,109),"diamond_block":(91,205,204),
        "obsidian":(39,32,53),"coal_block":(35,35,35),"anvil":(76,77,74),"tnt":(192,62,49),
        "beacon":(127,218,216),"red_stained_glass":(207,76,61),"blue_stained_glass":(70,107,205),
        "red_wool":(175,47,39),"blue_wool":(56,81,162),"light_blue_stained_glass":(115,184,213)}


def color(state):
    key=state.removeprefix("minecraft:").split("[")[0]
    if key in COLORS:return COLORS[key]
    for suffix,base in (("_slab",""),("_stairs",""),("_fence","_planks"),("_wall","")):
        if key.endswith(suffix):
            key=key.removesuffix(suffix)+base
            if key in COLORS:return COLORS[key]
    if "stone" in key or "andesite" in key:return (135,138,135)
    if "quartz" in key:return (231,227,215)
    if "glass" in key:return (148,121,180)
    if "oak" in key:return COLORS["oak_planks"]
    return (141,133,124)


def render(blocks,width,height,margin=30):
    blocks={p:s for p,s in blocks.items() if s!="minecraft:air"}
    def proj(x,y,z):return x-z,(x+z)*.5-y*1.2
    points=[proj(x,y,z) for x,y,z in blocks]
    lo=[min(p[i] for p in points) for i in (0,1)];hi=[max(p[i] for p in points)+2 for i in (0,1)]
    scale=min((width-2*margin)/(hi[0]-lo[0]),(height-2*margin)/(hi[1]-lo[1]))
    def pixel(p):
        q=proj(*p)
        return margin+(q[0]-lo[0])*scale,margin+(q[1]-lo[1])*scale
    im=Image.new("RGB",(width,height),(20,29,42));draw=ImageDraw.Draw(im)
    faces=[((1,0,0),[(1,0,0),(1,0,1),(1,1,1),(1,1,0)],.72),
           ((0,0,1),[(0,0,1),(1,0,1),(1,1,1),(0,1,1)],.87),
           ((0,1,0),[(0,1,0),(1,1,0),(1,1,1),(0,1,1)],1.1)]
    def shapes(state):
        key=state.split("[")[0]
        if key.endswith("_slab"):
            return [(0,.5 if "type=top" in state else 0,0,1,1 if "type=top" in state else .5,1)]
        if key.endswith("_pane"):
            return [(.44,0,0,.56,1,1),(0,0,.44,1,1,.56)]
        if key.endswith("_fence"):
            return [(.375,0,.375,.625,1,.625),(0,.4,.44,1,.6,.56),(0,.75,.44,1,.95,.56),(.44,.4,0,.56,.6,1),(.44,.75,0,.56,.95,1)]
        if key.endswith("_wall"):
            return [(.25,0,.25,.75,1,.75)]
        if key.endswith("_stairs"):
            half="half=top" in state
            base=(0,.5 if half else 0,0,1,1 if half else .5,1)
            y0,y1=(0,.5) if half else (.5,1)
            if "facing=east" in state:step=(.5,y0,0,1,y1,1)
            elif "facing=west" in state:step=(0,y0,0,.5,y1,1)
            elif "facing=south" in state:step=(0,y0,.5,1,y1,1)
            else:step=(0,y0,0,1,y1,.5)
            return [base,step]
        if key.endswith("anvil"):
            return [(0,0,0,1,.25,1),(.3,.25,.25,.7,.7,.75),(0,.7,.15,1,1,.85)]
        if key.endswith("enchanting_table"):
            return [(0,0,0,1,.75,1),(.25,.75,.25,.75,.9,.75)]
        if key.endswith("lever") or key.endswith("pressure_plate"):
            return [(.2,0,.2,.8,.15,.8)]
        return [(0,0,0,1,1,1)]
    for (x,y,z),state in sorted(blocks.items(),key=lambda item:6*item[0][0]+5*item[0][1]+6*item[0][2]):
        rgb=color(state)
        prisms=shapes(state)
        for x0,y0,z0,x1,y1,z1 in prisms:
            for (dx,dy,dz),corners,light in faces:
                neighbor=blocks.get((x+dx,y+dy,z+dz))
                if len(prisms)==1 and prisms[0]==(0,0,0,1,1,1) and neighbor and shapes(neighbor)==[(0,0,0,1,1,1)]:continue
                polygon=[pixel((x+(x1 if a else x0),y+(y1 if b else y0),z+(z1 if c else z0))) for a,b,c in corners]
                fill=tuple(min(255,int(c*light)) for c in rgb)
                draw.polygon(polygon,fill=fill)
    return im


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--assets",type=Path,required=True)
    p.add_argument("--farm",type=Path,required=True)
    p.add_argument("--output",type=Path,required=True)
    args=p.parse_args();args.output.mkdir(parents=True,exist_ok=True)
    track=read_nbt(gzip.decompress(args.farm.read_bytes()))
    scene=build["compose_track"](build["build_scene"](),track)
    report=build["write_schematic"](args.output/"Farm-Reconstructed.schem",scene,block_entities=track.get("BlockEntities",[]))
    render(scene.blocks,2400,2100).save(args.output/"Farm-preview.png")
    font=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",16)
    atlas=Image.new("RGB",(1850,2400),(20,29,42));draw=ImageDraw.Draw(atlas)
    families=["archer","artillery","ice","mage","poison","quake","sorcerer","zeus","turret","necromancer","leach"]
    draw.text((20,12),"Actual voxel reconstruction | TOP I-IV, BOTTOM I-IV | 82 model entries",font=font,fill="white")
    for row,family in enumerate(families):
        for col,(path,level) in enumerate((p,l) for p in ("top","bottom") for l in range(1,5)):
            file=args.assets/"reconstruction/tower-models"/f"{family}-{path}-{level}.json"
            if not file.exists():continue
            model=json.loads(file.read_text())
            blocks={(b["x"],b["y"],b["z"]):b["block-data"] for b in model["blocks"]}
            x=10+col*230;y=45+row*210
            atlas.paste(render(blocks,220,175,12),(x,y))
            draw.text((x+3,y+175),f"{family} {path[0].upper()}{level}",font=font,fill="white")
    atlas.save(args.output/"Tower-models-preview.png")
    (args.output/"reconstruction-audit.json").write_text(json.dumps(report,indent=2))
    print(json.dumps(report))


if __name__=="__main__":main()
