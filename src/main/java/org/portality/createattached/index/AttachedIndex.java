package org.portality.createattached.index;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.simulated_team.simulated.index.SimItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.ApiStatus;
import org.portality.createattached.Createattached;
import org.portality.createattached.attachedBlock.AttachedBE;
import org.portality.createattached.attachedBlock.AttachedBlock;
import org.portality.createattached.attachedBlock.AttachedItem;
import org.portality.createattached.attachedBlock.EntityConnectorItem;
import org.portality.createattached.magneticArmour.MagneticArmourItem;
import org.portality.createattached.movementSensor.MovementSensorBe;
import org.portality.createattached.movementSensor.MovementSensorBlock;
import org.portality.createattached.movementSensor.MovementSensorBlockStateGenerator;

import java.util.UUID;
import java.util.function.UnaryOperator;

import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;

public class AttachedIndex {

    private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Createattached.MODID);


    public static final BlockEntry<AttachedBlock> ATTACHED_BLOCK = Createattached.ATTACHED_REGISTRATE
            .block("attached_block", AttachedBlock::new)
            .lang("Attached Block")
            .initialProperties(() -> Blocks.OAK_LOG)
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_WHITE).noOcclusion())
            .blockstate(BlockStateGen.directionalBlockProvider(false))
            .transform(axeOrPickaxe())
            .item(AttachedItem::new)
            .build()
            .recipe((c, b) ->
                    SingleItemRecipeBuilder.stonecutting(
                                    Ingredient.of(AllBlocks.MECHANICAL_BEARING), RecipeCategory.MISC, c.get(), 1)
                            .unlockedBy("has_ingredient",
                                    RegistrateRecipeProvider.has(AllBlocks.MECHANICAL_BEARING))
                            .save(b))
            .register();

    public static final BlockEntry<MovementSensorBlock> MOVEMENT_SENSOR = Createattached.ATTACHED_REGISTRATE
            .block("movement_sensor", MovementSensorBlock::new)
            .lang("Movement sensor")
            .initialProperties(() -> Blocks.OAK_LOG)
            .initialProperties(SharedProperties::copperMetal)
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_WHITE).noOcclusion())
            .blockstate(new MovementSensorBlockStateGenerator()::generate)
            .recipe((c, b) ->
                    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern(" a ")
                            .pattern(" t ")
                            .pattern(" b ")
                            .define('a', Items.AMETHYST_SHARD)
                            .define('t', Blocks.REDSTONE_TORCH)
                            .define('b', AllBlocks.BRASS_CASING)
                            .unlockedBy("has_ingredient",
                                    RegistrateRecipeProvider.has(AllBlocks.BRASS_CASING))
                            .save(b)
            )
            .transform(axeOrPickaxe())
            .simpleItem()
            .register();

    public static final BlockEntityEntry<AttachedBE> ATTACHED_BE = Createattached.ATTACHED_REGISTRATE
            .blockEntity("attached_be", AttachedBE::new)
            .validBlocks(ATTACHED_BLOCK)
            .register();

    public static final BlockEntityEntry<MovementSensorBe> MOVEMENT_SENSOR_BE = Createattached.ATTACHED_REGISTRATE
            .blockEntity("movement_sensor_be", MovementSensorBe::new)
            .validBlocks(MOVEMENT_SENSOR)
            .register();


    public static final DataComponentType<UUID> ATTACHED = register(
            "attached",
            builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC)
    );

    public static final DataComponentType<BlockPos> ATTACHED_POS = register(
            "attached_pos",
            builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC)
    );

    public static final DataComponentType<UUID> ATTACHED_ENTITY = register(
            "attached_entity",
            builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC)
    );

    public static final DataComponentType<Direction> ATTACHED_FACING = register(
            "attached_facing",
            builder -> builder
                    .persistent(Direction.CODEC)
                    .networkSynchronized(StreamCodec.of(
                            (buf, dir) -> buf.writeByte(dir.ordinal()),
                            buf -> Direction.values()[buf.readByte() & 0xFF]
                    ))
    );

    public static final ItemEntry<MagneticArmourItem> MAGNET_BOOTS = Createattached.ATTACHED_REGISTRATE
            .item("magnet_boots", (p) -> new MagneticArmourItem(AttachedArmourMaterials.MAGNET, ArmorItem.Type.BOOTS, p))
            .properties(p -> p.stacksTo(1).fireResistant().rarity(Rarity.EPIC))
            .recipe((c, b) ->
                    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("   ")
                            .pattern("cic")
                            .pattern("lbl")
                            .define('c', AllItems.COPPER_SHEET)
                            .define('b', Items.IRON_BOOTS)
                            .define('l', Items.BLUE_DYE)
                            .define('i', AllBlocks.INDUSTRIAL_IRON_BLOCK)
                            .unlockedBy("has_ingredient",
                                    RegistrateRecipeProvider.has(Items.IRON_BOOTS))
                            .save(b)
            )
            .register();

    public static final ItemEntry<MagneticArmourItem> MAGNET_LEGGINGS = Createattached.ATTACHED_REGISTRATE
            .item("magnet_leggings", (p) -> new MagneticArmourItem(AttachedArmourMaterials.MAGNET, ArmorItem.Type.LEGGINGS, p))
            .properties(p -> p.stacksTo(1).fireResistant().rarity(Rarity.EPIC))
            .recipe((c, b) ->
                    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("clc")
                            .pattern("c c")
                            .pattern("i i")
                            .define('c', AllItems.COPPER_SHEET)
                            .define('l', Items.IRON_LEGGINGS)
                            .define('i', AllBlocks.INDUSTRIAL_IRON_BLOCK)
                            .unlockedBy("has_ingredient",
                                    RegistrateRecipeProvider.has(Items.IRON_BOOTS))
                            .save(b)
            )
            .register();

    public static final ItemEntry<MagneticArmourItem> MAGNET_CHEST = Createattached.ATTACHED_REGISTRATE
            .item("magnet_chestplate", (p) -> new MagneticArmourItem(AttachedArmourMaterials.MAGNET, ArmorItem.Type.CHESTPLATE, p))
            .properties(p -> p.stacksTo(1).fireResistant().rarity(Rarity.EPIC))
            .recipe((c, b) ->
                    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("c c")
                            .pattern("chc")
                            .pattern("iri")
                            .define('c', AllItems.COPPER_SHEET)
                            .define('h', Items.IRON_CHESTPLATE)
                            .define('i', AllBlocks.INDUSTRIAL_IRON_BLOCK)
                            .define('r', AllItems.POLISHED_ROSE_QUARTZ)
                            .unlockedBy("has_ingredient",
                                    RegistrateRecipeProvider.has(Items.IRON_BOOTS))
                            .save(b)
            )
            .register();

    public static final ItemEntry<MagneticArmourItem> MAGNET_HELMET = Createattached.ATTACHED_REGISTRATE
            .item("magnet_helmet", (p) -> new MagneticArmourItem(AttachedArmourMaterials.MAGNET, ArmorItem.Type.HELMET, p))
            .properties(p -> p.stacksTo(1).fireResistant().rarity(Rarity.EPIC))
            .recipe((c, b) ->
                    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("rrr")
                            .pattern("chc")
                            .pattern("i i")
                            .define('c', AllItems.COPPER_SHEET)
                            .define('h', Items.IRON_HELMET)
                            .define('i', AllBlocks.INDUSTRIAL_IRON_BLOCK)
                            .define('r', Items.REDSTONE)
                            .unlockedBy("has_ingredient",
                                    RegistrateRecipeProvider.has(Items.IRON_BOOTS))
                            .save(b)
            )
            .register();

    public static final ItemEntry<EntityConnectorItem> ENTITY_CONNECTOR = Createattached.ATTACHED_REGISTRATE
            .item("entity_connector", EntityConnectorItem::new)
            .properties(p -> p.stacksTo(1).fireResistant().rarity(Rarity.UNCOMMON))
            .recipe((c, b) ->
                    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("  b")
                            .pattern(" r ")
                            .pattern("b  ")
                            .define('r', SimItems.ROPE_COUPLING)
                            .define('b', AllBlocks.MECHANICAL_BEARING)
                            .unlockedBy("has_ingredient",
                                    RegistrateRecipeProvider.has(SimItems.ROPE_COUPLING))
                            .save(b)
            )
            .register();

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        DataComponentType<T> type = builder.apply(DataComponentType.builder()).build();
        DATA_COMPONENTS.register(name, () -> type);
        return type;
    }

    @ApiStatus.Internal
    public static void registerAllComponents(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }

    public static void register() {

    }
}
