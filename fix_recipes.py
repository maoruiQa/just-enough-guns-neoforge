#!/usr/bin/env python3
"""
Fix recipe JSON files for Minecraft 1.21.1 format.
Converts from 1.21.8 format to 1.21.1 format.
"""
import json
import os
from pathlib import Path

def fix_shaped_recipe(data):
    """Fix shaped recipe format."""
    if 'key' in data:
        new_key = {}
        for k, v in data['key'].items():
            if isinstance(v, str):
                # Convert string to object format
                if v.startswith('#'):
                    new_key[k] = {"tag": v[1:]}
                else:
                    new_key[k] = {"item": v}
            else:
                new_key[k] = v
        data['key'] = new_key

    # Fix result format - in 1.21.1, result should use "id" not "item"
    if 'result' in data and isinstance(data['result'], dict):
        if 'item' in data['result']:
            data['result']['id'] = data['result'].pop('item')

    return data

def fix_shapeless_recipe(data):
    """Fix shapeless recipe format."""
    if 'ingredients' in data and isinstance(data['ingredients'], list):
        new_ingredients = []
        for ing in data['ingredients']:
            if isinstance(ing, str):
                # Convert string to object format
                if ing.startswith('#'):
                    new_ingredients.append({"tag": ing[1:]})
                else:
                    new_ingredients.append({"item": ing})
            else:
                new_ingredients.append(ing)
        data['ingredients'] = new_ingredients

    # Fix result format - in 1.21.1, result should use "id" not "item"
    if 'result' in data and isinstance(data['result'], dict):
        if 'item' in data['result']:
            data['result']['id'] = data['result'].pop('item')

    return data

def fix_recipe_file(file_path):
    """Fix a single recipe file."""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)

        # Determine recipe type and fix accordingly
        recipe_type = data.get('type', '')

        if recipe_type == 'minecraft:crafting_shaped':
            data = fix_shaped_recipe(data)
        elif recipe_type == 'minecraft:crafting_shapeless':
            data = fix_shapeless_recipe(data)

        # Write back to file with proper formatting
        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write('\n')  # Add trailing newline

        return True, None
    except Exception as e:
        return False, str(e)

def main():
    """Main function to fix all recipe files."""
    recipe_dir = Path(__file__).parent / 'src' / 'main' / 'resources' / 'data' / 'jeg' / 'recipe'

    if not recipe_dir.exists():
        print(f"Error: Recipe directory not found: {recipe_dir}")
        return

    print(f"Scanning recipe directory: {recipe_dir}")

    fixed_count = 0
    error_count = 0

    for recipe_file in recipe_dir.glob('*.json'):
        print(f"Processing: {recipe_file.name}...", end=' ')
        success, error = fix_recipe_file(recipe_file)

        if success:
            print("[OK]")
            fixed_count += 1
        else:
            print(f"[ERROR] {error}")
            error_count += 1

    print(f"\n{'='*60}")
    print(f"Summary:")
    print(f"  Fixed: {fixed_count}")
    print(f"  Errors: {error_count}")
    print(f"  Total: {fixed_count + error_count}")
    print(f"{'='*60}")

if __name__ == '__main__':
    main()
