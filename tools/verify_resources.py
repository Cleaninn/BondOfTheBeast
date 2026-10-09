"""Check packaged resource references without starting Minecraft (Python 3)."""
from pathlib import Path
import json
import re
import struct
import sys

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src/main/resources"
ASSETS = RESOURCES / "assets/bondofthebeast"

def verify():
    errors = []
    documents = {}
    for path in RESOURCES.rglob("*.json"):
        try:
            documents[path] = json.loads(path.read_text(encoding="utf-8"))
        except (ValueError, UnicodeError) as error:
            errors.append(f"Invalid JSON: {path.relative_to(ROOT)}: {error}")

    def local_reference(value, kind):
        if isinstance(value, str) and value.startswith("bondofthebeast:"):
            name = value.split(":", 1)[1]
            target = ASSETS / kind / (name + (".png" if kind == "textures" else ".json"))
            if not target.is_file():
                errors.append(f"Missing {kind}: {value}")

    for path, document in documents.items():
        if "models" not in path.parts and "blockstates" not in path.parts:
            continue
        local_reference(document.get("parent"), "models")
        for value in document.get("textures", {}).values():
            local_reference(value, "textures")
        def walk(node):
            if isinstance(node, dict):
                local_reference(node.get("model"), "models")
                for value in node.values(): walk(value)
            elif isinstance(node, list):
                for value in node: walk(value)
        walk(document)

    item_code = (ROOT / "src/main/java/com/bondofthebeast/ModItems.java").read_text(encoding="utf-8")
    item_code = re.sub(r"//[^\n]*", "", item_code)
    for name in re.findall(r'= registerItem\("([a-z_]+)"', item_code):
        if not (ASSETS / "models/item" / f"{name}.json").is_file():
            errors.append(f"Missing registered item model: {name}")
    languages = {lang: documents[ASSETS / f"lang/{lang}.json"] for lang in ("en_us", "ru_ru")}
    for lang, entries in languages.items():
        for key, value in entries.items():
            if "??" in value:
                errors.append(f"Possible encoding damage in {lang} translation: {key}")
        for name in re.findall(r'= registerItem\("([a-z_]+)"', item_code):
            if f"item.bondofthebeast.{name}" not in entries:
                errors.append(f"Missing {lang} item name: {name}")
    for java in (ROOT / "src/main/java/com/bondofthebeast").rglob("*.java"):
        text = java.read_text(encoding="utf-8")
        local_text = re.sub(r'new Identifier\("minecraft",\s*"textures/[^"\n]+\.png"\)', '', text)
        for texture in re.findall(r'"(textures/[^"\n]+\.png)"', local_text):
            if not (ASSETS / texture).is_file(): errors.append(f"Missing renderer texture: {texture}")
        for key in re.findall(r'Text\.translatable\("(bondofthebeast\.[^"\n]+|(?:text|tooltip|gui|command|itemgroup)\.bondofthebeast\.[^"\n]+)"\)', text):
            for lang, entries in languages.items():
                if key not in entries: errors.append(f"Missing {lang} translation: {key}")

    for name in ("infused_collar", "lunar_oblivion_dust"):
        path = ASSETS / f"textures/item/{name}.png"
        if not path.is_file():
            errors.append(f"Missing new sprite: {name}")
            continue
        data = path.read_bytes()
        if data[:8] != b"\x89PNG\r\n\x1a\n": errors.append(f"Invalid PNG: {name}")
        else:
            width, height, depth, color = struct.unpack(">IIBB", data[16:26])
            if width != height or width not in (16, 32) or (depth, color) != (8, 6):
                errors.append(f"Expected transparent 16x16 or 32x32 RGBA sprite: {name}")
    return sorted(set(errors)), len(documents)

if __name__ == "__main__":
    errors, count = verify()
    for error in errors: print(error)
    print(f"Validated {count} JSON resources; {len(errors)} errors.")
    sys.exit(bool(errors))
