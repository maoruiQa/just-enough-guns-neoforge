"""Run the production config-type resolver against old and new FML loader jars.

Usage: python scripts/verify_config_type_compat.py <old-loader.jar> <new-loader.jar>
"""
from pathlib import Path
import os
import subprocess
import sys
import tempfile


def verify(old_loader, new_loader):
    root = Path(__file__).resolve().parents[1]
    source = (root / "src/main/java/ttv/migami/jeg/JustEnoughGuns.java").read_text(encoding="utf-8")
    start = source.index("    private static ModConfig.Type serverConfigType() {")
    end = source.index("\n    }", start) + len("\n    }")
    method = source[start:end]
    assert 'container.registerConfig(serverConfigType(), Config.SERVER_SPEC, Reference.MOD_ID + "-server.toml");' in source
    java = """import net.neoforged.fml.config.ModConfig;
class ConfigTypeCheck {
%s
    public static void main(String[] args) {
        if (!serverConfigType().name().equals(args[0]))
            throw new AssertionError("Wrong server config type: " + serverConfigType());
        System.out.println("Server config compatibility passed: " + args[0]);
    }
}
""" % method
    with tempfile.TemporaryDirectory(prefix="jeg-config-type-") as folder:
        harness = Path(folder) / "ConfigTypeCheck.java"
        harness.write_text(java, encoding="utf-8")
        subprocess.run(["javac", "-cp", str(old_loader), str(harness)], check=True)
        for loader, expected in ((old_loader, "SERVER"), (new_loader, "SYNCED")):
            subprocess.run(["java", "-ea", "-cp", os.pathsep.join((folder, str(loader))),
                            "ConfigTypeCheck", expected], check=True)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    verify(Path(sys.argv[1]).resolve(strict=True), Path(sys.argv[2]).resolve(strict=True))
