package ttv.migami.jeg.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.event.GunEvents;
import ttv.migami.jeg.event.MainThreadLevelActionScheduler;

@Mixin(Phantom.class)
public abstract class PhantomFinalizeSpawnMixin {
    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void jeg$finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnData, CallbackInfoReturnable<SpawnGroupData> cir) {
        if (level instanceof ServerLevel serverLevel) {
            Phantom phantom = (Phantom) (Object) this;
            MainThreadLevelActionScheduler.scheduleNextTick(serverLevel, () -> GunEvents.handlePhantomFinalize(phantom, serverLevel, difficulty, spawnType, spawnData));
        }
    }
}
