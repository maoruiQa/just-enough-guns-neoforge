"""Static integration check for gun glow coverage across all maintained variants."""

from __future__ import annotations

import json
import math
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MODULES = (
    "Just-Enough-Guns-Fabric-1.21.1",
    "Just-Enough-Guns-Fabric-26.2",
    "Just-Enough-Guns-Fabric-26.3",
    "Just-Enough-Guns-NeoForge-1.21.1",
    "Just-Enough-Guns-NeoForge-26.2",
    "Just-Enough-Guns-NeoForge-26.3",
)
MODERN = {"Fabric-26.2", "Fabric-26.3", "NeoForge-26.2", "NeoForge-26.3"}


def fail(message: str) -> None:
    raise AssertionError(message)


def read(path: Path) -> str:
    if not path.is_file():
        fail(f"missing {path}")
    return path.read_text(encoding="utf-8")


def glow_name(name: str) -> bool:
    return name.startswith(("glow", "flower", "static_flower")) or name == "flashlight_glow"


def model_bones(path: Path) -> tuple[dict[str, str | None], list[str]]:
    data = json.loads(read(path))
    bones = data.get("minecraft:geometry", [{}])[0].get("bones", [])
    parents: dict[str, str | None] = {}
    for bone in bones:
        name = bone["name"]
        if name in parents:
            fail(f"duplicate bone {name} in {path}")
        parents[name] = bone.get("parent")
    roots = [name for name, parent in parents.items() if parent is None]
    for name, parent in parents.items():
        if parent is not None and parent not in parents:
            fail(f"{name} points at missing parent {parent} in {path}")
    return parents, roots


def descendants(parents: dict[str, str | None], root: str) -> set[str]:
    result = {root}
    changed = True
    while changed:
        changed = False
        for name, parent in parents.items():
            if parent in result and name not in result:
                result.add(name)
                changed = True
    return result


def covered_by_renderer(parents: dict[str, str | None], roots: list[str]) -> set[str]:
    children: dict[str | None, list[str]] = {}
    for name, parent in parents.items():
        children.setdefault(parent, []).append(name)

    covered: set[str] = set()

    def visit(name: str) -> None:
        covered.add(name)
        if glow_name(name):
            covered.update(descendants(parents, name))
            return
        for child in children.get(name, []):
            visit(child)

    for root in roots:
        visit(root)
    return covered


def check_model(module: Path, relative: str, label: str, require_nested: bool = True) -> None:
    geckolib = module / "src/main/resources/assets/jeg/geckolib/models" / relative
    legacy = module / "src/main/resources/assets/jeg/geo" / relative
    path = geckolib if geckolib.is_file() else legacy
    parents, roots = model_bones(path)
    glow = {name for name in parents if glow_name(name)}
    if not glow:
        fail(f"{label}: no emissive bones in {path}")
    covered = covered_by_renderer(parents, roots)
    missing = glow - covered
    if missing:
        fail(f"{label}: renderer traversal misses {sorted(missing)}")
    if require_nested and not any(parent is not None and glow_name(name) and not glow_name(parent)
                                  for name, parent in parents.items()):
        fail(f"{label}: no nested emissive bone to exercise recursion")
    print(f"PASS {label}: {len(glow)} emissive bones, {len(roots)} roots")


def check_source(module: Path, modern: bool) -> None:
    if modern:
        source_root = module / ("src/client/java" if (module / "src/client/java").is_dir() else "src/main/java")
        layer = source_root / "ttv/migami/jeg/client/render/gun/layer/GunAttachmentLayer.java"
        text = read(layer)
        required = (
            "bone.children()",
            "RenderUtil.prepMatrixForBoneAndUpdateListeners",
            "passInfo.renderPosed",
            "Brightness.FULL_BRIGHT.pack()",
            "RenderTypes.entityTranslucentEmissive",
            'Reference.id("laser_pointer")',
            'hideBones(model, "glow")',
        )
        for token in required:
            if token not in text:
                fail(f"{module.name}: missing {token} in {layer}")
        scope = read(source_root / "ttv/migami/jeg/client/render/gun/layer/GunBuiltinScopeLayer.java")
        if "renderGlowModel" not in scope or "entityTranslucentEmissive" not in scope:
            fail(f"{module.name}: builtin scope emissive pass is missing")
        animated = read(module / "src/main/java/ttv/migami/jeg/item/AnimatedGunItem.java")
        if "controller.isPlayingTriggeredAnimation()" not in animated:
            fail(f"{module.name}: reload trigger guard is missing")
    else:
        renderer = (module / "src/client/java/ttv/migami/jeg/client/render/gun/AnimatedGunRenderer.java"
                    if (module / "src/client/java").is_dir()
                    else module / "src/main/java/ttv/migami/jeg/client/render/gun/AnimatedGunRenderer.java")
        text = read(renderer)
        for token in ("startsWith(\"flower\")", "startsWith(\"static_flower\")", "LightTexture.FULL_BRIGHT"):
            if token not in text:
                fail(f"{module.name}: missing {token} in {renderer}")
        layer = module / ("src/client/java" if (module / "src/client/java").is_dir() else "src/main/java")
        layer /= "ttv/migami/jeg/client/render/gun/layer/GunPositionedAttachmentLayer.java"
        text = read(layer)
        for token in ('Reference.id("laser_pointer")', "entityTranslucentEmissive", "LightTexture.FULL_BRIGHT", 'hideBones(model, "glow")'):
            if token not in text:
                fail(f"{module.name}: missing {token} in {layer}")


def check_fire_and_reload(module: Path) -> None:
    item = module / "src/main/java/ttv/migami/jeg/item/GunItem.java"
    text = read(item)
    if text.count("spawnBlossomParticles(") < 3:
        fail(f"{module.name}: blossom particle helper/calls are incomplete")
    fire_at = text.index("void fireAt(")
    directional = text.index("void fireDirectionallyFrom(")
    next_method = text.find("\n    public ", directional + 1)
    directional_body = text[directional: next_method if next_method != -1 else None]
    if "spawnBlossomParticles(" not in text[fire_at:directional] or "spawnBlossomParticles(" not in directional_body:
        fail(f"{module.name}: blossom particles are not wired into both fire paths")

    animation = module / "src/main/resources/assets/jeg/geckolib/animations/item/blossom_rifle.animation.json"
    data = json.loads(read(animation))
    reload = data.get("animations", {}).get("reload")
    if not reload or float(reload.get("animation_length", 0)) <= 0:
        fail(f"{module.name}: blossom reload animation is missing")
    if math.ceil(float(reload["animation_length"]) * 20) > 96:
        fail(f"{module.name}: blossom reload animation exceeds the 96 tick minimum")


def main() -> int:
    try:
        for name in MODULES:
            module = ROOT / name
            check_model(module, "item/gun/blossom_rifle.geo.json", f"{name} blossom")
            check_model(module, "item/attachment/laser_pointer.geo.json", f"{name} laser", require_nested=False)
            check_model(module, "item/attachment/reflex_sight.geo.json", f"{name} reflex", require_nested=True)
            check_source(module, any(part in name for part in MODERN))
            check_fire_and_reload(module)
            print(f"PASS {name}: source paths and fire/reload checks")
        print("ALL PASS: maintained emissive render coverage is structurally testable")
        return 0
    except (AssertionError, KeyError, IndexError, json.JSONDecodeError) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
