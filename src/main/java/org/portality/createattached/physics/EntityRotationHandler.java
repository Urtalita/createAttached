package org.portality.createattached.physics;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.portality.createattached.attachedBlock.Mount;

import java.util.HashMap;
import java.util.UUID;

public class EntityRotationHandler {
    private static final HashMap<UUID, Float> serverBodyRotations = new HashMap<>();
    private static final HashMap<UUID, Float> contraptionRotation = new HashMap<>();

    private static final double MAX_ROTATION_SPEED_DEG_PER_SEC = 400;
    final static double angleTolerance = Math.cos(Math.toRadians(20));

    public static float getBodyRotation(Player player) {
        UUID uuid = player.getUUID();

        float headRot = player.getYHeadRot();
        float currentBodyRot = serverBodyRotations.getOrDefault(uuid, headRot);

        Vec3 playerFacing = player.getLookAngle();
        Vec3 movement = player.getDeltaMovement();
        Vec3 invertedFacing = playerFacing.reverse();

        Vec3 xzInvertedFacing = new Vec3(invertedFacing.x(), 0, invertedFacing.z());
        Vec3 xzMovement = new Vec3(movement.x(), 0, movement.z());

        boolean movesBackwards = areVectorsSimilar(xzMovement, xzInvertedFacing);

        float diff = Mth.wrapDegrees(headRot - currentBodyRot);
        if (diff < -50.0F) {
            currentBodyRot = headRot + 50.0F;
        } else if (diff > 50.0F) {
            currentBodyRot = headRot - 50.0F;
        }

        double horizontalMovement = xzMovement.length();

        /*
        if (horizontalMovement > 0.0001 || !movesBackwards) {
            float movementAngle = (float) Math.toDegrees(Math.atan2(-movement.x, movement.z));

            float movementDiff = Mth.wrapDegrees(movementAngle - currentBodyRot);
            if (movementDiff < -70.0F) {
                currentBodyRot = movementAngle + 70.0F;
            } else if (movementDiff > 70.0F) {
                currentBodyRot = movementAngle - 70.0F;
            }
        }

         */

        currentBodyRot = Mth.wrapDegrees(currentBodyRot);
        serverBodyRotations.put(uuid, currentBodyRot);

        return Mth.wrapDegrees(currentBodyRot);
    }

    public static boolean areVectorsSimilar(Vec3 vecA, Vec3 vecB) {
        if (vecA.length() == 0 || vecB.length() == 0) {
            return true;
        }

        Vec3 normA = vecA.normalize();
        Vec3 normB = vecB.normalize();

        double dotProduct = normA.dot(normB);

        return dotProduct >= angleTolerance;
    }

    public static boolean areVectorsSimilar(Vector3d vecA, Vector3d vecB) {
        if (vecA.lengthSquared() == 0 || vecB.lengthSquared() == 0) {
            return true;
        }

        Vector3d normA = vecA.normalize();
        Vector3d normB = vecB.normalize();

        double dotProduct = normA.dot(normB);

        return dotProduct >= angleTolerance;
    }

    public static double getInterpolatedRotation(ServerPlayer player, ServerSubLevel serverSubLevel, double delta, Mount mount) {
        double target = mount.getXYDegRotation(player).y;
        if(!contraptionRotation.containsKey(serverSubLevel.getUniqueId())){
            contraptionRotation.put(serverSubLevel.getUniqueId(), (float) target);
            return target;
        }
        double current = Mth.wrapDegrees(contraptionRotation.get(serverSubLevel.getUniqueId()));

        double rotSpeed = delta * MAX_ROTATION_SPEED_DEG_PER_SEC;

        double diff = Mth.wrapDegrees(target - current);
        double absDiff = Math.abs(diff);

        if (absDiff < rotSpeed || absDiff <= 1e-5) {
            PlayerPhysicHandler.hasTurnedOffSpring.put(player.getUUID(), false);
            return Mth.wrapDegrees(target);
        }

        double direction = Math.signum(diff);

        double calculated = current + (direction * rotSpeed);
        if(Double.isNaN(calculated)) return target;
        contraptionRotation.put(serverSubLevel.getUniqueId(), (float) calculated);
        return calculated;
    }

    public static void syncBodyRotation(ServerPlayer player, float rotation){
        double last = serverBodyRotations.getOrDefault(player.getUUID(),  0F);
        serverBodyRotations.put(player.getUUID(), rotation);

        double rotSpeed = 0.07 * MAX_ROTATION_SPEED_DEG_PER_SEC;

        double diff = Mth.wrapDegrees(rotation - last);
        double absDiff = Math.abs(diff);

        if (absDiff < rotSpeed) {
            return;
        }

        PlayerPhysicHandler.hasTurnedOffSpring.put(player.getUUID(), true);
    }
}
