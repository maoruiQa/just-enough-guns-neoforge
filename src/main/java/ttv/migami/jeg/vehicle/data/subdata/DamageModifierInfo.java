package ttv.migami.jeg.vehicle.data.subdata;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.init.ModDamageTypes;
import ttv.migami.jeg.vehicle.projectile.VehicleMissileEntity;

/**
 * SW-style vehicle damage modifiers: immune / reduce / multiply, ordered immune → reduce → multiply.
 * <p>
 * Matching is intentionally strict so that SW rules which are different damage types in SuperbWarfare
 * do not all stack on a single JEG {@link DamageSource} (that previously caused one-shot vehicles).
 */
public final class DamageModifierInfo {
    public static final DamageModifierInfo DEFAULT = new DamageModifierInfo(1.0F, List.of());

    private static final Pattern RULE_PATTERN = Pattern.compile(
            "^(?<prefix>(@#|#|@)?)(?<id>[\\w/:.-]+)\\s*(?<operator>[-*])\\s*(?<value>[+-]?\\d+(\\.\\d*)?)$",
            Pattern.CASE_INSENSITIVE
    );

    private final float globalMultiplier;
    private final List<Rule> rules;

    public DamageModifierInfo(float globalMultiplier) {
        this(globalMultiplier, List.of());
    }

    public DamageModifierInfo(float globalMultiplier, List<Rule> rules) {
        this.globalMultiplier = globalMultiplier;
        this.rules = List.copyOf(rules);
    }

    public float globalMultiplier() {
        return this.globalMultiplier;
    }

    public List<Rule> rules() {
        return this.rules;
    }

    /** Legacy path: only global multiplier. Prefer {@link #apply(DamageSource, float)}. */
    public float apply(float damage) {
        return Math.max(0.0F, damage * this.globalMultiplier);
    }

    public float apply(@Nullable DamageSource source, float damage) {
        float result = damage * this.globalMultiplier;
        if (source == null || this.rules.isEmpty()) {
            return Math.max(0.0F, result);
        }
        for (Rule rule : this.rules) {
            if (rule.op == Op.IMMUNE && rule.matches(source)) {
                return 0.0F;
            }
        }
        for (Rule rule : this.rules) {
            if (rule.op == Op.REDUCE && rule.matches(source)) {
                result = Math.max(0.0F, result - rule.value);
            }
        }
        for (Rule rule : this.rules) {
            if (rule.op == Op.MULTIPLY && rule.matches(source)) {
                result *= rule.value;
            }
        }
        return Math.max(0.0F, result);
    }

    public static DamageModifierInfo fromRuleStrings(float globalMultiplier, List<String> rawRules) {
        List<Rule> parsed = new ArrayList<>();
        for (String raw : rawRules) {
            Rule rule = parseRule(raw);
            if (rule != null) {
                parsed.add(rule);
            }
        }
        return new DamageModifierInfo(globalMultiplier, parsed);
    }

