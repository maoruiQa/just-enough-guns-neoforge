package ttv.migami.jeg.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.entity.DroneEntity;

public final class DroneItem extends Item {
    public DroneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        Vec3 position = context.getClickLocation().add(0.0D, 0.25D, 0.0D);
        DroneEntity drone = new DroneEntity(level, context.getPlayer(), position);
        if (!level.addFreshEntity(drone)) {
            return InteractionResult.FAIL;
        }
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.SUCCESS;
    }
}
