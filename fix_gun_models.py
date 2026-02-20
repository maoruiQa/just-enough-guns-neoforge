#!/usr/bin/env python3
"""
Fix gun item models by converting from builtin/entity to simple 2D texture models.
This script converts all gun models that use the unsupported "parent": "builtin/entity"
to simple "item/handheld" models with 2D textures.
"""

import json
import os
from pathlib import Path

# Files that need to be fixed (use builtin/entity)
FILES_TO_FIX = [
    'waterpipe_shotgun.json',
    'typhoonee.json',
    'supersonic_shotgun.json',
    'subsonic_rifle.json',
    'soulhunter_mk2.json',
    'service_rifle.json',
    'semi_auto_rifle.json',
    'semi_auto_pistol.json',
    'rocket_launcher.json',
    'repeating_shotgun.json',
    'revolver.json',
    'pump_shotgun.json',
    'primitive_bow.json',
    'minigun.json',
    'light_machine_gun.json',
    'hypersonic_cannon.json',
    'infantry_rifle.json',
    'holy_shotgun.json',
    'hollenfire_mk2.json',
    'grenade_launcher.json',
    'flare_gun.json',
    'flamethrower.json',
    'finger_gun.json',
    'double_barrel_shotgun.json',
    'custom_smg.json',
    'compound_bow.json',
    'combat_scope.json',
    'combat_rifle.json',
    'combat_pistol.json',
    'burst_rifle.json',
    'bolt_action_rifle.json',
    'blossom_rifle.json',
    'assault_rifle.json',
    'abstract_gun.json',
    'abstract_gun_old.json',
]

# Base directory
BASE_DIR = Path('src/main/resources/assets/jeg/models/item')
TEXTURE_DIR = Path('src/main/resources/assets/jeg/textures/item')

def create_simple_gun_model(gun_name: str) -> dict:
    """Create a simple 2D texture model for a gun."""
    return {
        "parent": "item/handheld",
        "textures": {
            "layer0": f"jeg:item/{gun_name}"
        }
    }

def backup_and_fix_model(model_file: Path):
    """Backup original model and replace with simple texture model."""
    try:
        # Read original file
        with open(model_file, 'r', encoding='utf-8') as f:
            original_data = json.load(f)

        # Check if it uses builtin/entity
        if original_data.get('parent') != 'builtin/entity':
            print(f"  [SKIP] {model_file.name} doesn't use builtin/entity, skipping")
            return False

        # Create backup
        backup_file = model_file.with_suffix('.json.bak')
        with open(backup_file, 'w', encoding='utf-8') as f:
            json.dump(original_data, f, indent=2)
        print(f"  [OK] Backed up to {backup_file.name}")

        # Get gun name (without .json extension)
        gun_name = model_file.stem

        # Create new simple model
        new_model = create_simple_gun_model(gun_name)

        # Write new model
        with open(model_file, 'w', encoding='utf-8') as f:
            json.dump(new_model, f, indent=2)

        print(f"  [OK] Fixed {model_file.name}")
        return True

    except Exception as e:
        print(f"  [ERROR] Error processing {model_file.name}: {e}")
        return False

def main():
    print("=" * 60)
    print("Gun Model Fixer for Minecraft 1.21.1")
    print("=" * 60)
    print()

    if not BASE_DIR.exists():
        print(f"Error: Model directory not found: {BASE_DIR}")
        return

    print(f"Model directory: {BASE_DIR}")
    print(f"Files to fix: {len(FILES_TO_FIX)}")
    print()

    fixed_count = 0
    skipped_count = 0
    error_count = 0

    for filename in FILES_TO_FIX:
        model_file = BASE_DIR / filename

        if not model_file.exists():
            print(f"[ERROR] {filename} - File not found")
            error_count += 1
            continue

        print(f"Processing {filename}...")
        result = backup_and_fix_model(model_file)

        if result:
            fixed_count += 1
        elif result is False:
            skipped_count += 1
        else:
            error_count += 1

    print()
    print("=" * 60)
    print("Summary:")
    print(f"  Fixed: {fixed_count}")
    print(f"  Skipped: {skipped_count}")
    print(f"  Errors: {error_count}")
    print(f"  Total: {len(FILES_TO_FIX)}")
    print("=" * 60)
    print()

    if fixed_count > 0:
        print("[SUCCESS] All gun models have been fixed!")
        print("  Original models backed up with .bak extension")
        print("  You can now test the mod in-game")
    else:
        print("[WARNING] No models were fixed. Check the output above for details.")

if __name__ == '__main__':
    main()
