param(
    [Parameter(Mandatory = $true)][string]$AtlasPath,
    [Parameter(Mandatory = $true)][string]$OutputDirectory
)

# Import generated artwork into the same 16-pixel canvas used by vanilla items.
# Drop soft alpha fringes, trim excess padding and use a fixed material palette.
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;

public static class AbilityAtlasImporter {
    private static readonly int[] Palette = {
        0x2F302B, 0x513A24, 0x805530, 0xB38249, 0xD0AA73, 0xE2D6B8,
        0x555B55, 0x8A9389, 0xB9C0B2, 0xE0E4D7,
        0x234F4C, 0x478A82, 0x315329, 0x672721, 0xAF4937, 0x628447
    };

    private static Color Snap(Color source) {
        int nearest = Palette[0], best = int.MaxValue;
        foreach (int rgb in Palette) {
            int dr = source.R - ((rgb >> 16) & 255);
            int dg = source.G - ((rgb >> 8) & 255);
            int db = source.B - (rgb & 255);
            int distance = dr * dr + dg * dg + db * db;
            if (distance < best) { best = distance; nearest = rgb; }
        }
        return Color.FromArgb(255, (nearest >> 16) & 255, (nearest >> 8) & 255, nearest & 255);
    }

    public static void Import(string sourcePath, string outputDirectory) {
        string[] names = { "sit", "tp", "prot", "aura", "vampiric", "nobreak", "interact", "pacifist", "absorb", "armor_lock" };
        using (var source = new Bitmap(sourcePath)) {
            int cellWidth = source.Width / 5, cellHeight = source.Height / 2;
            for (int cell = 0; cell < names.Length; cell++) {
                int originX = cell % 5 * cellWidth, originY = cell / 5 * cellHeight;
                bool[] opaque = new bool[cellWidth * cellHeight];
                bool[] visited = new bool[opaque.Length];
                bool[] kept = new bool[opaque.Length];
                for (int y = 0; y < cellHeight; y++)
                    for (int x = 0; x < cellWidth; x++)
                        opaque[y * cellWidth + x] = source.GetPixel(originX + x, originY + y).A >= 220;

                // Ignore tiny disconnected scraps in otherwise transparent margins.
                int minimumArea = Math.Max(2, opaque.Length / 1500);
                for (int start = 0; start < opaque.Length; start++) {
                    if (!opaque[start] || visited[start]) continue;
                    var component = new List<int>();
                    var queue = new Queue<int>();
                    queue.Enqueue(start); visited[start] = true;
                    while (queue.Count > 0) {
                        int point = queue.Dequeue(); component.Add(point);
                        int px = point % cellWidth, py = point / cellWidth;
                        for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                            int nx = px + dx, ny = py + dy;
                            if (nx < 0 || ny < 0 || nx >= cellWidth || ny >= cellHeight) continue;
                            int next = ny * cellWidth + nx;
                            if (opaque[next] && !visited[next]) { visited[next] = true; queue.Enqueue(next); }
                        }
                    }
                    if (component.Count >= minimumArea) foreach (int point in component) kept[point] = true;
                }
                int left = cellWidth, top = cellHeight, right = -1, bottom = -1;
                for (int i = 0; i < kept.Length; i++) if (kept[i]) {
                    left = Math.Min(left, i % cellWidth); right = Math.Max(right, i % cellWidth);
                    top = Math.Min(top, i / cellWidth); bottom = Math.Max(bottom, i / cellWidth);
                }
                if (right < left) throw new InvalidOperationException("Empty ability sprite: " + names[cell]);
                int artWidth = right - left + 1, artHeight = bottom - top + 1;
                float scale = Math.Min(14f / artWidth, 14f / artHeight);
                int drawWidth = Math.Max(1, (int)Math.Round(artWidth * scale));
                int drawHeight = Math.Max(1, (int)Math.Round(artHeight * scale));
                int offsetX = (16 - drawWidth) / 2, offsetY = (16 - drawHeight) / 2;
                using (var sprite = new Bitmap(16, 16, PixelFormat.Format32bppArgb)) {
                    for (int y = 0; y < drawHeight; y++) for (int x = 0; x < drawWidth; x++) {
                        int sx = left + Math.Min(artWidth - 1, (int)((x + .5f) * artWidth / drawWidth));
                        int sy = top + Math.Min(artHeight - 1, (int)((y + .5f) * artHeight / drawHeight));
                        if (kept[sy * cellWidth + sx]) sprite.SetPixel(offsetX + x, offsetY + y,
                                Snap(source.GetPixel(originX + sx, originY + sy)));
                    }
                    // Resampling may disconnect a contour pixel from its original shape.
                    // Remove tiny isolated scraps on the final logical grid as well.
                    var finalVisited = new bool[256];
                    for (int start = 0; start < 256; start++) {
                        if (finalVisited[start] || sprite.GetPixel(start % 16, start / 16).A == 0) continue;
                        var component = new List<int>();
                        var queue = new Queue<int>();
                        queue.Enqueue(start); finalVisited[start] = true;
                        while (queue.Count > 0) {
                            int point = queue.Dequeue(); component.Add(point);
                            int px = point % 16, py = point / 16;
                            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                                int nx = px + dx, ny = py + dy;
                                if (nx < 0 || ny < 0 || nx >= 16 || ny >= 16) continue;
                                int next = ny * 16 + nx;
                                if (!finalVisited[next] && sprite.GetPixel(nx, ny).A > 0) {
                                    finalVisited[next] = true; queue.Enqueue(next);
                                }
                            }
                        }
                        if (component.Count <= 2) foreach (int point in component)
                            sprite.SetPixel(point % 16, point / 16, Color.FromArgb(0, 0, 0, 0));
                    }
                    int finalLeft = 16, finalTop = 16, finalRight = -1, finalBottom = -1;
                    for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                        if (sprite.GetPixel(x, y).A == 0) continue;
                        finalLeft = Math.Min(finalLeft, x); finalRight = Math.Max(finalRight, x);
                        finalTop = Math.Min(finalTop, y); finalBottom = Math.Max(finalBottom, y);
                    }
                    if (finalRight < finalLeft) throw new InvalidOperationException("Empty imported sprite: " + names[cell]);
                    // Match the actual chestplate bounds: x=1..14, y=2..14.
                    // Normalize after cleanup, since sampling can shrink a silhouette.
                    int finalWidth = finalRight - finalLeft + 1, finalHeight = finalBottom - finalTop + 1;
                    using (var fitted = new Bitmap(16, 16, PixelFormat.Format32bppArgb)) {
                        for (int y = 0; y < 13; y++) for (int x = 0; x < 14; x++) {
                            int sx = finalLeft + Math.Min(finalWidth - 1, (int)((x + .5f) * finalWidth / 14));
                            int sy = finalTop + Math.Min(finalHeight - 1, (int)((y + .5f) * finalHeight / 13));
                            fitted.SetPixel(x + 1, y + 2, sprite.GetPixel(sx, sy));
                        }
                        using (var enlarged = new Bitmap(32, 32, PixelFormat.Format32bppArgb)) {
                            for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++)
                                enlarged.SetPixel(x, y, fitted.GetPixel(x / 2, y / 2));
                            enlarged.Save(System.IO.Path.Combine(outputDirectory, names[cell] + ".png"), ImageFormat.Png);
                        }
                    }
                }
            }
        }
    }
}
'@

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
[AbilityAtlasImporter]::Import([System.IO.Path]::GetFullPath($AtlasPath), [System.IO.Path]::GetFullPath($OutputDirectory))
