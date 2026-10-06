package ttv.migami.jeg.vehicle.data.subdata;

/** Superb Warfare 0.8.8 damage settings; legacy JEG test vehicles retain their armor path. */
public record VehicleDamageProfile(boolean superbWarfare, float selfHurtPercent, float selfHurtAmount,
                                   float directionalArmor, boolean defaultImmunities, boolean lowHealthWarning, float mass) {
    public static final VehicleDamageProfile DEFAULT = new VehicleDamageProfile(false, 0.1F, 0.1F, 0.0F, true, true);
    public VehicleDamageProfile(boolean superbWarfare, float selfHurtPercent, float selfHurtAmount,
                                float directionalArmor, boolean defaultImmunities, boolean lowHealthWarning) {
        this(superbWarfare, selfHurtPercent, selfHurtAmount, directionalArmor, defaultImmunities, lowHealthWarning, 1.0F);
    }

    public static float impactDamage(double speed, double verticalSpeed, double roll, boolean horizontal,
                                     boolean vertical, boolean helicopter, boolean unsafeAircraftLanding) {
        if (speed < 0.3D) return 0.0F;
        double damage = horizontal ? 126 * (speed - .4D) * (speed - .4D) : 0;
        if (vertical) {
            if (unsafeAircraftLanding) damage += (8 + Math.abs(roll * .2D)) * (speed - .3D) * (speed - .3D);
            else if (helicopter) damage += 60 * (speed - .5D) * (speed - .5D);
            else if (Math.abs(verticalSpeed) > .4D) damage += 96 * (Math.abs(verticalSpeed) - .4D) * (speed - .3D) * (speed - .3D);
        }
        return (float) damage;
    }

    public static final float PART_MAX_HEALTH = 50.0F;
    public static final float PART_REPAIR_PER_TICK = 0.0025F * PART_MAX_HEALTH;

    public boolean decays(float health, float maxHealth) {
        return health > 0.0F && selfHurtPercent > 0.0F && health <= selfHurtPercent * maxHealth;
    }

    public float directionalMultiplier(double dot) {
        return (float) Math.max(1.0D - directionalArmor * dot, 0.5D);
    }

    public static boolean damaged(float health, boolean wasDamaged) {
        return health < 0.0F || (wasDamaged && health <= 0.95F * PART_MAX_HEALTH);
    }

    public static float repairPart(float health, float amount) {
        return Math.min(health + amount, PART_MAX_HEALTH);
    }

    public static float migratePart(float health, int version) {
        if (!Float.isFinite(health)) return PART_MAX_HEALTH;
        return version == 0 ? health * (PART_MAX_HEALTH / 10.0F) : health;
    }

    public static void main(String[] args) {
        assert impactDamage(.29D, 1, 0, true, true, false, false) == 0;
        assert Math.abs(impactDamage(1, 0, 0, true, false, false, false) - 45.36F) < .001F;
        assert impactDamage(1, 1, 0, false, true, true, false) == 15.0F;
        assert Math.abs(impactDamage(1, 1, 0, false, true, false, false) - 28.224F) < .001F;
        assert DEFAULT.decays(35.0F, 350.0F);
        assert !DEFAULT.decays(35.01F, 350.0F);
        assert !new VehicleDamageProfile(true, 0, 0.1F, 0, true, false).decays(1, 100);
        assert !damaged(0.0F, false) && damaged(-0.01F, false);
        assert damaged(47.5F, true) && !damaged(47.51F, true);
        assert !damaged(1, false);
        assert repairPart(-25, PART_REPAIR_PER_TICK) == -24.875F;
        assert repairPart(49.99F, PART_REPAIR_PER_TICK) == 50.0F;
        assert migratePart(Float.NaN, 0) == PART_MAX_HEALTH;
        assert migratePart(10, 0) == 50 && migratePart(0, 0) == 0 && migratePart(10, 1) == 10;
        VehicleDamageProfile directional = new VehicleDamageProfile(true, .1F, .1F, .4F, true, true);
        assert Math.abs(directional.directionalMultiplier(1) - .6F) < 1e-6;
        assert directional.directionalMultiplier(0) == 1 && Math.abs(directional.directionalMultiplier(-1) - 1.4F) < 1e-6;
    }
}
