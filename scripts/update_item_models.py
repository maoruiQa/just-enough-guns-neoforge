#!/usr/bin/env python3
import json
from pathlib import Path
from generate_recipes import FORGE_GUNS  # reuse path

ASSETS_DIR = Path('src/main/resources/assets/jeg/models/item')
TEXTURE_DIR = Path('src/main/resources/assets/jeg/textures/item')
FORGE_MODELS = Path('../Just-Enough-Guns/src/main/resources/assets/jeg/models/item')
SPECIAL_ROOT = Path('../Just-Enough-Guns/src/main/resources/assets/jeg/models/special')

# reuse ammo ids from ModItems? We'll define here
AMMO_IDS = [
    'pistol_ammo','rifle_ammo','shotgun_shell','handmade_shell','spectre_round',
    'blaze_round','pocket_bubble','water_bomb','rocket','grenade','flare'
]

MANUALS = ['gunsmith_manual']

SPECIAL_INDEX: dict[str, Path] = {}
if SPECIAL_ROOT.exists():
    for path in sorted(SPECIAL_ROOT.glob('**/*.json')):
        name = path.stem
        if name == 'main':
            name = path.parent.name
        unix_path = str(path).replace('\\', '/')
        if name not in SPECIAL_INDEX or '/gun/' in unix_path:
            SPECIAL_INDEX[name] = path

DEFAULT_EXTRA_PARTS: dict[str, list[str]] = {
    'service_rifle': ['handguard_light', 'stock_light', 'mag_default'],
    'hollenfire_mk2': ['magazine_default', 'stock_light'],
    'soulhunter_mk2': ['mag_default'],
    'infantry_rifle': ['magazine_default'],
    'subsonic_rifle': ['mag_default']
}


def texture_exists(name: str) -> bool:
    return (TEXTURE_DIR / f'{name}.png').exists()


def load_display(name: str):
    source = FORGE_MODELS / f'{name}.json'
    if not source.exists():
        return None

    try:
        data = json.loads(source.read_text(encoding='utf-8'))
    except json.JSONDecodeError:
        return None

    display = data.get('display')
    if isinstance(display, dict) and display:
        return display
    return None


def write_model(name: str):
    if not texture_exists(name):
        return

    geometry = None
    special_path = SPECIAL_INDEX.get(name)
    if special_path is not None:
        try:
            geometry = json.loads(special_path.read_text(encoding='utf-8'))
        except json.JSONDecodeError:
            geometry = None

    if geometry is not None:
        model = geometry
        display = load_display(name)
        if display:
            model['display'] = display
        model.setdefault('parent', 'builtin/entity')
        merge_default_parts(model, name)
    else:
        model = {
            'parent': 'minecraft:item/generated',
            'textures': {
                'layer0': f'jeg:item/{name}'
            }
        }
        display = load_display(name)
        if display:
            model['display'] = display

    path = ASSETS_DIR / f'{name}.json'
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('w', encoding='utf-8') as f:
        json.dump(model, f, indent=2)


def merge_default_parts(model: dict, name: str):
    extras = DEFAULT_EXTRA_PARTS.get(name)
    if not extras:
        return

    elements = model.setdefault('elements', [])
    for extra in extras:
        part_path = SPECIAL_ROOT / name / f'{extra}.json'
        if not part_path.exists():
            continue
        try:
            part_data = json.loads(part_path.read_text(encoding='utf-8'))
        except json.JSONDecodeError:
            continue
        for element in part_data.get('elements', []):
            # duplicate element to avoid shared references
            elements.append(json.loads(json.dumps(element)))


def main():
    names = set(AMMO_IDS + MANUALS)
    for gun_path in FORGE_GUNS.glob('*.json'):
        names.add(gun_path.stem)
    for name in sorted(names):
        write_model(name)

if __name__ == '__main__':
    main()
