# Original Flux butterfly finishes

All six finish textures are original procedural artwork created for Flux Tweaks. No Counter-Strike models, textures, sounds or extracted animations are included. The names describe these original finishes rather than copied CS inventory items.

`finishes.json` is the editable art palette and atlas contract. Regenerate with `python tools/butterfly_assets.py` using a Python environment with Pillow. Pillow is an authoring tool only, not a mod dependency. The generator writes six runtime PNGs, verifies size/opacity, and updates the authoring contact sheet and SHA-256 manifest.

Each 256x256 atlas has four 128x128 quadrants: blade at `(0,0)`, handle at `(128,0)`, hardware at `(0,128)`, accent at `(128,128)`. The native model's logical 128-unit UV canvas maps to these normalized quadrant coordinates, so texture resolution can increase without changing the UV layout. Opaque materials preserve hand depth and silhouette passes. The textures use brushed metal grain and restrained highlight bands; bevel geometry, lighting, movement and enchantment glint belong to the native renderer.

Geometry is authored in the shared native articulated-model source; this folder deliberately does not carry a disconnected second model. The image contact sheet is an asset-authoring preview, not proof of the final appearance in Minecraft.

No new audio is included. An original mechanical flip sound can be authored separately if desired; no audio encoder dependency was added for this optional detail.
