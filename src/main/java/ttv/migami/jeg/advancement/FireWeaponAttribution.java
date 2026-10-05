package ttv.migami.jeg.advancement;

import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.vehicle.projectile.VehicleMissileEntity;

/** Short-lived, saved ignition credit for delayed fire damage from JEG weapons. */
public final class FireWeaponAttribution {
    private static final String PREFIX = "jeg_fire_credit|";
    private static final int CREDIT_TICKS = 120;

    private FireWeaponAttribution() {}

    public static void remember(LivingEntity target, Entity source, Entity owner) {
        if (!(target.level() instanceof ServerLevel level) || !(owner instanceof ServerPlayer)
                || !target.isOnFire() || !GameplayActions.hostile(target, owner)) return;
        String weapon = GameplayActions.weapon(source);
        if (weapon.isEmpty()) return;
        String vehicle = source instanceof BulletEntity bullet ? bullet.advancementVehicleId()
                : source instanceof VehicleMissileEntity missile ? missile.advancementVehicleId() : "";
        clear(target);
        target.addTag(PREFIX + owner.getUUID() + "|" + weapon + "|" + vehicle + "|" + (level.getGameTime() + CREDIT_TICKS));
    }

    public static void onDamage(LivingEntity target, DamageSource source, boolean hurt, float healthBefore) {
        if (!(target.level() instanceof ServerLevel level)) return;
        String tag = find(target);
        if (tag == null) return;
        String[] fields = tag.substring(PREFIX.length()).split("\\|", -1);
        if (fields.length != 4) { target.removeTag(tag); return; }
        long expires;
        UUID ownerId;
        try {
            expires = Long.parseLong(fields[3]);
            ownerId = UUID.fromString(fields[0]);
        } catch (IllegalArgumentException invalid) {
            target.removeTag(tag);
            return;
        }
        if (level.getGameTime() >= expires) { target.removeTag(tag); return; }
        if (!hurt || target.getHealth() >= healthBefore || !source.is(DamageTypeTags.IS_FIRE)) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || !GameplayActions.hostile(target, owner)) return;
        GameplayActions.action(owner, "hit", fields[1]);
        GameplayActions.action(owner, "combat_hit");
        if (owner.distanceToSqr(target) >= 2500.0D) GameplayActions.action(owner, "range_hit");
        if (!fields[2].isEmpty()) GameplayActions.action(owner, "vehicle_hit", fields[2] + "/" + fields[1]);
    }

    /** Death can run inside hurt, before its RETURN callback. Consume credit here for lethal fire. */
    public static void onDeath(LivingEntity target, DamageSource source) {
        if (!(target.level() instanceof ServerLevel level) || !source.is(DamageTypeTags.IS_FIRE)) return;
        String tag = find(target);
        if (tag == null) return;
        String[] fields = tag.substring(PREFIX.length()).split("\\|", -1);
        target.removeTag(tag);
        if (fields.length != 4) return;
        try {
            if (level.getGameTime() >= Long.parseLong(fields[3])) return;
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(UUID.fromString(fields[0]));
            if (owner == null || !GameplayActions.hostile(target, owner)) return;
            GameplayActions.action(owner, "hit", fields[1]);
            GameplayActions.action(owner, "combat_hit");
            if (owner.distanceToSqr(target) >= 2500.0D) GameplayActions.action(owner, "range_hit");
            if (!fields[2].isEmpty()) GameplayActions.action(owner, "vehicle_hit", fields[2] + "/" + fields[1]);
            GameplayActions.weaponKill(owner, target, fields[1]);
        } catch (IllegalArgumentException invalid) {
            // Invalid or old entity tags never award progression.
        }
    }

    public static void prune(LivingEntity target) {
        if (target.tickCount % 20 != 0 || !(target.level() instanceof ServerLevel level)) return;
        for (String tag : new ArrayList<>(target.entityTags())) {
            if (!tag.startsWith(PREFIX)) continue;
            String[] fields = tag.substring(PREFIX.length()).split("\\|", -1);
            try {
                if (fields.length != 4 || level.getGameTime() >= Long.parseLong(fields[3]) || !target.isOnFire()) target.removeTag(tag);
            } catch (NumberFormatException invalid) {
                target.removeTag(tag);
            }
        }
    }

    private static String find(LivingEntity target) {
        for (String tag : target.entityTags()) if (tag.startsWith(PREFIX)) return tag;
        return null;
    }

    private static void clear(LivingEntity target) {
        for (String tag : new ArrayList<>(target.entityTags())) if (tag.startsWith(PREFIX)) target.removeTag(tag);
    }
}
