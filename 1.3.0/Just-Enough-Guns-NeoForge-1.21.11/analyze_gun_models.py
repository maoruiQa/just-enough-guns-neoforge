#!/usr/bin/env python3
"""
枪支模型修复工具
分析和修复NeoForge 1.21.10版本的枪支模型问题
"""

import os
import json
import shutil
from pathlib import Path

def analyze_model_file(file_path):
    """分析单个模型文件"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            model_data = json.load(f)

        issues = []
        recommendations = []

        # 检查parent引用
        if 'parent' in model_data:
            parent = model_data['parent']
            if parent == 'minecraft:item/handheld':
                # 对于复杂模型，handheld可能不合适
                if 'elements' in model_data and len(model_data.get('elements', [])) > 0:
                    issues.append("使用handheld parent但包含elements - 可能导致显示问题")
                    recommendations.append("考虑使用builtin/entity或移除elements")
            elif parent == 'builtin/entity':
                if 'elements' not in model_data:
                    recommendations.append("builtin/entity模型通常需要elements定义")
                elif 'textures' in model_data and 'layer0' in model_data['textures']:
                    issues.append("builtin/entity通常不使用layer0纹理格式")

        # 检查纹理引用
        if 'textures' in model_data:
            textures = model_data['textures']
            for key, value in textures.items():
                if key == 'layer0' and 'elements' in model_data:
                    issues.append(f"layer0纹理格式与elements不兼容: {value}")
                elif not value.startswith('jeg:'):
                    recommendations.append(f"纹理引用应使用命名空间前缀: {value}")

        # 检查elements结构
        if 'elements' in model_data:
            elements = model_data['elements']
            for i, element in enumerate(elements):
                if 'faces' in element:
                    faces = element['faces']
                    for face_name, face_data in faces.items():
                        if 'texture' in face_data:
                            texture_ref = face_data['texture']
                            if not texture_ref.startswith('#'):
                                issues.append(f"Element {i} face {face_name} 使用直接纹理引用而非局部引用: {texture_ref}")

        return {
            'file': str(file_path),
            'issues': issues,
            'recommendations': recommendations,
            'has_elements': 'elements' in model_data,
            'parent': model_data.get('parent', 'none'),
            'texture_format': 'layer0' if 'textures' in model_data and 'layer0' in model_data.get('textures', {}) else 'custom'
        }

    except Exception as e:
        return {
            'file': str(file_path),
            'issues': [f"无法解析文件: {e}"],
            'recommendations': [],
            'error': True
        }

def compare_with_reference(model_path, reference_path):
    """与参考文件对比"""
    if not os.path.exists(reference_path):
        return None

    try:
        with open(model_path, 'r', encoding='utf-8') as f:
            model_data = json.load(f)
        with open(reference_path, 'r', encoding='utf-8') as f:
            ref_data = json.load(f)

        differences = []

        # 检查parent差异
        model_parent = model_data.get('parent')
        ref_parent = ref_data.get('parent')
        if model_parent != ref_parent:
            differences.append(f"parent: {model_parent} vs {ref_parent}")

        # 检查纹理格式差异
        model_textures = model_data.get('textures', {})
        ref_textures = ref_data.get('textures', {})
        if list(model_textures.keys()) != list(ref_textures.keys()):
            differences.append(f"texture keys: {list(model_textures.keys())} vs {list(ref_textures.keys())}")

        return differences

    except Exception as e:
        return [f"对比失败: {e}"]

def main():
    # 路径配置
    current_dir = Path("D:/ai-workspace/Just Enough Gun 2/Just-Enough-Guns-NeoForge-1.21.10/src/main/resources/assets/jeg/models/item")
    reference_dir = Path("D:/ai-workspace/Just Enough Gun 2/Just-Enough-Guns/src/main/resources/assets/jeg/models/item")

    print("=== 枪支模型分析报告 ===\n")

    # 分析所有模型文件
    gun_files = []
    all_files = []

    for file_path in current_dir.glob("*.json"):
        all_files.append(file_path)

        # 识别枪支文件
        gun_keywords = ['rifle', 'pistol', 'shotgun', 'gun', 'cannon', 'launcher', 'smg', 'mg', 'weapon']
        file_name = file_path.name.lower()
        if any(keyword in file_name for keyword in gun_keywords):
            gun_files.append(file_path)

    print(f"发现 {len(all_files)} 个模型文件，其中 {len(gun_files)} 个枪支模型\n")

    # 分析枪支模型
    gun_analysis = []
    problematic_guns = []

    for gun_file in gun_files:
        print(f"分析: {gun_file.name}")
        analysis = analyze_model_file(gun_file)

        # 与参考版本对比
        ref_file = reference_dir / gun_file.name
        differences = compare_with_reference(gun_file, ref_file)
        if differences:
            analysis['differences_from_reference'] = differences

        gun_analysis.append(analysis)

        if analysis['issues'] or analysis.get('differences_from_reference'):
            problematic_guns.append(analysis)
            print(f"  [!] 发现问题:")
            for issue in analysis['issues']:
                print(f"    - {issue}")
            for diff in analysis.get('differences_from_reference', []):
                print(f"    - 版本差异: {diff}")

        if analysis['recommendations']:
            print(f"  [i] 建议:")
            for rec in analysis['recommendations']:
                print(f"    - {rec}")
        print()

    # 统计信息
    print("=== 统计信息 ===")
    print(f"问题模型数量: {len(problematic_guns)}/{len(gun_files)}")

    # 按问题类型分类
    parent_issues = sum(1 for gun in problematic_guns if 'handheld' in str(gun.get('issues', [])))
    texture_issues = sum(1 for gun in problematic_guns if 'layer0' in str(gun.get('issues', [])))
    element_issues = sum(1 for gun in problematic_guns if 'Element' in str(gun.get('issues', [])))

    print(f"Parent相关问题: {parent_issues}")
    print(f"纹理格式问题: {texture_issues}")
    print(f"Elements问题: {element_issues}")

    # 生成修复建议
    print("\n=== 修复建议 ===")

    # 1. Parent修复
    handheld_to_entity = [gun for gun in problematic_guns if gun.get('parent') == 'minecraft:item/handheld' and gun.get('has_elements')]
    if handheld_to_entity:
        print(f"1. 需要将{len(handheld_to_entity)}个模型从handheld改为builtin/entity:")
        for gun in handheld_to_entity[:5]:  # 只显示前5个
            print(f"   - {Path(gun['file']).name}")
        if len(handheld_to_entity) > 5:
            print(f"   - ... 还有{len(handheld_to_entity) - 5}个")

    # 2. 纹理修复
    texture_problems = [gun for gun in problematic_guns if 'layer0' in str(gun.get('issues', []))]
    if texture_problems:
        print(f"\n2. 需要修复{len(texture_problems)}个模型的纹理格式:")
        for gun in texture_problems[:5]:
            print(f"   - {Path(gun['file']).name}")
        if len(texture_problems) > 5:
            print(f"   - ... 还有{len(texture_problems) - 5}个")

    # 保存详细报告
    report_file = current_dir.parent.parent / "model_analysis_report.json"
    with open(report_file, 'w', encoding='utf-8') as f:
        json.dump({
            'summary': {
                'total_models': len(all_files),
                'gun_models': len(gun_files),
                'problematic_models': len(problematic_guns)
            },
            'detailed_analysis': gun_analysis
        }, f, indent=2, ensure_ascii=False)

    print(f"\n详细分析报告已保存到: {report_file}")

    return problematic_guns

if __name__ == "__main__":
    problematic_models = main()

    if problematic_models:
        print(f"\n发现 {len(problematic_models)} 个问题模型需要修复。")
        print("建议运行 fix_models.py 来自动修复这些问题。")
    else:
        print("\n所有模型文件看起来都正常！")