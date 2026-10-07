# Just Enough Guns New — Spieler- und Server-Wiki

**Referenzsprache:** English · **Release:** `1.8.2`

[Wiki-Index](README.md) · [English](en-US.md) · [简体中文](zh-CN.md) · [日本語](ja-JP.md) · [Español](es-ES.md)

## Über diese Mod

Just Enough Guns New ist ein inoffizieller moderner Port von MigaMis Forge-1.20.1-Projekt **Just Enough Guns**. Die Mod behält den Überlebensfortschritt im Vanilla-Stil und ergänzt Schusswaffen, Magazine, Anbauteile, feindliche Gunner, Fraktionsraids, Walkürenritt-Fahrzeuge, Spezialausrüstung und Luftbedrohungen.

Das Projekt besteht aus unabhängigen Fabric- und NeoForge-Modulen. Server und alle verbundenen Clients müssen dieselbe Minecraft-Version, Loader-Familie und Mod-Version verwenden.

## Schnellstart

1. Wähle in der [Kompatibilitätstabelle](#kompatibilität) die passende Minecraft-Version und Loader-Zeile.
2. Installiere den passenden Loader, Fabric API oder NeoForge sowie die dort genannte GeckoLib-Version.
3. Lege die passende `jegn-1.8.2`-JAR in den `mods`-Ordner der Instanz. Fabric- und NeoForge-JARs dürfen nicht gemischt werden.
4. Starte das Spiel einmal, erstelle oder kopiere eine Testwelt und prüfe, ob Just Enough Guns in der Mod-Liste erscheint.
5. Teste erst dann eine langfristige Welt, wenn Rezepte, Tasten, Serverkonfiguration und Zusatzmod-Abhängigkeiten geprüft sind.

### Checkliste für die erste Sitzung

- Stelle die passende Werkbank, Munition und ein kompatibles Magazin her oder finde sie.
- Lade ein Magazin mit der richtigen losen Munition, bevor du es in eine Magazinwaffe einsetzt.
- Prüfe Tasten-Konflikte und teste Schießen, Zielen, Nachladen und Inspektion.
- Nimm Ersatzmagazine, Reparatur- oder Kühlgegenstände und genügend Munition für den Feuermodus mit.
- Beginne auf einem neuen Server mit normalen Gunnern und aktiviere große Raids, Fahrzeuge oder Terror-Phantom-Ereignisse später.

## Kompatibilität

| Loader | Minecraft | Java | Mod | Erforderliche Abhängigkeiten |
| --- | --- | --- | --- | --- |
| Fabric | 1.21.1 | 21 | 1.8.2 | Fabric API, GeckoLib 4.8.3 |
| NeoForge | 1.21.1–1.21.4 | 21 | 1.8.2 | NeoForge 21.1.x, GeckoLib 4.8.3 |
| Fabric | 26.2 | 25 | 1.8.2 | Fabric API, GeckoLib 5.5+ |
| NeoForge | 26.2 | 25 | 1.8.2 | NeoForge 26.2.x, GeckoLib 5.5.1 |
| Fabric | 26.3 | 25 | 1.8.2 | Fabric API, GeckoLib 5.5.7 |
| NeoForge | 26.3 | 25 | 1.8.2 | NeoForge 26.3.x, GeckoLib 5.5.7 |

Fabric 26.1 und NeoForge 26.1 sind Legacy-Zweige. Verwende für die gepflegte Java-25-Reihe 26.2, sofern ein Modpack nicht ausdrücklich 26.1 verlangt.

## Steuerung

### Waffen

| Aktion | Standardtaste |
| --- | --- |
| Schießen | Linksklick |
| Anvisieren | Rechtsklick |
| Nachladen | `R` |
| Animierte Waffe inspizieren | `Y` |
| Waffen-Nahkampf oder Taschenlampe, falls unterstützt | `V` |
| Schleichen-Verhalten, falls unterstützt | `Shift` |

Dies sind die Standardbelegungen ab Version 1.3.0. Ältere Versionen nutzten Rechtsklick zum Schießen, `F` zum Nachladen und `Shift` zum Anvisieren. Prüfe die Tasten nach einem Update erneut.

### Fahrzeuge

| Aktion | Standardtaste |
| --- | --- |
| Einsteigen oder Interagieren | Rechtsklick |
| Lenken und Beschleunigen | `W` / `A` / `S` / `D` |
| Bremsen oder Rückwärtsfahren | `S` |
| Bordwaffe bewegen oder anvisieren | Mausbewegung |
| Aktive Waffe abfeuern | Linksklick |
| Zielen, Lock-on, Zoom oder Sekundärfunktion | Rechtsklick |
| Nachladen, falls unterstützt | `R` |
| Aussteigen | `Shift` |

Der Sitz bestimmt die verfügbaren Aktionen. Fahrersitze bewegen das Fahrzeug; Waffen- oder Copilotensitze können Geschütze, Raketen, Zielerfassung oder Gegenmaßnahmen steuern.

## Gameplay-Leitfaden

### Waffen und Munition

Das Arsenal umfasst Pistolen, Revolver, Gewehre, SMGs, Schrotflinten, Maschinengewehre, Werfer, Bögen, flammenwerferartige Waffen und späte Spezialwaffen. Die meisten Waffen benötigen den passenden Munitionstyp oder ein passendes Magazin.

Magazinwaffen verwenden geladene Magazine. Manuelle und Einzelschusswaffen verwenden ihre passende Munition direkt. Für unterstützte Gewehre, SMGs und Schrotflinten gibt es verlängerte und Trommelmagazine. Anbauteile, Schäfte, Griffe, Visiere, Skins, Abzeichen und Spezialmunition gehören zum normalen Überlebensfortschritt.

Rückstoß, Bewegungsausbreitung, Überhitzung, dynamisches Fadenkreuz, Trefferanzeige, Mündungsfeuer und Geschossspuren zeigen den Waffenstatus. Wenn eine Waffe nicht feuert, prüfe zuerst Munition, Magazin, Hitze, Nachladezustand und Serverkonfiguration.

### Ballistischer Schutz

Kugelsichere Helme und Westen reduzieren Schusswaffenschaden über einen Vergleich von effektiver Durchschlagskraft und Rüstungswert statt über Vanilla-Projektilschutz. Der effektive AP-Wert kommt aus Munition und Waffenmultiplikator. Bei einem geschützten Kopftreffer wird zuerst der Helm verwendet, bei anderen geschützten Körpertreffern die Weste. Ohne das passende Teil bleibt der Schaden unverändert.

Auch Schüsse unter dem Rüstungswert verursachen Teilschaden und verbrauchen Haltbarkeit. Überlegene AP-Werte verursachen mehr Schaden und höheren Haltbarkeitsdruck. Die genauen Werte sind versionsabhängige Balancing-Daten; für eine konkrete Version gelten Tooltip und Branch-Quelle.

### Gunner, Fraktionen und Raids

Gunner können bei Zombie-, Skelett-, Piglin-, Plünderer/Vindicator-, Phantom-, Ghoul- und Parched-Familien erscheinen. Fraktionsereignisse führen von Patrouillen und Fraktionsomen zu Heimkehr-Raids, Raidfackeln, Bossleisten und konfigurierbaren Wellen. Varianten wie der C4-Westenbomber werden per Serverkonfiguration gesteuert.

### Fahrzeuge

Walkürenritt enthält montierte Landfahrzeuge, Boote, Flugzeuge, Hubschrauber und feste Waffenplattformen. Fahrzeuge besitzen Sitze, Inventare, Reparaturwerkzeuge, Lade- oder Energieunterstützung, Raketen, Täuschkörper und eigene HUD-Anzeigen.

Schließe zuerst den Montagefortschritt ab und setze das Fahrzeug anschließend in der Welt ein. Lege erwartete Munition, Reparaturwerkzeuge und geladene Unterstützungsgegenstände in das richtige Fahrzeuginventar. Beobachte Bereitschaft, Nachladen, Lock-Warnungen, Schaden und Gegenmaßnahmen im HUD.

Die gegnerische Fahrzeug-KI kann je nach Fahrzeug patrouillieren, verfolgen, rückwärts fahren, ungeeignetes Gelände meiden und Geschütze steuern. Die gepflegten Zweige teilen das sichtbare Verhalten, obwohl ihre Loader-Implementierungen getrennt bleiben.

### Spezialausrüstung

- **FPV-Drohnen:** Mit dem Monitor werden Kamera und Nutzlast gesteuert; explosive Nutzlasten können in einen Kamikaze-Sturzflug wechseln.
- **C4 und Claymores:** Verwende den passenden Zünder oder Auslöser und nimm zum Entfernen feindlicher Ladungen einen C4-Entschärfer mit.
- **C4-Weste:** Eine konfigurierbare Bomber-Gunner-Variante trägt eine Sprengweste.
- **Javelin und Igla 9K38:** Lock-on funktioniert bei geeigneten Zielen in Reichweite und Sichtlinie; Rauch kann die Raketenerfassung verhindern.
- **Fahrzeug-Raketen-Lock-HUD:** Gültige Ziele in Reichweite erhalten Suchrahmen und akustisches Feedback.

### Terror-Phantom

Terror-Phantom-Inhalte sind seltene Luftbedrohungen mit Bound Terror Phantom, Phantom-Gunner-Beschwörungen, konfigurierbaren Todesdetonationen und End-Ship-Armada-Ereignissen. In der 1.8.0-Spezialausrüstungslinie ist natürliches Spawnen standardmäßig weich deaktiviert und kann in der Serverkonfiguration angepasst werden.

## Zu schwer oder zu einfach?

Ändere jeweils nur eine Gruppe und spiele einige Ingame-Tage. Die folgenden Beispiele gelten für diesen Fabric-26.2-Zweig; Änderungen benötigen OP-Stufe 2.

| Spielerproblem | Erste Anpassung | Wirkung |
| --- | --- | --- |
| Gunner sind am Anfang zu stark | `/justEnoughGuns config combat naturalGunnerDynamicDifficultyEnabled false` | Verhindert die Anpassung an die stärkste Ausrüstung in der Nähe. |
| Patrouillen stören jeden Heimweg | `/justEnoughGuns config patrol minimumDays 15` oder `... spawnChance 0.15` | Verschiebt den frühesten Tag oder senkt den Wurf. |
| Hüftfeuer streut zu stark | `/justEnoughGuns config combat hipFireSpreadMultiplier 1.0` | Der Standard 1.5 wird präziser. |
| Magazine sind zu aufwendig | `/justEnoughGuns config combat magazineFeed false` | Unterstützte Waffen laden direkt aus loser Munition; das Spiel zeigt die neue Munitionsregel an. |
| Raketen oder C4 erscheinen zu früh | Im Menü **Mobs** → **All Gunners** den `Rocket Launcher Start Day` oder `Bomber Gunner Start Day` erhöhen | Verzögert Spezialbedrohungen, ohne normale Gunner zu entfernen. |
| Feindfahrzeuge überfordern die Basis | `/justEnoughGuns config vehicle enemySpawning enabled false` | Stoppt natürliche Feindfahrzeug-Konversionen, Spielerfahrzeuge bleiben aktiv. |
| Explosionen schütteln den Bildschirm zu stark | `config/jeg-client.toml`: `rendering.explosionScreenShake = 0` | Deaktiviert nur die lokale Kamera-Rückmeldung. |

Patrouillen, Fraktionsraids, Gunner-Wachstum und Feindfahrzeuge sind getrennte Systeme. Setze nicht blind alle Chancen auf 0; `-1` bei Wachstumswerten bedeutet, dass der ausgeglichene Standard oder der **All Gunners**-Wert geerbt wird.

## Serverkonfiguration im Spiel

Fabric 26.2 fügt oben im Pausenmenü die Schaltfläche **JEGN Configuration** hinzu. Der Server muss dieselbe Mod-Version verwenden und du brauchst OP-Stufe 2 (`/op <player>` auf einem Dedicated Server).

1. `Esc` drücken und **JEGN Configuration** wählen.
2. **Interface**, **Patrols**, **Mobs**, **Combat** oder **Vehicles** öffnen.
3. Schalter bedienen oder Zahlen eingeben; unter **Mobs** zuerst mit links/rechts den Gunner-Typ auswählen.
4. Ein Tooltip zeigt Befehlsschlüssel, Wertebereich und die `-1`-Vererbung.
5. **Apply** validiert und speichert `config/jeg-server.toml`; **Reset** setzt nur die aktuelle Kategorie zurück. **Done** fragt bei ungespeicherten Änderungen nach dem Verwerfen.

Die Oberfläche deckt UI-Rückmeldung, Patrouillen, Gunner-Wachstum, Magazinmodus, Hüftfeuerstreuung, Geländestützen, Raid-Skalierung und Feindfahrzeuge ab. Lokale HUD-Anzeigen und Bildschirmwackeln bleiben in `jeg-client.toml`.

## Befehlsreferenz

Alle Befehle beginnen mit `/justEnoughGuns`; mit `Tab` werden Vorschläge angezeigt. Ohne letzten Wert wird der aktuelle Wert gelesen, mit Wert wird er gespeichert. Konfigurationsänderungen benötigen OP-Stufe 2.

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

Fraktionen: `night_of_the_undead`, `the_rattlers`, `nosy_business`, `bad_piggies`, `hell_hogs`, `lost_souls`. Patrouillengröße 1–20, Radius 0–16 (Standard 10). `unlockGunRecipes` ist ein spielerbezogener Testbefehl; Patrouillen funktionieren nicht auf Peaceful.

`config mob spawn` akzeptiert die Typen `all`, `skeleton`, `stray`, `zombie`, `husk`, `parched`, `drowned`, `zombieVillager`, `zombifiedPiglin`, `piglin`, `piglinBrute`, `witherSkeleton`, `pillager`, `vindicator`, `generic`. Wichtige Einstellungen sind `minSpawnChance`, `maxSpawnChance`, `spawnChancePerDay`, Waffen-/Rüstungs-Tierwerte, `rocketLauncherStartDay`, `rocketLauncherChance`, `rocketLauncherMaxChance`, `rocketLauncherChancePerDay`, `bomberStartDay`, `bomberChance`, `bomberMaxChance`, `bomberChancePerDay` und `weaponAggression`.

## Konfigurationsdateien

Für direkte Bearbeitung den Server zuerst stoppen. In `config/jeg-client.toml` liegen `rendering.showAmmoHud`, `rendering.showTimersHud`, `rendering.crosshair`, `rendering.showHitmarker`, `rendering.dynamicCrosshairDotMode` und `rendering.explosionScreenShake` (0–100). In `config/jeg-server.toml` sind besonders `combat.magazineFeed`, `combat.hipFireSpreadMultiplier` (0–5), `factionPatrol.*`, `factionRaid.*` und `vehicle.enemyVehicle*` relevant. Änderungen am Magazinmodus aktualisieren die Munitionsmeldung und gefilterte Rezepte.

## Serververwaltung

Nutze die oben beschriebene [Serverkonfiguration im Spiel](#serverkonfiguration-im-spiel) für Live-Änderungen, die [Befehlsreferenz](#befehlsreferenz) für kurze Tests und `config/jeg-server.toml` für nicht angezeigte Werte. Stoppe den Server und sichere die Welt vor direkter TOML-Bearbeitung.

## Fehlerbehebung

### Das Spiel lädt nicht

Vergleiche Minecraft, Loader, Java und GeckoLib mit der Kompatibilitätstabelle. Entferne doppelte oder loaderfremde JARs und teste nur mit den erforderlichen Abhängigkeiten und Just Enough Guns.

### Ein Dedicated Server stürzt beim Start ab

Prüfe, ob die JAR zum Server-Loader und zur Minecraft-Version passt. Client-only-Addons oder eine Fabric-JAR gehören nicht auf einen NeoForge-Server und umgekehrt. Reproduziere in einer sauberen Testinstanz und sichere das erste vollständige Startlog.

### Waffe, Rezept oder Fahrzeug fehlt

Prüfe nach einem sauberen Neustart Rezeptbuch, Item-Suche und Serverlog. Client und Server müssen dieselbe Mod-Datei und eine passende Abhängigkeitsreihe verwenden. Melde die genaue Item-ID und Logzeile.

### Tasten oder HUD erscheinen nicht

Suche nach Tasten-Konflikten, stelle die Standardbelegung wieder her und teste mit aktiviertem dynamischem Fadenkreuz, Trefferanzeige und Munitions-HUD. Fahrzeug-HUDs hängen zusätzlich von Sitz und aktiver Waffe ab.

### Eine Rakete erfasst kein Ziel

Prüfe Reichweite, Sichtlinie, Zieltyp, Munition und Launcher-Zustand. Rauch verhindert die Erfassung absichtlich. Ein Lock-Rahmen erscheint nur bei einem gültigen, sichtbaren Ziel in Reichweite.

## Fehlerberichte

Füge einem Issue Folgendes bei:

1. Minecraft-Version und Loader (Fabric oder NeoForge).
2. Just-Enough-Guns-Version und exakter JAR-Dateiname.
3. Java-, Fabric-API-, NeoForge- und GeckoLib-Version.
4. Kurze Reproduktionsschritte mit Weltzustand und relevanter Konfiguration.
5. Vollständigen Crash-Report oder aktuelles Log; bei visuellen oder Audiofehlern zusätzlich Screenshot oder Aufnahme.
6. Angabe, ob der Fehler ohne andere Addons in einer sauberen Instanz auftritt.

„Es stürzt ab“ oder nur ein Launcher-Screenshot reichen nicht. Die genaue Versionsmatrix und der erste aussagekräftige Stacktrace helfen bei der Zuordnung zu Loader, Abhängigkeit, Mod oder anderem Addon.

## Links für Entwickler und Maintainer

- [Root-README](../../README.md) und [description](../../description.md)
- [1.8.2-Release-Notes](../../CHANGELOG.md)
- [1.8.0-Feature-Notes](../../CHANGELOG.md)
- [Advancement-Leitfaden](../../docs/ADVANCEMENT_GUIDE.md)
- [Notizen zur Gegnerfahrzeug-KI](../../docs/vehicle_enemy_ai.md)
- [Validierungsnotizen](../../docs/VALIDATION.md)

Bei Änderungen am sichtbaren Verhalten zuerst die englische Seite aktualisieren und danach die vier Übersetzungen synchronisieren. Branch-spezifische Implementierungsdetails bleiben im `docs/`-Ordner des jeweiligen Moduls.

## Release, Credits und Lizenz

Release 1.8.2 ergänzt den vierteiligen Fortschrittsleitfaden, die Munitionsregel-Anzeige, Live-Konfigurationsrückmeldung und angepasste Fahrzeugsteuerung. Release 1.8.1 behebt Dedicated-Server-Startpfade und Serverkonfigurationsoptionen. Release 1.8.0 brachte FPV-Drohnen, C4, Claymores, C4-Westen, Javelin, Igla, Rauch gegen Lock-on, Raketen-Lock-HUD, Kill-Credit-Fixes und die Fahrzeug/Raketen-Balance.

Just Enough Guns New ist ein unabhängiger inoffizieller Port und nicht mit Just Enough Guns oder Superb Warfare verbunden oder von ihnen bestätigt. Auf JEG basierender Code steht unter GPL-3.0. Originale JEG-Assets sind ARR und werden mit Genehmigung des Autors verwendet. SBW-abgeleitete Fahrzeug- und Spezialausrüstungsmaterialien behalten ihre Attribution und Lizenzbedingungen; die vollständige Liste steht im Root-README.

