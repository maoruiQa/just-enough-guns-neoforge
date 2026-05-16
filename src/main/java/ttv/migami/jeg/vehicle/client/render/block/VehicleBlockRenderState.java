package ttv.migami.jeg.vehicle.client.render.block;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;

public final class VehicleBlockRenderState extends BlockEntityRenderState implements GeoRenderState {
    private final Map<DataTicket<?>, Object> geckoData = new HashMap<>();
    public boolean night;

    @Override
    public Map<DataTicket<?>, Object> getDataMap() {
        return this.geckoData;
    }
}
