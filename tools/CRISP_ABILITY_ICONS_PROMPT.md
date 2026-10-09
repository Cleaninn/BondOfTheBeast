# Crisp ability sprites

Mode: built-in image_gen, editing the previous atlas with the vanilla iron chestplate texture as style reference.
Final files: src/main/resources/assets/bondofthebeast/textures/gui/abilities/crisp/*.png.
Import: tools/import_ability_atlas.ps1 trims excess transparent margins, removes low-alpha fringes and tiny disconnected scraps, exports nearest-neighbor 16x16 sprites with binary alpha and a shared 16-color material palette. Final visible bounds match the actual vanilla chestplate: x=1..14 and y=2..14 (14x13 pixels). The renderer explicitly uses nearest-neighbor filtering.
Original generated files and previous imported variants are preserved.

Update beta_12: final sprites are enlarged to 32x32 by exact 2x pixel replication, without smoothing. The importer and renderer use this final size.

## Prompt

Edit target image 1, the 5x2 ability sprite atlas. Image 2 is ONLY the style reference: the actual tiny Minecraft iron chestplate item texture. Rework ALL ten icons to match that plain Minecraft item style: crisp hard edged chunky pixels, muted material colors, very limited flat palette, no smooth gradients, no fuzzy contours, no semitransparent pixels or shadows. Keep ten recognizable ability subjects and original order. Every subject must fill its cell to the same apparent size as a vanilla chestplate in a 16x16 item slot, with only ONE logical pixel of margin at most. No oversized empty padding. Simplify the seated wolf to recognizable short Minecraft wolf ears and blocky snout; shield with paw, teal return paw, green aura paw, blood droplet with fangs, pickaxe/hand/sword with red minus, brown book with teal spiral, leather chestplate with padlock. Exactly five columns, two rows, evenly sized square cells on genuinely transparent background. No stray specks, no labels, no decorative artwork. The icons must survive downsampling into hard 16x16 pixel sprites; silhouettes and marks are simple and large.
