package org.portality.createattached.attachedBlock;

import com.mojang.math.Axis;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.FreeConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.config.server.physics.SimPhysics;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import dev.simulated_team.simulated.service.SimConfigService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.portality.createattached.PlayerPhysicHandler;

import javax.annotation.Nullable;
import java.util.UUID;

public class AttachedConstraint {
    private static final double CONSTRAINT_DAMPING = (double)30.0F;
    private static final double CONSTRAINT_STIFFNESS = (double)240.0F;
    private final UUID playerId;
    private float scrollDistance;
    private final Vector3d localGoal = new Vector3d();
    private final Quaterniond orientation = new Quaterniond();
    private @Nullable PhysicsConstraintHandle constraintHandle;

    public AttachedConstraint(final UUID playerId, final float scrollDistance, final PhysicsConstraintHandle constraintHandle) {
        super();
        this.playerId = playerId;
        this.scrollDistance = scrollDistance;
        this.constraintHandle = constraintHandle;
    }

    public void physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, BlockPos pos) {
        ServerLevel level = subLevel.getLevel();

        this.removeJoint();
        Player player = level != null ? level.getPlayerByUUID(this.playerId) : null;
        if (player != null) {
            boolean isJumping = (player.getDeltaMovement().y > 0 || player.getDeltaMovement().y < 0) && !player.onGround();
            if (player.onGround() || isJumping || player.isInWater() || player.getAbilities().flying || player.onClimbable()) {
                SubLevel standingSubLevel = Sable.HELPER.getTrackingSubLevel(player);
                if (standingSubLevel != subLevel) {
                    Vector3d constraintGoal = JOMLConversion.toJOML(player.getEyePosition().add(player.getLookAngle().scale(Math.max((double)2.0F, (double)this.scrollDistance))));

                    Vector3d constraintPosition = new Vector3d(pos.getCenter().toVector3f());

                    double yawRad = Math.toRadians(-PlayerPhysicHandler.getBodyRotation(player));
                    boolean shifting = player.isShiftKeyDown();
                    double xRad = Math.toRadians((shifting) ? 30 : 0);

                    Direction facing = Direction.NORTH;

                    boolean axisAlongFirst = true; //state.getValue(AbstractDirectionalAxisBlock.AXIS_ALONG_FIRST_COORDINATE);
                    Vector3d forward = new Vector3d(
                            facing.getStepX(),
                            facing.getStepY(),
                            facing.getStepZ()
                    );
                    Vector3d up = Math.abs(forward.y) > 0.999D
                            ? new Vector3d(0, 0, 1)
                            : new Vector3d(0, 1, 0);
                    Quaterniond initialRot = new Quaterniond()
                            .lookAlong(forward, up)
                            .rotateY(Math.PI);


                    if (!axisAlongFirst) {
                        // facing方向に応じた回転軸の選択
                        switch (facing) {
                            case NORTH, SOUTH ->
                                    initialRot.rotateZ((Math.PI / 2.0D) * (facing.equals(Direction.NORTH) ? 1 : -1));
                            case UP, DOWN ->
                                    initialRot.rotateY((Math.PI / 2.0D) * (facing.equals(Direction.UP) ? 1 : -1));
                        }
                    } else {
                        switch (facing) {
                            case EAST, WEST ->
                                    initialRot.rotateX((Math.PI / 2.0D) * (facing.equals(Direction.EAST) ? 1 : -1));
                        }
                    }

                    // Y座標の健全性チェックと制限
                    final double MAX_Y_COORDINATE = 1000.0D;
                    boolean validConstraintGoal = !Double.isNaN(constraintGoal.y) && !Double.isInfinite(constraintGoal.y) && Math.abs(constraintGoal.y) <= MAX_Y_COORDINATE;
                    boolean validConstraintPosition = !Double.isNaN(constraintPosition.y) && !Double.isInfinite(constraintPosition.y) && Math.abs(constraintPosition.y) <= MAX_Y_COORDINATE;

                    if (validConstraintGoal && validConstraintPosition) {
                        double validRange = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue() + (double)2.0F;
                        double currentDistance = Sable.HELPER.distanceSquaredWithSubLevels(level, constraintGoal, constraintPosition);
                        if (!Mth.equal(-1.0F, this.scrollDistance) && !(currentDistance > validRange * validRange)) {
                            ServerSubLevelContainer container = SubLevelContainer.getContainer(subLevel.getLevel());

                            assert container != null;

                            SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
                            Quaterniond quaterniond = new Quaterniond().rotateX(-xRad).rotateY(-yawRad).invert().mul(initialRot);//.rotateY(Math.PI);
                            this.orientation.set(quaterniond);
                            this.orientation.transformInverse(JOMLConversion.toJOML(player.getEyePosition()));
                            FreeConstraintConfiguration configuration = new FreeConstraintConfiguration(JOMLConversion.ZERO, constraintPosition, this.orientation);// quaterniond);
                            this.constraintHandle = physicsSystem.getPipeline().addConstraint(null, subLevel, configuration);
                            if (this.constraintHandle != null) {
                                final SimPhysics config = SimConfigService.INSTANCE.server().physics;

                                final double stiffnessConstant = 3.0;
                                final double dampingConstant = 6.0;
                                double maxForce = (double) config.handleMaxForce.getF();

                                // Angular: lock orientation by setting motors with high stiffness/damping
                                final double angularStiffness = (double) config.physicsStaffAngularStiffness.getF()*stiffnessConstant;
                                final double angularDamping = (double) config.physicsStaffAngularDamping.getF()*dampingConstant;
                                for (ConstraintJointAxis axis : ConstraintJointAxis.ANGULAR) {
                                    this.constraintHandle.setMotor(axis, 0.0, angularStiffness, angularDamping, true, maxForce);
                                }

                                // Compute the desired local goal in constraint space
                                Vector3dc vector3dc = JOMLConversion.toJOML(player.getLookAngle().scale(Math.max((double)2.0F, (double)this.scrollDistance)));

                                final double partialTick = physicsSystem.getPartialPhysicsTick();

                                final double eyePosX = Mth.lerp(partialTick, player.xOld, player.getX());
                                final double eyePosY = Mth.lerp(partialTick, player.yOld, player.getY()) + player.getEyeHeight();
                                final double eyePosZ = Mth.lerp(partialTick, player.zOld, player.getZ());

                                this.localGoal.set(player.getEyePosition().add(0, -0.6, 0).toVector3f());// set(vector3dc).add(eyePosX, eyePosY, eyePosZ);
                                this.orientation.transformInverse(this.localGoal);

                                final double linearStiffness = (double) config.physicsStaffLinearStiffness.getF()*stiffnessConstant;
                                final double linearDamping = (double) config.physicsStaffLinearDamping.getF()*dampingConstant;

                                // Linear motors: use goal offsets and moderate stiffness/damping
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean hasJoint() {
        return this.constraintHandle != null;
    }

    public void removeJoint() {
        if (this.constraintHandle != null) {
            this.constraintHandle.remove();
            this.constraintHandle = null;
        }

    }

    public void setScrollDistance(float desiredRange) {
        this.scrollDistance = (float)Math.min((double)desiredRange, (double)2.5F);
    }
}
