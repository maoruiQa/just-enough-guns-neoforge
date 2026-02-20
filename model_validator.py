import json
import os
import sys
from pathlib import Path

def validate_and_fix_model_file(file_path):
    """Validate and fix common issues in JSON model files"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)

        issues = []
        fixed = False

        # Fix parent namespace
        if 'parent' in data:
            parent = data['parent']
            if isinstance(parent, str) and ':' not in parent:
                data['parent'] = 'minecraft:' + parent
                fixed = True
                issues.append(f"Fixed parent namespace: {data['parent']}")

        # Validate textures
        if 'textures' in data:
            textures = data['textures']
            if isinstance(textures, dict):
                # Check for invalid texture references
                for tex_name, tex_path in list(textures.items()):
                    if isinstance(tex_path, str) and tex_path.startswith('#'):
                        ref = tex_path[1:]
                        if ref not in textures:
                            # Remove invalid texture reference
                            del textures[tex_name]
                            fixed = True
                            issues.append(f"Removed invalid texture reference: {ref}")

        # Fix display transformations
        if 'display' in data:
            display = data['display']
            if isinstance(display, dict):
                for disp_name, disp_data in display.items():
                    if isinstance(disp_data, dict):
                        for field in ['translation', 'rotation', 'scale']:
                            if field in disp_data:
                                values = disp_data[field]
                                if isinstance(values, list) and len(values) == 3:
                                    # Ensure all values are numbers
                                    fixed_values = []
                                    for v in values:
                                        if isinstance(v, (int, float)):
                                            fixed_values.append(v)
                                        else:
                                            try:
                                                fixed_values.append(float(v))
                                            except:
                                                fixed_values.append(0)
                                    if fixed_values != values:
                                        disp_data[field] = fixed_values
                                        fixed = True
                                        issues.append(f"Fixed {field} values in {disp_name}")

        # Validate elements
        if 'elements' in data:
            elements = data['elements']
            if isinstance(elements, list):
                valid_elements = []
                for i, element in enumerate(elements):
                    if isinstance(element, dict):
                        # Check required fields
                        if 'from' in element and 'to' in element:
                            if (isinstance(element['from'], list) and len(element['from']) == 3 and
                                isinstance(element['to'], list) and len(element['to']) == 3):
                                valid_elements.append(element)
                            else:
                                issues.append(f"Removed invalid element {i} (invalid from/to)")
                                fixed = True
                        else:
                            issues.append(f"Removed invalid element {i} (missing from/to)")
                            fixed = True
                    else:
                        issues.append(f"Removed invalid element {i} (not a dict)")
                        fixed = True

                if len(valid_elements) != len(elements):
                    data['elements'] = valid_elements
                    fixed = True

        # Write fixes back to file
        if fixed:
            with open(file_path, 'w', encoding='utf-8') as f:
                json.dump(data, f, indent=2)
            print(f"FIXED {file_path}:")
            for issue in issues:
                print(f"  - {issue}")
            return True

        return False

    except json.JSONDecodeError as e:
        print(f"JSON ERROR in {file_path}: {e}")
        return False
    except Exception as e:
        print(f"ERROR processing {file_path}: {e}")
        return False

def main():
    """Main validation function"""
    models_dir = Path("src/main/resources/assets/jeg/models/item")

    if not models_dir.exists():
        print(f"Models directory not found: {models_dir}")
        return

    print("Validating and fixing model files...")

    total_files = 0
    fixed_files = 0
    error_files = 0

    for json_file in models_dir.glob("*.json"):
        total_files += 1
        if validate_and_fix_model_file(json_file):
            fixed_files += 1

    print(f"\nValidation complete:")
    print(f"  Total files: {total_files}")
    print(f"  Fixed files: {fixed_files}")
    print(f"  Error files: {error_files}")

if __name__ == "__main__":
    main()