package ttv.migami.jeg.vehicle.client;

import java.lang.reflect.Method;
import net.minecraft.world.entity.Entity;

public final class VehicleClientHooks {
    private VehicleClientHooks() {}

    public static boolean isLocalPlayer(Entity entity) {
        return entity != null && entity == localPlayer();
    }

    public static Entity localPlayer() {
        try {
            Object minecraft = minecraftInstance();
            return minecraft == null ? null : (Entity) minecraft.getClass().getField("player").get(minecraft);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            return null;
        }
    }

    public static void syncMousePosition() {
        try {
            Object minecraft = minecraftInstance();
            if (minecraft == null) {
                return;
            }
            Object mouseHandler = minecraft.getClass().getField("mouseHandler").get(minecraft);
            Method xpos = mouseHandler.getClass().getMethod("xpos");
            Method ypos = mouseHandler.getClass().getMethod("ypos");
            VehicleClientState.syncMousePosition((double) xpos.invoke(mouseHandler), (double) ypos.invoke(mouseHandler));
        } catch (ReflectiveOperationException | ClassCastException ignored) {
        }
    }

    public static boolean isFirstPersonCamera() {
        try {
            Object minecraft = minecraftInstance();
            if (minecraft == null) {
                return false;
            }
            Object options = minecraft.getClass().getField("options").get(minecraft);
            Object cameraType = options.getClass().getMethod("getCameraType").invoke(options);
            return (boolean) cameraType.getClass().getMethod("isFirstPerson").invoke(cameraType);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            return false;
        }
    }

    private static Object minecraftInstance() throws ReflectiveOperationException {
        Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
        return minecraftClass.getMethod("getInstance").invoke(null);
    }
}
