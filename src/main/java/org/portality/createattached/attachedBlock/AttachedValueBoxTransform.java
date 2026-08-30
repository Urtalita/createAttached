package org.portality.createattached.attachedBlock;

import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class AttachedValueBoxTransform extends CenteredSideValueBoxTransform {
    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8, 8, 7.5);
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction direction) {
        return true;
    }
}
