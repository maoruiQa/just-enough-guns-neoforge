"""Run the real shot tracker and spread getter with a deterministic clock (JDK 21+)."""
from pathlib import Path
import re
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
source = (root / "src/main/java/ttv/migami/jeg/item/GunItem.java").read_text(encoding="utf-8")
methods = []
for name in ("updateSpreadTracker", "getSpreadMultiplier"):
    match = re.search(r"    private static [^\n]+\b" + name + r"\([^\n]+\) \{\n.*?^    \}", source, re.M | re.S)
    assert match, f"Missing {name}"
    methods.append(re.sub(r"\b(?:Identifier|ResourceLocation)\b", "String", match[0])
                   .replace("System.currentTimeMillis()", "SpreadRecoveryCheck.now"))
constants = []
for name in ("SPREAD_THRESHOLD_MS", "SPREAD_MAX_COUNT", "MINIGUN_SPREAD_FLOOR"):
    match = re.search(r"private static final (?:int|float) " + name + r" = [^;]+;", source)
    assert match, f"Missing {name}"
    constants.append(match[0])

harness = """
import java.util.*;
class SpreadRecoveryCheck {
    static long now;
    static final Map<UUID, SpreadTrackerState> SPREAD_TRACKERS = new HashMap<>();
    record Player(UUID id) { UUID getUUID() { return id; } }
    record GunStats(String id) {}
    static class SpreadTrackerState { final Map<String, SpreadEntry> byGun = new HashMap<>(); }
    static class SpreadEntry { long lastFireMs = -1L; int spreadCount; }
    static boolean isMinigunWeapon(String id) { return id.equals("minigun"); }
    static float getFireSpreadCap(GunStats stats) { return 1.0F; }
    static void equal(float actual, float expected, String message) {
        if (Math.abs(actual - expected) > 0.00001F) throw new AssertionError(message + ": " + actual);
    }
    public static void main(String[] args) {
        for (String id : List.of("rifle", "minigun")) {
            for (boolean aiming : new boolean[]{false, true}) {
                SPREAD_TRACKERS.clear();
                Player player = new Player(UUID.randomUUID());
                GunStats gun = new GunStats(id);
                float baseline = isMinigunWeapon(id) ? MINIGUN_SPREAD_FLOOR : 0.0F;
                now = 1000L;
                equal(getSpreadMultiplier(player, gun), baseline, "No previous shots");
                for (int shot = 0; shot <= SPREAD_MAX_COUNT; shot++) {
                    updateSpreadTracker(player, id, aiming);
                    if (shot < SPREAD_MAX_COUNT) now += 50L;
                }
                float active = getSpreadMultiplier(player, gun);
                if (active <= baseline) throw new AssertionError("Rapid fire must build spread");
                equal(getSpreadMultiplier(player, new GunStats("unused")), 0.0F, "Unfired gun");
                now += SPREAD_THRESHOLD_MS - 1L;
                equal(getSpreadMultiplier(player, gun), active, "Keep spread before timeout");
                now++;
                equal(getSpreadMultiplier(player, gun), baseline, "Recover at timeout without firing");
                now += 10000L;
                equal(getSpreadMultiplier(player, gun), baseline, "Keep recovered spread while idle");
                updateSpreadTracker(player, id, aiming);
                equal(getSpreadMultiplier(player, gun), baseline, "First shot after idle");
                now += 50L;
                updateSpreadTracker(player, id, aiming);
                int count = SPREAD_TRACKERS.get(player.getUUID()).byGun.get(id).spreadCount;
                int expected = (aiming ? 1 : 2) + (isMinigunWeapon(id) ? 1 : 0);
                if (count != expected) throw new AssertionError("Spread must accumulate again");
            }
        }
        System.out.println("Spread recovery: timeout boundary, idle reads, next shot, ADS and minigun passed");
    }
""" + "\n".join(constants + methods) + "\n}\n"

with tempfile.TemporaryDirectory(prefix="jeg-spread-") as directory:
    path = Path(directory) / "SpreadRecoveryCheck.java"
    path.write_text(harness, encoding="utf-8")
    subprocess.run(["java", str(path)], check=True)
