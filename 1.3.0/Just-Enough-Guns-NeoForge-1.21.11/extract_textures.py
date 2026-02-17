#!/usr/bin/env python3
import json
import os
import re
from pathlib import Path

def extract_texture_references(model_file_path):
    """Extract all texture references from a JSON model file"""
    try:
        with open(model_file_path, 'r', encoding='utf-8') as f:
            model_data = json.load(f)

        textures = []

        # Check for textures object
        if 'textures' in model_data:
            for key, value in model_data['textures'].items():
                if value.startswith('#'):
                    # This is a reference to another texture
                    textures.append((key, value[1:]))  # Remove # prefix
                elif not value.startswith('minecraft:'):
                    # This is an external texture reference
                    textures.append((key, value))

        # Also check for direct texture references in elements
        if 'elements' in model_data:
            for element in model_data['elements']:
                if 'faces' in element:
                    for face_data in element['faces'].values():
                        if 'texture' in face_data:
                            texture_ref = face_data['texture']
                            if texture_ref.startswith('#'):
                                textures.append(('element_face', texture_ref[1:]))

        return textures
    except Exception as e:
        print(f"Error processing {model_file_path}: {e}")
        return []

def find_missing_textures():
    models_dir = Path("Just-Enough-Guns-NeoForge-1.21.10/src/main/resources/assets/jeg/models/item")
    textures_dir = Path("Just-Enough-Guns-NeoForge-1.21.10/src/main/resources/assets/jeg/textures")

    if not models_dir.exists():
        print(f"Models directory not found: {models_dir}")
        return

    # Collect all existing texture files
    existing_textures = set()
    for texture_file in textures_dir.rglob("*.png"):
        # Get path relative to textures directory
        rel_path = texture_file.relative_to(textures_dir)
        # Remove .png extension and convert forward slashes
        texture_name = str(rel_path)[:-4].replace('\\', '/')
        existing_textures.add(texture_name)

    print(f"Found {len(existing_textures)} existing texture files")

    # Find all model files and extract texture references
    all_texture_refs = {}
    missing_textures = {}

    for model_file in models_dir.rglob("*.json"):
        # Skip the non_animated_bak directory as those are likely backups
        if 'non_animated_bak' in str(model_file):
            continue

        texture_refs = extract_texture_references(model_file)
        if texture_refs:
            model_name = model_file.stem
            all_texture_refs[model_name] = texture_refs

            # Check if each referenced texture exists
            for ref_type, texture_ref in texture_refs:
                # Handle jeg: prefix removal
                if texture_ref.startswith('jeg:'):
                    texture_ref = texture_ref[4:]

                # Handle different texture path formats
                possible_paths = [
                    texture_ref,
                    f"item/{texture_ref}",
                    f"animated/gun/{texture_ref}",
                    f"animated/attachment/{texture_ref}",
                    f"block/{texture_ref}"
                ]

                found = False
                for path in possible_paths:
                    if path in existing_textures:
                        found = True
                        break

                if not found:
                    if texture_ref not in missing_textures:
                        missing_textures[texture_ref] = []
                    missing_textures[texture_ref].append((model_name, ref_type))

    # Print results
    print(f"\n=== TEXTURE REFERENCE ANALYSIS ===")
    print(f"Total models processed: {len(all_texture_refs)}")
    print(f"Total missing textures: {len(missing_textures)}")

    if missing_textures:
        print(f"\n=== MISSING TEXTURES ===")
        for texture_ref, models in sorted(missing_textures.items()):
            print(f"\nMissing texture: '{texture_ref}'")
            for model_name, ref_type in models:
                print(f"  - Referenced by: {model_name} (as {ref_type})")

            # Suggest possible file locations
            print(f"  Expected file locations:")
            print(f"    - Just-Enough-Guns-NeoForge-1.21.10/src/main/resources/assets/jeg/textures/{texture_ref}.png")
            print(f"    - Just-Enough-Guns-NeoForge-1.21.10/src/main/resources/assets/jeg/textures/item/{texture_ref}.png")
            print(f"    - Just-Enough-Guns-NeoForge-1.21.10/src/main/resources/assets/jeg/textures/animated/gun/{texture_ref}.png")
    else:
        print("\n✅ All texture references found!")

    return missing_textures

if __name__ == "__main__":
    find_missing_textures()