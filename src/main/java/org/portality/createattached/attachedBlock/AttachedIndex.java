package org.portality.createattached.attachedBlock;

import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.ApiStatus;
import org.portality.createattached.Createattached;

import java.util.UUID;
import java.util.function.UnaryOperator;

import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;

public class AttachedIndex {

    private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Createattached.MODID);


    public static final BlockEntry<AttachedBlock> ATTACHED_BLOCK = Createattached.ATTACHED_REGISTRATE
            .block("attached_block", AttachedBlock::new)
            .initialProperties(() -> Blocks.OAK_LOG)
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_WHITE).noOcclusion())
            .transform(axeOrPickaxe())
            .item(AttachedItem::new)
            .build()
            .lang("Attached item")
            .register();

    public static final BlockEntityEntry<AttachedBE> ATTACHED_BE = Createattached.ATTACHED_REGISTRATE
            .blockEntity("attached_be", AttachedBE::new)
            .validBlocks(ATTACHED_BLOCK)
            .register();

    public static final DataComponentType<UUID> ATTACHED = register(
            "attached",
            builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC)
    );

    public static final DataComponentType<BlockPos> ATTACHED_POS = register(
            "attached_pos",
            builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC)
    );

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
