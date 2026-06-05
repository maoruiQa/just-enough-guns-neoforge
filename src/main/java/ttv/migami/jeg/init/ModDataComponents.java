package ttv.migami.jeg.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.attachment.StoredAttachmentStack;

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
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> GUN_MEDALS_ENABLED = REGISTER.register(
            "gun_medals_enabled",
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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GUN_DRAW_TICKS_REMAINING = REGISTER.register(
            "gun_draw_ticks_remaining",
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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GUN_LOADED_MAGAZINE_ITEM = REGISTER.register(
            "gun_loaded_magazine_item",
            () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GUN_RELOAD_FROM_MAGAZINE_ITEM = REGISTER.register(
            "gun_reload_from_magazine_item",
            () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GUN_RELOAD_TO_MAGAZINE_ITEM = REGISTER.register(
            "gun_reload_to_magazine_item",
            () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GUN_SCOPE_ATTACHMENT = attachmentComponent("gun_scope_attachment");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GUN_BARREL_ATTACHMENT = attachmentComponent("gun_barrel_attachment");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GUN_STOCK_ATTACHMENT = attachmentComponent("gun_stock_attachment");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GUN_UNDER_BARREL_ATTACHMENT = attachmentComponent("gun_under_barrel_attachment");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GUN_MAGAZINE_ATTACHMENT = attachmentComponent("gun_magazine_attachment");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GUN_SPECIAL_ATTACHMENT = attachmentComponent("gun_special_attachment");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GUN_PAINT_JOB_ATTACHMENT = attachmentComponent("gun_paint_job_attachment");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GUN_DYE_ATTACHMENT = attachmentComponent("gun_dye_attachment");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GUN_KILL_EFFECT_ATTACHMENT = attachmentComponent("gun_kill_effect_attachment");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StoredAttachmentStack>> GUN_SCOPE_ATTACHMENT_STACK = attachmentStackComponent("gun_scope_attachment_stack");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StoredAttachmentStack>> GUN_BARREL_ATTACHMENT_STACK = attachmentStackComponent("gun_barrel_attachment_stack");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StoredAttachmentStack>> GUN_STOCK_ATTACHMENT_STACK = attachmentStackComponent("gun_stock_attachment_stack");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StoredAttachmentStack>> GUN_UNDER_BARREL_ATTACHMENT_STACK = attachmentStackComponent("gun_under_barrel_attachment_stack");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StoredAttachmentStack>> GUN_MAGAZINE_ATTACHMENT_STACK = attachmentStackComponent("gun_magazine_attachment_stack");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StoredAttachmentStack>> GUN_SPECIAL_ATTACHMENT_STACK = attachmentStackComponent("gun_special_attachment_stack");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StoredAttachmentStack>> GUN_PAINT_JOB_ATTACHMENT_STACK = attachmentStackComponent("gun_paint_job_attachment_stack");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StoredAttachmentStack>> GUN_DYE_ATTACHMENT_STACK = attachmentStackComponent("gun_dye_attachment_stack");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StoredAttachmentStack>> GUN_KILL_EFFECT_ATTACHMENT_STACK = attachmentStackComponent("gun_kill_effect_attachment_stack");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GUN_SCOPE_ATTACHMENT_DAMAGE = attachmentDamageComponent("gun_scope_attachment_damage");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GUN_BARREL_ATTACHMENT_DAMAGE = attachmentDamageComponent("gun_barrel_attachment_damage");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GUN_STOCK_ATTACHMENT_DAMAGE = attachmentDamageComponent("gun_stock_attachment_damage");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GUN_UNDER_BARREL_ATTACHMENT_DAMAGE = attachmentDamageComponent("gun_under_barrel_attachment_damage");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> GUN_FLASHLIGHT_POWERED = REGISTER.register(
            "gun_flashlight_powered",
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build()
    );
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GUN_FLASHLIGHT_BATTERY = REGISTER.register(
            "gun_flashlight_battery",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
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

    private static DeferredHolder<DataComponentType<?>, DataComponentType<StoredAttachmentStack>> attachmentStackComponent(String name) {
        return REGISTER.register(
                name,
                () -> DataComponentType.<StoredAttachmentStack>builder()
                        .persistent(StoredAttachmentStack.CODEC)
                        .networkSynchronized(StoredAttachmentStack.STREAM_CODEC)
                        .build()
        );
    }

    private static DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> attachmentDamageComponent(String name) {
        return REGISTER.register(
                name,
                () -> DataComponentType.<Integer>builder()
                        .persistent(Codec.INT)
                        .networkSynchronized(ByteBufCodecs.VAR_INT)
                        .build()
        );
    }
}
