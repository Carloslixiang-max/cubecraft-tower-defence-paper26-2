#!/usr/bin/env python3
"""Render actual generated block geometry for visual QA (not client screenshots)."""
import argparse
import gzip
import json
from pathlib import Path
import runpy
import io
import zipfile
from PIL import Image, ImageDraw, ImageFont, ImageEnhance

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
COLORS.update({"dark_oak_planks":(66,45,28),"stripped_spruce_log":(113,86,54),
    "stripped_dark_oak_log":(68,48,29),"calcite":(224,224,213),"tuff_bricks":(105,113,104),
    "polished_tuff":(112,119,110),"chiseled_tuff_bricks":(100,108,99),
    "polished_basalt":(80,81,80),"deepslate_bricks":(66,66,70),"deepslate_tiles":(52,52,57),
    "polished_deepslate":(74,74,78),"chiseled_deepslate":(63,63,67),
    "polished_blackstone_bricks":(45,39,47),"chiseled_polished_blackstone":(48,41,48),
    "polished_blackstone":(49,43,51),"polished_blackstone_brick":(45,39,47),
    "tuff_brick":(105,113,104),"deepslate_tile":(52,52,57),"iron_bars":(170,174,169),
    "blue_ice":(112,165,232),"amethyst_block":(143,105,191),
    "amethyst_cluster":(175,133,222),"tinted_glass":(63,52,73),"crying_obsidian":(67,35,99),
    "sea_lantern":(183,220,204),"shroomlight":(235,151,72),"verdant_froglight":(192,218,167),
    "pearlescent_froglight":(224,194,221),"lantern":(224,166,71),"soul_lantern":(90,213,221),
    "end_rod":(231,223,191),"lightning_rod":(185,105,78),"mossy_stone_bricks":(106,120,82),
    "red_nether_bricks":(77,24,29),"chiseled_quartz_block":(225,221,208),"soul_soil":(78,59,50)})


def color(state):
    key=state.removeprefix("minecraft:").split("[")[0]
    if key.endswith("_glass_pane"):
        key=key.removesuffix("_pane")
    if key in COLORS:return COLORS[key]
    if "copper" in key:
        return (72,145,121) if "oxidized" in key else (153,116,86) if "exposed" in key else (189,110,78)
    for suffix,base in (("_slab",""),("_stairs",""),("_fence","_planks"),("_wall","")):
        if key.endswith(suffix):
            key=key.removesuffix(suffix)+base
            if key in COLORS:return COLORS[key]
    if "stone" in key or "andesite" in key:return (135,138,135)
    if "quartz" in key:return (231,227,215)
    if "glass" in key:return (148,121,180)
    if "oak" in key:return COLORS["oak_planks"]
    return (141,133,124)


class ClientTextures:
    """Optional local client-JAR textures; no game assets are copied into builds."""
    def __init__(self, path):
        self.jar = zipfile.ZipFile(path)
        self.cache = {}

    def face(self, state, top):
        key = state.removeprefix("minecraft:").split("[")[0]
        cache_key = (key, top)
        if cache_key in self.cache:
            return self.cache[cache_key]
        def read(model):
            model = model.removeprefix("minecraft:")
            data = json.loads(self.jar.read("assets/minecraft/models/" + model + ".json"))
            textures = read(data["parent"]) if "parent" in data else {}
            return textures | data.get("textures", {})
        try:
            block = json.loads(self.jar.read(f"assets/minecraft/blockstates/{key}.json"))
            variant = next(iter(block["variants"].values())) if "variants" in block else block["multipart"][0]["apply"]
            if isinstance(variant, list): variant = variant[0]
            textures = read(variant["model"])
            tex = next(textures[t] for t in (("top", "end", "all", "side", "texture", "particle") if top else
                                           ("side", "all", "texture", "particle", "top", "end")) if t in textures)
            if isinstance(tex, dict): tex = tex.get("sprite", tex.get("texture"))
            for _ in range(12):
                if not isinstance(tex, str): raise ValueError("Unsupported texture reference")
                if not tex.startswith("#"): break
                tex = textures[tex[1:]]
            name = "assets/minecraft/textures/" + tex.removeprefix("minecraft:") + ".png"
            im = Image.open(io.BytesIO(self.jar.read(name))).convert("RGBA")
            im = im.crop((0, 0, im.width, im.width)) # first frame of animated textures
        except (KeyError, StopIteration, ValueError):
            im = None
        self.cache[cache_key] = im
        return im


