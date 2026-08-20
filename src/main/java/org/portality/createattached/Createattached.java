package org.portality.createattached;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.simulated_team.simulated.events.SimulatedCommonEvents;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.portality.createattached.attachedBlock.AttachedBE;
import org.portality.createattached.attachedBlock.AttachedIndex;
import org.portality.createattached.network.AttachedPackets;

@Mod(Createattached.MODID)
public class Createattached {
    public static final String MODID = "createattached";

    public static final CreateRegistrate ATTACHED_REGISTRATE =
            CreateRegistrate.create(MODID)
                    .defaultCreativeTab((ResourceKey<CreativeModeTab>) null)
                    .setTooltipModifierFactory(item -> new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                            .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
                    );

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("createattached_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(AttachedIndex.ATTACHED_BLOCK::asStack)
            .displayItems( (itemDisplayParameters, output) -> {
                output.accept(AttachedIndex.ATTACHED_BLOCK);
            })
            .title(Component.translatable("creativetab.createattached_tab"))
            .noScrollBar()
            .build());


    public Createattached(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        AttachedIndex.register();
        AttachedIndex.registerAllComponents(modEventBus);
        AttachedPackets.register();
        ATTACHED_REGISTRATE.registerEventListeners(modEventBus);

        CREATIVE_MODE_TABS.register(modEventBus);

        SableEventPlatform.INSTANCE.onPostPhysicsTick(AttachedBE::onPostPhysicsTick);
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
    }
}
