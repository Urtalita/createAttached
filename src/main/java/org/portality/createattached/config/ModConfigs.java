package org.portality.createattached.config;

import com.Portality.createsprings.config.CSCommon;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.infrastructure.config.CClient;
import net.createmod.catnip.config.ConfigBase;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.portality.createattached.physics.LivingEntityPhysicsHandler;
import org.portality.createattached.physics.PlayerPhysicHandler;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@EventBusSubscriber
public class ModConfigs {
    private static final Map<ModConfig.Type, ConfigBase> CONFIGS = new EnumMap<>(ModConfig.Type.class);

    private static CACommon common;
    private static CAClient client;
    private static CAServer server;

    public static CACommon common() {
        return common;
    }

    public static CAClient client() {
        return client;
    }

    public static CAServer server() {
        return server;
    }


    public static ConfigBase byType(ModConfig.Type type) {
        return CONFIGS.get(type);
    }

    private static <T extends ConfigBase> T register(Supplier<T> factory, ModConfig.Type side) {
        Pair<T, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(builder -> {
            T config = factory.get();
            config.registerAll(builder);
            return config;
        });

        T config = specPair.getLeft();
        config.specification = specPair.getRight();
        CONFIGS.put(side, config);
        return config;
    }

    public static void register(ModLoadingContext context, ModContainer container) {
        client = register(CAClient::new, ModConfig.Type.CLIENT);
        common = register(CACommon::new, ModConfig.Type.COMMON);
        server = register(CAServer::new, ModConfig.Type.SERVER);

        for (Map.Entry<ModConfig.Type, ConfigBase> pair : CONFIGS.entrySet())
            container.registerConfig(pair.getKey(), pair.getValue().specification);

        //BlockStressValues.IMPACTS.registerProvider(common().kinetics.stressValues::getImpact);
        //BlockStressValues.CAPACITIES.registerProvider(common().kinetics.stressValues::getCapacity);


    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        for (ConfigBase config : CONFIGS.values())
            if (config.specification == event.getConfig()
                    .getSpec())
                config.onLoad();
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        for (ConfigBase config : CONFIGS.values())
            if (config.specification == event.getConfig()
                    .getSpec())
                config.onReload();

        loadConstants();
    }



    public static void loadConstants(){
        LivingEntityPhysicsHandler.DEFAULT_WEIGHT_PER_BLOCK = server().entity_weight.get();
        LivingEntityPhysicsHandler.MAX_HANDLING_KPG_PER_BLOCK = server().entity_max_kpg.get();
        PlayerPhysicHandler.PLAYER_WEIGHT_KPG = server().player_weight.get();
        PlayerPhysicHandler.MAX_HANDLING_KPG = server().player_max_kpg.get();
    }
}
