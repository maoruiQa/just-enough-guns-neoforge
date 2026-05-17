package ttv.migami.jeg.vehicle.client.render.block;

import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;

public final class VehicleBlockRenderState extends BlockEntityRenderState implements GeoRenderState {
    public boolean night;

    @Override
    @SuppressWarnings("unchecked")
    public Map<DataTicket<?>, Object> getDataMap() {
        try {
            return (Map<DataTicket<?>, Object>) GeckoLibStateFieldHolder.FIELD.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access GeckoLib block render-state data map", e);
        }
    }

    private static final class GeckoLibStateFieldHolder {
        private static final Field FIELD = findField();

        private static Field findField() {
            try {
                Field f = BlockEntityRenderState.class.getDeclaredField("geckolib$data");
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("GeckoLib block render-state field 'geckolib$data' not found. Is GeckoLib loaded?", e);
            }
        }
    }
}
