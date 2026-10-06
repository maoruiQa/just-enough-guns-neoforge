"""Run three real localhost clients through delayed TCP, then check vehicle permissions and HUD state.

Requires an existing disposable flat client world (--world), accepted Minecraft EULA,
and the module's configured Java toolchain. Logs and disposable runtimes go to --output.
"""
import argparse
import csv
import hashlib
import json
import os
import re
from pathlib import Path
import secrets
import shutil
import socket
import socketserver
import struct
import subprocess
import tempfile
import threading
import time
import uuid


def exact(sock, count):
    result = b""
    while len(result) < count:
        part = sock.recv(count - len(result))
        if not part:
            raise EOFError("RCON disconnected")
        result += part
    return result


def packet(sock, kind, body):
    data = struct.pack("<ii", 7, kind) + body.encode() + b"\0\0"
    sock.sendall(struct.pack("<i", len(data)) + data)
    size = struct.unpack("<i", exact(sock, 4))[0]
    assert 10 <= size <= 65536, size
    return exact(sock, size)


class DelayProxy(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True


class Forward(socketserver.BaseRequestHandler):
    def handle(self):
        upstream = socket.create_connection(("127.0.0.1", self.server.target_port))
        def copy(source, destination):
            try:
                while data := source.recv(65536):
                    time.sleep(self.server.delay)
                    destination.sendall(data)
            except OSError:
                pass
            finally:
                try: destination.shutdown(socket.SHUT_WR)
                except OSError: pass
        outgoing = threading.Thread(target=copy, args=(self.request, upstream), daemon=True)
        outgoing.start()
        copy(upstream, self.request)
        outgoing.join(timeout=2)
        upstream.close()


def wait_for(log, phrase, process, seconds=180):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if phrase in log.read_text(encoding="utf8", errors="replace"): return
        if process.poll() is not None: raise RuntimeError(f"Process exited before {phrase}: {log}")
        time.sleep(.25)
    raise TimeoutError(f"Waiting for {phrase}: {log}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--world", type=Path, required=True)
    parser.add_argument("--port", type=int, default=25582)
    parser.add_argument("--delay-ms", type=int, default=100)
    args = parser.parse_args()
    module = Path(__file__).resolve().parents[1]
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    runtime = Path(tempfile.mkdtemp(prefix="runtime-", dir=output))
    server_dir = runtime / "server"
    server_dir.mkdir()
    shutil.copytree(args.world, server_dir / "world", ignore=shutil.ignore_patterns("session.lock"))
    secret = secrets.token_hex(16)
    (server_dir / "eula.txt").write_text("eula=true\n")
    (server_dir / "server.properties").write_text(f"server-ip=127.0.0.1\nserver-port={args.port}\n"
        f"enable-rcon=true\nrcon.port={args.port+100}\nrcon.password={secret}\nonline-mode=false\n"
        "white-list=false\nenforce-whitelist=false\ngamemode=creative\nforce-gamemode=true\nallow-flight=true\nview-distance=4\nsimulation-distance=4\n")
    init = runtime / "network.init.gradle"
    init.write_text('''allprojects { afterEvaluate {
 def directory = project.findProperty('vehicleCheckDir')
 def role = project.findProperty('vehicleCheckRole')
 def host = project.findProperty('vehicleCheckHost')
 def output = project.findProperty('vehicleCheckOutput')
 if (project.extensions.findByName('neoForge') != null) {
  if (role == null) neoForge.runs.named('server') { gameDirectory = project.file(directory) }
  else neoForge.runs.named('client') {
   gameDirectory = project.file(directory)
   systemProperty 'jeg.vehicleNetworkCheck', output.toString()
   systemProperty 'jeg.vehicleNetworkRole', role.toString()
   programArguments.addAll '--username', role.capitalize()+'Vehicle', '--uuid', project.findProperty('vehicleCheckUuid').toString(), '--quickPlayPath', 'network-quickplay.json', '--quickPlayMultiplayer', host.toString()
  }
 } else {
  if (role == null) loom.runs.named('server') { runDir project.relativePath(project.file(directory)) }
  else loom.runs.named('client') {
   runDir project.relativePath(project.file(directory))
   vmArg '-Djeg.vehicleNetworkCheck='+output
   vmArg '-Djeg.vehicleNetworkRole='+role
   programArgs '--username', role.capitalize()+'Vehicle', '--uuid', project.findProperty('vehicleCheckUuid').toString(), '--quickPlayPath', 'network-quickplay.json', '--quickPlayMultiplayer', host.toString()
  }
 }
} }
''', encoding="utf8")
    processes, handles = [], []
    def launch(kind, directory, role=None, relogin=False):
        log = output / (role or "server") / ("run-relogin.log" if relogin else "run.log")
        log.parent.mkdir(parents=True, exist_ok=True)
        handle = log.open("w", encoding="utf8")
        handles.append(handle)
        command = [str(module / "gradlew.bat"), "run"+kind, "--no-configuration-cache", "--no-problems-report", "--console=plain", "--init-script", str(init), "-PvehicleCheckDir="+str(directory)]
        if role:
            name = role.capitalize()+"Vehicle"
            identity = uuid.UUID(bytes=hashlib.md5(("OfflinePlayer:"+name).encode()).digest(), version=3)
            command += ["-PvehicleCheckRole="+role, "-PvehicleCheckUuid="+str(identity), "-PvehicleCheckOutput="+str(log.parent), "-PvehicleCheckHost=127.0.0.1:"+str(args.port+1)]
        process = subprocess.Popen(command, cwd=module, stdout=handle, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL)
        processes.append(process)
        return process, log
    proxy = DelayProxy(("127.0.0.1", args.port+1), Forward)
    proxy.target_port, proxy.delay = args.port, args.delay_ms/1000
    threading.Thread(target=proxy.serve_forever, daemon=True).start()
    server, server_log = launch("Server", server_dir)
    rcon = None
    roles = ("driver", "gunner", "observer")
    clients = {}
    try:
        wait_for(server_log, "Done (", server)
        rcon = socket.create_connection(("127.0.0.1", args.port+100), timeout=20)
        assert struct.unpack("<i", packet(rcon, 3, secret)[:4])[0] != -1
        def command(text):
            reply = packet(rcon, 2, text)[8:-2].decode(errors="replace")
            with (output / "rcon.log").open("a", encoding="utf8") as log: log.write(text+"\n"+reply+"\n")
            return reply
        reply = command("gamerule doMobSpawning false")
        if "now set to" not in reply: command("gamerule minecraft:spawn_mobs false")
        command("forceload add -64 -16 64 192")
        time.sleep(2)
        command("kill @e[type=!minecraft:player]")
        for role in roles:
            directory = runtime / role
            directory.mkdir()
            options = (module / "run/options.txt").read_text(encoding="utf8")
            options = "\n".join("pauseOnLostFocus:false" if line.startswith("pauseOnLostFocus:") else line for line in options.splitlines()) + "\n"
            (directory / "options.txt").write_text(options, encoding="utf8")
            client, log = launch("Client", directory, role)
            clients[role] = client
            wait_for(server_log, role.capitalize()+"Vehicle joined the game", client)
            print(role+" connected through delayed TCP", flush=True)
        command("gamemode spectator ObserverVehicle")
        results = []
        for name in ("bmp2", "lav150", "truck", "speedboat", "mi28", "ah6"):
            command("kill @e[type=!minecraft:player]")
            for y in range(-60,-50): command(f"fill -64 {y} -16 64 {y} 192 minecraft:air")
            command("fill -64 -61 -16 64 -61 192 minecraft:grass_block")
            ground_reply = command("execute if block 0 -61 0 minecraft:grass_block")
            assert "Test passed" in ground_reply, ("Fixture terrain failed", ground_reply, output / "rcon.log")
            if name == "speedboat":
                for y in range(-64,-60): command(f"fill -64 {y} -16 64 {y} 192 minecraft:water")
            for role in roles: command("tp "+role.capitalize()+"Vehicle 0 -53 -10")
            command(f"summon jeg:{name} 0 {30 if name in ('mi28','ah6') else -60} 0 {{Energy:100000,Invulnerable:1b}}")
            command(f"ride DriverVehicle mount @e[type=jeg:{name},limit=1]")
            command(f"ride GunnerVehicle mount @e[type=jeg:{name},limit=1]")
            time.sleep(4)
            command(f"data merge entity @e[type=jeg:{name},limit=1] {{Energy:0,PartDamageVersion:1,EngineHealth:25.0f}}")
            deadline = time.monotonic()+25
            while time.monotonic() < deadline:
                rows = {role: [row for row in csv.reader((output/role/"network.csv").open()) if len(row) == 21 and row[0] == name] for role in roles}
                if all(rows[role] and int(rows[role][-1][2]) >= 170 for role in roles): break
                time.sleep(.25)
            else: raise TimeoutError("Clients did not finish "+name)
            health_reply = command(f"data get entity @e[type=jeg:{name},limit=1] EngineHealth")
            engine_fraction = float(re.search(r"(-?\d+(?:\.\d+)?)f?\s*$", health_reply).group(1))/50
            for role in roles:
                latest = rows[role][-1]
                assert int(latest[13]) == 0, (name, role, "Energy HUD is stale", latest)
                assert abs(float(latest[15])-engine_fraction) < .03, (name, role, "Part HUD is stale", latest)
                assert abs(float(latest[11])) < .1, (name, role, "A non-driver changed heading", latest)
                if name in ("mi28", "ah6"):
                    assert latest[19] == "false", (name, role, "Empty energy did not switch off the engine", latest)
                else:
                    assert abs(float(latest[12])) < .002, (name, role, "Empty energy did not clear throttle", latest)
            assert int(rows["driver"][-1][4]) == -1, (name, "Dismount did not complete")
            assert all(row[17] == "false" for row in rows["observer"]), (name, "Observer opened an unauthorized inventory")
            assert any(row[17] == "true" for row in rows["gunner"]), (name, "Gunner inventory did not open")
            results.append({"vehicle": name, "roles": list(roles), "energy": 0, "engine_health_fraction": engine_fraction, "passed": True})
            print(name+": passenger permissions, inventory, dismount and replicated HUD passed", flush=True)
        # Reconnect the former driver after dismounting. Old input and aiming must stay cleared.
        (output / "driver/stop").touch()
        clients["driver"].wait(timeout=60)
        (output / "driver/stop").unlink()
        (output / "driver/network.csv").rename(output / "driver/network-before-relogin.csv")
        rejoined, relogin_log = launch("Client", runtime / "driver", "driver", relogin=True)
        clients["driver"] = rejoined
        deadline = time.monotonic()+180
        while time.monotonic() < deadline:
            csv_file = output / "driver/network.csv"
            rows = [row for row in csv.reader(csv_file.open()) if len(row) == 21] if csv_file.exists() else []
            if rows and int(rows[-1][2]) >= 30: break
            if rejoined.poll() is not None: raise RuntimeError("Relogin exited: " + str(relogin_log))
            time.sleep(.25)
        else: raise TimeoutError("Driver did not rejoin")
        latest = rows[-1]
        assert latest[0] == "ah6" and latest[4] == "-1" and latest[13] == "0" and latest[18] == "false" and latest[19] == "false", ("Stale driver state after relogin", latest)
        health_reply = command("data get entity @e[type=jeg:ah6,limit=1] EngineHealth")
        engine_fraction = float(re.search(r"(-?\d+(?:\.\d+)?)f?\s*$", health_reply).group(1))/50
        assert abs(float(latest[15])-engine_fraction) < .03, ("Part HUD stale after relogin", latest)
        print("Driver relogin: input, aiming, permissions and HUD cleared/restored correctly", flush=True)
        (output / "result.json").write_text(json.dumps({"tcp_delay_ms_each_direction": args.delay_ms, "driver_relogin_passed": True, "results": results}, indent=2)+"\n", encoding="utf8")
    finally:
        for role in roles:
            path = output / role
            path.mkdir(exist_ok=True)
            (path / "stop").touch()
            if role in clients: clients[role].wait(timeout=60)
        for process in processes[1:]: process.wait(timeout=60)
        if rcon:
            try: packet(rcon, 2, "stop")
            except (OSError, EOFError): pass
            rcon.close()
        server.wait(timeout=60)
        proxy.shutdown()
        proxy.server_close()
        for handle in handles: handle.close()
    assert all(process.returncode == 0 for process in processes)
    print(module.name+": multiplayer regression and all run processes exited cleanly", flush=True)


if __name__ == "__main__": main()