def render(blocks,width,height,margin=30,textures=None):
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
        if key.endswith("_pane") or key.endswith("iron_bars"):
            parts=[(.44,0,.44,.56,1,.56)]
            for direction,box in {"north":(.44,0,0,.56,1,.5),"south":(.44,0,.5,.56,1,1),
                                  "west":(0,0,.44,.5,1,.56),"east":(.5,0,.44,1,1,.56)}.items():
                if direction+"=true" in state:parts.append(box)
            return parts
        if key.endswith("_fence"):
            parts=[(.375,0,.375,.625,1,.625)]
            for low,high in ((.4,.6),(.75,.95)):
                for direction,box in {"north":(.44,low,0,.56,high,.5),"south":(.44,low,.5,.56,high,1),
                                      "west":(0,low,.44,.5,high,.56),"east":(.5,low,.44,1,high,.56)}.items():
                    if direction+"=true" in state:parts.append(box)
            return parts
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
            if "shape=outer_" in state:
                x0,a,z0,x1,b,z1=step
                right="shape=outer_right" in state
                if "facing=east" in state:z0,z1=(.5,1) if right else (0,.5)
                elif "facing=west" in state:z0,z1=(0,.5) if right else (.5,1)
                elif "facing=south" in state:x0,x1=(0,.5) if right else (.5,1)
                else:x0,x1=(.5,1) if right else (0,.5)
                step=(x0,a,z0,x1,b,z1)
            return [base,step]
        if key.endswith("anvil"):
            return [(0,0,0,1,.25,1),(.3,.25,.25,.7,.7,.75),(0,.7,.15,1,1,.85)]
        if key.endswith("enchanting_table"):
            return [(0,0,0,1,.75,1),(.25,.75,.25,.75,.9,.75)]
        if key.endswith("button"):
            return [(.35,0,.35,.65,.12,.65)]
        if key.endswith("lantern"):
            return [(.3,.1,.3,.7,.65,.7),(.25,.08,.25,.75,.18,.75),
                    (.25,.65,.25,.75,.73,.75),(.43,.73,.43,.57,1,.57)]
        if key.endswith("end_rod") or key.endswith("lightning_rod"):
            return [(.38,0,.38,.62,.12,.62),(.44,.12,.44,.56,1,.56)]
        if key.endswith("amethyst_cluster"):
            return [(.15,0,.2,.38,.5,.45),(.4,0,.35,.65,.9,.65),(.68,0,.5,.88,.65,.78)]
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
                texture = textures.face(state, bool(dy)) if textures else None
                if texture is not None:
                    # Affine project the texture onto this actual prism face.
                    ax,ay=polygon[0]; ux,uy=polygon[1][0]-ax,polygon[1][1]-ay
                    vx,vy=polygon[3][0]-ax,polygon[3][1]-ay
                    det=ux*vy-uy*vx
                    if abs(det) < .001: continue
                    left,top=int(min(p[0] for p in polygon)),int(min(p[1] for p in polygon))
                    right,bottom=int(max(p[0] for p in polygon))+1,int(max(p[1] for p in polygon))+1
                    n=texture.width
                    affine=(n*vy/det,-n*vx/det,n*((left-ax)*vy-(top-ay)*vx)/det,
                            -n*uy/det,n*ux/det,n*(-(left-ax)*uy+(top-ay)*ux)/det)
                    face=ImageEnhance.Brightness(texture).enhance(light).transform(
                        (right-left,bottom-top),Image.Transform.AFFINE,affine,Image.Resampling.NEAREST)
                    mask=Image.new("L",face.size);ImageDraw.Draw(mask).polygon([(x-left,y-top) for x,y in polygon],fill=255)
                    # Glass retains a solid tinted background for this QA view.
                    if face.getextrema()[3][0] < 255:
                        backing=Image.new("RGBA",face.size,fill+(255,));backing.alpha_composite(face);face=backing
                    im.paste(face,(left,top),mask)
    return im


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--assets",type=Path,required=True)
    p.add_argument("--farm",type=Path,required=True)
    p.add_argument("--output",type=Path,required=True)
    p.add_argument("--client-jar",type=Path,help="Optional official local client JAR for textured tower QA renders")
    args=p.parse_args();args.output.mkdir(parents=True,exist_ok=True)
    track=read_nbt(gzip.decompress(args.farm.read_bytes()))
    scene=build["compose_track"](build["build_scene"](),track)
    report=build["write_schematic"](args.output/"Farm-Reconstructed.schem",scene,block_entities=track.get("BlockEntities",[]))
    render(scene.blocks,2400,2100).save(args.output/"Farm-preview.png")
    font=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",16)
    atlas=Image.new("RGB",(1850,2400),(20,29,42));draw=ImageDraw.Draw(atlas)
    textures=ClientTextures(args.client_jar) if args.client_jar else None
    families=["archer","artillery","ice","mage","poison","quake","sorcerer","zeus","turret","necromancer","leach"]
    draw.text((20,12),"CPU voxel QA | TOP I-IV, BOTTOM I-IV | 82 entries" + (" | Minecraft client textures" if textures else ""),font=font,fill="white")
    for row,family in enumerate(families):
        for col,(path,level) in enumerate((p,l) for p in ("top","bottom") for l in range(1,5)):
            file=args.assets/"reconstruction/tower-models"/f"{family}-{path}-{level}.json"
            if not file.exists():continue
            model=json.loads(file.read_text())
            blocks={(b["x"],b["y"],b["z"]):b["block-data"] for b in model["blocks"]}
            x=10+col*230;y=45+row*210
            atlas.paste(render(blocks,220,175,12,textures),(x,y))
            draw.text((x+3,y+175),f"{family} {path[0].upper()}{level}",font=font,fill="white")
    atlas.save(args.output/"Tower-models-preview.png")
    (args.output/"reconstruction-audit.json").write_text(json.dumps(report,indent=2))
    print(json.dumps(report))


if __name__=="__main__":main()
