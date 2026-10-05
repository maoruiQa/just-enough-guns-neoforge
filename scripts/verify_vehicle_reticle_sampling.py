"""Check vehicle reticles sample their complete PNGs at the intended GUI sizes."""
from pathlib import Path
import re
import struct

root = Path(__file__).resolve().parents[1]
source_root = "src/client/java" if "-Fabric-" in root.name else "src/main/java"
hud = (root / source_root / "ttv/migami/jeg/vehicle/client/overlay/VehicleHudOverlay.java").read_text(encoding="utf-8")
textures = root / "src/main/resources/assets/jeg/textures/overlay/vehicle"

def size(name):
    header = (textures / name).read_bytes()[:24]
    assert header[:8] == b"\x89PNG\r\n\x1a\n" and header[12:16] == b"IHDR", name
    return struct.unpack(">II", header[16:24])

assert size("crosshair/third_camera.png") == (64, 64)
assert size("helicopter/crosshair_ind.png") == (32, 32)
assert "vehicle.selectedVehicleWeaponId(player) == null || !vehicle.canPassengerUseSelectedVehicleWeapon(player)" in hud, "weaponless seat must not draw a reticle"
assert re.search(r"CROSSHAIR_THIRD_CAMERA,\s*\(float\) screen\.x - 12\.0F,\s*\(float\) screen\.y - 12\.0F,\s*24, 24, 0\.0F, 0\.0F, 64, 64, 64, 64", hud), "rear camera must display 24x24 and sample all 64x64 pixels"
assert "int size = 16;" in hud and "int textureSize = helicopterHudTexture ? 32 : 64;" in hud
assert re.search(r"size, size, 0\.0F, 0\.0F, textureSize, textureSize, textureSize, textureSize", hud), "helicopter indicator must display 16x16 and sample its complete PNG"
print(f"{root.name}: vehicle reticle PNG sampling verified")
