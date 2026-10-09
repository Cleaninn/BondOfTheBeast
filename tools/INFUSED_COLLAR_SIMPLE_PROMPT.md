# Infused collar texture — 1.0.3-beta_1

Tool: built-in image_gen (edit, transparent background). Target: the previous infused collar sprite; style reference: the repository's ordinary collar sprite.

## Final prompt

Use case: style-transfer. Edit target: image 1 is the existing infused collar item sprite. Style reference: image 2 is this mod's ordinary collar item sprite. Simplify image 1 to match the plain vanilla Minecraft inventory item style and the ordinary collar reference. Deliver one centered transparent pixel-art sprite, conceptually EXACTLY 16 by 16 pixel cells, rendered at larger resolution with every logical pixel a uniform solid-color square and edges aligned to that 16x16 grid. A small flat oval purple leather collar, a simple rectangular gray iron buckle at the front, only 3 purple shades and 2 gray shades plus dark outline. Simple chunky silhouette like Minecraft leather/lead items. Leave a 2-cell transparent margin. Remove gemstone, runes, stitches, glitter, glow, fine details and gradients. No shadows outside the sprite, no antialiasing, no text, no background. Keep it recognizable as the enchanted purple version of the ordinary collar, with a very restrained palette. It must remain readable at native 16x16 inventory resolution.

## Integration

Generated PNG: `C:\Users\Kirillich611\.codex\generated_images\01a117ea-a724-7b63-ba0d-14664627bb87\exec-4d7ebd9e-a151-478c-b48a-ce83cfb4563c.png`.

Trimmed empty canvas margins (central 50% width and 45% height, starting at 25% width and 30% height), then resized with nearest-neighbor sampling into a 16×10 rectangle at (0, 3) on a transparent 16×16 RGBA canvas, preserving alpha. No palette repainting was applied. A nearest-neighbor 256×256 preview is included in the release verification folder.

Final asset: `src/main/resources/assets/bondofthebeast/textures/item/infused_collar.png`. Existing item JSON continues to reference this texture. The earlier 32×32 gemstone version remains in the previous release and historical source snapshot.
