package ttv.migami.jeg.entity.monster.phantom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;

/**
 * Terror Phantom / Bound Terror Phantom summoned Phantom Gunner variant.
 * Same behaviour as {@link PhantomGunner}, but with lower max health.
 */
public class PhantomGunnerMinion extends PhantomGunner {
    public static final double MAX_HEALTH = 40.0D;

    public PhantomGunnerMinion(EntityType<? extends Phantom> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        // Keep everything identical to PhantomGunner except MAX_HEALTH.
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D);
    }
}
