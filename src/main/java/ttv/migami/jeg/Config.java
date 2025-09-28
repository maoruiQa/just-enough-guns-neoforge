package ttv.migami.jeg;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import ttv.migami.jeg.client.DotRenderMode;
import ttv.migami.jeg.client.SwayType;
import ttv.migami.jeg.client.screen.ButtonAlignment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Config
{
	/**
	 * Client related config options
	 */
	public static class Client
	{
		public final Sounds sounds;
		public final Display display;
		public final Particle particle;
		public final Controls controls;
		public final Experimental experimental;
		public final ModConfigSpec.BooleanValue hideConfigButton;
		public final ModConfigSpec.EnumValue<ButtonAlignment> buttonAlignment;

		public Client(ModConfigSpec.Builder builder)
		{
			builder.push("client");
			{
				this.sounds = new Sounds(builder);
				this.display = new Display(builder);
				this.particle = new Particle(builder);
				this.controls = new Controls(builder);
				this.experimental = new Experimental(builder);
			}
			builder.pop();
			this.hideConfigButton = builder.comment("If enabled, hides the config button from the backpack screen").define("hideConfigButton", false);
			this.buttonAlignment = builder.comment("The alignment of the buttons in the backpack inventory screen").defineEnum("buttonAlignment", ButtonAlignment.RIGHT);
		}
	}

	/**
	 * Sound related config options
	 */
	public static class Sounds
	{
		public final ModConfigSpec.BooleanValue playHitMarkerSound;
		public final ModConfigSpec.BooleanValue playSoundWhenHeadshot;
		public final ModConfigSpec.ConfigValue<String> headshotSound;
		public final ModConfigSpec.BooleanValue playSoundWhenCritical;
		public final ModConfigSpec.ConfigValue<String> criticalSound;
		public final ModConfigSpec.DoubleValue impactSoundDistance;
		public final ModConfigSpec.BooleanValue enchantSound;

		public Sounds(ModConfigSpec.Builder builder)
		{
			builder.comment("Control sounds triggered by guns").push("sounds");
			{
				this.playHitMarkerSound = builder.comment("If true, a sound will play when you successfully hit a shot on a entity with a gun").define("playHitMarkerSound", true);
				this.playSoundWhenHeadshot = builder.comment("If true, a sound will play when you successfully hit a headshot on a entity with a gun").define("playSoundWhenHeadshot", true);
				this.headshotSound = builder.comment("The sound to play when a headshot occurs").define("headshotSound", "minecraft:entity.player.attack.knockback");
				this.playSoundWhenCritical = builder.comment("If true, a sound will play when you successfully hit a critical on a entity with a gun").define("playSoundWhenCritical", true);
				this.criticalSound = builder.comment("The sound to play when a critical occurs").define("criticalSound", "minecraft:entity.player.attack.crit");
				this.impactSoundDistance = builder.comment("The maximum distance impact sounds from bullet can be heard").defineInRange("impactSoundDistance", 32.0, 0.0, 32.0);
				this.enchantSound = builder.comment("If true, the guns will use a different sound when enchanted").define("enchantSound", true);
			}
			builder.pop();
		}
	}

	/**
	 * Display related config options
	 */
	public static class Display
	{
		public final ModConfigSpec.BooleanValue recyclerNote;
		public final ModConfigSpec.BooleanValue oldAnimations;
		public final ModConfigSpec.BooleanValue vanillaSwordTextures;
		public final ModConfigSpec.ConfigValue<String> crosshair;
		public final ModConfigSpec.BooleanValue hitmarker;
		public final ModConfigSpec.DoubleValue dynamicCrosshairBaseSpread;
		public final ModConfigSpec.DoubleValue dynamicCrosshairSpreadMultiplier;
		public final ModConfigSpec.DoubleValue dynamicCrosshairReactivity;
		public final ModConfigSpec.EnumValue<DotRenderMode> dynamicCrosshairDotMode;
		public final ModConfigSpec.BooleanValue onlyRenderDotWhileAiming;
		public final ModConfigSpec.DoubleValue dynamicCrosshairDotThreshold;
		public final ModConfigSpec.BooleanValue showAmmoGUI;
		public final ModConfigSpec.BooleanValue showTimersGUI;
		public final ModConfigSpec.BooleanValue aimingHidesTimers;
		public final ModConfigSpec.BooleanValue classicAmmoGUI;
		public final ModConfigSpec.IntValue displayAmmoGUIXOffset;
		public final ModConfigSpec.IntValue displayAmmoGUIYOffset;
		public final ModConfigSpec.BooleanValue cooldownIndicator;
		public final ModConfigSpec.BooleanValue weaponSway;
		public final ModConfigSpec.DoubleValue swaySensitivity;
		public final ModConfigSpec.EnumValue<SwayType> swayType;
		public final ModConfigSpec.BooleanValue cameraRollEffect;
		public final ModConfigSpec.DoubleValue cameraRollAngle;
		public final ModConfigSpec.BooleanValue restrictCameraRollToWeapons;
		public final ModConfigSpec.BooleanValue forceFirstPersonOnZoomedAim;
		public final ModConfigSpec.DoubleValue firstPersonAimZoomThreshold;
		public final ModConfigSpec.BooleanValue sprintAnimation;
		public final ModConfigSpec.DoubleValue bobbingIntensity;

		public Display(ModConfigSpec.Builder builder)
		{
			builder.comment("Configuration for display related options").push("display");
			{
				this.recyclerNote = builder.comment("If true, a note will appear on the Recycler Station GUI, showing the recipe for Gunmetal.").define("recyclerNote", true);
				this.oldAnimations = builder.comment("If true, uses the old animation poses for weapons. This is only for nostalgic reasons and not recommended to switch back.").define("oldAnimations", false);
				this.vanillaSwordTextures = builder.comment("If true, bayonets will instead use the Minecraft sword texture.").define("vanillaSwordTextures", false);
				this.crosshair = builder.comment("The custom crosshair to use for weapons. Go to (Options > Controls > Mouse Settings > Crosshair) in game to change this!").define("crosshair", "jeg:dynamic");
				this.hitmarker = builder.comment("If true, a hit marker will be rendered upon hitting entities").define("hitmarker", true);
				this.dynamicCrosshairBaseSpread = builder.comment("The resting size of the Dynamic Crosshair when spread is zero.").defineInRange("dynamicCrosshairBaseSpread", 1.0, 0, 5);
				this.dynamicCrosshairSpreadMultiplier = builder.comment("The bloom factor of the Dynamic Crosshair when spread increases.").defineInRange("dynamicCrosshairSpreadMultiplier", 1.0, 1.0, 1.5);
				this.dynamicCrosshairReactivity = builder.comment("How reactive the Dynamic Crosshair is to shooting.").defineInRange("dynamicCrosshairReactivity", 2.0, 0, 10);
				this.dynamicCrosshairDotMode = builder.comment("The rendering mode used for the Dynamic Crosshair's center dot. At Min Spread will only render the dot when gun spread is stable.").defineEnum("dynamicCrosshairDotMode", DotRenderMode.AT_MIN_SPREAD);
				this.onlyRenderDotWhileAiming = builder.comment("If true, the Dynamic Crosshair's center dot will only render while aiming. Obeys dynamicCrosshairDotMode, and has no effect when mode is set to Never.").define("onlyRenderDotWhileAiming", true);
				this.dynamicCrosshairDotThreshold = builder.comment("The threshold of spread (including modifiers) below which the Dynamic Crosshair's center dot is rendered. Affects the At Min Spread and Threshold modes only.").defineInRange("dynamicCrosshairDotThreshold", 0.8, 0, 90);
				this.showAmmoGUI = builder.comment("If enabled, renders a HUD element displaying the gun's current ammo and its name.").define("showAmmoGUI", true);
				this.showTimersGUI = builder.comment("If enabled, renders a HUD element displaying the gun's timers, such as Overheat, Charge and Hold fire timers.").define("showTimersGUI", true);
				this.aimingHidesTimers = builder.comment("If enabled, aiming will hide all the timers.").define("aimingHidesTimers", true);
				this.classicAmmoGUI = builder.comment("If enabled, renders the classic ammo HUD instead of the JEG one.").define("classicAmmoGUI", false);
				this.displayAmmoGUIXOffset = builder.comment("Offsets the ammo HUD by the specified X value.").defineInRange("displayAmmoGUIXOffset", 0, -650, 0);
				this.displayAmmoGUIYOffset = builder.comment("Offsets the ammo HUD by the specified Y value.").defineInRange("displayAmmoGUIYOffset", 0, -350, 0);
				this.cooldownIndicator = builder.comment("If enabled, renders a cooldown indicator to make it easier to learn when you fire again.").define("cooldownIndicator", true);
				this.weaponSway = builder.comment("If enabled, the weapon will sway when the player moves their look direction. This does not affect aiming and is only visual.").define("weaponSway", true);
				this.swaySensitivity = builder.comment("The sensistivity of the visual weapon sway when the player moves their look direciton. The higher the value the more sway.").defineInRange("swaySensitivity", 0.3, 0.0, 1.0);
				this.swayType = builder.comment("The animation to use for sway. Directional follows the camera better while Drag is more immersive").defineEnum("swayType", SwayType.DRAG);
				this.cameraRollEffect = builder.comment("If enabled, the camera will roll when strafing while holding a gun. This creates a more immersive feeling.").define("cameraRollEffect", true);
				this.cameraRollAngle = builder.comment("When Camera Roll Effect is enabled, this is the absolute maximum angle the roll on the camera can approach.").defineInRange("cameraRollAngle", 1.5F, 0F, 45F);
				this.restrictCameraRollToWeapons = builder.comment("When enabled, the Camera Roll Effect is only applied when holding a weapon.").define("restrictCameraRollToWeapons", true);
				this.forceFirstPersonOnZoomedAim = builder.comment("When enabled, temporarily switches the camera to first person while aiming. Aim zoom must be above firstPersonAimZoomThreshold, and only applies to third person rear camera modes.").define("forceFirstPersonOnZoomedAim", false);
				this.firstPersonAimZoomThreshold = builder.comment("The zoom threshold at which the camera switches to first person while aiming. Requires forceFirstPersonOnZoomedAim to be set to true.").defineInRange("firstPersonAimZoomThreshold", 0.5, 0.0, 1.0);
				this.sprintAnimation = builder.comment("Enables the sprinting animation on weapons for better immersion. This only applies to weapons that support a sprinting animation.").define("sprintingAnimation", true);
				this.bobbingIntensity = builder.comment("The intensity of the custom bobbing animation while holding a gun").defineInRange("bobbingIntensity", 1.0, 0.0, 2.0);
			}
			builder.pop();
		}
	}

	/**
	 * Particle related config options
	 */
	public static class Particle
	{
		public final ModConfigSpec.IntValue bulletHoleLifeMin;
		public final ModConfigSpec.IntValue bulletHoleLifeMax;
		public final ModConfigSpec.DoubleValue bulletHoleFadeThreshold;
		public final ModConfigSpec.BooleanValue enableBlood;
		public final ModConfigSpec.DoubleValue impactParticleDistance;

		public Particle(ModConfigSpec.Builder builder)
		{
			builder.comment("Properties relating to particles").push("particle");
			{
				this.bulletHoleLifeMin = builder.comment("The minimum duration in ticks before bullet holes will disappear").defineInRange("bulletHoleLifeMin", 150, 0, Integer.MAX_VALUE);
				this.bulletHoleLifeMax = builder.comment("The maximum duration in ticks before bullet holes will disappear").defineInRange("bulletHoleLifeMax", 200, 0, Integer.MAX_VALUE);
				this.bulletHoleFadeThreshold = builder.comment("The percentage of the maximum life that must pass before particles begin fading away. 0 makes the particles always fade and 1 removes facing completely").defineInRange("bulletHoleFadeThreshold", 0.98, 0, 1.0);
				this.enableBlood = builder.comment("If true, blood will will spawn from entities that are hit from a projectile").define("enableBlood", false);
				this.impactParticleDistance = builder.comment("The maximum distance impact particles can be seen from the player").defineInRange("impactParticleDistance", 32.0, 0.0, 64.0);
			}
			builder.pop();
		}
	}

	public static class Controls
	{
		public final ModConfigSpec.DoubleValue aimDownSightSensitivity;
		public final ModConfigSpec.BooleanValue flipControls;

		public Controls(ModConfigSpec.Builder builder)
		{
			builder.comment("Properties relating to controls").push("controls");
			{
				this.aimDownSightSensitivity = builder.comment("A value to multiple the mouse sensitivity by when aiming down weapon sights. Go to (Options > Controls > Mouse Settings > ADS Sensitivity) in game to change this!").defineInRange("aimDownSightSensitivity", 0.75, 0.0, 1.0);
				this.flipControls = builder.comment("When enabled, switches the shoot and aim controls of weapons. Due to technical reasons, you won't be able to use offhand items if you enable this setting.").define("flipControls", false);
			}
			builder.pop();
		}
	}

	public static class Experimental
	{
		public Experimental(ModConfigSpec.Builder builder)
		{
			builder.comment("Experimental options").push("experimental");
			{
			}
			builder.pop();
		}
	}

	/**
	 * Common config options
	 */
	public static class Common
	{
		public final Gameplay gameplay;
		public final World world;
		public final GunnerMobs gunnerMobs;
		public final Network network;
		public final AggroMobs aggroMobs;
		public final FleeingMobs fleeingMobs;
		public final Missiles missiles;
		public final Grenades grenades;
		public final StunGrenades stunGrenades;
		public final SmokeGrenades smokeGrenades;
		public final ProjectileSpread projectileSpread;

		public Common(ModConfigSpec.Builder builder)
		{
			builder.push("common");
			{
				this.gameplay = new Gameplay(builder);
				this.world = new World(builder);
				this.gunnerMobs = new GunnerMobs(builder);
				this.network = new Network(builder);
				this.aggroMobs = new AggroMobs(builder);
				this.fleeingMobs = new FleeingMobs(builder);
				this.missiles = new Missiles(builder);
				this.grenades = new Grenades(builder);
				this.stunGrenades = new StunGrenades(builder);
				this.smokeGrenades = new SmokeGrenades(builder);
				this.projectileSpread = new ProjectileSpread(builder);
			}
			builder.pop();
		}
	}

	/**
	 * Gameplay related config options
	 */
	public static class Gameplay
	{
		public final Griefing griefing;
		public final ModConfigSpec.BooleanValue dynamicLightsOnShooting;
		public final ModConfigSpec.BooleanValue mobDynamicLightsOnShooting;
		public final ModConfigSpec.BooleanValue drawAnimation;
		public final ModConfigSpec.IntValue bulletProtection;
		public final ModConfigSpec.BooleanValue mobsDropHelmets;
		public final ModConfigSpec.BooleanValue playersDropHelmets;
		public final ModConfigSpec.BooleanValue gunDurability;
		public final ModConfigSpec.BooleanValue gunJamming;
		public final ModConfigSpec.BooleanValue underwaterFiring;
		public final ModConfigSpec.BooleanValue gunAdvantage;
		public final ModConfigSpec.BooleanValue gunModifiers;
		public final ModConfigSpec.DoubleValue growBoundingBoxAmount;
		public final ModConfigSpec.BooleanValue enableHeadShots;
		//public final ModConfigSpec.DoubleValue headShotDamageMultiplier;
		public final ModConfigSpec.DoubleValue criticalDamageMultiplier;
		public final ModConfigSpec.BooleanValue ignoreLeaves;
		public final ModConfigSpec.BooleanValue enableKnockback;
		public final ModConfigSpec.DoubleValue knockbackStrength;
		public final ModConfigSpec.BooleanValue improvedHitboxes;
		public final ModConfigSpec.BooleanValue rocketRiding;
		public final ModConfigSpec.BooleanValue allowFlashlights;
		public final ModConfigSpec.IntValue flashlightDistance;
		public final ModConfigSpec.BooleanValue glowingLaserPointers;
		public final ModConfigSpec.BooleanValue overrideHideMedals;
		public final ModConfigSpec.IntValue maxResonanceLevel;

		public Gameplay(ModConfigSpec.Builder builder)
		{
			builder.comment("Properties relating to gameplay").push("gameplay");
			{
				this.griefing = new Griefing(builder);
				this.drawAnimation = builder.comment("If enabled, animated guns will play a Drawing animation when equipping.").define("drawAnimation", true);
				this.dynamicLightsOnShooting = builder.comment("If enabled, Mobs shooting will create a Dynamic Light block.").define("dynamicLightsOnShooting", true);
				this.mobDynamicLightsOnShooting = builder.comment("If enabled, shooting will create a Dynamic Light block.").define("mobDynamicLightsOnShooting", true);
				this.bulletProtection = builder.comment("The amount of seconds Bullet Protection is applied upon Player respawn.").defineInRange("bulletProtection", 10, 0, 60);
				this.mobsDropHelmets = builder.comment("If enabled, mobs that receive headshots, have a chance to drop their helmets!").define("mobsDropHelmets", true);
				this.playersDropHelmets = builder.comment("If enabled, if players receive a fatal blow to the head, the helmet will cover it!").define("playersDropHelmets", true);
				this.gunDurability = builder.comment("If enabled, both guns and attachment will receive damage upon firing a gun.").define("gunDurability", true);
				this.gunJamming = builder.comment("If enabled, guns will have an increasing chance of jamming the lower durability they have left.").define("gunJamming", true);
				this.gunAdvantage = builder.comment("If enabled, guns will deal less/more damage depending on their advantage.").define("gunAdvantage", true);
				this.gunModifiers = builder.comment("If enabled, guns will generate stat modifiers.").define("gunModifiers", true);
				this.underwaterFiring = builder.comment("If enabled, guns will be able to shoot underwater (There are guns that already do this).").define("underwaterFiring", false);
				this.growBoundingBoxAmount = builder.comment("The extra amount to expand an entity's bounding box when checking for projectile collision. Setting this value higher will make it easier to hit entities").defineInRange("growBoundingBoxAmount", 0.3, 0.0, 1.0);
				this.enableHeadShots = builder.comment("Enables the check for head shots for players. Projectiles that hit the head of a player will have increased damage.").define("enableHeadShots", true);
				//this.headShotDamageMultiplier = builder.comment("The value to multiply the damage by if projectile hit the players head").defineInRange("headShotDamageMultiplier", 1.25, 1.0, Double.MAX_VALUE);
				this.criticalDamageMultiplier = builder.comment("The value to multiply the damage by if projectile is a critical hit").defineInRange("criticalDamageMultiplier", 1.5, 1.0, Double.MAX_VALUE);
				this.ignoreLeaves = builder.comment("If true, projectiles will ignore leaves when checking for collision").define("ignoreLeaves", true);
				this.enableKnockback = builder.comment("If true, projectiles will cause knockback when an entity is hit. By default this is set to true to match the behaviour of Minecraft.").define("enableKnockback", true);
				this.knockbackStrength = builder.comment("Sets the strengthof knockback when shot by a bullet projectile. Knockback must be enabled for this to take effect. If value is equal to zero, knockback will use default minecraft value").defineInRange("knockbackStrength", 0.15, 0.0, 1.0);
				this.improvedHitboxes = builder.comment("If true, improves the accuracy of weapons by considering the ping of the player. This has no affect on singleplayer. This will add a little overhead if enabled.").define("improvedHitboxes", false);
				this.rocketRiding = builder.comment("If true, hitting an entity with a Rocket will make it ride it").define("rocketRiding", true);
				this.allowFlashlights = builder.comment("If false, flashlights will be disabled for the server's sake!").define("allowFlashlights", true);
				this.flashlightDistance = builder.comment("The max distance flashlights can illuminate. Watch out for performance!").defineInRange("flashlightDistance", 32, 1, 64);
				this.glowingLaserPointers = builder.comment("If false, laser pointers will apply Glowing to the entity it hits!").define("glowingLaserPointers", true);
				this.overrideHideMedals = builder.comment("If enabled, the Server will not have medals.").define("overrideHideMedals", false);
				this.maxResonanceLevel = builder.comment("The max Resonance level Guns apply!").defineInRange("maxResonanceLevel", 4, 1, 10);
			}
			builder.pop();
		}
	}

	/**
	 * World related config options
	 */
	public static class World
	{
		//public final ModConfigSpec.IntValue scrapOreWeight;
		public final ModConfigSpec.BooleanValue bossEnchants;
		public final ModConfigSpec.IntValue maxWitheredLevel;
		public final ModConfigSpec.BooleanValue bossRequirements;
		public final ModConfigSpec.BooleanValue booSpawning;
		public final ModConfigSpec.BooleanValue ghoulSpawning;
		public final ModConfigSpec.BooleanValue trumpetSpawning;
		public final ModConfigSpec.BooleanValue creepersDropLiveGrenades;
		public final ModConfigSpec.BooleanValue entitiesDropAmmo;
		//public final ModConfigSpec.BooleanValue spreadSculk;
		public final ModConfigSpec.IntValue playerGunfireVolume;
		public final ModConfigSpec.IntValue mobGunfireVolume;

		public World(ModConfigSpec.Builder builder)
		{
			builder.comment("Properties relating to the JEG world and its ecosystem").push("world");
			{
				//this.scrapOreWeight = builder.comment("Controls the veins per chunk of the Scrap Ore").defineInRange("scrapOreWeight", 15, 0, 99);
				this.bossEnchants = builder.comment("If enabled, killing bosses while holding a Gun/Firearm, will enchant the gun with their respective enchantment").define("bossEnchants", true);
				this.maxWitheredLevel = builder.comment("The max level of Withered by killing the Wither. The maximum cap is 99, have fun.").defineInRange("maxWitheredLevel", 2, 1, 99);
				this.bossRequirements = builder.comment("If enabled, boss enchanments will only be granted if their requirements are met").define("bossRequirements", true);
				this.booSpawning = builder.comment("If disabled, Boos will not generate.").define("booSpawning", true);
				this.ghoulSpawning = builder.comment("If disabled, Ghouls will not generate.").define("ghoulSpawning", true);
				this.trumpetSpawning = builder.comment("If disabled, Trumpet Skeletons will not generate.").define("trumpetSpawning", true);
				this.creepersDropLiveGrenades = builder.comment("If enabled, Creepers have a 5% chance of dropping a live grenade.").define("creepersDropLiveGrenades", true);
				this.entitiesDropAmmo = builder.comment("If enabled, all entities killed by SOME guns, have a chance to drop the Gun's Ammo.").define("entitiesDropEchoShards", true);
				//this.spreadSculk = builder.comment("If enabled, the Hyper-Sonic Cannon will spread Sculk when killing an entity.").define("spreadSculk", true);
				this.playerGunfireVolume = builder.comment("The volume for Player Gunfire. Default is 8").defineInRange("playerGunfireVolume", 8, 1, 10);
				this.mobGunfireVolume = builder.comment("The volume for Mob Gunfire. Default is 8").defineInRange("mobGunfireVolume", 8, 1, 10);
			}
			builder.pop();
		}
	}

	/**
	 * Gunner Mob related thingies
	 */
	public static class GunnerMobs
	{
		public final ModConfigSpec.BooleanValue gunnerMobSpawning;
		public final ModConfigSpec.BooleanValue explosiveMobs;
		public final ModConfigSpec.BooleanValue dropAmmo;
		public final ModConfigSpec.BooleanValue eliteSpawning;
		public final ModConfigSpec.DoubleValue eliteChance;
		public final ModConfigSpec.BooleanValue horsemen;
		public final ModConfigSpec.IntValue minimunDays;
		public final ModConfigSpec.IntValue initialChance;
		public final ModConfigSpec.IntValue chanceIncrement;
		public final ModConfigSpec.IntValue maxChance;
		public final ModConfigSpec.BooleanValue gunnerMobPatrols;
		public final ModConfigSpec.IntValue patrolIntervalDays;
		public final ModConfigSpec.IntValue randomIntervalMinTicks;
		public final ModConfigSpec.IntValue randomIntervalMaxTicks;
		public final ModConfigSpec.IntValue minimumDaysForPatrols;
		public final ModConfigSpec.BooleanValue gunnerMobRaids;
		public final ModConfigSpec.BooleanValue raidSupportMobs;
		public final ModConfigSpec.IntValue raidIntervalDays;
		public final ModConfigSpec.IntValue randomRaidIntervalMinTicks;
		public final ModConfigSpec.IntValue randomRaidIntervalMaxTicks;
		public final ModConfigSpec.IntValue minimumDaysForRaids;
		public final ModConfigSpec.ConfigValue<List<? extends String>> factions;
		public final ModConfigSpec.BooleanValue terrorPhantomDestroyBlocks;
		public final ModConfigSpec.BooleanValue phantomSwarm;
		public final ModConfigSpec.BooleanValue phantomGunnersReplacePhantoms;

		public GunnerMobs(ModConfigSpec.Builder builder) {
			builder.comment("Faction and Gun Configuration").push("gunner_config");
			{
				this.gunnerMobSpawning = builder.comment("If enabled, mobs inside the Factions config will have a chance to spawn with guns.").define("gunnerMobSpawning", true);
				this.explosiveMobs = builder.comment("If enabled, Raids will have a chance to spawn explosive mobs/mobs with explosive charges.").define("explosiveMobs", true);
				this.dropAmmo = builder.comment("If enabled, mobs with guns will have a chance to drop ammo for the gun they are using.").define("dropAmmo", true);
				this.eliteSpawning = builder.comment("If enabled, mobs will have a chance to spawn as Elites.").define("eliteSpawning", true);
				this.eliteChance = builder.comment("The chance for Elite Gunners to spawn, 1.0 is always, 0.0 is never.").defineInRange("eliteChance", 0.3D, 0.1D, 1.0D);
				this.horsemen = builder.comment("If enabled, Elite Gunners have a chance to spawn riding Horses.").define("horsemen", true);
				this.minimunDays = builder.comment("The in-game day where mobs will start spawning, and the spawn chance will increase every day after. 0 is the world-creation day.").defineInRange("minimunDays", 4, 0, 100);
				this.initialChance = builder.comment("This will define the initial chance of mobs spawning with guns. Goes from 0% to 100%").defineInRange("initialChance", 1, 0, 100);
				this.chanceIncrement = builder.comment("This defines the increment of the chance of mobs spawning with guns per day.").defineInRange("chanceIncrement", 1, 0, 100);
				this.maxChance = builder.comment("This will define the max chance of mobs spawning with guns.").defineInRange("maxChance", 50, 1, 100);
				this.terrorPhantomDestroyBlocks = builder.comment("If enabled, the Terror Phantom will destroy blocks with its grenades.").define("terrorPhantomDestroyBlocks", false);
				this.phantomSwarm = builder.comment("If enabled, defeating the Terror Phantom will enable the Phantom Swarm.").define("phantomSwarm", true);
				this.phantomGunnersReplacePhantoms = builder.comment("If enabled, Phantom Gunners will have a chance to replace Phantoms AFTER defeating the Terror Phantom.").define("phantomGunnersReplacePhantoms", true);
				this.gunnerMobPatrols = builder.comment("If enabled, Factions inside the config will have the same chance to spawn like Pillager Patrols.").define("gunnerMobPatrols", true);
				this.patrolIntervalDays = builder.comment("Fixed patrol interval in days. Set to 0 to use a random interval instead.").defineInRange("patrolIntervalDays", 5, 0, 30);
				this.randomIntervalMinTicks = builder.comment("Minimum random interval in ticks if patrolIntervalDays is 0.").defineInRange("randomIntervalMinTicks", 12000, 1, Integer.MAX_VALUE);
				this.randomIntervalMaxTicks = builder.comment("Maximum random interval in ticks if patrolIntervalDays is 0.").defineInRange("randomIntervalMaxTicks", 24000, 1, Integer.MAX_VALUE);
				this.minimumDaysForPatrols = builder.comment("Minimum number of in-game days before patrols can start spawning.").defineInRange("minimumDaysForPatrols", 5, 0, 30);
				this.gunnerMobRaids = builder.comment("If enabled, Factions inside the config will have the same chance to start Raids naturally.").define("gunnerMobRaids", true);
				this.raidSupportMobs = builder.comment("If enabled, Factions will spawn additional Mobs for support.").define("raidSupportMobs", true);
				this.raidIntervalDays = builder.comment("Fixed Raid interval in days. Set to 0 to use a random interval instead.").defineInRange("raidIntervalDays", 30, 0, 100);
				this.randomRaidIntervalMinTicks = builder.comment("Minimum random interval in ticks if raidIntervalDays is 0.").defineInRange("randomRaidIntervalMinTicks", 12000, 1, Integer.MAX_VALUE);
				this.randomRaidIntervalMaxTicks = builder.comment("Maximum random interval in ticks if raidIntervalDays is 0.").defineInRange("randomRaidIntervalMaxTicks", 24000, 1, Integer.MAX_VALUE);
				this.minimumDaysForRaids = builder.comment("Minimum number of in-game days before Factions can start Raids").defineInRange("minimumDaysForRaids", 15, 0, 100);
				this.factions = builder.comment("Define factions, their mobs, and gun pools. Format: faction_name|ai_difficulty|mob1,mob2...|closeGun1,closeGun2...|longGun1,longGun2...|eliteGun1,eliteGun2...")
						.defineList("factions", Arrays.asList(
								"night_of_the_undead" + "|1" +
										"|minecraft:zombie,minecraft:zombie_villager,minecraft:husk" +
										"|jeg:custom_smg,jeg:waterpipe_shotgun" +
										"|jeg:revolver" +
										"|jeg:double_barrel_shotgun",
								"the_rattlers" + "|2" +
										"|minecraft:skeleton,minecraft:stray" +
										"|jeg:custom_smg" +
										"|jeg:semi_auto_rifle" +
										"|jeg:bolt_action_rifle,jeg:semi_auto_rifle",
								"nosy_business" + "|3" +
										"|minecraft:pillager,minecraft:vindicator" +
										"|jeg:pump_shotgun" +
										"|jeg:semi_auto_rifle,jeg:assault_rifle" +
										"|jeg:repeating_shotgun,jeg:service_rifle",
								"bad_piggies" + "|2" +
										"|minecraft:piglin,minecraft:piglin_brute" +
										"|jeg:double_barrel_shotgun,jeg:waterpipe_shotgun" +
										"|jeg:assault_rifle,jeg:semi_auto_rifle"  +
										"|jeg:hollenfire_mk2",
								"hell_hogs" + "|3" +
										"|minecraft:zombified_piglin,minecraft:wither_skeleton" +
										"|jeg:custom_smg,jeg:waterpipe_shotgun,jeg:double_barrel_shotgun" +
										"|jeg:assault_rifle,jeg:semi_auto_rifle" +
										"|jeg:soulhunter_mk2",
								"lost_souls" + "|3" +
										"|jeg:ghoul" +
										"|jeg:waterpipe_shotgun,jeg:revolver" +
										"|jeg:custom_smg,jeg:assault_rifle,jeg:semi_auto_rifle" +
										"|jeg:blossom_rifle,jeg:service_rifle,jeg:infantry_rifle"
						), o -> o instanceof String);

			}
			builder.pop();
		}
	}

	/**
	 * Gun griefing related config options
	 */
	public static class Griefing
	{
		//public final ModConfigSpec.BooleanValue destructionDebri;
		public final ModConfigSpec.BooleanValue enableBlockRemovalOnExplosions;
		public final ModConfigSpec.BooleanValue enableGlassBreaking;
		public final ModConfigSpec.BooleanValue enableWoodBreaking;
		public final ModConfigSpec.BooleanValue enableStoneBreaking;
		public final ModConfigSpec.BooleanValue fragileBlockDrops;
		public final ModConfigSpec.DoubleValue fragileBaseBreakChance;
		public final ModConfigSpec.DoubleValue woodBaseBreakChance;
		public final ModConfigSpec.DoubleValue stoneBaseBreakChance;
		public final ModConfigSpec.BooleanValue setFireToBlocks;
		public final ModConfigSpec.BooleanValue extinguishFire;

		public Griefing(ModConfigSpec.Builder builder)
		{
			builder.comment("Properties related to gun griefing").push("griefing");
			{
				//this.destructionDebri = builder.comment("If true, some attacks will produce (visual) debri").define("destructionDebri", true);
				this.enableBlockRemovalOnExplosions = builder.comment("If enabled, allows block removal on explosions").define("enableBlockRemovalOnExplosions", true);
				this.enableGlassBreaking = builder.comment("If enabled, allows guns to shoot out glass and other fragile objects").define("enableGlassBreaking", true);
				this.enableWoodBreaking = builder.comment("If enabled, allows guns to shoot out wooden related blocks").define("enableWoodBreaking", true);
				this.enableStoneBreaking = builder.comment("If enabled, allows explosive bullets to shoot out stone related blocks").define("enableStoneBreaking", true);
				this.fragileBlockDrops = builder.comment("If enabled, fragile blocks will drop their loot when broken").define("fragileBlockDrops", true);
				this.fragileBaseBreakChance = builder.comment("The base chance that a fragile block is broken when impacted by a bullet. The hardness of a block will scale this value; the harder the block, the lower the final calculated chance will be.").defineInRange("fragileBlockBreakChance", 1.0, 0.0, 1.0);
				this.woodBaseBreakChance = builder.comment("The base chance that a wooden block is broken when impacted by a bullet. The hardness of a block will scale this value; the harder the block, the lower the final calculated chance will be.").defineInRange("woodBaseBreakChance", 0.1, 0.0, 1.0);
				this.stoneBaseBreakChance = builder.comment("The base chance that a wooden block is broken when impacted by a bullet. The hardness of a block will scale this value; the harder the block, the lower the final calculated chance will be.").defineInRange("stoneBaseBreakChance", 0.05, 0.0, 1.0);
				this.setFireToBlocks = builder.comment("If true, allows fire guns to light and spread fires on blocks").define("setFireToBlocks", true);
				this.extinguishFire = builder.comment("If true, allows water guns to extinguish blocks on fire").define("extinguishFire", true);
			}
			builder.pop();
		}
	}

	/**
	 * Network related config options
	 */
	public static class Network
	{
		public final ModConfigSpec.DoubleValue projectileTrackingRange;
		public final ModConfigSpec.BooleanValue firstJoinMessages;

		public Network(ModConfigSpec.Builder builder)
		{
			builder.comment("Properties relating to network").push("network");
			{
				this.firstJoinMessages = builder.comment("If true, Hosts, Creative mode players and OPs will get the welcoming messages and announcements.").define("firstJoinMessages", true);
				this.projectileTrackingRange = builder.comment("The distance players need to be within to be able to track new projectiles trails. Higher values means you can see projectiles from that start from further away.").defineInRange("projectileTrackingRange", 200.0, 1, Double.MAX_VALUE);
			}
			builder.pop();
		}
	}

	/**
	 * Mob aggression related config options
	 */
	public static class AggroMobs
	{
		public final ModConfigSpec.BooleanValue enabled;
		public final ModConfigSpec.BooleanValue angerHostileMobs;
		public final ModConfigSpec.DoubleValue unsilencedRange;
		public final ModConfigSpec.ConfigValue<List<? extends String>> exemptEntities;

		public AggroMobs(ModConfigSpec.Builder builder)
		{
			builder.comment("Properties relating to mob aggression").push("aggro_mobs");
			{
				this.enabled = builder.comment("If true, nearby mobs are angered and/or scared by the firing of guns.").define("enabled", true);
				this.angerHostileMobs = builder.comment("If true, in addition to causing peaceful mobs to panic, firing a gun will also cause nearby hostile mobs to target the shooter.").define("angerHostileMobs", true);
				this.unsilencedRange = builder.comment("Any mobs within a sphere of this radius will aggro on the shooter of an unsilenced gun.").defineInRange("unsilencedRange", 20.0, 0.0, Double.MAX_VALUE);
				this.exemptEntities = builder.comment("Any mobs of defined will not aggro on shooters").defineList("exemptMobs", Collections.emptyList(), o -> true);
			}
			builder.pop();
		}
	}

	/**
	 * Mob fleeing related stuff
	 */
	public static class FleeingMobs
	{
		public final ModConfigSpec.BooleanValue enabled;
		public final ModConfigSpec.DoubleValue silencedRange;
		public final ModConfigSpec.DoubleValue unsilencedRange;
		public final ModConfigSpec.ConfigValue<List<? extends String>> fleeingEntities;

		public FleeingMobs(ModConfigSpec.Builder builder)
		{
			builder.comment("Properties relating to mob fleeing and aggroing!").push("fleeing_mobs");
			{
				this.enabled = builder.comment("If true, nearby mobs will flee from the firing of guns.").define("enabled", true);
				this.unsilencedRange = builder.comment("Any mobs within a sphere of this radius will flee from the shooter of an unsilenced gun.").defineInRange("unsilencedRange", 50.0, 0.0, Double.MAX_VALUE);
				this.silencedRange = builder.comment("Any mobs within a sphere of this radius will flee from the shooter of a silenced gun.").defineInRange("silencedRange", 20.0, 0.0, Double.MAX_VALUE);
				this.fleeingEntities = builder.comment("Any mobs listed here will flee from shooters").defineList("fleeingEntities", Arrays.asList(
						"minecraft:cow", "minecraft:sheep", "minecraft:pig", "minecraft:chicken", "minecraft:wolf", "minecraft:axolotl", "minecraft:cat", "minecraft:frog",
						"minecraft:fox", "minecraft:allay", "minecraft:rabbit", "minecraft:horse", "minecraft:villager", "minecraft:bee", "minecraft:parrot", "minecraft:turtle",
						"minecraft:donkey", "minecraft:mule", "minecraft:llama", "minecraft:panda", "minecraft:mooshroom", "minecraft:strider", "minecraft:ocelot",
						"minecraft:bat", "minecraft:squid", "minecraft:glow_squid", "minecraft:camel"), o -> o instanceof String);
			}
			builder.pop();
		}
	}

	/**
	 * Missile related config options
	 */
	public static class Missiles
	{
		public final ModConfigSpec.DoubleValue explosionRadius;

		public Missiles(ModConfigSpec.Builder builder)
		{
			builder.comment("Properties relating to missiles").push("missiles");
			{
				this.explosionRadius = builder.comment("The max distance which the explosion is effective to").defineInRange("explosionRadius", 5.0, 0.0, Double.MAX_VALUE);
			}
			builder.pop();
		}
	}

	/**
	 * Grenade related config options
	 */
	public static class Grenades
	{
		public final ModConfigSpec.DoubleValue explosionRadius;

		public Grenades(ModConfigSpec.Builder builder)
		{
			builder.comment("Properties relating to grenades").push("grenades");
			{
				this.explosionRadius = builder.comment("The max distance which the explosion is effective to").defineInRange("explosionRadius", 5.0, 0.0, Double.MAX_VALUE);
			}
			builder.pop();
		}
	}

	/**
	 * Stun Grenade related config options
	 */
	public static class StunGrenades
	{
		public final Blind blind;
		public final Deafen deafen;

		public StunGrenades(ModConfigSpec.Builder builder)
		{
			builder.comment("Properties relating to Stun Grenades").push("stun_grenades");
			{
				this.blind = new Blind(builder);
				this.deafen = new Deafen(builder);
			}
			builder.pop();
		}
	}

	/**
	 * Stun Grenade related config options
	 */
	public static class SmokeGrenades
	{
		public final ModConfigSpec.DoubleValue smokeGrenadeCloudDiameter;
		public final ModConfigSpec.DoubleValue smokeGrenadeDamage;
		public final ModConfigSpec.DoubleValue smokeGrenadeCloudDuration;

		public SmokeGrenades(ModConfigSpec.Builder builder)
		{
			builder.comment("Properties relating to Smoke Grenades").push("smoke_grenades");
			{
				this.smokeGrenadeCloudDiameter = builder.comment("Diameter of a Smoke Grenade cloud. Use cautiously when setting high, might cause lag.").defineInRange("smokeGrenadeCloudDiameter", 6.0, 0.0, 15);
				this.smokeGrenadeDamage = builder.comment("Damage per second inside a Smoke Grenade cloud.").defineInRange("smokeGrenadeDamage", 0.0, 0.0, 20);
				this.smokeGrenadeCloudDuration = builder.comment("Duration of a Smoke Grenade cloud in seconds.").defineInRange("smokeGrenadeCloudDuration", 20.0, 0.0, 100);
			}
			builder.pop();
		}
	}

	/**
	 * Stun grenade blinding related config options
	 */
	public static class Blind
	{
		public final EffectCriteria criteria;
		public final ModConfigSpec.BooleanValue blindMobs;

		public Blind(ModConfigSpec.Builder builder)
		{
			builder.comment("Blinding properties of stun grenades").push("blind");
			{
				this.criteria = new EffectCriteria(builder, 15, 220, 10, 170, 0.75, true);
				this.blindMobs = builder.comment("If true, hostile mobs will be unable to target entities while they are blinded by a stun grenade.").define("blindMobs", true);
			}
			builder.pop();
		}
	}

	/**
	 * Stun grenade deafening related config options
	 */
	public static class Deafen
	{
		public final EffectCriteria criteria;
		public final ModConfigSpec.BooleanValue panicMobs;

		public Deafen(ModConfigSpec.Builder builder)
		{
			builder.comment("Deafening properties of stun grenades").push("deafen");
			{
				this.criteria = new EffectCriteria(builder, 15, 280, 100, 360, 0.75, true);
				this.panicMobs = builder.comment("If true, peaceful mobs will panic upon being deafened by a stun grenade.").define("panicMobs", true);
			}
			builder.pop();
		}
	}

	/**
	 * Config options for effect criteria
	 */
	public static class EffectCriteria
	{
		public final ModConfigSpec.DoubleValue radius;
		public final ModConfigSpec.IntValue durationMax;
		public final ModConfigSpec.IntValue durationMin;
		public final ModConfigSpec.DoubleValue angleEffect;
		public final ModConfigSpec.DoubleValue angleAttenuationMax;
		public final ModConfigSpec.BooleanValue raytraceOpaqueBlocks;

		public EffectCriteria(ModConfigSpec.Builder builder, double radius, int durationMax, int durationMin, double angleEffect, double angleAttenuationMax, boolean raytraceOpaqueBlocks)
		{
			builder.push("effect_criteria");
			{
				this.radius = builder.comment("Grenade must be no more than this many meters away to have an effect.").defineInRange("radius", radius, 0.0, Double.MAX_VALUE);
				this.durationMax = builder.comment("Effect will have this duration (in ticks) if the grenade is directly at the player's eyes while looking directly at it.").defineInRange("durationMax", durationMax, 0, Integer.MAX_VALUE);
				this.durationMin = builder.comment("Effect will have this duration (in ticks) if the grenade is the maximum distance from the player's eyes while looking directly at it.").defineInRange("durationMin", durationMin, 0, Integer.MAX_VALUE);
				this.angleEffect = builder.comment("Angle between the eye/looking direction and the eye/grenade direction must be no more than half this many degrees to have an effect.").defineInRange("angleEffect", angleEffect, 0, 360);
				this.angleAttenuationMax = builder.comment("After duration is attenuated by distance, it will be further attenuated depending on the angle (in degrees) between the eye/looking direction and the eye/grenade direction. This is done by multiplying it by 1 (no attenuation) if the angle is 0; and by this value if the angle is the maximum within the angle of effect.").defineInRange("angleAttenuationMax", angleAttenuationMax, 0.0, 1.0);
				this.raytraceOpaqueBlocks = builder.comment("If true, the effect is only applied if the line between the eyes and the grenade does not intersect any non-liquid blocks with an opacity greater than 0.").define("raytraceOpaqueBlocks", raytraceOpaqueBlocks);
			}
			builder.pop();
		}
	}

	/**
	 * Projectile spread config options
	 */
	public static class ProjectileSpread
	{
		public final ModConfigSpec.IntValue spreadThreshold;
		public final ModConfigSpec.IntValue maxCount;

		public ProjectileSpread(ModConfigSpec.Builder builder)
		{
			builder.comment("Properties relating to projectile spread").push("projectile_spread");
			{
				this.spreadThreshold = builder.comment("The amount of time in milliseconds before logic to apply spread is skipped. The value indicates a reasonable amount of time before a weapon is considered stable again.").defineInRange("spreadThreshold", 300, 0, 1000);
				this.maxCount = builder.comment("The amount of times a player has to shoot within the spread threshold before the maximum amount of spread is applied. Setting the value higher means it will take longer for the spread to be applied.").defineInRange("maxCount", 10, 1, Integer.MAX_VALUE);
			}
			builder.pop();
		}
	}

	/**
	 * Server related config options
	 */
	public static class Server
	{
		public final ModConfigSpec.IntValue alphaOverlay;
		public final ModConfigSpec.IntValue alphaFadeThreshold;
		public final ModConfigSpec.DoubleValue soundPercentage;
		public final ModConfigSpec.IntValue soundFadeThreshold;
		public final ModConfigSpec.DoubleValue ringVolume;
		public final ModConfigSpec.DoubleValue gunShotMaxDistance;
		public final ModConfigSpec.DoubleValue reloadMaxDistance;
		public final ModConfigSpec.BooleanValue enableCameraRecoil;
		public final ModConfigSpec.IntValue cooldownThreshold;
		public final Experimental experimental;

		public Server(ModConfigSpec.Builder builder)
		{
			builder.push("server");
			{
				builder.comment("Stun Grenade related properties").push("grenade");
				{
					this.alphaOverlay = builder.comment("After the duration drops to this many ticks, the transparency of the overlay when blinded will gradually fade to 0 alpha.").defineInRange("alphaOverlay", 255, 0, 255);
					this.alphaFadeThreshold = builder.comment("Transparency of the overlay when blinded will be this alpha value, before eventually fading to 0 alpha.").defineInRange("alphaFadeThreshold", 40, 0, Integer.MAX_VALUE);
					this.soundPercentage = builder.comment("Volume of most game sounds when deafened will play at this percent, before eventually fading back to %100.").defineInRange("soundPercentage", 0.05, 0.0, 1.0);
					this.soundFadeThreshold = builder.comment("After the duration drops to this many ticks, the ringing volume will gradually fade to 0 and other sound volumes will fade back to %100.").defineInRange("soundFadeThreshold", 90, 0, Integer.MAX_VALUE);
					this.ringVolume = builder.comment("Volume of the ringing sound when deafened will play at this volume, before eventually fading to 0.").defineInRange("ringVolume", 1.0, 0.0, 1.0);
				}
				builder.pop();

				builder.comment("Audio properties").push("audio");
				{
					this.gunShotMaxDistance = builder.comment("The maximum distance weapons can be heard by players.").defineInRange("gunShotMaxDistance", 100, 0, Double.MAX_VALUE);
					this.reloadMaxDistance = builder.comment("The maximum distance reloading can be heard by players.").defineInRange("reloadMaxDistance", 24, 0, Double.MAX_VALUE);
				}
				builder.pop();

				this.enableCameraRecoil = builder.comment("If true, enables camera recoil when firing a weapon").define("enableCameraRecoil", true);
				this.cooldownThreshold = builder.comment("The maximum amount of cooldown time remaining before the server will accept another shoot packet from a client. This allows for a litle slack since the server may be lagging").defineInRange("cooldownThreshold", 0, 75, 1000);

				this.experimental = new Experimental(builder);
			}
			builder.pop();
		}

		public static class Experimental
		{
			public final ModConfigSpec.BooleanValue forceDyeableAttachments;

			public Experimental(ModConfigSpec.Builder builder)
			{
				builder.push("experimental");
				this.forceDyeableAttachments = builder.comment("Forces all attachments to be dyeable regardless if they have an affect on the model. This is useful if your server uses custom models for attachments and the models have dyeable elements").define("forceDyeableAttachments", false);
				builder.pop();
			}
		}
	}

	static final ModConfigSpec clientSpec;
	public static final Config.Client CLIENT;

	static final ModConfigSpec commonSpec;
	public static final Config.Common COMMON;

	static final ModConfigSpec serverSpec;
	public static final Config.Server SERVER;

	static
	{
		final Pair<Client, ModConfigSpec> clientSpecPair = new ModConfigSpec.Builder().configure(Config.Client::new);
		clientSpec = clientSpecPair.getRight();
		CLIENT = clientSpecPair.getLeft();

		final Pair<Common, ModConfigSpec> commonSpecPair = new ModConfigSpec.Builder().configure(Common::new);
		commonSpec = commonSpecPair.getRight();
		COMMON = commonSpecPair.getLeft();

		final Pair<Server, ModConfigSpec> serverSpecPair = new ModConfigSpec.Builder().configure(Server::new);
		serverSpec = serverSpecPair.getRight();
		SERVER = serverSpecPair.getLeft();
	}

	public static void saveClientConfig()
	{
		clientSpec.save();
	}
}
