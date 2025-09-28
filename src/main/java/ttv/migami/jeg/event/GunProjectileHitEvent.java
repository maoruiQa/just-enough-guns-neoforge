package ttv.migami.jeg.event;

import ttv.migami.jeg.entity.projectile.ProjectileEntity;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.Cancelable;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * <p>Fired when a projectile hits a block or entity.</p>
 *
 * @author Ocelot
 */
@Cancelable
public class GunProjectileHitEvent extends EntityEvent implements ICancellableEvent
{
    private final HitResult result;
    private final ProjectileEntity projectile;

    public GunProjectileHitEvent(HitResult result, ProjectileEntity projectile)
    {
        super(projectile);
        this.result = result;
        this.projectile = projectile;
    }

    /**
     * @return The result of the entity's ray trace
     */
    public HitResult getRayTrace()
    {
        return result;
    }

    /**
     * @return The projectile that hit
     */
    public ProjectileEntity getProjectile()
    {
        return projectile;
    }
}
