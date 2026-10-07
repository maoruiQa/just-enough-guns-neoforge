# Just Enough Guns New — Wiki de jugadores y servidores

**Idioma de referencia:** English · **Versión:** `1.8.2`

[Índice de la wiki](README.md) · [English](en-US.md) · [简体中文](zh-CN.md) · [日本語](ja-JP.md) · [Deutsch](de-DE.md)

## Sobre este mod

Just Enough Guns New es un port moderno no oficial del proyecto **Just Enough Guns** de MigaMi para Forge 1.20.1. Mantiene una progresión de supervivencia con estilo vanilla y añade armas de fuego, cargadores, accesorios, artilleros hostiles, incursiones de facciones, vehículos Walkürenritt, equipo especial y amenazas aéreas.

El proyecto está dividido en módulos independientes de Fabric y NeoForge. El servidor y todos los clientes conectados deben usar la misma versión de Minecraft, familia de cargador y versión del mod.

## Inicio rápido

1. Elige en la [tabla de compatibilidad](#compatibilidad) la fila que corresponda a tu versión de Minecraft y cargador.
2. Instala el cargador adecuado, Fabric API o NeoForge, y la versión de GeckoLib indicada.
3. Coloca el JAR `jegn-1.8.2` correspondiente en la carpeta `mods` de la instancia. No mezcles JAR de Fabric y NeoForge.
4. Inicia el juego una vez, crea o copia un mundo de prueba y confirma que Just Enough Guns aparece en la lista de mods.
5. Mantén el primer mundo de prueba separado del servidor permanente hasta comprobar recetas, teclas, configuración y dependencias de addons.

### Lista de comprobación para la primera sesión

- Fabrica o encuentra la mesa de trabajo, munición y cargador compatibles.
- Carga el cargador con la munición suelta correcta antes de introducirlo en un arma que use cargadores.
- Revisa los conflictos de teclas y prueba disparar, apuntar, recargar e inspeccionar.
- Lleva cargadores de repuesto, objetos de reparación o refrigeración y munición suficiente para el modo de disparo.
- En un servidor nuevo, empieza con encuentros normales de artilleros y activa después las incursiones grandes, los vehículos o los eventos Terror Phantom.

## Compatibilidad

| Cargador | Minecraft | Java | Mod | Dependencias necesarias |
| --- | --- | --- | --- | --- |
| Fabric | 1.21.1 | 21 | 1.8.2 | Fabric API, GeckoLib 4.8.3 |
| NeoForge | 1.21.1–1.21.4 | 21 | 1.8.2 | NeoForge 21.1.x, GeckoLib 4.8.3 |
| Fabric | 26.2 | 25 | 1.8.2 | Fabric API, GeckoLib 5.5+ |
| NeoForge | 26.2 | 25 | 1.8.2 | NeoForge 26.2.x, GeckoLib 5.5.1 |
| Fabric | 26.3 | 25 | 1.8.2 | Fabric API, GeckoLib 5.5.7 |
| NeoForge | 26.3 | 25 | 1.8.2 | NeoForge 26.3.x, GeckoLib 5.5.7 |

Fabric 26.1 y NeoForge 26.1 son líneas heredadas. Usa 26.2 para la línea mantenida de Java 25, salvo que un modpack necesite explícitamente 26.1.

## Controles

### Armas

| Acción | Entrada predeterminada |
| --- | --- |
| Disparar | Clic izquierdo |
| Apuntar con la mira | Clic derecho |
| Recargar | `R` |
| Inspeccionar un arma animada | `Y` |
| Cuerpo a cuerpo o linterna, si está disponible | `V` |
| Comportamiento de agacharse, si está disponible | `Shift` |

Estos son los controles predeterminados desde la versión 1.3.0. Las versiones anteriores usaban clic derecho para disparar, `F` para recargar y `Shift` para apuntar. Comprueba las teclas después de actualizar.

### Vehículos

| Acción | Entrada predeterminada |
| --- | --- |
| Entrar o interactuar | Clic derecho |
| Girar y acelerar | `W` / `A` / `S` / `D` |
| Frenar o retroceder | `S` |
| Mirar o apuntar un arma montada | Movimiento del ratón |
| Disparar el arma activa | Clic izquierdo |
| Apuntar, fijar, hacer zoom o usar función secundaria | Clic derecho |
| Recargar, si está disponible | `R` |
| Bajar del vehículo | `Shift` |

El asiento determina las acciones disponibles. Los asientos de conductor mueven el vehículo; los asientos de armas o copiloto pueden controlar torretas, misiles, fijación de objetivos o contramedidas.

## Guía de juego

### Armas y munición

El arsenal incluye pistolas, revólveres, rifles, SMG, escopetas, ametralladoras, lanzadores, arcos, armas de estilo lanzallamas y armas especiales de fase avanzada. La mayoría necesita el tipo de munición o cargador correcto.

Las armas que usan cargadores emplean cargadores ya cargados. Las armas manuales o de un solo proyectil usan directamente su munición correspondiente. Hay cargadores extendidos y de tambor para rifles, SMG y escopetas compatibles. Accesorios, culatas, empuñaduras, miras, skins, insignias y munición especial forman parte de la progresión normal.

El retroceso, la dispersión al moverse, el sobrecalentamiento, la mira dinámica, los marcadores de impacto, los efectos de boca y las trazas muestran el estado del arma. Si no dispara, comprueba primero la munición, el cargador, el calor, la recarga y la configuración del servidor.

### Protección balística

Los cascos y chalecos antibalas interceptan el daño de armas mediante una comparación entre penetración efectiva y valor de armadura, en lugar de usar la Protección contra Proyectiles de vanilla. La AP efectiva procede de la munición y del multiplicador del arma. Un impacto protegido en la cabeza usa primero el casco; otros impactos protegidos usan el chaleco. Sin la pieza correspondiente, el daño no cambia.

Los disparos por debajo del valor de armadura todavía causan daño parcial y reducen la durabilidad. Los disparos que lo superan causan más daño y más presión sobre la durabilidad. Los valores exactos son datos de equilibrio de cada rama; para una versión concreta, consulta la descripción del juego y el código de esa rama.

### Artilleros, facciones e incursiones

Los artilleros pueden aparecer en familias de zombis, esqueletos, piglins, saqueadores/vindicadores, fantasmas, necrófagos y parched. Los eventos de facción avanzan desde patrullas y presagios de facción hasta incursiones al volver a casa, bengalas, barras de jefe y oleadas configurables. Algunas variantes, como el artillero con chaleco C4, dependen de la configuración del servidor.

### Vehículos

Walkürenritt incluye vehículos terrestres ensamblados, barcos, aeronaves, helicópteros y plataformas de armas fijas. Los vehículos tienen asientos, inventarios, herramientas de reparación, carga o energía, misiles, señuelos y HUD específico.

Completa el proceso de ensamblaje y despliega el vehículo en el mundo. Coloca la munición, las herramientas de reparación y los objetos cargados que espera el vehículo en su inventario correcto. Observa en el HUD el estado del arma, la recarga, las advertencias de fijación, el daño y las contramedidas.

La IA de vehículos enemigos puede patrullar, perseguir, retroceder, evitar terrenos inadecuados y controlar torretas según el vehículo. Las ramas mantenidas comparten el comportamiento visible, aunque el código de cada cargador sea independiente.

### Equipo especial

- **Drones FPV:** usa el monitor para controlar la cámara y la carga; las cargas explosivas pueden iniciar un descenso kamikaze guiado.
- **C4 y claymores:** coloca las cargas y usa el detonador o activador correcto; lleva un desactivador C4 para retirar cargas enemigas.
- **Chaleco C4:** una variante configurable de artillero bombardero puede llevar un chaleco explosivo.
- **Javelin e Igla 9K38:** los lanzadores fijan objetivos que están dentro del alcance y en línea de visión; el humo puede impedir la fijación.
- **HUD de fijación de misiles:** los objetivos válidos y visibles dentro del alcance reciben marcos de búsqueda y audio.

### Terror Phantom

Terror Phantom es una amenaza aérea poco frecuente que incluye Bound Terror Phantom, invocaciones de artilleros fantasma, explosiones de muerte configurables y encuentros End Ship Armada. En la línea de equipo especial 1.8.0, la aparición natural está desactivada de forma flexible por defecto; el administrador puede activarla o ajustarla en la configuración.

## ¿Demasiado difícil o demasiado fácil?

Cambia una sola familia de opciones cada vez y juega varios días. Estos comandos corresponden a Fabric 26.2 y requieren permiso de operador nivel 2.

| Lo que notas como jugador | Primer ajuste | Efecto |
| --- | --- | --- |
| Los artilleros son demasiado fuertes al principio | `/justEnoughGuns config combat naturalGunnerDynamicDifficultyEnabled false` | Detiene la adaptación a la mejor armadura y armas cercanas. |
| Las patrullas interrumpen cada regreso | `/justEnoughGuns config patrol minimumDays 15` o `... spawnChance 0.15` | Retrasa el primer día o reduce la probabilidad. |
| El fuego desde la cadera no acierta | `/justEnoughGuns config combat hipFireSpreadMultiplier 1.0` | Reduce la dispersión frente al valor predeterminado 1.5. |
| Los cargadores exigen demasiada logística | `/justEnoughGuns config combat magazineFeed false` | Cambia las armas compatibles a alimentación directa y muestra las nuevas reglas de munición. |
| Los cohetes o el C4 llegan demasiado pronto | En **Mobs** → **All Gunners**, aumenta `Rocket Launcher Start Day` o `Bomber Gunner Start Day` | Retrasa las amenazas especiales sin quitar los artilleros normales. |
| Los vehículos enemigos dominan la base | `/justEnoughGuns config vehicle enemySpawning enabled false` | Detiene la conversión natural de vehículos enemigos y mantiene los vehículos del jugador. |
| El temblor de explosiones cansa | En `config/jeg-client.toml`, usa `rendering.explosionScreenShake = 0` | Desactiva solo el efecto visual local. |

Patrullas, incursiones de facción, crecimiento de artilleros y vehículos enemigos son sistemas separados. No pongas todas las probabilidades a cero; `-1` significa heredar el valor equilibrado o la sobrescritura de **All Gunners**.

## Configuración del servidor desde el juego

En Fabric 26.2 aparece **JEGN Configuration** en la parte superior del menú de pausa. El servidor debe usar la misma versión del mod y necesitas operador nivel 2 (`/op <player>` en un servidor dedicado).

1. Pulsa `Esc` y abre **JEGN Configuration**.
2. Elige **Interface**, **Patrols**, **Mobs**, **Combat** o **Vehicles**.
3. Pulsa los interruptores o escribe números; en **Mobs**, selecciona primero el tipo de artillero con los botones izquierdo/derecho.
4. Pasa el ratón por una etiqueta para ver la clave de comando, el rango permitido y la regla de herencia `-1`.
5. **Apply** valida y guarda `config/jeg-server.toml`; **Reset** restablece solo la categoría actual. **Done** pide confirmar si hay cambios sin guardar.

La pantalla controla interfaz, patrullas, crecimiento de artilleros, alimentación por cargador, dispersión desde la cadera, apoyo de terreno, dificultad de incursiones y vehículos enemigos. El HUD local y el temblor de cámara se ajustan en `jeg-client.toml`.

## Referencia de comandos

Todos empiezan por `/justEnoughGuns`; pulsa `Tab` para ver sugerencias. Sin el valor final se muestra el valor actual; con un valor se guarda el cambio. Los cambios de configuración requieren operador nivel 2.

```text
/justEnoughGuns unlockGunRecipes
/justEnoughGuns spawnPatrol <faction> <size> <pos> [forceGuns] [spawnRadius]
/justEnoughGuns simulatePatrol <faction> <size> <player> [forceGuns]
/justEnoughGuns config patrol minimumDays 15
/justEnoughGuns config patrol spawnChance 0.15
/justEnoughGuns config combat hipFireSpreadMultiplier 1.0
/justEnoughGuns config combat magazineFeed false
/justEnoughGuns config vehicle enemySpawning startDay 120
/justEnoughGuns config mob spawn all bomberStartDay 100
```

Las facciones son `night_of_the_undead`, `the_rattlers`, `nosy_business`, `bad_piggies`, `hell_hogs` y `lost_souls`. El tamaño de patrulla es 1–20 y el radio 0–16 (10 por defecto). `unlockGunRecipes` es para jugadores y pruebas; las patrullas no funcionan en Peaceful.

`config mob spawn` acepta `all`, `skeleton`, `stray`, `zombie`, `husk`, `parched`, `drowned`, `zombieVillager`, `zombifiedPiglin`, `piglin`, `piglinBrute`, `witherSkeleton`, `pillager`, `vindicator` y `generic`. Sus opciones incluyen `minSpawnChance`, `maxSpawnChance`, `spawnChancePerDay`, valores de tier de arma/armadura, `rocketLauncherStartDay`, `rocketLauncherChance`, `rocketLauncherMaxChance`, `rocketLauncherChancePerDay`, `bomberStartDay`, `bomberChance`, `bomberMaxChance`, `bomberChancePerDay` y `weaponAggression`.

## Archivos de configuración

Detén el servidor antes de editar directamente. En `config/jeg-client.toml` puedes cambiar `rendering.showAmmoHud`, `rendering.showTimersHud`, `rendering.crosshair`, `rendering.showHitmarker`, `rendering.dynamicCrosshairDotMode` y `rendering.explosionScreenShake` (0–100). En `config/jeg-server.toml` consulta `combat.magazineFeed`, `combat.hipFireSpreadMultiplier` (0–5), `factionPatrol.*`, `factionRaid.*` y `vehicle.enemyVehicle*`. Cambiar el modo de cargador actualiza el aviso de munición y las recetas filtradas.

## Administración del servidor

Usa la [configuración del servidor desde el juego](#configuración-del-servidor-desde-el-juego) para cambios en vivo, la [referencia de comandos](#referencia-de-comandos) para pruebas rápidas y `config/jeg-server.toml` para opciones avanzadas que no aparecen en la pantalla. Detén el servidor y haz una copia del mundo antes de editar TOML.

## Solución de problemas

### El juego no carga

Compara Minecraft, cargador, Java y GeckoLib con la tabla de compatibilidad. Elimina JAR duplicados o de otro cargador y prueba solo con las dependencias necesarias y Just Enough Guns.

### El servidor dedicado se cierra al iniciar

Confirma que el JAR coincide con el cargador del servidor y la versión de Minecraft. No pongas addons solo de cliente ni un JAR de Fabric en un servidor NeoForge, ni al contrario. Reproduce el problema en una instancia limpia y guarda el primer registro de inicio fallido.

### Falta un arma, receta o vehículo

Tras reiniciar, revisa el libro de recetas, la búsqueda de objetos y el registro del servidor. Cliente y servidor deben usar el mismo archivo del mod y una dependencia de la misma línea de Minecraft. Incluye el ID del objeto y la línea del registro al informar de una receta ausente.

### No aparecen los controles o el HUD

Busca conflictos en la pantalla de teclas, restaura la asignación predeterminada y prueba con mira dinámica, marcadores de impacto y HUD de munición activados. El HUD de vehículos también depende del asiento y del arma activa.

### Un misil no fija el objetivo

Comprueba distancia, línea de visión, tipo de objetivo, munición y estado del lanzador. El humo bloquea la fijación intencionadamente. El marco solo aparece para un objetivo válido, visible y dentro del alcance.

## Informes de errores

Incluye en el issue:

1. Versión de Minecraft y cargador (Fabric o NeoForge).
2. Versión de Just Enough Guns y nombre exacto del archivo JAR.
3. Versiones de Java, Fabric API, NeoForge y GeckoLib.
4. Pasos breves de reproducción, estado del mundo y configuración relevante.
5. Informe de crash completo o registro más reciente; añade captura o vídeo para problemas visuales o de audio.
6. Si se reproduce en una instancia limpia sin otros addons.

“Se cierra” o una captura del launcher no basta. La matriz exacta de versiones y el primer stack trace útil ayudan a separar un problema del cargador, dependencia, mod u otro addon.

## Enlaces para desarrolladores y mantenedores

- [README raíz](../../README.md) y [copia de description](../../description.md)
- [Notas de la versión 1.8.2](../../CHANGELOG.md)
- [Notas de funciones 1.8.0](../../CHANGELOG.md)
- [Guía de avances](../../docs/ADVANCEMENT_GUIDE.md)
- [Notas de IA de vehículos enemigos](../../docs/vehicle_enemy_ai.md)
- [Notas de validación](../../docs/VALIDATION.md)

Cuando cambie el comportamiento visible para jugadores, actualiza primero la página inglesa y sincroniza después las otras cuatro traducciones. Los detalles de implementación de cada rama permanecen en su carpeta `docs/`.

## Versión, créditos y licencia

La versión pública 1.8.2 añade la guía de progreso de cuatro capítulos, el aviso de reglas de munición, comentarios de configuración en vivo y controles de vehículos ajustados. La 1.8.1 corrigió el inicio de servidores dedicados y las opciones de configuración. La versión de funciones 1.8.0 añadió drones FPV, C4, claymores, chaleco C4, Javelin, Igla, humo contra fijación, HUD de fijación de misiles, correcciones de crédito de bajas y ajustes de vehículos/misiles/cohetes.

Just Enough Guns New es un port no oficial independiente y no está afiliado ni respaldado por Just Enough Guns o Superb Warfare. El código basado en Just Enough Guns usa GPL-3.0. Los recursos originales de JEG son ARR y se usan con autorización del autor. Los materiales de vehículos y equipo especial derivados de SBW conservan sus requisitos de atribución y licencia; consulta el README raíz para la lista completa.

