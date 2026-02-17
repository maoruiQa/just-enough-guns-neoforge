#!/usr/bin/env python3
"""
Verification script to check all texture references in Just Enough Guns NeoForge 1.21.10
"""

import os
import json
from pathlib import Path

def check_all_texture_references():
    """Check all texture references in model files"""
    base_path = Path("src/main/resources/assets/jeg")
    models_dir = base_path / "models/item"
    animated_gun_dir = base_path / "textures/animated/gun"
    item_dir = base_path / "textures/item"
    entity_dir = base_path / "textures/entity"

    animated_guns = {f.stem for f in animated_gun_dir.glob("*.png")}
    item_textures = {f.stem for f in item_dir.glob("*.png")}
    entity_textures = {}

    # Collect all entity textures
    for root, dirs, files in os.walk(entity_dir):
        for file in files:
            if file.endswith('.png'):
                entity_textures[file.split('.')[0]] = str(Path(root) / file)

    print(f"Animated gun textures available: {len(animated_guns)}")
    print(f"Item textures available: {len(item_textures)}")
    print(f"Entity textures available: {len(entity_textures)}")

    issues = []

    # Check all model files
    for model_file in models_dir.glob("*.json"):
        try:
            with open(model_file, 'r', encoding='utf-8') as f:
                model_data = json.load(f)

            if 'textures' in model_data:
                for key, texture_path in model_data['textures'].items():
                    if texture_path.startswith('jeg:'):
                        clean_path = texture_path[4:]  # Remove 'jeg:' prefix

                        if clean_path.startswith('animated/gun/'):
                            gun_name = clean_path.split('/')[-1]
                            if gun_name not in animated_guns:
                                issues.append(f"Missing animated texture: {model_file.name} -> {texture_path}")

                        elif clean_path.startswith('item/'):
                            item_name = clean_path.split('/')[-1]
                            if item_name not in item_textures:
                                issues.append(f"Missing item texture: {model_file.name} -> {texture_path}")

                        elif clean_path.startswith('entity/'):
                            # For entity textures, we need to check the full path
                            entity_name = clean_path.replace('/', '_').replace('.png', '')
                            if entity_name not in entity_textures:
                                issues.append(f"Missing entity texture: {model_file.name} -> {texture_path}")

        except Exception as e:
            issues.append(f"Error reading {model_file}: {e}")

    return issues

def check_specific_guns():
    """Check specific guns mentioned in the original error log"""
    base_path = Path("src/main/resources/assets/jeg")
    models_dir = base_path / "models/item"

    problem_guns = [
        'holy_shotgun', 'semi_auto_rifle', 'blossom_rifle', 'light_machine_gun',
        'repeating_shotgun', 'combat_pistol', 'semi_auto_pistol', 'waterpipe_shotgun',
        'infantry_rifle', 'subsonic_rifle', 'finger_gun', 'burst_rifle',
        'grenade_launcher', 'double_barrel_shotgun', 'combat_rifle',
        'assault_rifle', 'bolt_action_rifle', 'pump_shotgun', 'revolver'
    ]

    print("\nChecking specific gun models:")
    for gun in problem_guns:
        model_file = models_dir / f"{gun}.json"
        if model_file.exists():
            try:
                with open(model_file, 'r', encoding='utf-8') as f:
                    model_data = json.load(f)

                if 'textures' in model_data:
                    for texture_path in model_data['textures'].values():
                        print(f"  {gun}: {texture_path}")
                        break
            except:
                print(f"  {gun}: ERROR reading model")
        else:
            print(f"  {gun}: No model file")

def main():
    print("Just Enough Guns - Texture Reference Verification")
    print("=" * 50)

    print("\n1. Checking all texture references...")
    issues = check_all_texture_references()

    if issues:
        print(f"\nFound {len(issues)} texture reference issues:")
        for issue in issues:
            print(f"  - {issue}")
    else:
        print("\nNo texture reference issues found!")

    print("\n2. Checking specific gun models...")
    check_specific_guns()

    print(f"\nVerification complete!")

if __name__ == "__main__":
    main()