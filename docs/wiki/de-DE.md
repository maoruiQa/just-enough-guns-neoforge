# Just Enough Guns New — Spieler- und Server-Wiki

**Referenzsprache:** English · **Release:** `1.8.1`

[Wiki-Index](README.md) · [English](en-US.md) · [简体中文](zh-CN.md) · [日本語](ja-JP.md) · [Español](es-ES.md)

## Über diese Mod

Just Enough Guns New ist ein inoffizieller moderner Port von MigaMis Forge-1.20.1-Projekt **Just Enough Guns**. Die Mod behält den Überlebensfortschritt im Vanilla-Stil und ergänzt Schusswaffen, Magazine, Anbauteile, feindliche Gunner, Fraktionsraids, Walkürenritt-Fahrzeuge, Spezialausrüstung und Luftbedrohungen.

Das Projekt besteht aus unabhängigen Fabric- und NeoForge-Modulen. Server und alle verbundenen Clients müssen dieselbe Minecraft-Version, Loader-Familie und Mod-Version verwenden.

## Schnellstart

1. Wähle in der [Kompatibilitätstabelle](#kompatibilität) die passende Minecraft-Version und Loader-Zeile.
2. Installiere den passenden Loader, Fabric API oder NeoForge sowie die dort genannte GeckoLib-Version.
3. Lege die passende `jegn-1.8.1`-JAR in den `mods`-Ordner der Instanz. Fabric- und NeoForge-JARs dürfen nicht gemischt werden.
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
| Fabric | 1.21.1 | 21 | 1.8.1 | Fabric API, GeckoLib 4.8.3 |
| NeoForge | 1.21.1–1.21.4 | 21 | 1.8.1 | NeoForge 21.1.x, GeckoLib 4.8.3 |
| Fabric | 26.2 | 25 | 1.8.1 | Fabric API, GeckoLib 5.5+ |
| NeoForge | 26.2 | 25 | 1.8.1 | NeoForge 26.2.x, GeckoLib 5.5.1 |
| Fabric | 26.3 | 25 | 1.8.1 | Fabric API, GeckoLib 5.5.7 |
| NeoForge | 26.3 | 25 | 1.8.1 | NeoForge 26.3.x, GeckoLib 5.5.7 |

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

## Serververwaltung

Öffne die Serverkonfiguration über das Menü, wenn der Branch dies anbietet, oder bearbeite die erzeugte Konfiguration bei gestopptem Server. Teste Änderungen zuerst mit einer Kopie der Welt.

Die Bereiche umfassen:

- **UI:** Munitions-HUD, Timer-HUD, Fadenkreuz, dynamisches Fadenkreuz und Trefferanzeigen.
- **Gunner:** Umwandlungswahrscheinlichkeiten, Parched-Umwandlung, Todesdetonationen von Phantom Gunnern und Terror-Phantom-Verhalten.
- **Raids:** Fraktionspatrouillen, Raid-Zeit, Wellenanzahl und Genauigkeitsskalierung.
- **Fahrzeuge:** Montage, feindliche Fahrzeugspawns, Kampfverhalten und Unterstützungssysteme.

Neuere Zweige bieten Gunner-Wachstum außerdem über Serverkonfigurationsbefehle. Namen und Optionen können zwischen Loadern und Minecraft-Versionen abweichen; nutze die Hilfe der installierten Version.

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
- [1.8.1-Release-Notes](../../CHANGELOG.md)
- [1.8.0-Feature-Notes](../../CHANGELOG.md)
- [Advancement-Leitfaden](../../docs/ADVANCEMENT_GUIDE.md)
- [Notizen zur Gegnerfahrzeug-KI](../../docs/vehicle_enemy_ai.md)
- [Validierungsnotizen](../../docs/VALIDATION.md)

Bei Änderungen am sichtbaren Verhalten zuerst die englische Seite aktualisieren und danach die vier Übersetzungen synchronisieren. Branch-spezifische Implementierungsdetails bleiben im `docs/`-Ordner des jeweiligen Moduls.

## Release, Credits und Lizenz

Das öffentliche Release 1.8.1 behebt Dedicated-Server-Startpfade und die Verarbeitung von Serverkonfigurationsoptionen in den gepflegten Zweigen. Release 1.8.0 brachte FPV-Drohnen, C4, Claymores, C4-Westen, Javelin, Igla, Rauch gegen Lock-on, Raketen-Lock-HUD, Kill-Credit-Fixes und die Fahrzeug/Raketen-Balance.

Just Enough Guns New ist ein unabhängiger inoffizieller Port und nicht mit Just Enough Guns oder Superb Warfare verbunden oder von ihnen bestätigt. Auf JEG basierender Code steht unter GPL-3.0. Originale JEG-Assets sind ARR und werden mit Genehmigung des Autors verwendet. SBW-abgeleitete Fahrzeug- und Spezialausrüstungsmaterialien behalten ihre Attribution und Lizenzbedingungen; die vollständige Liste steht im Root-README.

