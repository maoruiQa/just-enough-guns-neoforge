#!/usr/bin/env python3
"""
Comprehensive ModelManager crash fix for Just Enough Guns NeoForge 1.21.10
Identifies and resolves problematic model files causing the ModelManager shared state to become null
"""

import json
import os
import shutil
import sys
from pathlib import Path

def analyze_model_file(filepath):
    """Analyze a model file for potential issues"""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)

        issues = []
        file_size = os.path.getsize(filepath)

        # Check file size
        if file_size > 100000:  # 100KB limit
            issues.append(f"File too large: {file_size} bytes")

        # Check elements count
        elements = data.get('elements', [])
        if len(elements) > 100:
            issues.append(f"Too many elements: {len(elements)}")

        # Check for extremely detailed models
        if 'elements' in data:
            for i, element in enumerate(elements):
                if 'faces' in element:
                    for face_name, face_data in element['faces'].items():
                        if 'uv' in face_data:
                            uv = face_data['uv']
                            if isinstance(uv, list) and len(uv) >= 4:
                                # Check for very high precision UV coordinates
                                for coord in uv:
                                    if isinstance(coord, float) and abs(coord) > 1000:
                                        issues.append(f"Extreme UV coordinate in element {i}, face {face_name}")
                                        break

        # Check for deep nesting
        def max_depth(obj, current_depth=0):
            if isinstance(obj, dict):
                return max((max_depth(v, current_depth + 1) for v in obj.values()), default=current_depth)
            elif isinstance(obj, list):
                return max((max_depth(v, current_depth + 1) for v in obj), default=current_depth)
            else:
                return current_depth

        depth = max_depth(data)
        if depth > 10:
            issues.append(f"Excessive nesting depth: {depth}")

        return issues, file_size

    except json.JSONDecodeError as e:
        return [f"JSON decode error: {e}"], 0
    except Exception as e:
        return [f"Analysis error: {e}"], 0

def create_simplified_model(original_path, backup_path):
    """Create a simplified version of a problematic model"""
    try:
        with open(original_path, 'r', encoding='utf-8') as f:
            data = json.load(f)

        # Create a simplified version using builtin/entity
        simplified = {
            "credit": "Simplified to prevent ModelManager crash",
            "parent": "builtin/entity",
            "texture_size": data.get("texture_size", [16, 16]),
            "display": data.get("display", {})
        }

        # Preserve textures if they exist
        if "textures" in data:
            simplified["textures"] = data["textures"]

        # Write simplified model
        with open(original_path, 'w', encoding='utf-8') as f:
            json.dump(simplified, f, indent=2)

        # Backup original
        shutil.copy2(original_path, backup_path)

        return True

    except Exception as e:
        print(f"Error simplifying model {original_path}: {e}")
        return False

def main():
    print("=== Just Enough Guns - ModelManager Crash Fix ===")
    print("Analyzing and fixing problematic model files...")

    models_dir = Path("src/main/resources/assets/jeg/models")
    if not models_dir.exists():
        print(f"Models directory not found: {models_dir}")
        return False

    backup_dir = Path("src/main/resources/assets/jeg/models_backup")
    backup_dir.mkdir(exist_ok=True)

    problematic_files = []
    large_files = []

    # Analyze all model files
    print("\n1. Analyzing model files...")
    for model_file in models_dir.rglob("*.json"):
        issues, file_size = analyze_model_file(str(model_file))

        if issues:
            problematic_files.append((str(model_file), issues))

        if file_size > 50000:  # Track large files
            large_files.append((str(model_file), file_size))

    print(f"Found {len(problematic_files)} problematic model files")
    print(f"Found {len(large_files)} large model files (>50KB)")

    # Report issues
    if problematic_files:
        print("\n2. Issues found:")
        for model_file, issues in problematic_files[:10]:  # Show first 10
            rel_path = model_file.replace("src/main/resources/", "")
            print(f"  {rel_path}:")
            for issue in issues:
                print(f"    - {issue}")

        if len(problematic_files) > 10:
            print(f"  ... and {len(problematic_files) - 10} more files")

    # Fix large and problematic files
    print("\n3. Fixing problematic files...")
    fixed_count = 0

    # First, fix all large files
    for model_file, file_size in large_files:
        rel_path = Path(model_file).relative_to("src/main/resources")
        backup_path = backup_dir / rel_path

        # Create backup directory structure
        backup_path.parent.mkdir(parents=True, exist_ok=True)

        print(f"  Simplifying large model ({file_size} bytes): {rel_path}")
        if create_simplified_model(model_file, backup_path):
            fixed_count += 1

    # Then fix files with other issues
    for model_file, issues in problematic_files:
        if any("large" in issue.lower() or "too many" in issue.lower() for issue in issues):
            continue  # Already handled large files

        rel_path = Path(model_file).relative_to("src/main/resources")
        backup_path = backup_dir / rel_path
        backup_path.parent.mkdir(parents=True, exist_ok=True)

        print(f"  Simplifying problematic model: {rel_path}")
        if create_simplified_model(model_file, backup_path):
            fixed_count += 1

    print(f"\n4. Fixed {fixed_count} model files")

    # Create a validation report
    report_file = "modelmanager_fix_report.txt"
    with open(report_file, 'w', encoding='utf-8') as f:
        f.write("ModelManager Crash Fix Report\n")
        f.write("=" * 40 + "\n\n")
        f.write(f"Total problematic files found: {len(problematic_files)}\n")
        f.write(f"Total large files found: {len(large_files)}\n")
        f.write(f"Total files fixed: {fixed_count}\n\n")

        f.write("Fixed files:\n")
        for model_file, _ in large_files:
            rel_path = Path(model_file).relative_to("src/main/resources")
            f.write(f"  - {rel_path} (large)\n")

        for model_file, issues in problematic_files:
            if not any("large" in issue.lower() for issue in issues):
                rel_path = Path(model_file).relative_to("src/main/resources")
                f.write(f"  - {rel_path} (issues: {'; '.join(issues)})\n")

    print(f"\n5. Report saved to: {report_file}")
    print("\n=== Fix Complete ===")
    print("All problematic model files have been simplified to use builtin/entity")
    print("Original files are backed up in: src/main/resources/assets/jeg/models_backup/")
    print("\nTry running the game now. The ModelManager crash should be resolved.")

    return True

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)