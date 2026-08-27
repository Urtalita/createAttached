package org.portality.createattached.attachedBlock;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.portality.createattached.index.AttachedIndex;
import org.portality.createattached.magneticArmour.MagneticArmourItem;
import org.portality.createattached.physics.PlayerPhysicHandler;

import static org.portality.createattached.physics.PlayerPhysicHandler.OVERLOAD_JUMP_ID;
import static org.portality.createattached.physics.PlayerPhysicHandler.OVERLOAD_SPEED_ID;

public class AttachedItem extends BlockItem {
    public AttachedItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @Nullable EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.CHEST;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int p_41407_, boolean p_41408_) {
        if(!(entity instanceof Player player)) return;
        if(!(player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof AttachedItem)){return;}
        onArmourTick(stack, level, player);
    }

    public void onArmourTick(ItemStack stack, Level level, Player player){

    }

    @Nullable
    public static AttachedItem getWornBy(Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return null;
        }
        if (!(livingEntity.getItemBySlot(slot).getItem() instanceof AttachedItem item)) {
            return null;
        }
        return item;
    }

    public static void unequip(ItemStack stack, ServerLevel serverLevel){
        if(!stack.has(AttachedIndex.ATTACHED)) return;

        BlockPos position = stack.get(AttachedIndex.ATTACHED_POS);

        if(position == null) return;
        BlockEntity entity = serverLevel.getBlockEntity(position);
        if(entity instanceof AttachedBE attachedBE){
            SubLevel subLevel = Sable.HELPER.getContaining(serverLevel, attachedBE.getBlockPos());
            if(subLevel == null) return;
            PlayerPhysicHandler.sublevelToEntity.remove(subLevel.getUniqueId());
            attachedBE.reset();
        }
    }

    public static void cleanAttributes(ServerPlayer serverPlayer){
        var movementInstance = serverPlayer.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementInstance != null) {
            movementInstance.removeModifier(OVERLOAD_SPEED_ID);
        }

        var jumpingInstance = serverPlayer.getAttribute(Attributes.JUMP_STRENGTH);
        if (jumpingInstance != null) {
            jumpingInstance.removeModifier(OVERLOAD_JUMP_ID);
        }
    }
}
