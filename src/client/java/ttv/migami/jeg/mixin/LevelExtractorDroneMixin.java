package ttv.migami.jeg.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ttv.migami.jeg.client.SpecialEquipmentClientEvents;

/**
 * MC 26.2 {@code LevelExtractor.extractVisibleEntities} skips {@link LocalPlayer} whenever the
 * camera entity is not the local player. That breaks drone FPV: the pilot is never drawn.
 * <p>
 * The skip is:
 * {@code if (entity instanceof LocalPlayer && camera.entity() != entity) continue;}
 * The 4th {@code Camera.entity()} call in that method is only reached for LocalPlayer. Returning
 * the local player there makes the comparison succeed so extraction continues.
 */
@Mixin(LevelExtractor.class)
public final class LevelExtractorDroneMixin {
    @WrapOperation(
            method = "extractVisibleEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;entity()Lnet/minecraft/world/entity/Entity;",
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
