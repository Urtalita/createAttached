package org.portality.createattached.movementSensor;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

public class MovementSensorSlotTransform extends ValueBoxTransform.Sided {
    private Function<BlockState, Direction> getDirection;
    private Vec2 positionOnSide;

    public MovementSensorSlotTransform(Function<BlockState, Direction> getDirection, float x, float y){
        this.getDirection = getDirection;
        this.positionOnSide = new Vec2(x, y);
    }

    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(positionOnSide.x, positionOnSide.y, 3.5f);
    }

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        Vec3 location = getSouthLocation();

        location = VecHelper.rotateCentered(location, AngleHelper.horizontalAngle(getSide()), Direction.Axis.Y);
        location = VecHelper.rotateCentered(location, AngleHelper.verticalAngle(getSide()), Direction.Axis.X);

        return location;
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction direction) {
        return getDirection.apply(state).getAxis() == direction.getAxis();
    }

    @Override
    public float getScale() {
        return 0.5f;
    }
}