    @Nullable
    public static Rule parseRule(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        Matcher matcher = RULE_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            if (trimmed.equalsIgnoreCase("All") || trimmed.equalsIgnoreCase("All 0")) {
                return new Rule(Op.IMMUNE, 0.0F, MatchKind.ALL, "");
            }
            return null;
        }
        String prefix = matcher.group("prefix") == null ? "" : matcher.group("prefix");
        String id = matcher.group("id");
        String operator = matcher.group("operator");
        float value = Float.parseFloat(matcher.group("value"));
        Op op = switch (operator) {
            case "-" -> Op.REDUCE;
            case "*" -> Op.MULTIPLY;
            default -> Op.IMMUNE;
        };
        MatchKind kind;
        String matchId;
        if (id.equalsIgnoreCase("All")) {
            kind = MatchKind.ALL;
            matchId = "";
        } else if (prefix.equals("@#")) {
            kind = MatchKind.ENTITY_TAG;
            matchId = id;
        } else if (prefix.equals("@")) {
            kind = MatchKind.ENTITY_ID;
            matchId = id;
        } else if (prefix.equals("#")) {
            kind = MatchKind.DAMAGE_TAG;
            matchId = id;
        } else {
            kind = MatchKind.DAMAGE_TYPE;
            matchId = id;
        }
        return new Rule(op, value, kind, matchId);
    }

    public enum Op {
        IMMUNE,
        REDUCE,
        MULTIPLY
    }

    public enum MatchKind {
        ALL,
        DAMAGE_TYPE,
        DAMAGE_TAG,
        ENTITY_ID,
        ENTITY_TAG
    }

    public record Rule(Op op, float value, MatchKind kind, String matchId) {
        public boolean matches(DamageSource source) {
            return switch (this.kind) {
                case ALL -> true;
                case DAMAGE_TYPE -> matchesDamageType(source, this.matchId);
                case DAMAGE_TAG -> matchesDamageTag(source, this.matchId);
                case ENTITY_ID -> matchesEntityId(source, this.matchId);
                case ENTITY_TAG -> matchesEntityTag(source, this.matchId);
            };
        }

        /**
         * Strict type matching: each JEG hit should map to at most one SW explosion family rule,
         * mirroring SW where damage types are mutually exclusive.
         * <ul>
         *   <li>Missile explosion ({@link VehicleMissileEntity} + explosion) → {@code projectile_explosion} only</li>
         *   <li>Other explosions (rocket HE, TNT, vanilla) → exact {@code minecraft:explosion} / {@code player_explosion}</li>
         *   <li>{@code custom_explosion} → non-missile explosion that is not a vanilla explosion type key path (unused for rockets)</li>
         * </ul>
         */
        private static boolean matchesDamageType(DamageSource source, String id) {
            String key = normalize(id);
            boolean missileExplosion = isExplosion(source) && isMissileDirect(source);
            boolean nonMissileExplosion = isExplosion(source) && !isMissileDirect(source);

            // Vanilla types: exact only, and never for missile warheads (those use projectile_explosion in SW).
            if (key.equals("minecraft:explosion") || key.equals("explosion")) {
                return nonMissileExplosion && source.is(DamageTypes.EXPLOSION) && !source.is(DamageTypes.PLAYER_EXPLOSION);
            }
            if (key.equals("minecraft:player_explosion") || key.equals("player_explosion")) {
                return nonMissileExplosion && source.is(DamageTypes.PLAYER_EXPLOSION);
            }

            // SW missile blast type — exclusive with vanilla explosion rules above.
            if (key.equals("superbwarfare:projectile_explosion")
                    || key.equals("jeg:projectile_explosion")) {
                return missileExplosion;
            }

            // SW custom HE — only non-missile, and only when not already classified as vanilla explosion/player_explosion.
            // (Prevents stacking custom_explosion * N on top of minecraft:explosion * M for the same rocket hit.)
            if (key.equals("superbwarfare:custom_explosion") || key.equals("jeg:custom_explosion")) {
                return false;
            }

            // SW projectile hit (missile kinetic / direct) — thrown / non-explosion missile sources.
            if (key.equals("superbwarfare:projectile_hit") || key.equals("jeg:projectile_hit")) {
                return isMissileDirect(source) && !isExplosion(source);
            }

            if (key.equals("minecraft:lava") || key.equals("lava")) {
                return source.is(DamageTypes.LAVA) || source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE);
            }
            // Do NOT treat every BulletEntity as an arrow (that zeroed rocket damage on helicopters).
            if (key.equals("minecraft:arrow") || key.equals("arrow")) {
                return source.is(DamageTypes.ARROW);
            }
            if (key.equals("minecraft:trident") || key.equals("trident")) {
                return source.is(DamageTypes.TRIDENT);
            }
            if (key.equals("minecraft:mob_attack") || key.equals("mob_attack")) {
                return source.is(DamageTypes.MOB_ATTACK);
            }
            if (key.equals("minecraft:mob_attack_no_aggro") || key.equals("mob_attack_no_aggro")) {
                return source.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
            }
            if (key.equals("minecraft:mob_projectile") || key.equals("mob_projectile")) {
                return source.is(DamageTypes.MOB_PROJECTILE);
            }
            if (key.equals("minecraft:player_attack") || key.equals("player_attack")) {
                return source.is(DamageTypes.PLAYER_ATTACK);
            }
            if (key.equals("jeg:vehicle_strike") || key.equals("superbwarfare:vehicle_strike")) {
                return source.is(ModDamageTypes.VEHICLE_STRIKE);
            }
            // Kinetic bullets only — not rocket HE (explosion source).
            if (key.equals("jeg:bullet") || key.equals("superbwarfare:bullet")) {
                return isKineticBullet(source);
            }
            return false;
        }

        private static boolean matchesDamageTag(DamageSource source, String id) {
            String key = normalize(id);
            if (key.endsWith("vehicle_strike") || key.equals("superbwarfare:vehicle_strike") || key.equals("jeg:vehicle_strike")) {
                return source.is(ModDamageTypes.VEHICLE_STRIKE);
            }
            // #superbwarfare:projectile — ballistic projectiles only, never HE/rocket/missile blasts.
            if (key.contains("projectile")) {
                return isKineticBullet(source);
            }
            return false;
        }

        private static boolean matchesEntityId(DamageSource source, String id) {
            String key = normalize(id);
            Entity direct = source.getDirectEntity();
            Entity causing = source.getEntity();
            if (direct instanceof VehicleMissileEntity missile) {
                String weapon = missile.weaponId().toString();
                String path = missile.weaponId().getPath();
                if (matchesMissileAlias(key, weapon, path)) {
                    return true;
                }
            }
            if (key.equals("jeg:vehicle_missile") || key.equals("superbwarfare:vehicle_missile")) {
                return direct instanceof VehicleMissileEntity;
            }
            if (direct != null) {
                ResourceLocation typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(direct.getType());
                if (typeId != null && (typeId.toString().equals(key) || typeId.getPath().equals(key))) {
                    return true;
                }
            }
            if (causing != null) {
                ResourceLocation typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(causing.getType());
                if (typeId != null && (typeId.toString().equals(key) || typeId.getPath().equals(key))) {
                    return true;
                }
            }
            if (key.equals("minecraft:tnt") || key.equals("tnt")) {
                return direct != null && direct instanceof net.minecraft.world.entity.item.PrimedTnt;
            }
            if (key.equals("minecraft:tnt_minecart") || key.equals("tnt_minecart")) {
                return direct != null && "tnt_minecart".equals(direct.getType().toShortString());
            }
            return false;
        }

        private static boolean matchesEntityTag(DamageSource source, String id) {
            String key = normalize(id);
            // Do not treat every explosion as aerial bomb / AT rocket.
            if (key.contains("aerial_bomb")) {
                return false;
            }
            if (key.contains("at_rocket")) {
                // Shoulder rockets / vehicle rockets as BulletEntity HE
                return isExplosion(source) && source.getDirectEntity() instanceof BulletEntity;
            }
            return false;
        }

        private static boolean matchesMissileAlias(String key, String weaponId, String weaponPath) {
            if (key.equals(weaponId) || key.equals(weaponPath) || key.equals("jeg:" + weaponPath)) {
                return true;
            }
            if ((key.equals("superbwarfare:javelin_missile") || key.equals("javelin_missile"))
                    && weaponPath.equals("javelin")) {
                return true;
            }
            if ((key.equals("superbwarfare:igla_9k38_missile") || key.equals("igla_9k38_missile"))
                    && weaponPath.equals("igla_9k38")) {
                return true;
            }
            if (key.contains("missile") && key.contains(weaponPath)) {
                return true;
            }
            return false;
        }

        private static boolean isExplosion(DamageSource source) {
            return source.is(DamageTypeTags.IS_EXPLOSION)
                    || source.is(DamageTypes.EXPLOSION)
                    || source.is(DamageTypes.PLAYER_EXPLOSION);
        }

        private static boolean isMissileDirect(DamageSource source) {
            return source.getDirectEntity() instanceof VehicleMissileEntity;
        }

        private static boolean isMissile(DamageSource source) {
            return source.getDirectEntity() instanceof VehicleMissileEntity
                    || source.getEntity() instanceof VehicleMissileEntity;
        }

        /** Normal gun bullets — excludes rocket HE (explosion) and missiles. */
        private static boolean isKineticBullet(DamageSource source) {
            if (isExplosion(source) || isMissileDirect(source)) {
                return false;
            }
            if (source.is(ModDamageTypes.BULLET)) {
                return true;
            }
            return source.getDirectEntity() instanceof BulletEntity;
        }

        private static String normalize(String id) {
            return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        }
    }
}
