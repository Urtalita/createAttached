package org.portality.createattached.attachedBlock;

import com.simibubi.create.foundation.block.IBE;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.portality.createattached.HitboxHelper;

public class AttachedBlock extends SimpleDierectionalBlock implements IBE<AttachedBE> {
    public static final BooleanProperty ASSEMBLED = BooleanProperty.create("assembled");

    public AttachedBlock(Properties p_49795_) {
        super(p_49795_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ASSEMBLED, false));
    }

    @Override
    public Class<AttachedBE> getBlockEntityClass() {
        return AttachedBE.class;
    }

    @Override
    public BlockEntityType<? extends AttachedBE> getBlockEntityType() {
        return AttachedIndex.ATTACHED_BE.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(ASSEMBLED);
        super.createBlockStateDefinition(pBuilder);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return HitboxHelper.calculateDierectionalVoxelShape(this, Direction.NORTH, new Vec3(0, 0 ,12), new Vec3(16, 16, 16));
    }

    @Override
    protected void onRemove(BlockState p_60515_, Level level, BlockPos pos, BlockState p_60518_, boolean p_60519_) {
        withBlockEntityDo(level, pos, AttachedBE::onRemove);
        super.onRemove(p_60515_, level, pos, p_60518_, p_60519_);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return onBlockEntityUse(level, pos, b -> b.clickedOn(player).result());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand p_316595_, BlockHitResult p_316140_) {
        return onBlockEntityUseItemOn(level, pos, b -> b.clickedOn(player));
    }
}
