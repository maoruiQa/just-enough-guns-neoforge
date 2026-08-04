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
 * C4 vest bomber gunner kit: equipment, tags, goal wiring, detonation, and loot helpers.
 */
public final class BomberGunnerHelper {
    public static final String TAG = "BomberGunner";
    public static final String ARMED_TAG = "BomberArmed";
    public static final String DETONATED_TAG = "BomberDetonated";

    /** Steady chase C4 beep interval (medium rate, only with line of sight). */
    public static final int IDLE_BEEP_INTERVAL = 12;
    /** Accelerating beep phase after entering detonation range (1.5s). */
    public static final int ACCEL_TICKS = 30;
    /**
     * First portion of the final C4 sound during which the bomber may still move (1.5s).
     * After this, it freezes until detonation.
     */
    public static final int FINAL_MOVABLE_TICKS = 30;
    /** Match PlacedExplosiveEntity final C4 phase length after {@code item.c4.final} starts. */
    public static final int FINAL_TICKS = 39;
    public static final double DETONATE_RANGE = 3.0D;
    public static final float RUSH_SPEED = 1.35F;

    public static final int GUNPOWDER_MIN = 6;
    public static final int GUNPOWDER_MAX = 12;

    private BomberGunnerHelper() {}

    public static boolean isBomber(LivingEntity entity) {
        return entity != null && entity.getTags().contains(TAG);
    }

    public static boolean isArmed(LivingEntity entity) {
        return isBomber(entity) && entity.getTags().contains(ARMED_TAG);
    }

    public static boolean hasDetonated(LivingEntity entity) {
        return entity != null && entity.getTags().contains(DETONATED_TAG);
    }

    public static boolean wearingC4Vest(LivingEntity entity) {
        return entity != null && entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof C4VestItem;
    }

    /**
     * Equip C4 vest, Speed I, clear hands, tag, and bomber detonation goal.
     * Call after normal gunner armor equip so the vest overrides chest armor.
     * Does not set a custom name / nameplate.
     */
    public static void applyBomberKit(PathfinderMob mob) {
        applyBomberKit(mob, mob.goalSelector);
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
        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 0, false, true));
        ensureBomberGoal(mob, goals);
    }

    public static void ensureBomberGoal(PathfinderMob mob) {
        ensureBomberGoal(mob, mob.goalSelector);
    }

    public static void ensureBomberGoal(PathfinderMob mob, GoalSelector goals) {
        if (mob.level().isClientSide() || !isBomber(mob) || hasBomberGoal(goals)) {
            return;
        }
        goals.addGoal(1, new BomberDetonateGoal(mob));
    }

    public static boolean hasBomberGoal(PathfinderMob mob) {
        return hasBomberGoal(mob.goalSelector);
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

    /**
     * Beep interval during the 1.5s accelerating phase: medium-fast → very fast.
     */
    public static int accelBeepInterval(int fuseTicks) {
        // 10 ticks at start of accel → 2 ticks at end.
        int t = Math.max(0, Math.min(ACCEL_TICKS - 1, fuseTicks));
        return Math.max(2, 10 - (t * 8) / Math.max(1, ACCEL_TICKS - 1));
    }

    /** True once the final C4 sound has started (after the accel beep phase). */
    public static boolean isInFinalPhase(int fuseTicks) {
        return fuseTicks >= ACCEL_TICKS;
    }

    /** True after the first 1.5s of the final sound — bomber freezes until detonation. */
    public static boolean isFrozen(int fuseTicks) {
        return fuseTicks >= ACCEL_TICKS + FINAL_MOVABLE_TICKS;
    }

    public static boolean shouldDetonate(int fuseTicks) {
        return fuseTicks >= ACCEL_TICKS + FINAL_TICKS;
    }

    /**
     * Blast equal to a worn C4 vest (2× remote C4). Safe to call from death or fuse completion.
     */
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

    /** Fuse completed: explode and remove the bomber. */
    public static void detonateAndKill(PathfinderMob bomber) {
        explodeVest(bomber);
        if (bomber.isAlive()) {
            bomber.kill();
        }
    }

    public static ItemStack rollGunpowderDrop(LivingEntity entity) {
        int amount = GUNPOWDER_MIN + entity.getRandom().nextInt(GUNPOWDER_MAX - GUNPOWDER_MIN + 1);
        return new ItemStack(Items.GUNPOWDER, amount);
    }
}
