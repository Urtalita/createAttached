package org.portality.createattached.attachedBlock;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class SimpleDierectionalBlock extends DirectionalBlock implements IWrenchable {
    public SimpleDierectionalBlock(Properties p_52591_) {
        super(p_52591_);
    }
    public static final MapCodec<SimpleDierectionalBlock> CODEC = simpleCodec(SimpleDierectionalBlock::new);

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection();

        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) facing = facing.getOpposite();

        return defaultBlockState().setValue(FACING, facing);
    }
}
