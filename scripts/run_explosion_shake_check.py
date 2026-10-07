"""Run the rendered explosion scenarios and a dedicated-server smoke check in disposable copies.

Usage: python scripts/run_explosion_shake_check.py --world PATH --output PATH
The source world is never opened or modified. Requires the configured Minecraft/Java runtime.
"""
import argparse
import csv
from pathlib import Path
import shutil
import subprocess
import time

INIT = '''allprojects { afterEvaluate {
 def directory = project.findProperty('shakeDir')
 def output = project.findProperty('shakeOutput')
 if (project.extensions.findByName('neoForge') != null) {
  neoForge.runs.named('server') { gameDirectory = project.file(directory) }
  neoForge.runs.named('client') {
   gameDirectory = project.file(directory)
   systemProperty 'jeg.explosionShakeCheck', output.toString()
   programArguments.addAll '--quickPlayPath', 'shake-quickplay.json', '--quickPlaySingleplayer', 'Shake Check', '--width', '1280', '--height', '720'
  }
 } else {
  loom.runs.named('server') { runDir project.relativePath(project.file(directory)) }
  loom.runs.named('client') {
   runDir project.relativePath(project.file(directory))
   vmArg '-Djeg.explosionShakeCheck='+output
   programArgs '--quickPlayPath', 'shake-quickplay.json', '--quickPlaySingleplayer', 'Shake Check', '--width', '1280', '--height', '720'
  }
 }
 tasks.named('runServer', JavaExec) { standardInput = System.in }
} }
'''

def verify(output):
    rows = list(csv.DictReader((output / 'shake.csv').open(encoding='utf8')))
    scenes = {row['scene'] for row in rows}
    assert len(scenes) == 13, scenes
    for name in scenes:
        # Tick zero is the boundary before the next scene installs its configuration.
        samples = [row for row in rows if row['scene'] == name and int(row['tick']) > 0]
        moving = [row for row in samples if any(float(row[axis]) != 0 for axis in ('yaw', 'pitch', 'roll'))]
        assert bool(moving) == (name not in ('disabled', 'excluded')), name
        if moving:
            assert any(all(float(row[axis]) != 0 for axis in ('yaw', 'pitch', 'roll')) for row in moving), name
        if name == 'recovery':
            assert all(float(samples[-1][axis]) == 0 for axis in ('yaw', 'pitch', 'roll'))
        if name == 'far':
            assert max(int(row['due_tick']) for row in moving) >= 2, 'Missing shock arrival delay'
            assert all(int(row['state_tick']) >= int(row['due_tick']) for row in moving), 'Shock arrived before its scheduled tick'
    assert len(list((output / 'screenshots').glob('*.png'))) == 24, 'Missing rendered captures'
    print(f'Rendered shake verified: {len(scenes)} scenes, {len(rows)} camera frames, 24 screenshots', flush=True)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--world', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--skip-server', action='store_true')
    parser.add_argument('--resume', action='store_true', help='Verify an already completed client capture and finish its server check')
    args = parser.parse_args()
    module = Path(__file__).resolve().parents[1]
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    runtime = output / 'runtime'
    if runtime.exists() and not args.resume:
        raise RuntimeError('Choose a fresh output directory to preserve previous evidence')
    init = output / 'shake.init.gradle'
    init.write_text(INIT)
    def command(task, directory):
        return [str(module/'gradlew.bat'), task, '--console=plain', '--no-configuration-cache',
                '--init-script', str(init), '-PshakeDir='+str(directory), '-PshakeOutput='+str(output)]

    if not args.resume:
        world = runtime / 'saves' / 'Shake Check'
        shutil.copytree(args.world, world, ignore=shutil.ignore_patterns('session.lock'))
        (runtime / 'options.txt').write_text('pauseOnLostFocus:false\nmaxFps:60\nrenderDistance:4\nsimulationDistance:4\nsoundCategory_master:0.0\ntutorialStep:none\n')

        with (output/'client.log').open('w', encoding='utf8') as handle:
            client = subprocess.Popen(command('runClient', runtime), cwd=module, stdout=handle,
                                      stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL)
            try:
                client.wait(timeout=360)
            except subprocess.TimeoutExpired:
                # Terminate this test's process tree; never target other running game instances.
                subprocess.run(['taskkill', '/PID', str(client.pid), '/T', '/F'], capture_output=True)
                raise
            assert client.returncode == 0, (output/'client.log').read_text(encoding='utf8')[-10000:]
    assert (output/'passed.txt').exists(), 'Client did not complete scenarios'
    verify(output)
    if args.skip_server or (output/'server-passed.txt').exists(): return
    server_dir = output / 'server-runtime'
    if not server_dir.exists():
        shutil.copytree(args.world, server_dir/'world', ignore=shutil.ignore_patterns('session.lock'))
    (server_dir/'eula.txt').write_text('eula=true\n')
    (server_dir/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nview-distance=3\nsimulation-distance=3\n')
    log = output/'server.log'
    with log.open('w', encoding='utf8') as handle:
        server = subprocess.Popen(command('runServer', server_dir), cwd=module, stdout=handle,
                                  stderr=subprocess.STDOUT, stdin=subprocess.PIPE)
        try:
            deadline = time.monotonic()+180
            while time.monotonic() < deadline:
                source = log.read_text(encoding='utf8', errors='replace')
                if 'Done (' in source: break
                if server.poll() is not None: raise RuntimeError(source[-10000:])
                time.sleep(0.5)
            else: raise TimeoutError('Server did not start')
            server.stdin.write(b'stop\n'); server.stdin.flush()
            server.wait(timeout=60)
            assert server.returncode == 0, log.read_text(encoding='utf8')[-10000:]
            (output/'server-passed.txt').write_text('Dedicated server started and stopped cleanly\n')
            print('Dedicated server verified', flush=True)
        finally:
            if server.poll() is None:
                subprocess.run(['taskkill', '/PID', str(server.pid), '/T', '/F'], capture_output=True)

if __name__ == '__main__': main()
