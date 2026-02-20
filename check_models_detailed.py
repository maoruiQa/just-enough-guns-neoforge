import json
import os
import sys
from pathlib import Path

def check_model_files():
    """Check for specific ModelManager crash issues in NeoForge 1.21.10"""
    models_dir = Path("src/main/resources/assets/jeg/models/item")

    print("Checking for ModelManager crash issues...")

    problem_files = []

    for json_file in models_dir.glob("*.json"):
        try:
            with open(json_file, 'r', encoding='utf-8') as f:
                data = json.load(f)

            filename = json_file.name
            issues = []

            # Check for oversized models (can cause memory issues during loading)
            file_size = json_file.stat().st_size
            if file_size > 100 * 1024:  # > 100KB
                issues.append(f"Large file size: {file_size // 1024}KB")

            # Check for excessive elements (can cause performance issues)
            if 'elements' in data:
                elements = data['elements']
                if isinstance(elements, list) and len(elements) > 1000:
                    issues.append(f"Too many elements: {len(elements)}")

            # Check for invalid parent references
            if 'parent' in data:
                parent = data['parent']
                if isinstance(parent, str):
                    # Check for invalid parent format
                    if parent.count('/') > 2:  # Too deep path
                        issues.append(f"Suspicious parent path: {parent}")
                    # Check for non-existent parent files for custom models
                    if parent.startswith('jeg:'):
                        parent_path = parent.replace('jeg:', 'models/')
                        parent_file = models_dir.parent / f"{parent_path}.json"
                        if not parent_file.exists():
                            issues.append(f"Missing parent file: {parent}")

            # Check for texture reference loops
            if 'textures' in data:
                textures = data['textures']
                if isinstance(textures, dict):
                    for tex_name, tex_path in textures.items():
                        if isinstance(tex_path, str) and tex_path.startswith('#'):
                            ref = tex_path[1:]
                            if ref not in textures:
                                issues.append(f"Invalid texture reference: {ref}")

            # Check for malformed display transformations
            if 'display' in data:
                display = data['display']
                if isinstance(display, dict):
                    for disp_name, disp_data in display.items():
                        if isinstance(disp_data, dict):
                            for field in ['translation', 'rotation', 'scale']:
                                if field in disp_data:
                                    values = disp_data[field]
                                    if not (isinstance(values, list) and len(values) == 3):
                                        issues.append(f"Malformed {field} in {disp_name}")
                                    else:
                                        # Check for NaN or infinite values
                                        for v in values:
                                            if isinstance(v, float) and (v != v or abs(v) == float('inf')):
                                                issues.append(f"Invalid {field} value in {disp_name}")

            # Check for elements with invalid coordinates
            if 'elements' in data:
                elements = data['elements']
                if isinstance(elements, list):
                    for i, element in enumerate(elements):
                        if isinstance(element, dict):
                            for coord_field in ['from', 'to']:
                                if coord_field in element:
                                    coords = element[coord_field]
                                    if isinstance(coords, list) and len(coords) == 3:
                                        for coord in coords:
                                            if not isinstance(coord, (int, float)):
                                                issues.append(f"Invalid coordinate in element {i}")
                                            elif abs(coord) > 1000:  # Suspiciously large coordinate
                                                issues.append(f"Suspicious coordinate in element {i}")

            if issues:
                problem_files.append((filename, issues))
                print(f"\n{filename}:")
                for issue in issues:
                    print(f"  - {issue}")

        except json.JSONDecodeError as e:
            problem_files.append((filename, [f"JSON decode error: {e}"]))
            print(f"\n{filename}: JSON decode error - {e}")
        except Exception as e:
            problem_files.append((filename, [f"Processing error: {e}"]))
            print(f"\n{filename}: Processing error - {e}")

    # Find the most problematic files
    if problem_files:
        print(f"\nFound {len(problem_files)} files with issues:")

        # Sort by number of issues
        problem_files.sort(key=lambda x: len(x[1]), reverse=True)

        for filename, issues in problem_files[:10]:  # Top 10
            print(f"  {filename}: {len(issues)} issues")

        # Check for specific patterns that cause ModelManager crashes
        critical_issues = []
        for filename, issues in problem_files:
            for issue in issues:
                if any(keyword in issue.lower() for keyword in ['large file size', 'too many elements', 'json decode', 'invalid coordinate']):
                    critical_issues.append((filename, issue))

        if critical_issues:
            print(f"\nCritical issues (likely causing ModelManager crash):")
            for filename, issue in critical_issues:
                print(f"  {filename}: {issue}")
    else:
        print("No obvious issues found in model files.")

if __name__ == "__main__":
    check_model_files()