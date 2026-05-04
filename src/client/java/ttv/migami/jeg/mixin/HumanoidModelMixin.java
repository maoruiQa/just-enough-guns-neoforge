package ttv.migami.jeg.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.item.GunItem;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {
    @Shadow
    public ModelPart rightArm;

    @Shadow
    public ModelPart leftArm;

    @Unique
    private static final int JEG_RELOAD_STAGE_START = 1;

    @Unique
    private static final int JEG_RELOAD_STAGE_LOOP = 2;

    @Unique
    private static final int JEG_RELOAD_STAGE_STOP = 3;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void jeg$animateReloadingGunArms(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (!(entity instanceof Player player)) {
            return;
        }

        ItemStack stack = jeg$getReloadingGun(player);
        if (stack.isEmpty()) {
            return;
        }

        int remainingTicks = stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0);
        int totalTicks = Math.max(1, stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_TOTAL.get(), remainingTicks));
        float progress = 1.0F - Mth.clamp(remainingTicks / (float) totalTicks, 0.0F, 1.0F);
        float wave = Mth.sin(progress * ((float) Math.PI * 4.0F));
        int stage = stack.getOrDefault(ModDataComponents.GUN_RELOAD_STAGE.get(), 0);

        float lift = switch (stage) {
            case JEG_RELOAD_STAGE_START -> -0.10F;
            case JEG_RELOAD_STAGE_LOOP -> -0.18F;
            case JEG_RELOAD_STAGE_STOP -> -0.06F;
            default -> -0.14F;
        };
        float pump = wave * (stage == JEG_RELOAD_STAGE_LOOP ? 0.16F : 0.10F);
        float roll = 0.08F + Mth.abs(wave) * 0.04F;
        int direction = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;

        this.rightArm.xRot += lift + pump;
        this.leftArm.xRot += lift - pump * 0.6F;
        this.rightArm.yRot += direction * wave * 0.04F;
        this.leftArm.yRot -= direction * wave * 0.04F;
        this.rightArm.zRot += direction * roll;
        this.leftArm.zRot -= direction * roll;
    }

    @Unique
    private static ItemStack jeg$getReloadingGun(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (jeg$isReloadingGun(mainHand)) {
            return mainHand;
        }
        ItemStack offHand = player.getOffhandItem();
        return jeg$isReloadingGun(offHand) ? offHand : ItemStack.EMPTY;
    }

    @Unique
    private static boolean jeg$isReloadingGun(ItemStack stack) {
        return stack.getItem() instanceof GunItem
                && stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0) > 0;
    }
}
