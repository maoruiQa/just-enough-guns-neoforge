package ttv.migami.jeg.mixin;

import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.advancement.AdvancementMigration;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMigrationMixin {
    @Shadow @Final private Map<AdvancementHolder, AdvancementProgress> progress;
    @Shadow @Final private Set<AdvancementHolder> progressChanged;
    @Shadow private void markForVisibilityUpdate(AdvancementHolder advancement) {}

    @Inject(method = "load", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/server/PlayerAdvancements;checkForAutomaticTriggers(Lnet/minecraft/server/ServerAdvancementManager;)V"))
    private void jeg$importCareer(ServerAdvancementManager manager, CallbackInfo ci) {
        for (AdvancementHolder holder : AdvancementMigration.apply(manager, this.progress)) {
            this.progressChanged.add(holder);
            this.markForVisibilityUpdate(holder);
        }
    }
}
