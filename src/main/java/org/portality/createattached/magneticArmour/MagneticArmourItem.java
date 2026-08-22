package org.portality.createattached.magneticArmour;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class MagneticArmourItem extends ArmorItem {
    private final Type currentPiece;
    private final EquipmentSlot currentSlot;

    public MagneticArmourItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
        currentPiece = type;
        currentSlot = type.getSlot();
    }

    @Nullable
    public static MagneticArmourItem getWornBy(Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return null;
        }
        if (!(livingEntity.getItemBySlot(slot).getItem() instanceof MagneticArmourItem item)) {
            return null;
        }
        return item;
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
