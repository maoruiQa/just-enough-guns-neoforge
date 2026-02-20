#!/usr/bin/env python3
"""
Restore 3D gun models from backups for Minecraft 1.21.1.
Removes the unsupported 'builtin/entity' parent while keeping 3D geometry.
"""
import json
import os
from pathlib import Path

def restore_3d_model(backup_path, output_path):
    """
    Restore 3D model from backup, removing builtin/entity parent.
    In 1.21+, we can use 3D elements without a parent for items.
    """
    try:
        with open(backup_path, 'r', encoding='utf-8') as f:
            data = json.load(f)

        # Remove builtin/entity parent if it exists
        if 'parent' in data and data['parent'] == 'builtin/entity':
            del data['parent']

        # Ensure we have elements (3D geometry)
        if 'elements' not in data:
            print(f"Warning: {backup_path.name} has no elements, skipping")
            return False

        # Write the restored model
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write('\n')

        return True
    except Exception as e:
        print(f"Error processing {backup_path.name}: {e}")
        return False

def main():
    """Restore all gun models from backups."""
    models_dir = Path(__file__).parent / 'src' / 'main' / 'resources' / 'assets' / 'jeg' / 'models' / 'item'

    if not models_dir.exists():
        print(f"Error: Models directory not found: {models_dir}")
        return

    print(f"Restoring 3D gun models from backups in: {models_dir}")

    restored_count = 0
    skipped_count = 0

    # Find all .bak files
    for backup_file in sorted(models_dir.glob('*.json.bak')):
        # Skip old backups
        if backup_file.name.endswith('_old.json.bak') or backup_file.name.endswith('.bak2'):
            continue

        # Get the original file name
        original_name = backup_file.name.replace('.json.bak', '.json')
        original_path = models_dir / original_name

        print(f"Processing: {original_name}...", end=' ')

        if restore_3d_model(backup_file, original_path):
            print("[OK]")
            restored_count += 1
        else:
            print("[SKIP]")
            skipped_count += 1

    print(f"\n{'='*60}")
    print(f"Summary:")
    print(f"  Restored: {restored_count}")
    print(f"  Skipped: {skipped_count}")
    print(f"  Total: {restored_count + skipped_count}")
    print(f"{'='*60}")

if __name__ == '__main__':
    main()
