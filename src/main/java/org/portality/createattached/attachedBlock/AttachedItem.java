package org.portality.createattached;

import com.mojang.math.Axis;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.UUID;

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

    private @Nullable SubLevel getAttachedSubLevel(UUID uuid, Level serverLevel) {
        final SubLevelContainer container = SubLevelContainer.getContainer(serverLevel);
        return container.getSubLevel(uuid);
    }
}
