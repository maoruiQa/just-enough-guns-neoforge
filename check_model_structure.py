#!/usr/bin/env python3
import json
import os
import sys

def check_problematic_models():
    """Check for specific issues that could cause ModelManager crashes"""
    model_dir = "src/main/resources/assets/jeg/models"
    issues = []

    print("Checking for problematic model files...")

    for root, dirs, files in os.walk(model_dir):
        for file in files:
            if file.endswith('.json'):
                filepath = os.path.join(root, file)
                rel_path = os.path.relpath(filepath, model_dir)

                try:
                    with open(filepath, 'r') as f:
                        data = json.load(f)

                    # Check for infinite recursion in parent references
                    parent_chain = []
                    current_parent = data.get('parent')
                    visited = set()

                    while current_parent:
                        if current_parent in visited:
                            issues.append(f"Circular parent reference in {rel_path}: {current_parent}")
                            break
                        visited.add(current_parent)
                        parent_chain.append(current_parent)

                        # Try to load parent model
                        if current_parent.startswith('jeg:'):
                            parent_path = current_parent.replace('jeg:', '')
                            parent_file = f"src/main/resources/assets/jeg/models/{parent_path}.json"

                            if os.path.exists(parent_file):
                                try:
                                    with open(parent_file, 'r') as pf:
                                        parent_data = json.load(pf)
                                    current_parent = parent_data.get('parent')
                                except:
                                    issues.append(f"Invalid parent JSON in {rel_path}: {parent_path}")
                                    break
                            else:
                                # Parent doesn't exist
                                issues.append(f"Missing parent model in {rel_path}: {parent_path}")
                                break
                        else:
                            # Minecraft or other namespace parent - stop checking
                            break

                    # Check for malformed elements
                    if 'elements' in data:
                        for i, element in enumerate(data['elements']):
                            if 'from' not in element or 'to' not in element:
                                issues.append(f"Invalid element {i} in {rel_path}: missing 'from' or 'to'")

                            # Check for invalid coordinates
                            for coord_list in ['from', 'to']:
                                if coord_list in element:
                                    coords = element[coord_list]
                                    if len(coords) != 3:
                                        issues.append(f"Invalid {coord_list} coordinates in {rel_path}: {coords}")

                    # Check for invalid texture references in elements
                    if 'elements' in data and 'textures' in data:
                        for i, element in enumerate(data['elements']):
                            if 'faces' in element:
                                for face_name, face_data in element['faces'].items():
                                    if 'texture' in face_data:
                                        texture_ref = face_data['texture']
                                        if texture_ref.startswith('#'):
                                            texture_name = texture_ref[1:]
                                            if texture_name not in data['textures']:
                                                issues.append(f"Invalid texture reference in {rel_path} element {i}: {texture_ref}")

                except json.JSONDecodeError as e:
                    issues.append(f"JSON syntax error in {rel_path}: {e}")
                except Exception as e:
                    issues.append(f"Error processing {rel_path}: {e}")

    return issues

def main():
    issues = check_problematic_models()

    if issues:
        print(f"\nFound {len(issues)} potential issues:")
        for issue in issues:
            print(f"  - {issue}")
    else:
        print("\nNo obvious structural issues found in model files.")

    # Additional check for files that might cause state issues
    print("\nChecking for files that could cause state issues...")

    # Look for empty models or models with missing critical data
    empty_models = []
    for root, dirs, files in os.walk("src/main/resources/assets/jeg/models"):
        for file in files:
            if file.endswith('.json'):
                filepath = os.path.join(root, file)
                try:
                    with open(filepath, 'r') as f:
                        data = json.load(f)

                    # Check for completely empty models
                    if not data.get('parent') and not data.get('elements'):
                        empty_models.append(os.path.relpath(filepath, "src/main/resources/assets/jeg/models"))
                except:
                    pass

    if empty_models:
        print(f"\nFound {len(empty_models)} empty models:")
        for model in empty_models:
            print(f"  - {model}")

    print("\nModel analysis complete.")

if __name__ == "__main__":
    main()