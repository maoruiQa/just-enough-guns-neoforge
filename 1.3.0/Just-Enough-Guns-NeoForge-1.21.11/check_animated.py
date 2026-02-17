#!/usr/bin/env python3
import json
import os
from pathlib import Path

def find_animated_texture_issues():
    """Find files with animated texture references"""
    project_dir = Path('.')
    models_dir = project_dir / 'src' / 'main' / 'resources' / 'assets' / 'jeg' / 'models'

    animated_issues = []

    for model_file in models_dir.rglob('*.json'):
        try:
            with open(model_file, 'r', encoding='utf-8') as f:
                data = json.load(f)

            # Check for animated texture references
            if 'textures' in data:
                textures = data['textures']
                for key, value in textures.items():
                    if '/animated/' in value:
                        relative_path = model_file.relative_to(project_dir)
                        animated_issues.append((str(relative_path), key, value))

        except json.JSONDecodeError:
            continue
        except Exception:
            continue

    return animated_issues

def main():
    animated_issues = find_animated_texture_issues()

    print("=== ANIMATED TEXTURE ISSUES ===")
    if animated_issues:
        print(f"Found {len(animated_issues)} files with animated texture references:")
        for file_path, key, value in animated_issues:
            print(f"  {file_path}: '{key}' -> '{value}'")
    else:
        print("No animated texture issues found.")

if __name__ == "__main__":
    main()