package ttv.migami.jeg.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ttv.migami.jeg.client.SpecialEquipmentClientEvents;

/**
 * MC 1.21.1 {@code LevelRenderer.renderLevel} skips {@link LocalPlayer} whenever the camera entity
 * is not the local player:
 * {@code if (entity instanceof LocalPlayer && camera.getEntity() != entity) continue;}
 * <p>
 * That breaks drone FPV: the pilot is never drawn. The 4th {@code Camera.getEntity()} call in the
 * entity loop is only reached for that LocalPlayer check. Returning the local player there makes
 * the comparison succeed so rendering continues (same approach as Fabric-26.2 LevelExtractorDroneMixin).
 */
@Mixin(LevelRenderer.class)
public final class LevelRendererDroneMixin {
    @WrapOperation(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;getEntity()Lnet/minecraft/world/entity/Entity;",
                    ordinal = 3
            )
    )
    private Entity jeg$allowLocalPlayerWhileDrone(Camera camera, Operation<Entity> original) {
        Entity cameraEntity = original.call(camera);
        if (SpecialEquipmentClientEvents.isControllingDrone()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                return player;
            }
        }
        return cameraEntity;
    }
}
