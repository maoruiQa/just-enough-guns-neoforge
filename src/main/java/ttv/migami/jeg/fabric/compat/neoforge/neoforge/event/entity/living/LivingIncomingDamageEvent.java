package ttv.migami.jeg.fabric.compat.neoforge.neoforge.event.entity.living;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;

public class LivingIncomingDamageEvent {
    private final LivingEntity entity;
    private final DamageSource source;
    private float amount;
    private boolean canceled;

    public LivingIncomingDamageEvent(LivingEntity entity, float amount) {
        this(entity, entity.damageSources().generic(), amount);
    }

    public LivingIncomingDamageEvent(LivingEntity entity, DamageSource source, float amount) {
        this.entity = entity;
        this.source = source;
        this.amount = amount;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public float getAmount() {
        return amount;
    }

    public DamageSource getSource() {
        return source;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public boolean isCanceled() {
        return canceled;
    }
}
