package org.portality.createattached.magneticArmour;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.portality.createattached.attachedBlock.AttachedItem;

public class MagneticArmourItem extends ArmorItem {
    private Type currentPiece;

    public MagneticArmourItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
        currentPiece = type;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int p_41407_, boolean p_41408_) {
        if(!(entity instanceof Player player)) return;
        if(!(player.getItemBySlot(currentPiece.getSlot()).getItem() instanceof MagneticArmourItem)){return;}
        onArmourTick(stack, level, player);
    }

    public void onArmourTick(ItemStack stack, Level level, Player player){

    }
}
