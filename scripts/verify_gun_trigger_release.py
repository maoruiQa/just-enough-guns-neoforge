"""Exercise the real release predicates with delayed server lock synchronization."""
from pathlib import Path
import re


def verify(source):
    predicates = []
    for stack in ("heldMain", "stack"):
        match = re.search(
            rf"(?:else )?if \(([^\n]+)\) \{{\s*GunItem.clearTriggerLock\({stack}\);"
            rf"\s*(?:Client)?NetworkHandler.sendTriggerRelease\((?:net.minecraft.world.)?InteractionHand.MAIN_HAND\);",
            source,
        )
        assert match, f"Missing release path for {stack}"
        expression = match[1].replace(f"GunItem.isTriggerLocked({stack})", "client_locked")
        expression = expression.replace("gun.isAutomatic()", "automatic")
        expression = expression.replace("attackHeldLastTick", "was_held")
        expression = expression.replace("&&", " and ").replace("||", " or ").replace("!", "not ")
        predicates.append(compile(expression, "release predicate", "eval"))

    for predicate in predicates:
        def release(was_held, client_locked, automatic=False):
            return eval(predicate, {"__builtins__": {}}, {
                "was_held": was_held, "client_locked": client_locked, "automatic": automatic,
            })

        server_locked = False
        shots = 0
        for shot in range(200):
            assert not server_locked, f"Trigger stuck before shot {shot + 1}"
            server_locked = True
            shots += 1
            # Creative shots do not consume ammo: no local component update is assumed.
            client_locked = shot % 2 == 0
            if release(True, client_locked):
                server_locked = False
            assert not server_locked, "Mouse release must unlock before lock synchronization"
            # A stale locked snapshot can arrive after release; repair it while idle.
            if release(False, True):
                client_locked = False
            else:
                client_locked = True
            assert not client_locked, "Late lock snapshot must not leave the client stuck"
        assert shots == 200
        assert not release(False, False), "Idle unlocked guns must not send release packets"

    assert not eval(predicates[0], {"__builtins__": {}}, {
        "was_held": True, "client_locked": True, "automatic": True,
    }), "Automatic fire must keep its existing input behavior"


if __name__ == "__main__":
    root = Path(__file__).resolve().parents[1]
    source = root / ("src/client/java/ttv/migami/jeg/client/FabricClientBootstrap.java"
                     if "-Fabric-" in root.name else "src/main/java/ttv/migami/jeg/client/GunClientEvents.java")
    verify(source.read_text(encoding="utf-8"))
    print("Gun trigger release: 200 shots per path and delayed synchronization passed")
