package ttv.migami.jeg.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.world.entity.EntityType;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModTags;
import ttv.migami.jeg.init.ModEntities;

import java.util.concurrent.CompletableFuture;

public class EntityTagGen extends EntityTypeTagsProvider
{
    public EntityTagGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)
    {
        super(output, lookupProvider, Reference.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider)
    {
        this.tag(ModTags.Entities.NONE);

        this.tag(ModTags.Entities.GUNNER)
                .add(EntityType.ZOMBIE)
                .add(EntityType.DROWNED)
                .add(EntityType.SKELETON)
                .add(EntityType.HUSK)
                .add(EntityType.ZOMBIE_VILLAGER)
                .add(EntityType.ZOMBIFIED_PIGLIN)
                .add(EntityType.PIGLIN)
                .add(EntityType.PIGLIN_BRUTE)
                .add(EntityType.VINDICATOR)
                .add(EntityType.PILLAGER)
                .add(EntityType.STRAY)
                .add(EntityType.WITHER_SKELETON)
                .add(ModEntities.GHOUL.get());

        this.tag(ModTags.Entities.HEAVY)
                .add(EntityType.GHAST)
                .add(EntityType.HOGLIN)
                .add(EntityType.POLAR_BEAR)
                .add(EntityType.RAVAGER)
                .add(EntityType.SNIFFER)
                .add(EntityType.TURTLE)
                .add(EntityType.ZOGLIN)
                .add(EntityType.IRON_GOLEM);

        this.tag(ModTags.Entities.VERY_HEAVY)
                .add(EntityType.ELDER_GUARDIAN)
                .add(EntityType.ENDER_DRAGON)
                .add(EntityType.WARDEN)
                .add(EntityType.WITHER)
                .add(EntityType.GIANT)
                .add(ModEntities.TERROR_PHANMTOM.get());

        this.tag(ModTags.Entities.UNDEAD)
                .add(EntityType.ZOMBIE)
                .add(EntityType.DROWNED)
                .add(EntityType.HUSK)
                .add(EntityType.PHANTOM)
                .add(EntityType.SKELETON)
                .add(EntityType.SKELETON_HORSE)
                .add(EntityType.STRAY)
                .add(EntityType.WITHER)
                .add(EntityType.WITHER_SKELETON)
                .add(EntityType.ZOGLIN)
                .add(EntityType.ZOMBIE)
                .add(EntityType.ZOMBIE_HORSE)
                .add(EntityType.ZOMBIFIED_PIGLIN)
                .add(EntityType.ZOMBIE_VILLAGER)
                .add(ModEntities.GHOUL.get())
                .add(ModEntities.BOO.get())
                .add(ModEntities.TERROR_PHANMTOM.get())
                .add(ModEntities.PHANTOM_GUNNER.get());

        this.tag(ModTags.Entities.GHOST)
                .add(ModEntities.BOO.get());

        this.tag(ModTags.Entities.FIRE)
                .add(EntityType.BLAZE)
                .add(EntityType.GHAST)
                .add(EntityType.HOGLIN)
                .add(EntityType.HUSK)
                .add(EntityType.MAGMA_CUBE)
                .add(EntityType.PIGLIN)
                .add(EntityType.PIGLIN_BRUTE)
                .add(EntityType.STRIDER)
                .add(EntityType.WITHER)
                .add(EntityType.WITHER_SKELETON)
                .add(EntityType.ENDERMAN);
    }
}
