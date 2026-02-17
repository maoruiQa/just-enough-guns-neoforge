#!/usr/bin/env python3
"""
枪支模型自动修复工具
修复NeoForge 1.21.10版本的枪支模型问题
"""

import os
import json
import shutil
from pathlib import Path

def fix_model_file(file_path, reference_path=None):
    """修复单个模型文件"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            model_data = json.load(f)

        changes = []
        original_parent = model_data.get('parent')

        # 备份原文件
        backup_path = file_path.with_suffix('.json.backup')
        if not backup_path.exists():
            shutil.copy2(file_path, backup_path)
            changes.append("已创建备份文件")

        # 1. 修复parent引用
        if 'elements' in model_data:
            # 复杂模型应该使用builtin/entity
            if original_parent != 'builtin/entity':
                model_data['parent'] = 'builtin/entity'
                changes.append(f"将parent从 {original_parent} 改为 builtin/entity")
        else:
            # 简单模型保持原有parent或使用item/generated
            if original_parent == 'minecraft:item/handheld' and 'layer0' not in model_data.get('textures', {}):
                model_data['parent'] = 'item/generated'
                changes.append(f"将parent从 {original_parent} 改为 item/generated")

        # 2. 修复纹理引用
        if 'textures' in model_data:
            textures = model_data['textures']

            # 如果有elements，使用局部纹理引用
            if 'elements' in model_data:
                # 移除layer0格式，使用局部引用
                if 'layer0' in textures:
                    del textures['layer0']
                    changes.append("移除不兼容的layer0纹理引用")

                # 确保有必要的纹理引用
                if not textures:
                    # 尝试从文件名推断纹理
                    file_name = file_path.stem
                    textures['gun'] = f"jeg:item/gun/{file_name}"
                    textures['particle'] = f"jeg:item/gun/{file_name}"
                    changes.append(f"添加默认纹理引用: gun, particle")
            else:
                # 简单模型使用layer0格式
                if 'layer0' not in textures and 'gun' in textures:
                    # 将gun转换为layer0
                    model_data['textures'] = {'layer0': textures['gun']}
                    changes.append("将gun纹理引用转换为layer0格式")

        # 3. 移除不必要的display（如果继承自abstract_gun）
        if 'parent' in model_data and model_data['parent'] == 'jeg:item/abstract_gun':
            if 'display' in model_data:
                del model_data['display']
                changes.append("移除重复的display定��（继承自abstract_gun）")

        # 4. 确保texture_size存在（对于复杂模型）
        if 'elements' in model_data and 'texture_size' not in model_data:
            model_data['texture_size'] = [32, 32]
            changes.append("添加默认texture_size")

        # 5. 检查并修复elements中的纹理引用
        if 'elements' in model_data:
            for i, element in enumerate(model_data['elements']):
                if 'faces' in element:
                    for face_name, face_data in element['faces'].items():
                        if 'texture' in face_data:
                            texture_ref = face_data['texture']
                            if texture_ref.startswith('jeg:'):
                                # 转换为局部引用
                                if texture_ref.startswith('jeg:item/gun/'):
                                    texture_name = texture_ref.replace('jeg:item/gun/', '')
                                    face_data['texture'] = f'#{texture_name}'
                                    changes.append(f"Element {i} face {face_name}: 转换纹理引用为局部格式")

        # 保存修复后的文件
        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump(model_data, f, indent=2, ensure_ascii=False)

        return {
            'file': str(file_path),
            'changes': changes,
            'success': True
        }

    except Exception as e:
        return {
            'file': str(file_path),
            'error': str(e),
            'success': False
        }

def fix_abstract_gun_first():
    """首先修复abstract_gun.json"""
    current_dir = Path("D:/ai-workspace/Just Enough Gun 2/Just-Enough-Guns-NeoForge-1.21.10/src/main/resources/assets/jeg/models/item")
    abstract_gun_path = current_dir / "abstract_gun.json"

    try:
        with open(abstract_gun_path, 'r', encoding='utf-8') as f:
            model_data = json.load(f)

        changes = []

        # 修复abstract_gun的parent
        if model_data.get('parent') != 'builtin/entity':
            model_data['parent'] = 'builtin/entity'
            changes.append("将abstract_gun parent改为builtin/entity")

        # 移除textures（abstract_gun不应该有具体纹理）
        if 'textures' in model_data:
            del model_data['textures']
            changes.append("移除abstract_gun中的textures定义")

        # 保存
        with open(abstract_gun_path, 'w', encoding='utf-8') as f:
            json.dump(model_data, f, indent=2, ensure_ascii=False)

        print(f"[OK] 修复abstract_gun.json: {', '.join(changes)}")
        return True

    except Exception as e:
        print(f"[ERROR] 修复abstract_gun.json失败: {e}")
        return False

def main():
    # 路径配置
    current_dir = Path("D:/ai-workspace/Just Enough Gun 2/Just-Enough-Guns-NeoForge-1.21.10/src/main/resources/assets/jeg/models/item")

    print("=== 枪支模型自动修复工具 ===\n")

    # 首先修复abstract_gun
    print("1. 修复abstract_gun.json...")
    if not fix_abstract_gun_first():
        return

    # 找到所有枪支模型文件
    gun_files = []
    for file_path in current_dir.glob("*.json"):
        if file_path.name == "abstract_gun.json":
            continue

        # 识别枪支文件
        gun_keywords = ['rifle', 'pistol', 'shotgun', 'gun', 'cannon', 'launcher', 'smg', 'mg', 'weapon']
        file_name = file_path.name.lower()
        if any(keyword in file_name for keyword in gun_keywords):
            gun_files.append(file_path)

    print(f"\n2. 开始修复 {len(gun_files)} 个枪支模型文件...\n")

    successful_fixes = 0
    failed_fixes = 0

    for gun_file in gun_files:
        print(f"修复: {gun_file.name}")
        result = fix_model_file(gun_file)

        if result['success']:
            successful_fixes += 1
            print(f"  [OK] {len(result['changes'])} changes applied")
        else:
            failed_fixes += 1
            print(f"  [ERROR] Fix failed: {result['error']}")
        print()

    # 修复其他简单模型的parent问题
    print("3. 修复其他简单模型的parent问题...")
    simple_models = current_dir.glob("*.json")

    for model_file in simple_models:
        if model_file.name in [f.name for f in gun_files] or model_file.name == "abstract_gun.json":
            continue

        try:
            with open(model_file, 'r', encoding='utf-8') as f:
                model_data = json.load(f)

            changes = []
            original_parent = model_data.get('parent')

            # 对于简单物品，使用item/generated而不是handheld
            if original_parent == 'minecraft:item/handheld':
                model_data['parent'] = 'item/generated'
                changes.append(f"将parent从 {original_parent} 改为 item/generated")

                # 保存修改
                with open(model_file, 'w', encoding='utf-8') as f:
                    json.dump(model_data, f, indent=2, ensure_ascii=False)

                if changes:
                    print(f"  [OK] {model_file.name}: {', '.join(changes)}")
                    successful_fixes += 1

        except Exception as e:
            print(f"  [ERROR] {model_file.name}: 处理失败 - {e}")
            failed_fixes += 1

    print("\n=== 修复完成 ===")
    print(f"成功修复: {successful_fixes} 个文件")
    print(f"修复失败: {failed_fixes} 个文件")

    if failed_fixes == 0:
        print("\n[SUCCESS] 所有模型文件都已成功修复！")
        print("\n建议:")
        print("1. 启动游戏测试模型显示")
        print("2. 如果还有问题，检查对应的纹理文件是否存在")
        print("3. 备份文件已保存为 .backup 后缀")
    else:
        print(f"\n[WARNING] 有 {failed_fixes} 个文件修复失败，请手动检查")

if __name__ == "__main__":
    main()