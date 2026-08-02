package ttv.migami.jeg.client;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.gun.GunDefinitions;

public class FabricModelRegistration implements ModelLoadingPlugin {
    @Override
    public void onInitializeModelLoader(Context context) {
        // Register GUI models for all guns
        GunDefinitions.ALL.keySet().forEach(id -> {
            context.addModels(Reference.id("item/first_person/" + id.getPath()));
            context.addModels(Reference.id("item/gui/" + id.getPath()));
        });

        // Guided launchers are registered outside GunDefinitions.ALL
        context.addModels(Reference.id("item/gui/javelin"));
        context.addModels(Reference.id("item/gui/igla_9k38"));

        // Register special models
        context.addModels(Reference.id("special/holy_shotgun/main"));
        context.addModels(Reference.id("special/holy_shotgun/pumpy"));
        context.addModels(Reference.id("special/typhoonee/main"));
    }
}
