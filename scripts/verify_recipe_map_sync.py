"""Verify real recipe-map login, mode changes, reload, relogin and restart.

Run with Java 25: python scripts/verify_recipe_map_sync.py --world WORLD_COPY_SOURCE --output NEW_DIRECTORY
Fabric only: --expect-crash records the unfixed bySyncedSerializer login failure.
The source world is copied, and the fixture JAR is built outside production source sets.
"""
import argparse
import json
from pathlib import Path
import shutil
import socket
import subprocess
import time
import zipfile


INIT = '''allprojects { afterEvaluate {
 def fabric = project.extensions.findByName('loom') != null
 tasks.register('recipeCheckClasspath') {
  dependsOn 'classes'
  if (fabric) dependsOn 'clientClasses'
  doLast {
   def sources = fabric ? sourceSets.client : sourceSets.main
   file(project.property('recipeCheckClasspath')).text = (sources.runtimeClasspath + sources.compileClasspath + sourceSets.main.output).asPath
  }
 }
 def directory = project.property('recipeCheckRuntime')
 def properties = ['recipecheck.server': project.property('recipeCheckServer'), 'recipecheck.client': project.property('recipeCheckClient')]
 def host = project.findProperty('recipeCheckHost')
 def args = ['--username', 'RecipeCheck', '--quickPlayPath', 'recipe-check.json', '--width', '960', '--height', '540']
 if (host) args.addAll(['--quickPlayMultiplayer', host.toString()])
 else args.addAll(['--quickPlaySingleplayer', 'Recipe Check'])
 if (fabric) {
  loom.runs.named('server') { runDir project.relativePath(file(directory)); properties.each { k,v -> vmArg '-D'+k+'='+v } }
  loom.runs.named('client') { runDir project.relativePath(file(directory)); properties.each { k,v -> vmArg '-D'+k+'='+v }; programArgs args }
 } else {
  neoForge.runs.named('server') { gameDirectory = file(directory); properties.each { k,v -> systemProperty k,v } }
  neoForge.runs.named('client') { gameDirectory = file(directory); properties.each { k,v -> systemProperty k,v }; programArguments.addAll(args) }
 }
 tasks.named('runServer', JavaExec) { standardInput = System.in }
} }
'''


def read(path):
    try:
        return json.loads(path.read_text(encoding='utf8'))
    except (FileNotFoundError, json.JSONDecodeError):
        return None


def wait_for(predicate, processes, description, seconds=240):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        result = predicate()
        if result:
            return result
        if any(process.poll() is not None for process in processes):
            raise RuntimeError('Process exited while waiting for ' + description)
        time.sleep(.25)
    raise TimeoutError(description)


def joined(directory):
    files = list(directory.glob('joined-*.json'))
    return read(max(files, key=lambda path: int(path.stem.split('-')[1]))) if files else None


