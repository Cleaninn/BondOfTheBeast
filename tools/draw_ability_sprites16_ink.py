"""Hand-drawn ink-and-gold 16x16 grimoire icons; no resampled artwork.

Each symbol is one native pixel. The 14x14 drawings have a transparent
one-pixel border. All ability and navigation icons share two ink colors.
Run from the project root; requires only Python's stdlib.
"""
import argparse
from pathlib import Path
import struct
import zlib

PALETTE = {
    '.': (0, 0, 0, 0),
    'd': (75, 45, 45, 255),
    'a': (195, 153, 88, 255),
}

SPRITES = {
    'sit': [
        'dd..........dd',
        'ddd........ddd',
        'dddddddddddddd',
        'ddaaaaaaaaaadd',
        'dddddddddddddd',
        'ddd.dddddd.ddd',
        'ddd.dddddd.ddd',
        'dddddddddddddd',
        '.ddddaaaadddd.',
        '.dddaaddaaddd.',
        '..ddaaaaaadd..',
        '...dddddddd...',
        '....dddddd....',
        '..............',
    ],
    'tp': [
        '......dd......',
        '.....ddd......',
        '....dddddddd..',
        '...dddddddddd.',
        '..dddddaaaaddd',
        '...dddd...aadd',
        '....ddd....add',
        '.....dd....add',
        '...........add',
        '...........add',
        '...dd......add',
        '...ddddddaaadd',
        '....ddddddddd.',
        '.....ddddddd..',
    ],
    'prot': [
        '.dddddddddddd.',
        '.dddddddddddd.',
        '.ddaa.....ddd.',
        '.ddaa.....ddd.',
        '.ddaa.....ddd.',
        '.ddaa.....ddd.',
        '.ddaa.....ddd.',
        '..dda.....dd..',
        '..dda.....dd..',
        '...dd....dd...',
        '...dd....dd...',
        '....dd..dd....',
        '.....dddd.....',
        '......dd......',
    ],
    'aura': [
        '..dd....dd....',
        '.dddd..dddd...',
        '.dddd..dddd...',
        '..dd....dd....',
        'dd..........dd',
        'ddd..dddd..ddd',
        'ddd.dddddd.ddd',
        '.d.dddddddd.d.',
        '..dddddddddd..',
        '.dddddddddddd.',
        '.dddaaddddddd.',
        '.dddaaaaddddd.',
        '..ddaaaadddd..',
        '...dddddddd...',
    ],
    'vampiric': [
        '......dd......',
        '.....dddd.....',
        '.....dddd.....',
        '....dddddd....',
        '....dddddd....',
        '...dddddddd...',
        '...daaddddd...',
        '..ddaadddddd..',
        '.dddaaddddddd.',
        '.dddddddddddd.',
        '.dddddddddddd.',
        '..dddddddddd..',
        '...dddddddd...',
        '..............',
    ],
    'nobreak': [
        '..dddddd......',
        '.dddddddd.....',
        'dddddddddd....',
        'ddd...dddd....',
        '......dddd....',
        '.....aaddd....',
        '....aad.dd....',
        '...aad........',
        '..aad.........',
        '.aad..........',
        'aad.....dddddd',
        'ad......aaaaaa',
        'd.......dddddd',
        '..............',
    ],
    'interact': [
        '.......aa.....',
        '......aad.....',
        '.....aad......',
        '....aad.......',
        '...aad........',
        '..aad.........',
        '.dddddddddd...',
        'dddddddddddd..',
        'ddaaaaaaaadd..',
        'dddddddddddd..',
        '.dddddddddd...',
        '..dddd..dddddd',
        '........aaaaaa',
        '........dddddd',
    ],
    'pacifist': [
        '...........dd.',
        '..........ddd.',
        '.........dddd.',
        '........dddd..',
        '.......dddd...',
        '......dddd....',
        '.....dddd.....',
        '..dddddd......',
        '..ddddd.......',
        '...ddd..dddddd',
        '..aad...aaaaaa',
        '.aad....dddddd',
        'aad...........',
        '.d............',
    ],
    'absorb': [
        '.dddddddddd...',
        '.daaddddddddd.',
        '.daaddddddddd.',
        '.daaddddddddd.',
        '.daaddddddddd.',
        '.daaddaaaaddd.',
        '.daaddaaaaddd.',
        '.daaddddddddd.',
        '.daaddddddddd.',
        '.dddddddddddd.',
        '.ddaaaaaaaadd.',
        '.dddddddddddd.',
        '..dddddddddd..',
        '..............',
    ],
    'armor_lock': [
        '....dddddd....',
        '...dddddddd...',
        '..ddd....ddd..',
        '..ddd....ddd..',
        '..ddd....ddd..',
        '.dddddddddddd.',
        '.ddaaaaaaaadd.',
        '.ddaaaaaaaadd.',
        '.ddaaa..aaadd.',
        '.ddaaa..aaadd.',
        '.ddaaaaaaaadd.',
        '.ddaaaaaaaadd.',
        '.dddddddddddd.',
        '.dddddddddddd.',
    ],
    'armor': [
        '..ddd....ddd..',
        '.dddd....dddd.',
        'dddddddddddddd',
        'dddddddddddddd',
        'dddaaaaaaaaddd',
        '.dddddddddddd.',
        '...dddddddd...',
        '...dddddddd...',
        '...ddaaaadd...',
        '...ddaaaadd...',
        '...dddddddd...',
        '...dddddddd...',
        '...dddddddd...',
        '..............',
    ],
    'blocks': [
        'dddddd..dddddd',
        'dddddd..dddddd',
        'ddaadd..ddaadd',
        'ddaadd..ddaadd',
        'dddddd..dddddd',
        'dddddd..dddddd',
        '..............',
        '..............',
        'dddddd..dddddd',
        'dddddd..dddddd',
        'ddaadd..ddaadd',
        'ddaadd..ddaadd',
        'dddddd..dddddd',
        'dddddd..dddddd',
    ],
    'blacklist': [
        '.dddddddddddd.',
        '.dddddddddddd.',
        '.dd........dd.',
        '.dd........dd.',
        '.dd........dd.',
        '.dd.aaaaaa.dd.',
        '.dd.aaaaaa.dd.',
        '.dd........dd.',
        '.dd........dd.',
        '.dd........dd.',
        '.dd........dd.',
        '.dddddddddddd.',
        '.dddddddddddd.',
        '..............',
    ],
    'whitelist': [
        '.dddddddddddd.',
        '.dddddddddddd.',
        '.dd........dd.',
        '.dd...aa...dd.',
        '.dd...aa...dd.',
        '.dd.aaaaaa.dd.',
        '.dd.aaaaaa.dd.',
        '.dd...aa...dd.',
        '.dd...aa...dd.',
        '.dd........dd.',
        '.dd........dd.',
        '.dddddddddddd.',
        '.dddddddddddd.',
        '..............',
    ],
}


