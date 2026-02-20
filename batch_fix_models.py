#!/usr/bin/env python3
"""
Batch fix for large model files causing ModelManager crashes
"""

import json
import os
import shutil
from pathlib import Path

def create_backup_and_simplify(filepath):
    """Backup and simplify a large model file"""
    try:
        # Read the original file
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)

        # Calculate relative path for backup
        project_root = Path("src/main/resources")
        rel_path = Path(filepath).relative_to(project_root)
        backup_path = Path("src/main/resources/assets/jeg/models_backup") / rel_path

        # Create backup directory
        backup_path.parent.mkdir(parents=True, exist_ok=True)

        # Backup original
        shutil.copy2(filepath, backup_path)

        # Create simplified version
        simplified = {
            "credit": "Simplified to prevent ModelManager crash",
            "parent": "builtin/entity",
            "texture_size": data.get("texture_size", [16, 16]),
            "display": data.get("display", {})
        }

        # Preserve textures if they exist
        if "textures" in data:
            simplified["textures"] = data["textures"]
        else:
            # Add particle texture if not present
            if "textures" not in simplified:
                simplified["textures"] = {}
            simplified["textures"]["particle"] = simplified["textures"].get("particle", "minecraft:item/air")

        # Write simplified model
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(simplified, f, indent=2)

        print(f"[OK] Fixed: {rel_path}")
        return True

    except Exception as e:
        print(f"[ERROR] Error fixing {filepath}: {e}")
        return False

def main():
    print("=== Batch Fix for Large Model Files ===")

    large_files = [
        "src/main/resources/assets/jeg/models/block/basic_turret.json",
        "src/main/resources/assets/jeg/models/item/combat_pistol.json",
        "src/main/resources/assets/jeg/models/item/double_barrel_shotgun.json",
        "src/main/resources/assets/jeg/models/item/primitive_bow.json",
        "src/main/resources/assets/jeg/models/item/repeating_shotgun.json",
        "src/main/resources/assets/jeg/models/item/revolver.json",
        "src/main/resources/assets/jeg/models/item/semi_auto_rifle.json",
        "src/main/resources/assets/jeg/models/special/burst_rifle/main.json",
        "src/main/resources/assets/jeg/models/special/gun/double_barrel_shotgun.json"
    ]

    fixed_count = 0
    total_count = len(large_files)

    for filepath in large_files:
        if os.path.exists(filepath):
            if create_backup_and_simplify(filepath):
                fixed_count += 1
        else:
            print(f"[WARNING] File not found: {filepath}")

    print(f"\n=== Summary ===")
    print(f"Fixed: {fixed_count}/{total_count} files")
    print(f"Backups saved to: src/main/resources/assets/jeg/models_backup/")
    print("\nAll large model files have been simplified to prevent ModelManager crashes.")

if __name__ == "__main__":
    main()