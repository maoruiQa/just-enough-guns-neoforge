package net.neoforged.neoforge.registries;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DeferredRegister<T> {
    private final Object registryKey;
    private final String modId;
    private final List<Entry<T, ? extends T>> entries = new ArrayList<>();

    private DeferredRegister(Object registryKey, String modId) {
        this.registryKey = registryKey;
        this.modId = modId;
    }

    public static <T> DeferredRegister<T> create(Object registryKey, String modId) {
        return new DeferredRegister<>(registryKey, modId);
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Supplier<I> supplier) {
        DeferredHolder<T, I> holder = new DeferredHolder<>(supplier);
        entries.add(new Entry<>(name, holder));
        return holder;
    }

    public void register(IEventBus bus) {
        Registry<T> registry = resolveRegistry();
        for (Entry<T, ? extends T> entry : entries) {
            Identifier id = Identifier.fromNamespaceAndPath(modId, entry.name);
            if (!registry.containsKey(id)) {
                Registry.register(registry, id, entry.holder.get());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Registry<T> resolveRegistry() {
        if (registryKey == Registries.ITEM) {
            return (Registry<T>) BuiltInRegistries.ITEM;
        }
        if (registryKey == Registries.ENTITY_TYPE) {
            return (Registry<T>) BuiltInRegistries.ENTITY_TYPE;
        }
        if (registryKey == Registries.SOUND_EVENT) {
            return (Registry<T>) BuiltInRegistries.SOUND_EVENT;
        }
        if (registryKey == Registries.PARTICLE_TYPE) {
            return (Registry<T>) BuiltInRegistries.PARTICLE_TYPE;
        }
        if (registryKey == Registries.MOB_EFFECT) {
            return (Registry<T>) BuiltInRegistries.MOB_EFFECT;
        }
        if (registryKey == Registries.DATA_COMPONENT_TYPE) {
            return (Registry<T>) BuiltInRegistries.DATA_COMPONENT_TYPE;
        }
        if (registryKey == Registries.STRUCTURE_TYPE) {
            return (Registry<T>) BuiltInRegistries.STRUCTURE_TYPE;
        }
        if (registryKey == Registries.STRUCTURE_PIECE) {
            return (Registry<T>) BuiltInRegistries.STRUCTURE_PIECE;
        }
        throw new IllegalStateException("Unsupported registry key: " + registryKey);
    }

    private static final class Entry<R, V extends R> {
        private final String name;
        private final DeferredHolder<R, V> holder;

        private Entry(String name, DeferredHolder<R, V> holder) {
            this.name = name;
            this.holder = holder;
        }
    }
}
