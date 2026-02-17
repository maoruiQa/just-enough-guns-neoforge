#!/usr/bin/env python3
import json
import os
import sys

def check_model_file(filepath):
    """Check a model file for issues"""
    try:
        with open(filepath, 'r') as f:
            data = json.load(f)

        issues = []

        # Check if parent exists
        if 'parent' in data:
            parent = data['parent']

            # Handle different parent types
            if parent.startswith('minecraft:'):
                # Minecraft built-in parent, should be fine
                pass
            elif parent.startswith('jeg:'):
                # Custom parent - need to verify it exists
                parent_path = parent.replace('jeg:', '')

                # Try different possible locations
                possible_paths = [
                    f"src/main/resources/assets/jeg/models/{parent_path}.json",
                    f"src/main/resources/assets/jeg/models/{parent_path}",
                    f"src/main/resources/assets/jeg/models/block/{parent_path.replace('block/', '')}.json",
                    f"src/main/resources/assets/jeg/models/item/{parent_path.replace('item/', '')}.json"
                ]

                parent_found = False
                for possible_path in possible_paths:
                    if os.path.exists(possible_path):
                        parent_found = True
                        break

                if not parent_found:
                    issues.append(f"Missing parent: {parent}")
            else:
                # Unknown namespace
                issues.append(f"Unknown parent namespace: {parent}")

        # Check texture references
        if 'textures' in data:
            for texture_name, texture_path in data['textures'].items():
                if texture_path.startswith('jeg:'):
                    tex_path = texture_path.replace('jeg:', '')

                    # Try different possible texture locations
                    possible_tex_paths = [
                        f"src/main/resources/assets/jeg/{tex_path}.png",
                        f"src/main/resources/assets/jeg/{tex_path}",
                    ]

                    tex_found = False
                    for possible_tex_path in possible_tex_paths:
                        if os.path.exists(possible_tex_path):
                            tex_found = True
                            break

                    if not tex_found:
                        issues.append(f"Missing texture: {texture_path}")

        # Check for display issues
        if 'display' in data:
            # Validate display transforms
            for display_name, display_data in data['display'].items():
                if 'translation' in display_data:
                    translation = display_data['translation']
                    if len(translation) != 3:
                        issues.append(f"Invalid translation in display '{display_name}': {translation}")

                if 'scale' in display_data:
                    scale = display_data['scale']
                    if len(scale) != 3:
                        issues.append(f"Invalid scale in display '{display_name}': {scale}")

                if 'rotation' in display_data:
                    rotation = display_data['rotation']
                    if len(rotation) != 3:
                        issues.append(f"Invalid rotation in display '{display_name}': {rotation}")

        return issues

    except json.JSONDecodeError as e:
        return [f"JSON syntax error: {e}"]
    except Exception as e:
        return [f"Error reading file: {e}"]

def main():
    """Main function to check all model files"""
    model_dir = "src/main/resources/assets/jeg/models"

    if not os.path.exists(model_dir):
        print(f"Model directory not found: {model_dir}")
        return

    all_issues = []

    # Walk through all JSON files in the model directory
    for root, dirs, files in os.walk(model_dir):
        for file in files:
            if file.endswith('.json'):
                filepath = os.path.join(root, file)
                issues = check_model_file(filepath)

                if issues:
                    print(f"\nIssues in {filepath}:")
                    for issue in issues:
                        print(f"  - {issue}")
                    all_issues.extend(issues)

    if not all_issues:
        print("No issues found in model files!")
    else:
        print(f"\nTotal issues found: {len(all_issues)}")

        # Try to identify the most critical issues
        critical_issues = [issue for issue in all_issues if 'Missing parent' in issue]
        if critical_issues:
            print(f"\nCritical issues (missing parents): {len(critical_issues)}")
            print("These are likely causing the ModelManager crash!")

if __name__ == "__main__":
    main()