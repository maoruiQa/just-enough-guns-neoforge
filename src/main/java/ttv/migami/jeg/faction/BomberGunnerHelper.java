package ttv.migami.jeg.faction;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.PlacedExplosiveEntity;
import ttv.migami.jeg.entity.ai.BomberDetonateGoal;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.item.C4VestItem;
import ttv.migami.jeg.util.SpecialExplosion;

/**
 * C4 vest bomber gunner kit for Fabric 26.2. Goal selector must be passed via MobAccessor.
 */
public final class BomberGunnerHelper {
    public static final String TAG = "BomberGunner";
    public static final String ARMED_TAG = "BomberArmed";
    public static final String DETONATED_TAG = "BomberDetonated";

    public static final int IDLE_BEEP_INTERVAL = 12;
    public static final int ACCEL_TICKS = 30;
    public static final int FINAL_MOVABLE_TICKS = 30;
    public static final int FINAL_TICKS = 39;
    public static final double DETONATE_RANGE = 3.0D;
    public static final float RUSH_SPEED = 1.35F;

    public static final int GUNPOWDER_MIN = 6;
    public static final int GUNPOWDER_MAX = 12;

    private BomberGunnerHelper() {}

    public static boolean isBomber(LivingEntity entity) {
        return entity != null && entity.entityTags().contains(TAG);
    }

    public static boolean isArmed(LivingEntity entity) {
        return isBomber(entity) && entity.entityTags().contains(ARMED_TAG);
    }

    public static boolean hasDetonated(LivingEntity entity) {
        return entity != null && entity.entityTags().contains(DETONATED_TAG);
    }

    public static boolean wearingC4Vest(LivingEntity entity) {
        return entity != null && entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof C4VestItem;
    }

    public static void applyBomberKit(PathfinderMob mob, GoalSelector goals) {
        if (mob.level().isClientSide()) {
            return;
        }

        mob.addTag(TAG);
        mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        mob.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.C4_VEST.get()));
        mob.setDropChance(EquipmentSlot.CHEST, 0.0F);
        mob.addEffect(new MobEffectInstance(MobEffects.SPEED, -1, 0, false, true));
        ensureBomberGoal(mob, goals);
    }

    public static void ensureBomberGoal(PathfinderMob mob, GoalSelector goals) {
        if (mob.level().isClientSide() || !isBomber(mob) || hasBomberGoal(goals)) {
            return;
        }
        goals.addGoal(1, new BomberDetonateGoal(mob));
    }

    public static boolean hasBomberGoal(GoalSelector goals) {
        return goals.getAvailableGoals().stream()
                .anyMatch(goal -> goal.getGoal() instanceof BomberDetonateGoal);
    }

    public static void markArmed(LivingEntity entity) {
        if (entity != null) {
            entity.addTag(ARMED_TAG);
        }
    }

    public static void playBeep(LivingEntity entity) {
        playC4Sound(entity, "item.c4.beep", 0.9F, 1.0F);
    }

    public static void playFinalBeep(LivingEntity entity) {
        playC4Sound(entity, "item.c4.final", 1.15F, 1.0F);
    }

    private static void playC4Sound(LivingEntity entity, String path, float volume, float pitch) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        var holder = ModSounds.ALL.get(Reference.id(path));
        if (holder != null) {
            entity.level().playSound(
                    null,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    holder.get(),
                    SoundSource.HOSTILE,
                    volume,
                    pitch
            );
        }
    }

    public static int accelBeepInterval(int fuseTicks) {
        int t = Math.max(0, Math.min(ACCEL_TICKS - 1, fuseTicks));
        return Math.max(2, 10 - (t * 8) / Math.max(1, ACCEL_TICKS - 1));
    }

    public static boolean isInFinalPhase(int fuseTicks) {
        return fuseTicks >= ACCEL_TICKS;
    }

    public static boolean isFrozen(int fuseTicks) {
        return fuseTicks >= ACCEL_TICKS + FINAL_MOVABLE_TICKS;
    }

    public static boolean shouldDetonate(int fuseTicks) {
        return fuseTicks >= ACCEL_TICKS + FINAL_TICKS;
    }

    public static void explodeVest(LivingEntity bomber) {
        if (bomber == null || bomber.level().isClientSide() || hasDetonated(bomber)) {
            return;
        }
        bomber.addTag(DETONATED_TAG);
        bomber.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);

        if (!(bomber.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        for (int i = 0; i < 2; i++) {
            SpecialExplosion.explodeAt(
                    serverLevel,
                    bomber.position(),
                    bomber,
                    PlacedExplosiveEntity.C4_REMOTE_DAMAGE,
                    PlacedExplosiveEntity.C4_REMOTE_RADIUS,
                    SpecialExplosion.Tier.HUGE
            );
        }
    }

    public static void detonateAndKill(PathfinderMob bomber) {
        explodeVest(bomber);
        if (bomber.isAlive() && bomber.level() instanceof ServerLevel serverLevel) {
            bomber.kill(serverLevel);
        }
    }

    public static ItemStack rollGunpowderDrop(LivingEntity entity) {
        int amount = GUNPOWDER_MIN + entity.getRandom().nextInt(GUNPOWDER_MAX - GUNPOWDER_MIN + 1);
        return new ItemStack(Items.GUNPOWDER, amount);
    }
}
