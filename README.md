# Just Enough Guns New - Fabric 1.21.1

Version `1.8.2` adds a four-chapter combat-career guide with legacy progress migration, SW-aligned vehicle controls and part damage, ADS/spread adjustments, and server magazine-mode notices with matching recipes. It also fixes delayed trigger release, anti-armor impacts, truck fall damage, and config-menu feedback.

### Highlights

- **Combat career** — 72 visible advancement nodes and 53 core objectives across four chapters, with legacy progress preserved silently.
- **Vehicle warfare** — SW-aligned controls, cameras, crew HUDs, enemy AI, component hitboxes, damage, repair, and collision behavior.
- **Gun handling** — revised ADS recoil and spread, tighter shotgun grouping, configurable hip-fire spread, and reliable trigger release under delayed synchronization.
- **Magazine rules** — a confirmed server-mode notice and recipes that follow the active magazine or direct-ammo mode.

### Fixed

- Corrected anti-armor rocket damage and missile contact with unmounted targets.
- Corrected truck fall immunity and restored the player-skin Finger Gun inventory icon.
- Fixed config tooltip line breaks and guarded config-menu opening failures.
- Corrected vehicle scroll and crew HUD visibility on Fabric 1.21.1.

The dedicated-server monitor tooltip and gunner-growth configuration fixes from `1.8.1` remain included. The FPV drones, C4, mines, guided launchers, smoke denial, and kill-credit work from `1.8.0` remain available.

Requires Java 21, Fabric API, and GeckoLib 4.8.3.

Controls: left click to shoot, right click to aim, R to reload, Y to inspect, and V for supported gun melee/flashlight actions.

Full release notes: [CHANGELOG.md](CHANGELOG.md). Progression: [advancement guide](docs/ADVANCEMENT_GUIDE.md).

Code is GPL-3.0; original JEG assets are ARR under author authorization. See [LICENSE](LICENSE) and the repository asset notices for attribution and asset terms.
