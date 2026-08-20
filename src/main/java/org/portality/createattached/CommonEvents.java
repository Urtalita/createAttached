package org.portality.createattached;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.portality.createattached.attachedBlock.AttachedIndex;
import org.portality.createattached.attachedBlock.AttachedItem;
import org.portality.createattached.physics.PlayerPhysicHandler;

@EventBusSubscriber
public class CommonEvents {

    public static final int VALIDATE_TICKS = 20;

    @net.neoforged.bus.api.SubscribeEvent
    public static void onCommonTick(LevelTickEvent.Post event) {
        if(event.getLevel() instanceof ServerLevel serverLevel) onServerTick(serverLevel);
        if(event.getLevel() instanceof ClientLevel clientLevel) onClientTick(clientLevel);


    }

    public static void onServerTick(ServerLevel serverLevel){
        if(serverLevel.getGameTime() % 20 == 0){
            //PlayerPhysicHandler.validateMaps(serverLevel);
        }
    }

    public static void onClientTick(ClientLevel clientLevel){

    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            ItemStack oldStack = event.getFrom();
            ItemStack newStack = event.getTo();

            if (!oldStack.isEmpty() && newStack.isEmpty()) {
                if(oldStack.is(AttachedIndex.ATTACHED_BLOCK.asItem())){
                    if(event.getEntity().level() instanceof ServerLevel serverLevel){
                        AttachedItem.unequip(oldStack, serverLevel);
                    }
                }
            }
        }
    }
}
