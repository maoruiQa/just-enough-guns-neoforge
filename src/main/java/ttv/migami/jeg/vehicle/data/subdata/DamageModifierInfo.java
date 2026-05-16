package ttv.migami.jeg.vehicle.data.subdata;

public record DamageModifierInfo(float globalMultiplier) {
    public static final DamageModifierInfo DEFAULT = new DamageModifierInfo(1.0F);

    public float apply(float damage) {
        return Math.max(0.0F, damage * this.globalMultiplier);
    }
}
