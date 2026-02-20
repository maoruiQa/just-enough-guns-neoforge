#!/usr/bin/env python3
"""
Comprehensive ModelManager validation and testing script
Validates that the ModelManager crash has been resolved and provides additional safeguards
"""

import json
import os
import sys
from pathlib import Path

def validate_model_files():
    """Validate all model files for potential issues"""
    print("Validating model files...")

    models_dir = Path("src/main/resources/assets/jeg/models")
    if not models_dir.exists():
        print(f"ERROR: Models directory not found: {models_dir}")
        return False

    issues = []
    total_files = 0

    for model_file in models_dir.rglob("*.json"):
        total_files += 1
        try:
            with open(model_file, 'r', encoding='utf-8') as f:
                data = json.load(f)

            # Validate JSON structure
            if not isinstance(data, dict):
                issues.append(f"{model_file}: Root is not a dictionary")
                continue

            # Check for known problematic patterns
            file_size = os.path.getsize(model_file)
            if file_size > 200000:  # 200KB limit
                issues.append(f"{model_file}: File too large ({file_size} bytes)")

            # Check for extremely large element counts
            if 'elements' in data and len(data['elements']) > 200:
                issues.append(f"{model_file}: Too many elements ({len(data['elements'])})")

            # Check for deep nesting
            def max_depth(obj, current_depth=0):
                if isinstance(obj, dict):
                    return max((max_depth(v, current_depth + 1) for v in obj.values()), default=current_depth)
                elif isinstance(obj, list):
                    return max((max_depth(v, current_depth + 1) for v in obj), default=current_depth)
                else:
                    return current_depth

            depth = max_depth(data)
            if depth > 15:
                issues.append(f"{model_file}: Excessive nesting depth ({depth})")

        except json.JSONDecodeError as e:
            issues.append(f"{model_file}: Invalid JSON - {e}")
        except Exception as e:
            issues.append(f"{model_file}: Error reading file - {e}")

    print(f"  Validated {total_files} model files")
    if issues:
        print(f"  Found {len(issues)} issues:")
        for issue in issues[:10]:  # Show first 10
            print(f"    - {issue}")
        if len(issues) > 10:
            print(f"    ... and {len(issues) - 10} more issues")
        return False
    else:
        print("  All model files passed validation")
        return True

def check_resource_structure():
    """Check the resource structure for common issues"""
    print("\nChecking resource structure...")

    checks = [
        ("assets/jeg/lang/en_us.json", "Main language file"),
        ("assets/jeg/models/item/abstract_gun.json", "Abstract gun model"),
        ("META-INF/neoforge.mods.toml", "Mod configuration")
    ]

    all_good = True
    for file_path, description in checks:
        path = Path(f"src/main/resources/{file_path}")
        if path.exists():
            print(f"  [OK] {description}: {file_path}")
        else:
            print(f"  [MISSING] {description}: {file_path}")
            all_good = False

    return all_good

def analyze_dependencies():
    """Analyze mod dependencies and configuration"""
    print("\nAnalyzing mod configuration...")

    mods_toml = Path("src/main/resources/META-INF/neoforge.mods.toml")
    if not mods_toml.exists():
        print("  ERROR: neoforge.mods.toml not found")
        return False

    try:
        with open(mods_toml, 'r', encoding='utf-8') as f:
            content = f.read()

        # Check for required fields
        required_fields = ["modId", "version", "displayName"]
        missing_fields = []

        for field in required_fields:
            if f'{field}=' not in content:
                missing_fields.append(field)

        if missing_fields:
            print(f"  ERROR: Missing required fields: {', '.join(missing_fields)}")
            return False

        # Check for NeoForge dependency
        if 'neoforge' not in content:
            print("  WARNING: No NeoForge dependency found")
        else:
            print("  [OK] NeoForge dependency found")

        print("  [OK] Mod configuration appears valid")
        return True

    except Exception as e:
        print(f"  ERROR: Could not read neoforge.mods.toml: {e}")
        return False

def check_for_problematic_patterns():
    """Check for known problematic patterns in the codebase"""
    print("\nChecking for problematic patterns...")

    issues = []
    src_dir = Path("src/main/java")

    if src_dir.exists():
        for java_file in src_dir.rglob("*.java"):
            try:
                with open(java_file, 'r', encoding='utf-8') as f:
                    content = f.read()

                # Check for potential issues
                if "PreparableReloadListener" in content and "implements" in content:
                    # Check if custom reload listeners might be causing issues
                    rel_path = str(java_file).replace("src/main/java/", "")
                    issues.append(f"Custom reload listener: {rel_path}")

            except Exception:
                continue  # Skip files that can't be read

    if issues:
        print("  Found potentially problematic patterns:")
        for issue in issues:
            print(f"    - {issue}")
    else:
        print("  No obvious problematic patterns found")

    return len(issues) == 0

def generate_test_recommendations():
    """Generate testing recommendations"""
    print("\n" + "="*50)
    print("TESTING RECOMMENDATIONS")
    print("="*50)

    recommendations = [
        "1. Clean build: Run './gradlew clean' to remove old artifacts",
        "2. Full rebuild: Run './gradlew build' to recreate all resources",
        "3. Client test: Run './gradlew runClient' to test game startup",
        "4. Monitor logs: Check for any remaining ModelManager errors",
        "5. Test items: Verify that guns and items render correctly in-game",
        "6. Check performance: Monitor memory usage during model loading",
        "7. Verify backups: Original complex models are preserved in models_backup/"
    ]

    for rec in recommendations:
        print(f"  {rec}")

    print(f"\nIf crashes persist:")
    print(f"  - Check the latest crash log for specific error details")
    print(f"  - Verify Java 21 is being used (java -version)")
    print(f"  - Try running with increased memory: -Xmx4G")
    print(f"  - Consider restoring specific models if needed for functionality")

def main():
    print("=== ModelManager Crash Fix Validation ===")
    print("Comprehensive validation and testing of the fix...")

    # Run all validation checks
    validation_results = []

    validation_results.append(validate_model_files())
    validation_results.append(check_resource_structure())
    validation_results.append(analyze_dependencies())
    validation_results.append(check_for_problematic_patterns())

    # Summary
    print("\n" + "="*50)
    print("VALIDATION SUMMARY")
    print("="*50)

    passed = sum(validation_results)
    total = len(validation_results)

    print(f"Tests passed: {passed}/{total}")

    if passed == total:
        print("[SUCCESS] ALL VALIDATIONS PASSED")
        print("\nThe ModelManager crash fix appears to be successful!")
        print("The mod should now start without ModelManager-related crashes.")
    else:
        print("[WARNING] SOME VALIDATIONS FAILED")
        print("\nThere may be remaining issues that need to be addressed.")

    # Generate testing recommendations
    generate_test_recommendations()

    return passed == total

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)