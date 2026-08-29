package org.portality.createattached;

import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;
import org.portality.createattached.attachedBlock.AttachedItem;
import org.portality.createattached.index.AttachedIndex;
import org.portality.createattached.network.UpdateSpeedOnClient;
import org.portality.createattached.physics.PlayerPhysicHandler;

import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public class CommonEvents {

    @net.neoforged.bus.api.SubscribeEvent
    public static void onCommonTick(LevelTickEvent.Post event) {
        if(event.getLevel() instanceof ServerLevel serverLevel) onServerTick(serverLevel);
        if(event.getLevel() instanceof ClientLevel clientLevel) onClientTick(clientLevel);


    }

    public static void onServerTick(ServerLevel serverLevel){
        for(Map.Entry<UUID, Vector3f> entry : PlayerPhysicHandler.queuedVelocity.entrySet()){
            Entity entity = serverLevel.getEntity(entry.getKey());

            if(!(entity instanceof ServerPlayer serverPlayer)) {
                PlayerPhysicHandler.queuedVelocity.remove(entry.getKey());
                continue;
            }

            Vector3f added = entry.getValue();
            PacketDistributor.sendToPlayer(serverPlayer, new UpdateSpeedOnClient(added));
            PlayerPhysicHandler.queuedVelocity.remove(entry.getKey());
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

                        if(event.getEntity() instanceof ServerPlayer serverPlayer){
                            AttachedItem.cleanAttributes(serverPlayer);
                        }
                    }
                }
            }
        }
    }


    @SubscribeEvent
    public static void onPlayerEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (event.getTarget() instanceof LivingEntity livingTarget) {

            Player player = event.getEntity();
            ItemStack itemStack = event.getItemStack();

            itemStack.set(AttachedIndex.ATTACHED_ENTITY, livingTarget.getUUID());
            if(player instanceof ServerPlayer serverPlayer){
                serverPlayer.sendSystemMessage(Component.literal("entity UUID has been written to stack data").withStyle(ChatFormatting.GREEN), true);
            }
        }
    }
}
