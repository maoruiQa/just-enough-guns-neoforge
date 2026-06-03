package ttv.migami.jeg.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;

public final class ModDataComponents {
    private ModDataComponents() {}

    public static final DeferredRegister<DataComponentType<?>> REGISTER = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Reference.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GUN_AMMO = REGISTER.register(
            "gun_ammo",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GUN_HEAT = REGISTER.register(
            "gun_heat",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> ARMORED_HARNESS_PLATING = REGISTER.register(
            "armored_harness_plating",
            () -> DataComponentType.<Float>builder()
                    .persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT)
                    .build()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> GUN_TRIGGER_LOCK = REGISTER.register(
            "gun_trigger_lock",
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GUN_RELOAD_TICKS_TOTAL = REGISTER.register(
            "gun_reload_ticks_total",
            () -> DataComponentType.<Integer>builder()
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GUN_RELOAD_TICKS_REMAINING = REGISTER.register(
            "gun_reload_ticks_remaining",
            () -> DataComponentType.<Integer>builder()
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GUN_RELOAD_STAGE = REGISTER.register(
            "gun_reload_stage",
            () -> DataComponentType.<Integer>builder()
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GUN_WATER_COOLING_TICKS_TOTAL = REGISTER.register(
            "gun_water_cooling_ticks_total",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GUN_WATER_COOLING_TICKS_REMAINING = REGISTER.register(
            "gun_water_cooling_ticks_remaining",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> MAGAZINE_AMMO_ITEM = REGISTER.register(
            "magazine_ammo_item",
            () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MAGAZINE_AMMO_COUNT = REGISTER.register(
            "magazine_ammo_count",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GUN_SCOPE_ATTACHMENT = attachmentComponent("gun_scope_attachment");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GUN_BARREL_ATTACHMENT = attachmentComponent("gun_barrel_attachment");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GUN_STOCK_ATTACHMENT = attachmentComponent("gun_stock_attachment");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GUN_UNDER_BARREL_ATTACHMENT = attachmentComponent("gun_under_barrel_attachment");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GUN_MAGAZINE_ATTACHMENT = attachmentComponent("gun_magazine_attachment");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GUN_SPECIAL_ATTACHMENT = attachmentComponent("gun_special_attachment");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> GUN_FLASHLIGHT_POWERED = REGISTER.register(
            "gun_flashlight_powered",
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build()
    );

    private static DeferredHolder<DataComponentType<?>, DataComponentType<String>> attachmentComponent(String name) {
        return REGISTER.register(
                name,
                () -> DataComponentType.<String>builder()
                        .persistent(Codec.STRING)
                        .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                        .build()
        );
    }
}
