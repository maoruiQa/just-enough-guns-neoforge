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
                    .ignoreSwapAnimation()
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

    // Reload animation state (server-driven, synced to clients so render can stay consistent)
    // 0 = none, 1 = start, 2 = loop, 3 = stop
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GUN_RELOAD_STAGE = REGISTER.register(
            "gun_reload_stage",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GUN_RELOAD_TICKS_REMAINING = REGISTER.register(
            "gun_reload_ticks_remaining",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GUN_RELOAD_TICKS_TOTAL = REGISTER.register(
            "gun_reload_ticks_total",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> GUN_RELOAD_END_TICK = REGISTER.register(
            "gun_reload_end_tick",
            () -> DataComponentType.<Long>builder()
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG)
                    .build()
    );
}