def write_png(path, pixels, width, height):
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))
    rows = b''.join(b'\x00' + bytes(channel for pixel in pixels[y * width:(y + 1) * width]
                                   for channel in pixel) for y in range(height))
    header = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
    path.write_bytes(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', header)
                     + chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b''))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=Path(
        'src/main/resources/assets/bondofthebeast/textures/gui/abilities/ink'))
    parser.add_argument('--preview', type=Path)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    sheet_height = ((len(SPRITES) + 4) // 5) * 80
    sheet = [(224, 207, 170, 255)] * (400 * sheet_height)
    for index, (name, rows) in enumerate(SPRITES.items()):
        if len(rows) != 14 or any(len(row) != 14 for row in rows):
            raise ValueError(f'{name}: expected fourteen rows of fourteen pixels: {[len(r) for r in rows]}')
        pixels = [PALETTE['.']] * 256
        for y, row in enumerate(rows):
            for x, symbol in enumerate(row):
                pixels[(y + 1) * 16 + x + 1] = PALETTE[symbol]
        write_png(args.output / f'{name}.png', pixels, 16, 16)
        for y in range(16):
            for x in range(16):
                color = pixels[y * 16 + x]
                if color[3] == 0:
                    continue
                for dy in range(4):
                    for dx in range(4):
                        px = (index % 5) * 80 + 8 + x * 4 + dx
                        py = (index // 5) * 80 + 8 + y * 4 + dy
                        sheet[py * 400 + px] = color
        print(f'{name}: hand-drawn 16x16')
    if args.preview:
        args.preview.parent.mkdir(parents=True, exist_ok=True)
        write_png(args.preview, sheet, 400, sheet_height)


if __name__ == '__main__':
    main()
