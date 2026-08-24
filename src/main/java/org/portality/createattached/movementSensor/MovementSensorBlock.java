package org.portality.createattached.movementSensor;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.portality.createattached.HitboxHelper;
import org.portality.createattached.attachedBlock.AttachedBE;
import org.portality.createattached.attachedBlock.SimpleDierectionalBlock;
import org.portality.createattached.index.AttachedIndex;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;

public class MovementSensorBlock extends SimpleDierectionalBlock implements IBE<MovementSensorBe> {
    public static final MapCodec<LeverBlock> CODEC = simpleCodec(LeverBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public MovementSensorBlock(Properties p_49795_) {
        super(p_49795_);
    }

    @Override
    protected void onRemove(BlockState p_54647_, Level p_54648_, BlockPos p_54649_, BlockState p_54650_, boolean p_54651_) {
        if (!p_54651_ && !p_54647_.is(p_54650_.getBlock())) {
            if (p_54647_.getValue(POWERED)) {
                this.updateNeighbours(p_54647_, p_54648_, p_54649_);
            }

            super.onRemove(p_54647_, p_54648_, p_54649_, p_54650_, p_54651_);
        }

    }

    @Override
    protected int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        return (Boolean)blockState.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        return (Boolean)blockState.getValue(POWERED) && getConnectedDirection(blockState) == side ? 15 : 0;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    private void updateNeighbours(BlockState state, Level level, BlockPos pos) {
        level.updateNeighborsAt(pos, this);

        level.updateNeighborsAt(pos.relative(getConnectedDirection(state).getOpposite()), this);
    }

    private Direction getConnectedDirection(BlockState blockState) {
        return Direction.NORTH;
    }

    @Override
    public Class<MovementSensorBe> getBlockEntityClass() {
        return MovementSensorBe.class;
    }

    @Override
    public BlockEntityType<? extends MovementSensorBe> getBlockEntityType() {
        return AttachedIndex.MOVEMENT_SENSOR_BE.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        state = (BlockState)state.setValue(FACING, context.getClickedFace());
        return state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        if(facing.getAxis() == Direction.Axis.Z) facing = facing.getOpposite();
        return HitboxHelper.calculateDierectionalVoxelShape(this, facing, new Vec3(1, 1, 0), new Vec3(15, 15, 3));
    }
}
