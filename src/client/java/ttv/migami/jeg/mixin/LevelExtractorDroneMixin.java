package ttv.migami.jeg.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ttv.migami.jeg.client.SpecialEquipmentClientEvents;

/**
 * MC 26.2 {@link LevelExtractor} skips {@link LocalPlayer} whenever the camera entity is not the
 * local player. That breaks drone FPV: the pilot is never drawn in the world.
 * <p>
 * When controlling a drone, treat the local player as "camera-equal" for that one check so the
 * extractor continues and submits the player avatar.
 */
@Mixin(LevelExtractor.class)
public final class LevelExtractorDroneMixin {
    /**
     * The extract loop does:
     * {@code if (entity instanceof LocalPlayer && camera.entity() != entity) skip;}
     * Returning {@code entity} here makes the comparison succeed so the player is extracted.
     */
    @WrapOperation(
            method = "extract",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;entity()Lnet/minecraft/world/entity/Entity;",
                    ordinal = 3
            )
    )
    private Entity jeg$allowLocalPlayerWhileDrone(
            Camera camera,
            Operation<Entity> original,
            @Local(ordinal = 0) Entity entity
    ) {
        Entity cameraEntity = original.call(camera);
        if (entity instanceof LocalPlayer
                && cameraEntity != entity
                && SpecialEquipmentClientEvents.isControllingDrone()) {
            return entity;
        }
        return cameraEntity;
    }
}
