param(
    [Parameter(Mandatory = $true)][string]$SourceDirectory,
    [Parameter(Mandatory = $true)][string]$OutputDirectory
)

# Import separately redrawn artwork directly onto a 32x32 grid, without a 16x16 intermediate.
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;

public static class AbilitySprite32Importer {
    private static readonly int[] Palette = {
        0x242724, 0x513A24, 0x805530, 0xB38249, 0xD0AA73, 0xE2D6B8,
        0x434743, 0x626A62, 0x8A9389, 0xB9C0B2, 0xE0E4D7,
        0x234F4C, 0x478A82, 0x79B3A3, 0x315329, 0x628447,
        0x94B76A, 0x672721, 0xAF4937
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
    public static void Import(string input, string output) {
        using (var source = new Bitmap(input)) {
            int left = source.Width, top = source.Height, right = -1, bottom = -1;
            for (int y = 0; y < source.Height; y++) for (int x = 0; x < source.Width; x++) {
                if (source.GetPixel(x, y).A < 220) continue;
                left = Math.Min(left, x); right = Math.Max(right, x);
                top = Math.Min(top, y); bottom = Math.Max(bottom, y);
            }
            if (right < left) throw new InvalidOperationException("Empty artwork: " + input);
            int w = right - left + 1, h = bottom - top + 1;
            float scale = Math.Min(28f / w, 28f / h);
            int dw = Math.Max(1, (int)Math.Round(w * scale));
            int dh = Math.Max(1, (int)Math.Round(h * scale));
            using (var sprite = new Bitmap(32, 32, PixelFormat.Format32bppArgb)) {
                for (int y = 0; y < dh; y++) for (int x = 0; x < dw; x++) {
                    int sx = left + Math.Min(w - 1, (int)((x + .5f) * w / dw));
                    int sy = top + Math.Min(h - 1, (int)((y + .5f) * h / dh));
                    Color pixel = source.GetPixel(sx, sy);
                    if (pixel.A >= 220) sprite.SetPixel((32 - dw) / 2 + x, (32 - dh) / 2 + y, Snap(pixel));
                }
                sprite.Save(output, ImageFormat.Png);
            }
        }
    }
}
'@

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
Get-ChildItem -LiteralPath $SourceDirectory -Filter '*.png' | ForEach-Object {
    [AbilitySprite32Importer]::Import($_.FullName, [System.IO.Path]::GetFullPath((Join-Path $OutputDirectory $_.Name)))
}
