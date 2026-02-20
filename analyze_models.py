#!/usr/bin/env python3
import json
import os
import re
from pathlib import Path

def check_texture_references(file_path):
    """Check a JSON model file for problematic texture references"""
    issues = []

    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)

        # Check texture references
        if 'textures' in data:
            textures = data['textures']
            for key, value in textures.items():
                # Check for numeric texture names (invalid)
                if re.match(r'^\d+$', key):
                    issues.append(f"Invalid numeric texture name: '{key}'")

                # Check for problematic texture paths
                if '/animated/' in value:
                    issues.append(f"Invalid animated texture path: '{value}'")
                elif value.endswith('#missing'):
                    issues.append(f"Missing texture reference: '{value}'")
                elif value.endswith('#texture'):
                    issues.append(f"Generic texture reference: '{value}'")
                elif not value.startswith('jeg:') and not value.startswith('minecraft:'):
                    issues.append(f"Missing namespace in texture: '{value}'")

        # Check parent references
        if 'parent' in data:
            parent = data['parent']
            if parent.startswith('item/') or parent.startswith('block/'):
                issues.append(f"Missing namespace in parent: '{parent}'")
            elif parent == 'builtin/generated':
                issues.append(f"Deprecated parent reference: '{parent}'")

        # Check for invalid texture references in elements
        if 'elements' in data:
            for element in data['elements']:
                if 'faces' in element:
                    for face_name, face_data in element['faces'].items():
                        if isinstance(face_data, dict) and 'texture' in face_data:
                            texture_ref = face_data['texture']
                            if texture_ref.startswith('#'):
                                texture_var = texture_ref[1:]
                                if re.match(r'^\d+$', texture_var):
                                    issues.append(f"Invalid numeric texture variable in face: '{texture_ref}'")
                                elif texture_var not in textures:
                                    issues.append(f"Undefined texture variable in face: '{texture_ref}'")

    except json.JSONDecodeError as e:
        issues.append(f"JSON parsing error: {e}")
    except Exception as e:
        issues.append(f"File reading error: {e}")

    return issues

def main():
    project_dir = Path('.')
    models_dir = project_dir / 'src' / 'main' / 'resources' / 'assets' / 'jeg' / 'models'

    if not models_dir.exists():
        print(f"Models directory not found: {models_dir}")
        return

    all_issues = {}
    total_files = 0

    # Find all JSON model files
    model_files = list(models_dir.rglob('*.json'))
    print(f"Found {len(model_files)} model files")

    for model_file in model_files:
        relative_path = model_file.relative_to(project_dir)
        issues = check_texture_references(model_file)

        if issues:
            all_issues[str(relative_path)] = issues
            total_files += 1

    # Print results
    print(f"\n=== MODEL FILE ANALYSIS RESULTS ===")
    print(f"Total problematic files: {total_files}")
    print(f"Total issues found: {sum(len(issues) for issues in all_issues.values())}")

    if all_issues:
        print(f"\n=== PROBLEMATIC FILES ===")
        for file_path, issues in sorted(all_issues.items()):
            print(f"\n{file_path}:")
            for issue in issues:
                print(f"  - {issue}")
    else:
        print("\nNo issues found!")

if __name__ == "__main__":
    main()