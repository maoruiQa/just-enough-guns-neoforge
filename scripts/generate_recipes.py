#!/usr/bin/env python3
import json
from pathlib import Path
import hashlib

FORGE_GUNS = Path('../Just-Enough-Guns/src/generated/resources/data/jeg/guns')
OUTPUT = Path('src/main/resources/data/jeg/recipe')

AMMO_INGREDIENTS = {
    'jeg:pistol_ammo': ['minecraft:iron_ingot', 'minecraft:gunpowder', 'minecraft:copper_ingot'],
    'jeg:rifle_ammo': ['minecraft:iron_ingot', 'minecraft:gunpowder', 'minecraft:redstone'],
    'jeg:shotgun_shell': ['minecraft:copper_ingot', 'minecraft:paper', 'minecraft:gunpowder'],
    'jeg:handmade_shell': ['minecraft:paper', 'minecraft:gunpowder', 'minecraft:string'],
    'jeg:spectre_round': ['minecraft:amethyst_shard', 'minecraft:gunpowder', 'minecraft:glowstone_dust'],
    'jeg:blaze_round': ['minecraft:blaze_powder', 'minecraft:iron_nugget', 'minecraft:gunpowder'],
    'jeg:pocket_bubble': ['minecraft:prismarine_shard', 'minecraft:water_bucket'],
    'jeg:water_bomb': ['minecraft:prismarine_crystals', 'minecraft:water_bucket'],
    'jeg:rocket': ['minecraft:tnt', 'minecraft:iron_ingot'],
    'jeg:grenade': ['minecraft:tnt', 'minecraft:slime_ball'],
    'jeg:flare': ['minecraft:glowstone_dust', 'minecraft:paper'],
}

AMMO_RECIPES = {
    'pistol_ammo': ['minecraft:iron_ingot', 'minecraft:gunpowder'],
    'rifle_ammo': ['minecraft:iron_ingot', 'minecraft:gunpowder', 'minecraft:redstone'],
    'shotgun_shell': ['minecraft:copper_ingot', 'minecraft:paper', 'minecraft:gunpowder'],
    'handmade_shell': ['minecraft:paper', 'minecraft:string', 'minecraft:gunpowder'],
    'spectre_round': ['minecraft:amethyst_shard', 'minecraft:gunpowder', 'minecraft:glowstone_dust'],
    'blaze_round': ['minecraft:blaze_powder', 'minecraft:iron_nugget', 'minecraft:gunpowder'],
    'pocket_bubble': ['minecraft:prismarine_shard', 'minecraft:water_bucket'],
    'water_bomb': ['minecraft:prismarine_crystals', 'minecraft:water_bucket'],
    'rocket': ['minecraft:tnt', 'minecraft:iron_ingot', 'minecraft:gunpowder'],
    'grenade': ['minecraft:tnt', 'minecraft:slime_ball', 'minecraft:iron_ingot'],
    'flare': ['minecraft:glowstone_dust', 'minecraft:paper', 'minecraft:redstone']
}

DYES = [
    'minecraft:white_dye','minecraft:orange_dye','minecraft:magenta_dye','minecraft:light_blue_dye',
    'minecraft:yellow_dye','minecraft:lime_dye','minecraft:pink_dye','minecraft:gray_dye',
    'minecraft:light_gray_dye','minecraft:cyan_dye','minecraft:purple_dye','minecraft:blue_dye',
    'minecraft:brown_dye','minecraft:green_dye','minecraft:red_dye','minecraft:black_dye'
]

BASE_INGREDIENTS = ['minecraft:iron_ingot', 'minecraft:redstone', 'minecraft:gunpowder']


def hash_dye(name: str) -> str:
    digest = hashlib.sha256(name.encode('utf-8')).digest()
    return DYES[digest[0] % len(DYES)]


def write_json(path: Path, data: dict):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('w', encoding='utf-8') as f:
        json.dump(data, f, indent=2)


def generate_gun_recipes():
    for gun_path in sorted(FORGE_GUNS.glob('*.json')):
        gun_id = gun_path.stem
        data = json.loads(gun_path.read_text())
        ammo = data.get('projectile', {}).get('item')
        ingredients = list(BASE_INGREDIENTS)
        if ammo in AMMO_INGREDIENTS:
            ingredients.extend(AMMO_INGREDIENTS[ammo])
        elif ammo and ammo.startswith('minecraft:'):
            ingredients.append(ammo)
        else:
            ingredients.append('minecraft:iron_nugget')

        ingredients.append(hash_dye(gun_id))
        ingredient_list = list(ingredients)
        recipe = {
            'type': 'minecraft:crafting_shapeless',
            'category': 'equipment',
            'ingredients': ingredient_list,
            'result': {'id': f'jeg:{gun_id}'}
        }
        write_json(OUTPUT / f'{gun_id}.json', recipe)


def generate_ammo_recipes():
    for ammo_name, items in AMMO_RECIPES.items():
        ingredients = list(items)
        result = {'id': f'jeg:{ammo_name}', 'count': 4}
        recipe = {
            'type': 'minecraft:crafting_shapeless',
            'category': 'misc',
            'ingredients': ingredients,
            'result': result
        }
        write_json(OUTPUT / f'{ammo_name}.json', recipe)


def main():
    if OUTPUT.exists():
        for path in OUTPUT.glob('*.json'):
            path.unlink()
    generate_ammo_recipes()
    generate_gun_recipes()

if __name__ == '__main__':
    main()
