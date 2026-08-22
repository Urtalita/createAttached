package org.portality.createattached.index;

import dev.simulated_team.simulated.index.SimBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.ApiStatus;
import org.portality.createattached.Createattached;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

public class AttachedArmourMaterials {
    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Createattached.MODID);

    public static final Holder<ArmorMaterial> MAGNET = register(
            "magnet",
            new int[] {3, 7, 5, 3, 6}, // helmet chestplate leggings boots body(ignore)
            7,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            2F,
            0.0F,
            () -> Ingredient.of(SimBlocks.REDSTONE_MAGNET)
    );

    private static Holder<ArmorMaterial> register(
            String name,
            int[] defense,
            int enchantmentValue,
            Holder<SoundEvent> equipSound,
            float toughness,
            float knockbackResistance,
            Supplier<Ingredient> repairIngredient
    ) {
        List<ArmorMaterial.Layer> list = List.of(new ArmorMaterial.Layer(Createattached.asResource(name)));
        return register(name, defense, enchantmentValue, equipSound, toughness, knockbackResistance, repairIngredient, list);
    }

    private static Holder<ArmorMaterial> register(
            String name,
            int[] defense,
            int enchantmentValue,
            Holder<SoundEvent> equipSound,
            float toughness,
            float knockbackResistance,
            Supplier<Ingredient> repairIngridient,
            List<ArmorMaterial.Layer> layers
    ) {
        EnumMap<ArmorItem.Type, Integer> enummap = new EnumMap<>(ArmorItem.Type.class);

        for (ArmorItem.Type armoritem$type : ArmorItem.Type.values()) {
            enummap.put(armoritem$type, defense[armoritem$type.ordinal()]);
        }

        return ARMOR_MATERIALS.register(name,
                () -> new ArmorMaterial(enummap, enchantmentValue, equipSound, repairIngridient, layers, toughness, knockbackResistance)
        );
    }

    @ApiStatus.Internal
    public static void register(IEventBus eventBus) {
        ARMOR_MATERIALS.register(eventBus);
    }
}
