# RecipeMap login regression

The production mixin must use `RecipeMap.create`: directly invoking its constructor
skips Fabric Recipe API's synchronized-serializer index initialization. The same
factory path is used on NeoForge for compatibility with other injected indexes.

From the module directory, with its Java 25 toolchain and dependencies available:

```powershell
python scripts/verify_recipe_map_sync.py --world "PATH/TO/SOURCE_WORLD" --output "PATH/TO/NEW_EVIDENCE_DIRECTORY"
```

The source world is only copied. The dedicated server binds to localhost on a
temporary port and uses offline mode with no whitelist. The fixture JAR is compiled
into the evidence directory and installed only into its disposable game directories;
it is not a source set or dependency of the production mod.

The default run checks 24 steps: initial login, both magazine modes, switching back,
and reload in both modes, in each of four sessions (dedicated login, same-player
relogin, server restart, and singleplayer). Every step compares the full recipe set,
key lookup and learnable set with the reloadable registry filtered by the existing
rules, checks that all 14 controlled recipe IDs exist, and compares the client's
recipe-book display IDs with the server. Relogin/restart must retain the player UUID,
and the disabled mode must persist across the server restart.

On Fabric, the fixture explicitly enables `ShapelessRecipe.SERIALIZER` synchronization
on both sides. Each change/reload must produce a new client sync event whose complete
ID set matches the server's shapeless recipes. This activates the index access that
does not occur when no serializers are requested. On the unfixed Fabric version,
`--expect-crash --scenario dedicated` must record the original null-index exception
and `Couldn't place player in world` during a real client login. It must fail when
the target exception is absent. NeoForge does not load Fabric or test its sync API.

`--scenario dedicated` or `--scenario singleplayer` runs only that part of the matrix.
All waits are bounded; missing evidence and unexpected process exits fail the run.
Evidence includes dependency versions, baseline revision, runtime logs, server
recipe snapshots, client joins, Fabric sync snapshots, and a final `passed.json`.
These are regression checks for this path, not arbitrary modpack compatibility tests.
