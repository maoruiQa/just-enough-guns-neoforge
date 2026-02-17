package net.neoforged.neoforge.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;

public class BuildCreativeModeTabContentsEvent {
    private final ResourceKey<CreativeModeTab> tabKey;

    public BuildCreativeModeTabContentsEvent(ResourceKey<CreativeModeTab> tabKey) {
        this.tabKey = tabKey;
    }

    public ResourceKey<CreativeModeTab> getTabKey() {
        return tabKey;
    }

    public void accept(ItemLike itemLike) {
    }
}
