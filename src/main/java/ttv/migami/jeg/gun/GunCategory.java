package ttv.migami.jeg.gun;

/**
 * Shared gun category metadata for accuracy and range balancing.
 */
public enum GunCategory {
    SMG(0.85F, 0.45F, 54.0D),
    PISTOL(0.90F, 0.50F, 48.0D),
    RIFLE(1.35F, 0.60F, 80.0D),
    SNIPER(1.50F, 0.45F, 120.0D),
    LMG(1.80F, 0.85F, 96.0D),
    SHOTGUN(1.60F, 0.90F, 40.0D),
    HEAVY(1.45F, 0.70F, 96.0D),
    SPECIAL(1.00F, 0.65F, 60.0D);

    private final float hipMultiplier;
    private final float crouchMultiplier;
    private final double maxRange;

    GunCategory(float hipMultiplier, float crouchMultiplier, double maxRange) {
        this.hipMultiplier = hipMultiplier;
        this.crouchMultiplier = crouchMultiplier;
        this.maxRange = maxRange;
    }

    public float hipMultiplier() {
        return this.hipMultiplier;
    }

    public float crouchMultiplier() {
        return this.crouchMultiplier;
    }

    public double maxRange() {
        return this.maxRange;
    }

    public static GunCategory fromStats(GunStats stats) {
        String path = stats.id().getPath();
        return switch (path) {
            case "custom_smg", "phantom_smg" -> SMG;
            case "combat_pistol", "semi_auto_pistol", "revolver", "finger_gun" -> PISTOL;
            case "bolt_action_rifle", "semi_auto_rifle" -> SNIPER;
            case "light_machine_gun", "minigun", "hollenfire_mk2", "soulhunter_mk2", "flamethrower" -> LMG;
            case "double_barrel_shotgun", "holy_shotgun", "pump_shotgun", "repeating_shotgun", "supersonic_shotgun", "waterpipe_shotgun" -> SHOTGUN;
            case "grenade_launcher", "rocket_launcher", "typhoonee", "hypersonic_cannon" -> HEAVY;
            case "assault_rifle", "blossom_rifle", "burst_rifle", "combat_rifle", "infantry_rifle", "service_rifle", "subsonic_rifle", "abstract_gun" -> RIFLE;
            case "flare_gun" -> SPECIAL;
            default -> SPECIAL;
        };
    }
}
