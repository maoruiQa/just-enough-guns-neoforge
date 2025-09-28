package ttv.migami.jeg.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.annotation.Validator;
import ttv.migami.jeg.client.util.Easings;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.network.PacketHandler;
import ttv.migami.jeg.network.message.S2CMessageUpdateGuns;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Author: MrCrayfish
 */
// @Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class NetworkGunManager extends SimplePreparableReloadListener<Map<ResourceLocation, Gun>>
{
    public static final Path CONFIG_PACK_DIR = FMLPaths.CONFIGDIR.get()
            .resolve("jeg");

    public static final Path CONFIG_GUN_DIR = FMLPaths.CONFIGDIR.get()
            .resolve("jeg")
            .resolve("data")
            .resolve("guns");

    public static final String CONFIG_NAMESPACE = Reference.MOD_ID + "_cfg";

    private static final int FILE_TYPE_LENGTH_VALUE = ".json".length();
    private static final Gson GSON_INSTANCE = Util.make(() -> {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(ResourceLocation.class, JsonDeserializers.RESOURCE_LOCATION);
        builder.registerTypeAdapter(FireMode.class, JsonDeserializers.FIRE_MODE);
        builder.registerTypeAdapter(ReloadType.class, JsonDeserializers.RELOAD_TYPE);
        builder.registerTypeAdapter(GripType.class, JsonDeserializers.GRIP_TYPE);
        builder.registerTypeAdapter(Easings.class, JsonDeserializers.EASING);
        builder.excludeFieldsWithModifiers(Modifier.TRANSIENT);
        return builder.create();
    });

    private static final List<GunItem> clientRegisteredGuns = new ArrayList<>();
    private static NetworkGunManager instance;

    private Map<ResourceLocation, Gun> registeredGuns = new HashMap<>();
    private static final Map<ResourceLocation, Gun> clientConfigGuns = new HashMap<>();
    public static final Map<ResourceLocation, Gun> clientConfigGunIDs = new HashMap<>();

    public static Gun getClientGun(ResourceLocation id) {
        return clientConfigGuns.get(id);
    }

    @Override
    protected Map<ResourceLocation, Gun> prepare(ResourceManager rm,
                                                 ProfilerFiller profiler) {

        Map<ResourceLocation, Gun> map = new HashMap<>();

        // This method created a new Data Gun for every single Gun in other mods...
        /*rm.listResources("guns", path -> path.getPath().endsWith(".json"))
                .forEach((resLoc, resource) -> {
                    try (Reader reader = new BufferedReader(
                            new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {

                        Gun gun = GsonHelper.fromJson(GSON_INSTANCE, reader, Gun.class);
                        if (gun != null && Validator.isValidObject(gun)) {

                            String fileName = resLoc.getPath();
                            String idPath = fileName.substring(
                                    fileName.lastIndexOf('/') + 1, fileName.length() - FILE_TYPE_LENGTH_VALUE);

                            ResourceLocation gunId = new ResourceLocation(resLoc.getNamespace(), idPath);
                            map.put(gunId, gun);
                        } else {
                            JustEnoughGuns.LOGGER.error("Malformed gun file {}", resLoc);
                        }
                    } catch (Exception ex) {
                        JustEnoughGuns.LOGGER.error("Couldn't read {}", resLoc, ex);
                    }
                });*/

        /* Original hard-coded Guns which are still really cool! */
        java.util.stream.StreamSupport.stream(BuiltInRegistries.ITEM.spliterator(), false)
                .filter(it -> it instanceof GunItem)
                .forEach(item -> {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                    if (id == null) return;

                    rm.listResources("guns",
                                    path -> path.getPath().endsWith(id.getPath() + ".json"))
                            .keySet()
                            .stream()
                            .sorted((r1, r2) ->
                                    Boolean.compare(r2.getNamespace().equals(Reference.MOD_ID),
                                            r1.getNamespace().equals(Reference.MOD_ID)))
                            .forEach(resLoc -> readJson(resLoc.getNamespace(), resLoc.getPath(),
                                    () -> rm.getResource(resLoc).get().open(), map, id));
                });

        /* Awesome new Config/Data Guns! */
        try {
            Files.createDirectories(CONFIG_GUN_DIR);

            try (Stream<Path> files = Files.walk(CONFIG_GUN_DIR)) {
                files.filter(p -> p.toString().endsWith(".json"))
                        .forEach(p -> {
                            readJson(CONFIG_NAMESPACE,
                                    p.getFileName().toString(),
                                    () -> Files.newInputStream(p),
                                    map, null);

                            String base = p.getFileName().toString().replace(".json", "");
                            ResourceLocation id = new ResourceLocation(CONFIG_NAMESPACE, base);
                            clientConfigGunIDs.put(id, map.get(id));
                        });
            }
        } catch (IOException e) {
            JustEnoughGuns.LOGGER.error("Couldn't scan {}", CONFIG_GUN_DIR, e);
        }

        return map;
    }

    @Override
    protected void apply(Map<ResourceLocation, Gun> parsedGuns,
                         ResourceManager rm, ProfilerFiller profiler) {

        this.registeredGuns = ImmutableMap.copyOf(parsedGuns);

        parsedGuns.forEach((id, gun) -> {
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item instanceof GunItem gunItem) {
                gunItem.setGun(new Supplier(gun));
            }
        });

        if (FMLEnvironment.dist == Dist.CLIENT) {
            List<ItemStack> samples = new ArrayList<>();

            for (ResourceLocation id : parsedGuns.keySet()) {
                Item item = BuiltInRegistries.ITEM.get(id);
                ItemStack stack;
                if (item instanceof GunItem) {
                    stack = new ItemStack(item);
                } else {
                    stack = GunItem.makeGunStack(id);
                    samples.add(stack);
                }
            }
            //ClientSideCache.INSTANCE.setCreativeSamples(samples);
        }
    }

    /**
     * Writes all registered guns into the provided packet buffer
     *
     * @param buffer a packet buffer get
     */
    public void writeRegisteredGuns(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(this.registeredGuns.size());
        this.registeredGuns.forEach((id, gun) -> {
            buffer.writeResourceLocation(id);
            buffer.writeNbt(gun.serializeNBT());
        });
    }

    /**
     * Reads all registered guns from the provided packet buffer
     *
     * @param buffer a packet buffer get
     * @return a map of registered guns from the server
     */
    public static ImmutableMap<ResourceLocation, Gun> readRegisteredGuns(FriendlyByteBuf buffer)
    {
        int size = buffer.readVarInt();
        if(size > 0)
        {
            ImmutableMap.Builder<ResourceLocation, Gun> builder = ImmutableMap.builder();
            for(int i = 0; i < size; i++)
            {
                ResourceLocation id = buffer.readResourceLocation();
                Gun gun = Gun.create(buffer.readNbt());
                builder.put(id, gun);
            }
            return builder.build();
        }
        return ImmutableMap.of();
    }

    public static boolean updateRegisteredGuns(S2CMessageUpdateGuns message)
    {
        return updateRegisteredGuns(message.getRegisteredGuns());
    }

    /**
     * Updates registered guns from data provided by the server
     *
     * @return true if all registered guns were able to update their corresponding gun item
     */
    private static boolean updateRegisteredGuns(Map<ResourceLocation, Gun> guns)
    {
        clientRegisteredGuns.clear();
        guns.forEach((id, gun) -> {
            Item item = BuiltInRegistries.ITEM.get(id);

            GunItem targetItem;
            if (item instanceof GunItem gi) {
                targetItem = gi;

                targetItem.setGun(new Supplier(gun));
                clientRegisteredGuns.add(targetItem);
            } else {
                targetItem = ModItems.ABSTRACT_GUN.get();
                clientConfigGuns.put(id, gun);
            }
        });
        return true;
    }

    /**
     * Gets a map of all the registered guns objects. Note, this is an immutable map.
     *
     * @return a map of registered gun objects
     */
    public Map<ResourceLocation, Gun> getRegisteredGuns()
    {
        return this.registeredGuns;
    }

    public ImmutableMap<ResourceLocation, Gun> getRegisteredGunMap()
    {
        return ImmutableMap.copyOf(this.registeredGuns);
    }

    /**
     * Gets a list of all the guns registered on the client side. Note, this is an immutable list.
     *
     * @return a map of guns registered on the client
     */
    public static List<GunItem> getClientRegisteredGuns()
    {
        return ImmutableList.copyOf(clientRegisteredGuns);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event)
    {
        NetworkGunManager.instance = null;
    }

    @SubscribeEvent
    public static void addReloadListenerEvent(AddServerReloadListenersEvent event)
    {
        NetworkGunManager networkGunManager = new NetworkGunManager();
        event.addListener(networkGunManager);
        NetworkGunManager.instance = networkGunManager;
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event)
    {
        if(event.getPlayer() == null)
        {
            NetworkGunManager manager = NetworkGunManager.get();
            CustomGunLoader loader = CustomGunLoader.get();
            if(manager != null && loader != null)
            {
                PacketHandler.getPlayChannel().sendToAll(new S2CMessageUpdateGuns(manager.getRegisteredGunMap(), loader.getCustomGunMap()));
            }
        }
    }

    /**
     * Gets the network gun manager. This will be null if the client isn't running an integrated
     * server or the client is connected to a dedicated server.
     *
     * @return the network gun manager
     */
    @Nullable
    public static NetworkGunManager get()
    {
        return instance;
    }

    /**
     * A simple wrapper for a gun object to pass to GunItem. This is to indicate to developers that
     * Gun instances shouldn't be changed on GunItems as they are controlled by NetworkGunManager.
     * Changes to gun properties should be made through the JSON file.
     */
    public static class Supplier
    {
        private final Gun gun;

        private Supplier(Gun gun)
        {
            this.gun = gun;
        }

        public Gun getGun()
        {
            return this.gun;
        }
    }

    private void readJson(String namespace,
                          String filePath,
                          IOSupplier<InputStream> opener,
                          Map<ResourceLocation, Gun> sink,
                          @Nullable ResourceLocation forcedId) {

        try (Reader r = new BufferedReader(
                new InputStreamReader(opener.get(), StandardCharsets.UTF_8))) {

            Gun gun = GsonHelper.fromJson(GSON_INSTANCE, r, Gun.class);
            if (gun == null || !Validator.isValidObject(gun)) {
                JustEnoughGuns.LOGGER.error("Malformed gun file {}", filePath);
                return;
            }

            ResourceLocation id;
            if (forcedId != null) {
                id = forcedId;
            } else {
                String base = filePath.substring(0, filePath.length() - 5);
                if (base.contains("/")) {
                    base = base.substring(base.lastIndexOf('/') + 1);
                }

                String uniqueBase = base;
                int counter = 1;
                ResourceLocation testId = new ResourceLocation(namespace, uniqueBase);

                while (sink.containsKey(testId)) {
                    uniqueBase = base + "_" + counter++;
                    testId = new ResourceLocation(namespace, uniqueBase);
                }

                id = testId;
            }

            sink.put(id, gun);
            JustEnoughGuns.LOGGER.info("Registered gun '{}'", id);
        } catch (Exception ex) {
            JustEnoughGuns.LOGGER.error("Couldn't read {}", filePath, ex);
        }
    }

    /* tiny functional interface */
    @FunctionalInterface
    private interface IOSupplier<T> { T get() throws IOException; }
}