def stop(process, client_output=None):
    if process.poll() is not None:
        return
    if client_output:
        (client_output / 'stop').touch()
    else:
        process.stdin.write(b'stop\n')
        process.stdin.flush()
    try:
        process.wait(timeout=60)
    except subprocess.TimeoutExpired:
        subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True)
        raise
    assert process.returncode == 0, f'Game exited with {process.returncode}'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--world', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--expect-crash', action='store_true')
    parser.add_argument('--scenario', choices=['all', 'dedicated', 'singleplayer'], default='all')
    args = parser.parse_args()
    module = Path(__file__).resolve().parents[1]
    fabric = 'Fabric' in module.name
    assert args.world.is_dir(), args.world
    assert not args.expect_crash or (fabric and args.scenario != 'singleplayer')
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    init = output / 'recipe-check.init.gradle'
    init.write_text(INIT, encoding='utf8')
    fixture = output / 'fixture.jar'
    classpath_file = output / 'classpath.txt'
    processes, handles, results = [], [], []
    (output / 'environment.json').write_text(json.dumps({
        'module': module.name,
        'commit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=module, text=True).strip(),
        'java': subprocess.check_output(['java', '-version'], stderr=subprocess.STDOUT, text=True),
        'gradle_properties': (module / 'gradle.properties').read_text(),
    }, indent=2), encoding='utf8')

    def command(task, runtime, server_out, client_out, host=None):
        result = [str(module / 'gradlew.bat'), task, '--console=plain', '--no-configuration-cache',
                  '--init-script', str(init), '-PrecipeCheckRuntime=' + str(runtime),
                  '-PrecipeCheckServer=' + str(server_out), '-PrecipeCheckClient=' + str(client_out),
                  '-PrecipeCheckClasspath=' + str(classpath_file)]
        if host:
            result.append('-PrecipeCheckHost=' + host)
        return result

    def launch(task, runtime, server_out, client_out, log_name, host=None):
        (runtime / 'mods').mkdir(parents=True, exist_ok=True)
        shutil.copy2(fixture, runtime / 'mods' / fixture.name)
        if task == 'runClient' and not (runtime / 'options.txt').exists():
            (runtime / 'options.txt').write_text('onboardAccessibility:false\nskipMultiplayerWarning:true\ntutorialStep:none\npauseOnLostFocus:false\nmaxFps:40\nrenderDistance:3\nsimulationDistance:3\nsoundCategory_master:0.0\n')
        server_out.mkdir(parents=True, exist_ok=True)
        client_out.mkdir(parents=True, exist_ok=True)
        handle = (output / log_name).open('w', encoding='utf8')
        handles.append(handle)
        process = subprocess.Popen(command(task, runtime, server_out, client_out, host), cwd=module,
                                   stdout=handle, stderr=subprocess.STDOUT,
                                   stdin=subprocess.PIPE if task == 'runServer' else subprocess.DEVNULL)
        processes.append(process)
        return process

    def control(server_out, label, action, active):
        temporary = server_out / 'control.tmp'
        temporary.write_text(label + '|' + action, encoding='utf8')
        temporary.replace(server_out / 'control.txt')
        snapshot = wait_for(lambda: read(server_out / (label + '.json')), active, label)
        assert not (server_out / 'failure.json').exists(), read(server_out / 'failure.json')
        return snapshot

    def verify_client(client_out, snapshot, active, previous_sync=0):
        assert snapshot['players'] == 1, snapshot['players']
        wait_for(lambda: (joined(client_out) or {}).get('displays') == snapshot['displays'], active, 'client recipe book')
        if fabric:
            def matching_sync():
                for path in client_out.glob('sync-*.json'):
                    if int(path.stem.split('-')[1]) > previous_sync and read(path) == snapshot['shapeless']:
                        return int(path.stem.split('-')[1])
            return wait_for(matching_sync, active, 'matching Fabric synchronized recipe set')
        return 0

    def session(server, runtime, server_out, label, host, expected_player=None):
        client_out = output / label
        client = launch('runClient', runtime, server_out, client_out, label + '.log', host)
        active = [server, client] if server else [client]
        identity = wait_for(lambda: joined(client_out), active, label + ' joined')
        if expected_player:
            assert identity['player'] == expected_player, 'Relogin changed the player UUID'
        sync = 0
        for name, action in [('initial', 'snapshot'), ('off', 'mode|false'), ('on', 'mode|true'),
                             ('reload-on', 'reload'), ('off-again', 'mode|false'), ('reload-off', 'reload')]:
            snapshot = control(server_out, label + '-' + name, action, active)
            # A no-op config change does not resend recipes.
            prior = sync if name in ('on', 'reload-on', 'off-again', 'reload-off') else 0
            sync = verify_client(client_out, snapshot, active, prior)
            results.append({'session': label, 'step': name, 'mode': snapshot['mode'], 'recipes': len(snapshot['all']), 'sync': sync})
            print(label + ' ' + name + ': recipe sets and client book verified', flush=True)
        stop(client, client_out)
        return identity['player']

    try:
        with (output / 'compile.log').open('w', encoding='utf8') as log:
            subprocess.run(command('recipeCheckClasspath', output / 'unused', output, output), cwd=module,
                           stdout=log, stderr=subprocess.STDOUT, check=True, timeout=300)
        classes = output / 'fixture-classes'
        classes.mkdir()
        sources = module / 'scripts' / 'recipe_map_check'
        java_files = [sources / 'CommonCheck.java', sources / 'ClientCheck.java']
        java_files += sorted(sources.glob('Fabric*.java' if fabric else 'Neo*.java'))
        compiler_args = ['--release', '25', '-cp', classpath_file.read_text(), '-d', str(classes)] + [str(p) for p in java_files]
        argfile = output / 'javac.args'
        argfile.write_text('\n'.join('"' + arg.replace('\\', '/') + '"' for arg in compiler_args), encoding='utf8')
        subprocess.run(['javac', '@' + str(argfile)], check=True)
        with zipfile.ZipFile(fixture, 'w') as jar:
            for path in classes.rglob('*.class'):
                jar.write(path, path.relative_to(classes).as_posix())
            if fabric:
                jar.writestr('fabric.mod.json', json.dumps({'schemaVersion': 1, 'id': 'recipe_map_check', 'version': '1.0.0',
                    'entrypoints': {'main': ['recipecheck.FabricCheck'], 'client': ['recipecheck.FabricClientCheck']}}))
            else:
                jar.writestr('META-INF/neoforge.mods.toml', 'license="MIT"\n[[mods]]\nmodId="recipe_map_check"\nversion="1.0.0"\ndisplayName="Recipe Map Regression"\n')
        if args.scenario != 'singleplayer':
            server_dir = output / 'server-runtime'
            shutil.copytree(args.world, server_dir / 'world', ignore=shutil.ignore_patterns('session.lock'))
            (server_dir / 'eula.txt').write_text('eula=true\n')
            with socket.socket() as sock:
                sock.bind(('127.0.0.1', 0))
                port = sock.getsockname()[1]
            (server_dir / 'server.properties').write_text(f'server-ip=127.0.0.1\nserver-port={port}\nonline-mode=false\nwhite-list=false\nenforce-whitelist=false\nenforce-secure-profile=false\nview-distance=3\nsimulation-distance=3\n')
            server_out = output / 'server'
            server = launch('runServer', server_dir, server_out, output / 'unused', 'server.log')
            wait_for(lambda: read(server_out / 'ready.json'), [server], 'dedicated server ready')
            client_dir = output / 'client-runtime'
            if args.expect_crash:
                client_out = output / 'baseline-client'
                client = launch('runClient', client_dir, server_out, client_out, 'baseline-client.log', f'127.0.0.1:{port}')
                def reproduced():
                    log = (output / 'server.log').read_text(encoding='utf8', errors='replace')
                    return 'bySyncedSerializer' in log and 'NullPointerException' in log and "Couldn't place player in world" in log
                wait_for(reproduced, [server, client], 'original bySyncedSerializer login crash')
                stop(client, client_out)
                stop(server)
                (output / 'baseline-reproduced.txt').write_text('Original RecipeMap initialization failure reproduced during real client login.\n')
                print('Original bySyncedSerializer login crash reproduced', flush=True)
                return
            player_id = session(server, client_dir, server_out, 'dedicated', f'127.0.0.1:{port}')
            session(server, client_dir, server_out, 'relogin', f'127.0.0.1:{port}', player_id)
            stop(server)
            restart_out = output / 'restarted-server'
            server = launch('runServer', server_dir, restart_out, output / 'unused', 'restarted-server.log')
            wait_for(lambda: read(restart_out / 'ready.json'), [server], 'restarted server ready')
            snapshot = control(restart_out, 'persisted-mode', 'snapshot', [server])
            assert snapshot['mode'] is False, 'Mode did not persist across restart'
            session(server, client_dir, restart_out, 'restart', f'127.0.0.1:{port}', player_id)
            stop(server)
        if args.scenario != 'dedicated':
            runtime = output / 'singleplayer-runtime'
            shutil.copytree(args.world, runtime / 'saves' / 'Recipe Check', ignore=shutil.ignore_patterns('session.lock'))
            session(None, runtime, output / 'integrated-server', 'singleplayer', None)
        for log in output.glob('*.log'):
            source = log.read_text(encoding='utf8', errors='replace')
            assert 'bySyncedSerializer' not in source and "Couldn't place player in world" not in source, log
        (output / 'passed.json').write_text(json.dumps(results, indent=2), encoding='utf8')
        print(f'Passed {len(results)} live recipe scenarios', flush=True)
    finally:
        for process in processes:
            if process.poll() is None:
                subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True)
        for handle in handles:
            handle.close()


if __name__ == '__main__':
    main()
