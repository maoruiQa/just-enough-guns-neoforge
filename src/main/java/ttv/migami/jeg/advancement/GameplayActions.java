package ttv.migami.jeg.advancement;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.entity.GrenadeEntity;
import ttv.migami.jeg.entity.PlacedExplosiveEntity;
import ttv.migami.jeg.entity.DroneEntity;
import ttv.migami.jeg.entity.TimedThrowableItemProjectile;
import ttv.migami.jeg.event.GunEvents;
import ttv.migami.jeg.faction.GunnerType;
import ttv.migami.jeg.faction.BomberGunnerHelper;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;
import ttv.migami.jeg.faction.FactionSpawnHelper;
import ttv.migami.jeg.gun.GunCategory;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.projectile.VehicleMissileEntity;
import ttv.migami.jeg.vehicle.ai.EnemyVehicleController;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class GameplayActions {
    public static final DeferredRegister<CriterionTrigger<?>> REGISTER = DeferredRegister.create(Registries.TRIGGER_TYPE, Reference.MOD_ID);
    public static final DeferredHolder<CriterionTrigger<?>, GameplayActionTrigger> ACTION = REGISTER.register("gameplay_action", GameplayActionTrigger::new);
    private static final Map<ServerPlayer, Drive> DRIVES = new WeakHashMap<>();

    private GameplayActions() {}

    public static void action(Entity player, String action) { action(player, action, ""); }
    public static void action(Entity player, String action, Object subject) {
        if (player instanceof ServerPlayer serverPlayer) ACTION.get().fire(serverPlayer, action, String.valueOf(subject));
    }

    public static boolean hostile(Entity target, Entity shooter) {
        if (target == null || target == shooter || target instanceof Player || shooter == null || shooter.isAlliedTo(target)) return false;
        if (target instanceof VehicleEntity) return target != shooter.getVehicle()
                && target.getTags().contains(EnemyVehicleController.ENEMY_VEHICLE_TAG);
        return isGunner(target)
                || target instanceof Mob mob && (mob.getType().getCategory() == MobCategory.MONSTER || mob.getTarget() == shooter);
    }

    private static boolean isGunner(Entity target) {
        return target.getTags().contains(GunEvents.JEG_GUNNER_TAG)
                || target.getTags().contains(GunEvents.JEG_ELITE_GUNNER_TAG)
                || target.getTags().contains("jeg_pillager_gunner")
                || target instanceof PhantomGunner
                || target instanceof LivingEntity living && BomberGunnerHelper.isBomber(living);
    }

    public static String weapon(Entity source) {
        if (source instanceof BulletEntity bullet) return bullet.advancementWeaponId();
        if (source instanceof VehicleMissileEntity missile) return missile.advancementWeaponId();
        if (source instanceof GrenadeEntity grenade) return grenade.advancementWeaponId();
        if (source instanceof PlacedExplosiveEntity explosive) return "jeg:" + switch (explosive.kind()) {
            case C4 -> "c4_bomb"; case CLAYMORE -> "claymore_mine"; case TM_62 -> "tm_62";
        };
        if (source instanceof DroneEntity) return "jeg:drone";
        if (source instanceof TimedThrowableItemProjectile throwable) return BuiltInRegistries.ITEM.getKey(throwable.getItem().getItem()).toString();
        return "";
    }

    public static void hit(Entity shooter, Entity source, Entity target, boolean headshot) {
        if (shooter instanceof ServerPlayer player && player.getVehicle() instanceof VehicleEntity mounted && mounted.usesSwControls() && mounted.canPassengerUseSelectedVehicleWeapon(player)) {
            boolean killed = target instanceof VehicleEntity vehicleTarget ? vehicleTarget.vehicleHealth() <= 0 : target instanceof LivingEntity livingTarget && livingTarget.isDeadOrDying();
            ttv.migami.jeg.network.NetworkHandler.sendHitMarker(player, headshot, target instanceof VehicleEntity, killed);
        }
        if (!hostile(target, shooter)) return;
        String weapon = weapon(source);
        if (!weapon.isEmpty()) action(shooter, "hit", weapon);
        String vehicle = source instanceof BulletEntity bullet ? bullet.advancementVehicleId()
                : source instanceof VehicleMissileEntity missile ? missile.advancementVehicleId() : "";
        if (!vehicle.isEmpty() && !weapon.isEmpty()) action(shooter, "vehicle_hit", vehicle + "/" + weapon);
        if (source instanceof VehicleMissileEntity missile && !missile.advancementGuidance().isEmpty()) action(shooter, "guided_hit", missile.advancementGuidance());
        action(shooter, "combat_hit");
        if (shooter.distanceToSqr(target) >= 2500) action(shooter, "range_hit");
        if (headshot) {
            action(shooter, "headshot");
            if (shooter.distanceToSqr(target) >= 2500.0D) action(shooter, "long_headshot");
        }
        if (source instanceof BulletEntity bullet) {
            var stats = bullet.getGunStats();
            if (ModItems.GUNS.containsKey(stats.id())) {
                action(shooter, "fire_mode_hit", GunItem.isAutomatic(stats) ? "automatic" : "single");
                action(shooter, "feed_hit", stats.reloadType());
            }
            if (bullet.advancementAimed()) action(shooter, "aimed_hit");
            for (String attachment : bullet.advancementAttachments().split(",")) if (!attachment.isEmpty()) action(shooter, "attachment_use", attachment);
        }
        if (target instanceof VehicleEntity vehicleTarget && vehicleTarget.vehicleHealth() <= 0) killed(shooter, target, weapon);
        if (target instanceof LivingEntity living && living.isDeadOrDying()) killed(shooter, target, weapon);
    }

    public static void died(LivingEntity target, DamageSource source) {
        if (source != null) hit(source.getEntity(), source.getDirectEntity(), target, false);
    }

    public static void weaponKill(Entity shooter, Entity target, String weapon) {
        if (target instanceof LivingEntity living && living.isDeadOrDying()) killed(shooter, target, weapon);
    }

    private static void killed(Entity shooter, Entity target, String weapon) {
        if (!hostile(target, shooter)) return;
        action(shooter, "combat_kill");
        if (isGunner(target)) action(shooter, "first_gunner");
        if (target.getTags().contains(GunEvents.JEG_ELITE_GUNNER_TAG)) action(shooter, "elite_gunner_kill");
        if (target instanceof LivingEntity living && BomberGunnerHelper.isBomber(living)
                && BomberGunnerHelper.wearingC4Vest(living) && !BomberGunnerHelper.hasDetonated(living)) {
            action(shooter, "bomber_gunner_kill");
        }
        if (isGunner(target) && target instanceof PathfinderMob mob) {
            action(shooter, "gunner_kill", GunnerType.keyFor(mob));
            for (String tag : target.getTags()) {
                if (tag.startsWith(FactionSpawnHelper.PATROL_FACTION_TAG_PREFIX)) action(shooter, "faction_kill", tag.substring(FactionSpawnHelper.PATROL_FACTION_TAG_PREFIX.length()));
            }
        }
        if (target instanceof VehicleEntity vehicle) action(shooter, "enemy_vehicle_kill", vehicle.vehicleDataId());
        action(shooter, "enemy_kill", BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()));
        var holder = ModItems.GUNS.get(Reference.id(weapon.startsWith("jeg:") ? weapon.substring(4) : "unknown"));
        if (holder != null) action(shooter, "category_kill", GunCategory.fromStats(holder.get().getStats()).name().toLowerCase(java.util.Locale.ROOT));
    }

    public static void login(ServerPlayer player) {
        // Re-evaluate possessions, never infer past combat or successful actions from inventory.
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty()) CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), stack);
        }
    }

    public static void tick(ServerPlayer player) {
        if (player.isSpectator() || player.isCreative()) { DRIVES.remove(player); return; }
        if (player.tickCount % 20 == 0) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = player.getItemBySlot(slot);
                if ((slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST) && !stack.isEmpty()) action(player, "equip", BuiltInRegistries.ITEM.getKey(stack.getItem()));
            }
            if (player.containerMenu instanceof ChestMenu) action(player, "open_container");
        }
        if (!(player.getVehicle() instanceof VehicleEntity vehicle)) { DRIVES.remove(player); return; }
        String id = vehicle.vehicleDataId().toString();
        action(player, "mount", id);
        if (vehicle.getControllingPassenger() != player) { DRIVES.remove(player); return; }
        Drive drive = DRIVES.get(player);
        if (drive == null || drive.vehicle != vehicle) {
            drive = new Drive(vehicle); DRIVES.put(player, drive);
        }
        Vec3 current = vehicle.position();
        double step = current.distanceTo(drive.last);
        // Teleports and server corrections do not teach driving.
        if (step <= 8.0D && vehicle.hasDrivingInput()) drive.distance += current.subtract(drive.last).horizontalDistance();
        else if (step > 8.0D) { drive.distance = 0; drive.startY = current.y; drive.airborne = false; }
        drive.last = current;
        boolean helicopter = id.equals("jeg:ah6") || id.equals("jeg:mi28");
        if (!helicopter && drive.distance >= 20 && (!id.equals("jeg:speedboat") || vehicle.isInWater())) action(player, "drive", id);
        if (helicopter && vehicle.hasDrivingInput() && !vehicle.onGround() && current.y - drive.startY >= 10) {
            drive.airborne = true; action(player, "takeoff", id);
        }
        if (helicopter && drive.airborne && vehicle.onGround() && drive.verticalSpeed >= -0.4D && vehicle.vehicleHealth() >= drive.health) {
            action(player, "land", id); action(player, "drive", id); drive.airborne = false;
        }
        drive.verticalSpeed = vehicle.getDeltaMovement().y;
        drive.health = vehicle.vehicleHealth();
    }

    private static final class Drive {
        final VehicleEntity vehicle; Vec3 last; double distance; double startY; double verticalSpeed; float health; boolean airborne;
        Drive(VehicleEntity vehicle) { this.vehicle = vehicle; last = vehicle.position(); startY = last.y; health = vehicle.vehicleHealth(); }
    }
}
