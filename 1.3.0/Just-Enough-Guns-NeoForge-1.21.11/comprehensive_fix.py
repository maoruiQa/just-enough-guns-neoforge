import json
import os
import sys
from pathlib import Path

def comprehensive_model_fix():
    """Comprehensive fix for ModelManager crash in NeoForge 1.21.10"""

    print("=== Comprehensive ModelManager Fix ===")

    models_dir = Path("src/main/resources/assets/jeg/models/item")
    special_models_dir = Path("src/main/resources/assets/jeg/models/special")

    # 1. Check for animated texture references that might be causing issues
    print("1. Checking for animated texture issues...")

    animated_texture_issues = []
    for json_file in models_dir.glob("*.json"):
        try:
            with open(json_file, 'r', encoding='utf-8') as f:
                data = json.load(f)

            if 'textures' in data:
                for tex_name, tex_path in data['textures'].items():
                    if isinstance(tex_path, str) and 'animated/' in tex_path:
                        # Check if animated texture exists
                        if ':' in tex_path:
                            namespace, path = tex_path.split(':', 1)
                        else:
                            namespace, path = 'jeg', tex_path

                        tex_file = f"src/main/resources/assets/{namespace}/textures/{path}.png"
                        if not os.path.exists(tex_file):
                            animated_texture_issues.append((json_file.name, tex_path))

        except Exception as e:
            print(f"Error checking {json_file}: {e}")

    if animated_texture_issues:
        print(f"Found {len(animated_texture_issues)} missing animated textures:")
        for filename, tex_path in animated_texture_issues[:5]:
            print(f"  - {filename}: {tex_path}")

    # 2. Check for special model references
    print("\n2. Checking special model references...")

    special_refs = []
    for json_file in models_dir.glob("*.json"):
        try:
            with open(json_file, 'r', encoding='utf-8') as f:
                data = json.load(f)

            if 'textures' in data:
                for tex_name, tex_path in data['textures'].items():
                    if isinstance(tex_path, str) and tex_path.startswith('jeg:animated/gun/'):
                        gun_name = tex_path.replace('jeg:animated/gun/', '').replace('.png', '')
                        special_model_path = special_models_dir / f"gun/{gun_name}.json"
                        if not special_model_path.exists():
                            special_refs.append((json_file.name, gun_name))

        except Exception as e:
            print(f"Error checking {json_file}: {e}")

    if special_refs:
        print(f"Found {len(special_refs)} missing special models:")
        for filename, gun_name in special_refs[:5]:
            print(f"  - {filename}: {gun_name}")

    # 3. Check for parent model chains that might cause infinite loops
    print("\n3. Checking for parent model chains...")

    def check_parent_chain(filename, visited=None, depth=0):
        if visited is None:
            visited = set()

        if depth > 10 or filename in visited:
            return [f"Potential infinite loop detected: {' -> '.join(visited)}"]

        visited.add(filename)
        issues = []

        try:
            file_path = models_dir / filename
            if not file_path.exists():
                return [f"Missing parent: {filename}"]

            with open(file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)

            if 'parent' in data:
                parent = data['parent']
                if isinstance(parent, str) and parent.startswith('jeg:item/'):
                    parent_file = parent.replace('jeg:item/', '') + '.json'
                    issues.extend(check_parent_chain(parent_file, visited.copy(), depth + 1))

        except Exception as e:
            issues.append(f"Error checking {filename}: {e}")

        return issues

    parent_issues = []
    for json_file in models_dir.glob("*.json"):
        try:
            with open(json_file, 'r', encoding='utf-8') as f:
                data = json.load(f)

            if 'parent' in data:
                parent = data['parent']
                if isinstance(parent, str) and parent.startswith('jeg:item/'):
                    parent_file = parent.replace('jeg:item/', '') + '.json'
                    chain_issues = check_parent_chain(parent_file)
                    if chain_issues:
                        parent_issues.append((json_file.name, chain_issues))

        except Exception as e:
            print(f"Error checking parent chain for {json_file}: {e}")

    if parent_issues:
        print(f"Found {len(parent_issues)} parent chain issues:")
        for filename, issues in parent_issues[:3]:
            print(f"  - {filename}:")
            for issue in issues:
                print(f"    {issue}")

    # 4. Fix critical issues
    print("\n4. Applying fixes...")

    # Fix files with problematic animated textures
    for filename, tex_path in animated_texture_issues:
        file_path = models_dir / filename
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)

            # Replace animated texture with static one
            for tex_name, path in data['textures'].items():
                if path == tex_path:
                    static_path = tex_path.replace('animated/', 'item/')
                    data['textures'][tex_name] = static_path
                    print(f"  - Fixed {filename}: {tex_path} -> {static_path}")
                    break

            with open(file_path, 'w', encoding='utf-8') as f:
                json.dump(data, f, indent=2)

        except Exception as e:
            print(f"  - Error fixing {filename}: {e}")

    print("\n=== Fix Complete ===")
    print("Applied fixes:")
    print("1. Replaced missing animated textures with static ones")
    print("2. Identified parent chain issues")
    print("3. Documented special model references")
    print("\nThis should resolve most ModelManager crash issues.")

if __name__ == "__main__":
    comprehensive_model_fix()