#!/usr/bin/env python3
import json
import os
import sys

def fix_texture_references():
    """Fix texture references in model files"""
    model_dir = "src/main/resources/assets/jeg/models"
    texture_dir = "src/main/resources/assets/jeg/textures"

    # Find which textures are actually missing
    missing_textures = set()
    existing_textures = set()

    # Collect all existing textures
    for root, dirs, files in os.walk(texture_dir):
        for file in files:
            if file.endswith('.png'):
                # Get relative path from texture_dir
                rel_path = os.path.relpath(os.path.join(root, file), texture_dir)
                rel_path = rel_path.replace('\\', '/')  # Normalize path separators
                rel_path = rel_path[:-4]  # Remove .png extension
                existing_textures.add(f"jeg:{rel_path}")

    print(f"Found {len(existing_textures)} existing textures")

    # Check all model files for missing textures
    for root, dirs, files in os.walk(model_dir):
        for file in files:
            if file.endswith('.json'):
                filepath = os.path.join(root, file)
                try:
                    with open(filepath, 'r') as f:
                        data = json.load(f)

                    if 'textures' in data:
                        for texture_name, texture_path in data['textures'].items():
                            if texture_path.startswith('jeg:'):
                                if texture_path not in existing_textures:
                                    missing_textures.add(texture_path)
                except:
                    pass

    print(f"Found {len(missing_textures)} missing textures")

    if missing_textures:
        print("\nMissing textures:")
        for texture in sorted(missing_textures):
            print(f"  - {texture}")

        # Suggest fixes for common patterns
        print("\nCommon fixes needed:")

        # Check for animated gun textures
        missing_gun_textures = [t for t in missing_textures if 'animated/gun/' in t]
        if missing_gun_textures:
            print(f"\nMissing animated gun textures ({len(missing_gun_textures)}):")
            for texture in sorted(missing_gun_textures):
                gun_name = texture.split('/')[-1]
                print(f"  - {texture}")
                print(f"    Suggestion: Create or copy from existing gun texture")

        # Check for block textures
        missing_block_textures = [t for t in missing_textures if 'block/' in t]
        if missing_block_textures:
            print(f"\nMissing block textures ({len(missing_block_textures)}):")
            for texture in sorted(missing_block_textures):
                print(f"  - {texture}")

        # Check for item textures
        missing_item_textures = [t for t in missing_textures if 'item/' in t]
        if missing_item_textures:
            print(f"\nMissing item textures ({len(missing_item_textures)}):")
            for texture in sorted(missing_item_textures)[:10]:  # Show first 10
                print(f"  - {texture}")
            if len(missing_item_textures) > 10:
                print(f"  ... and {len(missing_item_textures) - 10} more")

    return missing_textures

def check_critical_models():
    """Check models that are most likely to cause crashes"""
    critical_models = [
        "src/main/resources/assets/jeg/models/item/abstract_gun.json",
        "src/main/resources/assets/jeg/models/block/brimstone_ore.json"
    ]

    print("\nChecking critical models:")
    for model_path in critical_models:
        if os.path.exists(model_path):
            print(f"\n{model_path}:")
            try:
                with open(model_path, 'r') as f:
                    data = json.load(f)

                if 'parent' in data:
                    print(f"  Parent: {data['parent']}")
                    if data['parent'] == 'builtin/entity':
                        print(f"  WARNING: Using builtin/entity parent - this may cause issues!")

                if 'textures' in data:
                    print(f"  Textures: {len(data['textures'])}")
                    for name, path in data['textures'].items():
                        print(f"    {name}: {path}")

            except Exception as e:
                print(f"  ERROR: {e}")
        else:
            print(f"  MISSING: {model_path}")

if __name__ == "__main__":
    print("Analyzing texture references...")
    missing = fix_texture_references()
    check_critical_models()

    if missing:
        print(f"\nSUMMARY: Found {len(missing)} missing texture references")
        print("The ModelManager crash is likely caused by missing textures during resource loading.")
        print("Fix missing textures or update model references to resolve the issue.")