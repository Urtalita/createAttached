package org.portality.createattached.attachedBlock;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static org.portality.createattached.physics.PlayerPhysicHandler.getAttachedServerSubLevel;

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

    public static void unequip(ItemStack stack, ServerLevel serverLevel){
        if(!stack.has(AttachedIndex.ATTACHED)) return;

        BlockPos position = stack.get(AttachedIndex.ATTACHED_POS);

        BlockEntity entity = serverLevel.getBlockEntity(position);
        if(entity instanceof AttachedBE attachedBE){
            serverLevel.setBlock(position, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
