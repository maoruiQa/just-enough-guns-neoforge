package ttv.migami.jeg.vehicle.client.render;

import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

final class VehicleRenderState extends EntityRenderState implements GeoRenderState {
    VehicleEntity vehicle;
    boolean hideWhileZooming;
    float rootY;
    float yaw;
    float pitch;
    float roll;

    @Override
    public Map<DataTicket<?>, Object> getDataMap() {
        try {
            return (Map<DataTicket<?>, Object>) GeckoLibStateFieldHolder.FIELD.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access GeckoLib render-state data map", e);
        }
    }

    private static final class GeckoLibStateFieldHolder {
        private static final Field FIELD = findField();

        private static Field findField() {
            try {
                Field f = EntityRenderState.class.getDeclaredField("geckolib$data");
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("GeckoLib render-state field 'geckolib$data' not found. Is GeckoLib loaded?", e);
            }
        }
    }
}
