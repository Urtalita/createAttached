package org.portality.createattached.physics;

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
import dev.simulated_team.simulated.service.SimConfigService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.portality.createattached.attachedBlock.AttachedBlock;

import javax.annotation.Nullable;
import java.util.UUID;

public class AttachedConstraint {
    private final UUID entityId;
    private final float scrollDistance;
    private final Vector3d localGoal = new Vector3d();
    private final Quaterniond orientation = new Quaterniond();
    private @Nullable PhysicsConstraintHandle constraintHandle;

    final static double stiffnessConstant = 30.0;
    final static double dampingConstant = 10.0;

    final static double angleTolerance = Math.cos(Math.toRadians(5));

    public AttachedConstraint(final UUID entityId, final float scrollDistance, final PhysicsConstraintHandle constraintHandle) {
        super();
        this.entityId = entityId;
        this.scrollDistance = scrollDistance;
        this.constraintHandle = constraintHandle;
    }

    public void physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, BlockPos pos, BlockState state) {
        ServerLevel level = subLevel.getLevel();

        this.removeJoint();
        Entity entity = level != null ? level.getEntity(this.entityId) : null;
        if (entity == null) return;
        if(!(entity instanceof LivingEntity livingEntity)) return;

        boolean isJumping = (livingEntity.getDeltaMovement().y > 0 || livingEntity.getDeltaMovement().y < 0) && !livingEntity.onGround();
        //if (!(livingEntity.onGround() || isJumping || livingEntity.isInWater() || livingEntity.fl || player.onClimbable())) return;

        SubLevel standingSubLevel = Sable.HELPER.getTrackingSubLevel(livingEntity);
        if (standingSubLevel == subLevel) return;

        Vector3d constraintGoal = JOMLConversion.toJOML(livingEntity.getEyePosition().add(livingEntity.getLookAngle().scale(Math.max((double)2.0F, (double)this.scrollDistance))));

        Vector3d constraintPosition = new Vector3d(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d);

        Direction facing = state.getValue(AttachedBlock.FACING);

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

        double degRotation = 0;
        boolean shifting = false;
        if(livingEntity instanceof ServerPlayer player){
            degRotation = PlayerPhysicHandler.getBodyRotation(player);
            shifting = player.isShiftKeyDown();
        } else {
            degRotation = LivingEntityPhysicsHandler.getRotation(livingEntity);
        }

        if(facing.getAxis() == Direction.Axis.Y) degRotation += 180;

        double yawRad = Math.toRadians(Mth.wrapDegrees(-degRotation));
        double xRad = Math.toRadians((shifting) ? 30 : 0);


        final double MAX_Y_COORDINATE = 1000.0D;
        boolean validConstraintGoal = !Double.isNaN(constraintGoal.y) && !Double.isInfinite(constraintGoal.y) && Math.abs(constraintGoal.y) <= MAX_Y_COORDINATE;
        boolean validConstraintPosition = !Double.isNaN(constraintPosition.y) && !Double.isInfinite(constraintPosition.y) && Math.abs(constraintPosition.y) <= MAX_Y_COORDINATE;

        if (!(validConstraintGoal || validConstraintPosition)) return;

        ServerSubLevelContainer container = SubLevelContainer.getContainer(subLevel.getLevel());

        assert container != null;

        SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
        Quaterniond quaterniond = new Quaterniond().rotateX(-xRad).rotateY(-yawRad).invert().mul(initialRot);
        this.orientation.set(quaterniond);
        this.orientation.transformInverse(JOMLConversion.toJOML(livingEntity.getEyePosition()));
        FreeConstraintConfiguration configuration = new FreeConstraintConfiguration(new Vector3d(0f, 0, 0f), constraintPosition, this.orientation);// quaterniond);

        this.constraintHandle = physicsSystem.getPipeline().addConstraint(null, subLevel, configuration);

        if (this.constraintHandle == null) return;

        final SimPhysics config = SimConfigService.INSTANCE.server().physics;

        double maxForce = config.handleMaxForce.getF();

        final double angularStiffness = (double) config.physicsStaffAngularStiffness.getF()*stiffnessConstant;
        final double angularDamping = (double) config.physicsStaffAngularDamping.getF()*dampingConstant;
        for (ConstraintJointAxis axis : ConstraintJointAxis.ANGULAR) {
            this.constraintHandle.setMotor(axis, 0.0, angularStiffness, angularDamping, true, maxForce);
        }

        Vec3 goal = PlayerPhysicHandler.getTarget(livingEntity);
        localGoal.set(goal.toVector3f());
        orientation.transformInverse(localGoal);

        final double linearStiffness = (double) config.physicsStaffLinearStiffness.getF()*stiffnessConstant;
        final double linearDamping = (double) config.physicsStaffLinearDamping.getF()*dampingConstant;

        // Linear motors: use goal offsets and moderate stiffness/damping

        /*
        constraintHandle.setMotor(ConstraintJointAxis.LINEAR_X, localGoal.x(), linearStiffness, linearDamping, true, maxForce);
        constraintHandle.setMotor(ConstraintJointAxis.LINEAR_Y, localGoal.y(), linearStiffness, linearDamping, true, maxForce);
        constraintHandle.setMotor(ConstraintJointAxis.LINEAR_Z, localGoal.z(), linearStiffness, linearDamping, true, maxForce);

         */

        //applyForceToAttachedPlayer(subLevel, handle, pos, (ServerPlayer) player);
    }

    public void applyForceToAttachedPlayer(ServerSubLevel subLevel, RigidBodyHandle handle,
                                           BlockPos pos, ServerPlayer player){
        /*
        Vector3d movementVector = subLevel.latestLinearVelocity.div(20); //to blocks / regular t
        Vector3d angularVector = subLevel.latestAngularVelocity.div(20); //to radians / second
        Vector3d playerMovementVector = new Vector3d(player.getDeltaMovement().toVector3f());

        Vector3d movementVectorRelativeToPlayer = movementVector.sub(playerMovementVector);

        Vector3d subLevelTarget = new Vector3d(pos.getCenter().toVector3f());
        Vector3d target = subLevel.logicalPose().transformPosition(subLevelTarget);

        Vector3d position = new Vector3d(PlayerPhysicHandler.getTarget(player).toVector3f());
        Vector3d diff = target.sub(position);

        if(areVectorsSimilar(playerMovementVector, movementVector)){
            return;
        }

        Vector3d force = diff
                .mul(stiffnessConstant / 10)
                .sub(movementVectorRelativeToPlayer.mul(dampingConstant / 10));

        if(force.lengthSquared() > 5 * 5){
            force = force.normalize().mul(5);
        }

        if(force.lengthSquared() < 0.001) return;

        if(force.y() < 0){
            player.addDeltaMovement(new Vec3(force.x(), force.y(), force.z()));
        }

        force = force.div(100);
        player.addDeltaMovement(new Vec3(force.x(), force.y(), force.z()));
        player.hurtMarked = true;

         */
    }

    public boolean areVectorsSimilar(Vector3d vecA, Vector3d vecB) {
        if (vecA.lengthSquared() == 0 || vecB.lengthSquared() == 0) {
            return true;
        }

        Vector3d normA = vecA.normalize();
        Vector3d normB = vecB.normalize();

        double dotProduct = normA.dot(normB);

        return dotProduct >= angleTolerance;
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
}
