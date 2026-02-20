#!/usr/bin/env python3
import os
import json

def diagnose_resource_loading():
    """Diagnose potential resource loading issues"""
    print("Diagnosing resource loading issues...")

    # Check for potential issues with resource files
    assets_dir = "src/main/resources/assets/jeg"

    issues = []

    # Check JSON files for potential issues
    for root, dirs, files in os.walk(assets_dir):
        for file in files:
            if file.endswith('.json'):
                filepath = os.path.join(root, file)
                rel_path = os.path.relpath(filepath, assets_dir)

                try:
                    with open(filepath, 'r', encoding='utf-8') as f:
                        content = f.read()
                        # Basic JSON validation
                        json.loads(content)
                except json.JSONDecodeError as e:
                    issues.append(f"Invalid JSON in {rel_path}: {e}")
                except UnicodeDecodeError as e:
                    issues.append(f"Encoding issue in {rel_path}: {e}")
                except Exception as e:
                    issues.append(f"Error reading {rel_path}: {e}")

    # Check for empty files that might cause issues
    empty_files = []
    for root, dirs, files in os.walk(assets_dir):
        for file in files:
            filepath = os.path.join(root, file)
            rel_path = os.path.relpath(filepath, assets_dir)

            try:
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read().strip()
                    if not content:
                        empty_files.append(rel_path)
            except:
                # Probably binary file
                pass

    if empty_files:
        print(f"\nFound {len(empty_files)} empty files:")
        for f in empty_files[:10]:  # Show first 10
            print(f"  - {f}")
        if len(empty_files) > 10:
            print(f"  ... and {len(empty_files) - 10} more")

    # Check for files with suspicious names or characters
    suspicious_files = []
    for root, dirs, files in os.walk(assets_dir):
        for file in files:
            if any(char in file for char in ['<', '>', ':', '"', '|', '?', '*']):
                rel_path = os.path.relpath(os.path.join(root, file), assets_dir)
                suspicious_files.append(rel_path)

    if suspicious_files:
        print(f"\nFound {len(suspicious_files)} files with suspicious characters:")
        for f in suspicious_files:
            print(f"  - {f}")

    # Check mod metadata files
    mod_files = [
        "src/main/resources/META-INF/mods.toml",
        "src/main/resources/neoforge.mods.toml"
    ]

    print("\nChecking mod metadata files:")
    for mod_file in mod_files:
        if os.path.exists(mod_file):
            print(f"  Found: {mod_file}")
            try:
                with open(mod_file, 'r') as f:
                    content = f.read()
                    if content.strip():
                        print(f"    Size: {len(content)} characters")
                    else:
                        issues.append(f"Empty mod file: {mod_file}")
            except Exception as e:
                issues.append(f"Error reading {mod_file}: {e}")
        else:
            print(f"  Missing: {mod_file}")

    # Check for potential problematic resource patterns
    print("\nChecking for common resource issues...")

    # Check for very large files that might cause loading issues
    large_files = []
    for root, dirs, files in os.walk(assets_dir):
        for file in files:
            filepath = os.path.join(root, file)
            try:
                size = os.path.getsize(filepath)
                if size > 10 * 1024 * 1024:  # 10MB
                    rel_path = os.path.relpath(filepath, assets_dir)
                    large_files.append((rel_path, size))
            except:
                pass

    if large_files:
        print(f"\nFound {len(large_files)} large files (>10MB):")
        for f, size in large_files:
            print(f"  - {f}: {size // (1024*1024)}MB")

    return issues

def main():
    issues = diagnose_resource_loading()

    if issues:
        print(f"\nFound {len(issues)} issues:")
        for issue in issues:
            print(f"  - {issue}")
    else:
        print("\nNo obvious resource file issues found.")

    print("\nResource diagnosis complete.")

if __name__ == "__main__":
    main()