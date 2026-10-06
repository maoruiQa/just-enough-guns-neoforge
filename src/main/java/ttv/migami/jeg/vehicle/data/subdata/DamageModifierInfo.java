package ttv.migami.jeg.vehicle.data.subdata;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
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
                if (result <= 0.0F) return 0.0F;
            }
        }
        for (Rule rule : this.rules) {
            if (rule.op == Op.MULTIPLY && rule.matches(source)) {
                result *= rule.value;
                if (result <= 0.0F) return 0.0F;
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
        // SW also accepts type/tag/entity immunity rules without an operator (e.g. minecraft:fall 0).
        if (trimmed.matches("[^\\s]+\\s+0")) trimmed = trimmed.replaceFirst("\\s+0$", "") + " * 0";
        Matcher matcher = RULE_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            if (trimmed.equalsIgnoreCase("All") || trimmed.equalsIgnoreCase("All 0")) {
                return new Rule(Op.IMMUNE, 0.0F, MatchKind.ALL, "");
            }
            return null;
        }
        String prefix = matcher.group("prefix") == null ? "" : matcher.group("prefix");
        String id = matcher.group("id");
        if (!id.equalsIgnoreCase("All") && ResourceLocation.tryParse(id.toLowerCase(Locale.ROOT)) == null) return null;
        String operator = matcher.group("operator");
        float value;
        try {
            value = Float.parseFloat(matcher.group("value"));
        } catch (NumberFormatException invalid) {
            return null;
        }
        if (!Float.isFinite(value)) return null;
        Op op = switch (operator) {
            case "-" -> Op.REDUCE;
            case "*" -> value == 0.0F ? Op.IMMUNE : Op.MULTIPLY;
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

        /** Exact registry keys plus aliases for JEG shared projectile entities. */
        private static boolean matchesDamageType(DamageSource source, String id) {
            String key = normalize(id);
            boolean missileExplosion = isExplosion(source) && isMissileDirect(source);
            boolean nonMissileExplosion = isExplosion(source) && !isMissileDirect(source) && !isShellOrRocket(source);

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

            // Cannon/rocket HE and vehicle destruction map to SW custom_explosion.
            if (key.equals("superbwarfare:custom_explosion") || key.equals("jeg:custom_explosion")) {
                return source.is(ModDamageTypes.CUSTOM_EXPLOSION) || isExplosion(source) && isShellOrRocket(source);
            }

            // SW projectile hit (missile kinetic / direct) — thrown / non-explosion missile sources.
            if (key.equals("superbwarfare:projectile_hit") || key.equals("jeg:projectile_hit")) {
                return (isMissileDirect(source) || isShellOrRocket(source)) && !isExplosion(source);
            }

            if (key.equals("minecraft:lava") || key.equals("lava")) {
                return source.is(DamageTypes.LAVA);
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
            return source.is(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(key)));
        }

        private static boolean matchesDamageTag(DamageSource source, String id) {
            String key = normalize(id);
            if (source.is(TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(key)))) return true;
            if (key.equals("superbwarfare:vehicle_strike")) return source.is(ModDamageTypes.VEHICLE_STRIKE);
            // Absolute projectile damage is a separate SW tag, never stacked on normal JEG bullets.
            if (key.equals("superbwarfare:projectile_absolute")) return source.is(TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("jeg:sw_projectile_absolute")));
            return key.equals("superbwarfare:projectile") && !isExplosion(source) && !isMissileDirect(source) && !isShellOrRocket(source)
                    && (isKineticBullet(source) || source.is(TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("jeg:sw_projectile"))));
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
            if (direct instanceof BulletEntity bullet && key.equals("superbwarfare:small_cannon_shell")) {
                String weapon = bullet.getGunStats().id().getPath();
                if (weapon.equals("vehicle_20mm_cannon") || weapon.equals("vehicle_30mm_cannon")) return true;
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
            if (direct == null && causing != null) {
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
            Entity direct = source.getDirectEntity();
            if (direct == null) return false;
            if (net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(direct.getType()).is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(key)))) return true;
            return key.equals("superbwarfare:at_rocket") && direct instanceof BulletEntity bullet
                    && ttv.migami.jeg.gun.BallisticProtection.isRocketDirectHit(bullet.getGunStats());
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

        private static boolean isShellOrRocket(DamageSource source) {
            if (!(source.getDirectEntity() instanceof BulletEntity bullet)) return false;
            return switch (bullet.getGunStats().id().getPath()) {
                case "vehicle_20mm_cannon", "vehicle_30mm_cannon", "rocket_launcher", "vehicle_70mm_rocket", "vehicle_80mm_rocket" -> true;
                default -> false;
            };
        }

        /** Normal gun bullets — excludes rocket HE (explosion) and missiles. */
        private static boolean isKineticBullet(DamageSource source) {
            if (isExplosion(source) || isMissileDirect(source) || isShellOrRocket(source)) {
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

    public static void main(String[] args) {
        Rule immune = parseRule("minecraft:fall 0");
        assert immune != null && immune.op() == Op.IMMUNE && immune.kind() == MatchKind.DAMAGE_TYPE;
        assert parseRule("minecraft:fall\t0").op() == Op.IMMUNE;
        assert parseRule("@#superbwarfare:aerial_bomb * 2").kind() == MatchKind.ENTITY_TAG;
        assert parseRule("#superbwarfare:projectile_absolute * .7") == null;
        assert parseRule("minecraft:lava - -13").value() == -13.0F;
        assert parseRule("All - 13").kind() == MatchKind.ALL;
        assert parseRule("minecraft:bad:rule * 2") == null;
        assert parseRule("broken") == null && parseRule("All * NaN") == null;
        assert parseRule("All * 99999999999999999999999999999999999999999999999999999") == null;
    }
}
