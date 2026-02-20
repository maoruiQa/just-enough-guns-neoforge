#!/usr/bin/env python3
"""
Fix Happy Ghast Armor Harness texture references to use correct variants
"""

import json
from pathlib import Path

def fix_happy_ghast_harnesses():
    """Fix Happy Ghast harness models to use correct texture variants"""
    base_path = Path("src/main/resources/assets/jeg")
    models_dir = base_path / "models/item"

    harness_colors = [
        'black', 'blue', 'brown', 'cyan', 'gray', 'green', 'light_blue',
        'light_gray', 'lime', 'magenta', 'orange', 'pink', 'purple',
        'red', 'white', 'yellow'
    ]

    harness_types = ['', '_diamond', '_netherite']

    fixed_count = 0

    for color in harness_colors:
        for harness_type in harness_types:
            if harness_type == '':
                model_name = f"armored_joy_harness_{color}"
                texture_name = f"{color}_harness"
            else:
                model_name = f"armored_joy_harness_{color}{harness_type}"
                texture_name = f"{color}{harness_type}_harness"

            model_file = models_dir / f"{model_name}.json"

            if model_file.exists():
                try:
                    with open(model_file, 'r', encoding='utf-8') as f:
                        model_data = json.load(f)

                    # Check if texture reference needs fixing
                    if 'textures' in model_data and 'layer0' in model_data['textures']:
                        current_texture = model_data['textures']['layer0']
                        expected_texture = f"jeg:entity/equipment/happy_ghast_body/{texture_name}"

                        if current_texture != expected_texture:
                            print(f"Fixing {model_name}: {current_texture} -> {expected_texture}")
                            model_data['textures']['layer0'] = expected_texture

                            with open(model_file, 'w', encoding='utf-8') as f:
                                json.dump(model_data, f, indent=2)
                            fixed_count += 1
                        else:
                            print(f"Already correct: {model_name}")

                except Exception as e:
                    print(f"Error processing {model_name}: {e}")
            else:
                print(f"Model not found: {model_name}")

    return fixed_count

def main():
    print("Fixing Happy Ghast Armor Harness texture references...")
    print("=" * 50)

    fixed_count = fix_happy_ghast_harnesses()

    print(f"\nFixed {fixed_count} Happy Ghast harness models")

if __name__ == "__main__":
    main()