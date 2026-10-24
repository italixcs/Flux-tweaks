"""Author original butterfly finishes; requires Pillow only when regenerating art."""

from pathlib import Path
import hashlib
import json
import math
import random

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "assets-src/butterfly"
OUTPUT = ROOT / "src/main/resources/assets/fluxtweaks/textures/item/butterfly"
SPEC = json.loads((SOURCE / "finishes.json").read_text(encoding="utf-8"))
SIZE = SPEC["atlasSize"]
QUAD = SPEC["quadrantSize"]


def rgb(value):
    return tuple(int(value[i:i + 2], 16) for i in (1, 3, 5))


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(a[i] * (1.0 - t) + b[i] * t for i in range(3))


def palette(colors, t):
    return mix(colors[0], colors[1], t * 2) if t < .5 else mix(colors[1], colors[2], (t - .5) * 2)


def turbulence(x, y):
    # Analytic waves keep the finish stable and editable without external images.
    return (math.sin(x * 11 + math.sin(y * 9) * 1.8) * .46
            + math.sin(y * 19 - x * 7 + math.cos(x * 17)) * .24
            + math.sin(x * 31 + y * 27) * .1)


def blade_color(style, colors, x, y):
    flow = turbulence(x, y)
    if style == "brushed":
        return mix(colors[0], colors[1], .55 + .13 * math.sin(x * 6 + y * 2))
    if style == "flow":
        return palette(colors, .5 + .36 * math.sin(y * 7.5 + x * 4 + flow * 1.2))
    if style == "fade":
        return palette(colors, .08 + .84 * ((y * .72 + x * .28) % 1))
    if style == "marble":
        vein = .5 + .5 * math.sin(x * 9 + y * 6 + flow * 3.4)
        color = mix(colors[0], colors[1], .3 + .65 * vein)
        return mix(color, colors[2], max(0.0, (vein - .87) / .13) * .8)
    vein = abs(math.sin(x * 13 - y * 9 + flow * 2.2))
    color = mix(colors[0], colors[1], max(0.0, 1 - vein / .25) * .9)
    return mix(color, colors[2], max(0.0, 1 - vein / .035) * .55)


def material_pixel(finish, region, x, y, noise):
    colors = [rgb(value) for value in finish["blade"]]
    if region == "blade":
        color = blade_color(finish["pattern"], colors, x, y)
        grain = math.sin(y * 850 + math.sin(x * 8)) * 2.4 + noise * 2.3
        # A soft ground-metal sheen; native lighting and enchantment glint remain dynamic.
        light = 1 + .065 * math.cos(x * 9 - .7)
    elif region == "handle":
        color = rgb(finish["handle"])
        # Restrained machined longitudinal grain, without pretending to draw screw geometry.
        grain = math.sin(x * 260) * 1.4 + noise * 1.2
        light = 1 + .08 * math.cos(x * 12)
    elif region == "hardware":
        color = rgb(finish["hardware"])
        grain = math.sin(y * 640) * 2 + noise * 1.8
        light = .93 + .1 * math.cos(x * 7)
    else:
        color = rgb(finish["accent"])
        grain = noise * 1.1
        light = .95 + .06 * math.cos(x * 8 + y * 4)
    return tuple(max(0, min(255, round(c * light + grain))) for c in color) + (255,)


def generate():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    manifest = {}
    sheets = []
    for slug, finish in SPEC["finishes"].items():
        image = Image.new("RGBA", (SIZE, SIZE))
        pixels = image.load()
        rng = random.Random(17041)  # Shared grain between finishes avoids accidental UV mismatch.
        for region, (left, top, width, height) in SPEC["regions"].items():
            assert width == height == QUAD
            for y in range(height):
                for x in range(width):
                    pixels[left + x, top + y] = material_pixel(
                        finish, region, x / (width - 1), y / (height - 1), rng.uniform(-1, 1))
        destination = OUTPUT / (slug + ".png")
        image.save(destination, optimize=True)
        with Image.open(destination) as saved:
            assert saved.mode == "RGBA" and saved.size == (SIZE, SIZE)
            assert saved.getextrema()[3] == (255, 255), "Knife materials must remain opaque"
        manifest[slug] = hashlib.sha256(destination.read_bytes()).hexdigest()
        sheets.append((finish["label"], image))
    (SOURCE / "texture-manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")

    # Authoring contact sheet only; not packaged into the mod.
    sheet = Image.new("RGB", (940, 680), "#15191f")
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 19)
        small = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 12)
    except OSError:
        font = small = ImageFont.load_default()
    for index, (label, image) in enumerate(sheets):
        x = 24 + (index % 3) * 312
        y = 18 + (index // 3) * 330
        draw.text((x, y), label, fill="#e7e9ee", font=font)
        sheet.paste(image.resize((256, 256), Image.Resampling.NEAREST), (x, y + 32))
        draw.text((x, y + 294), "Blade / Handle     Hardware / Accent", fill="#aab2c2", font=small)
    sheet.save(SOURCE / "finish-contact-sheet.png", optimize=True)
    print(f"Generated and checked {len(sheets)} original 128x128 opaque finish atlases.")


if __name__ == "__main__":
    generate()
