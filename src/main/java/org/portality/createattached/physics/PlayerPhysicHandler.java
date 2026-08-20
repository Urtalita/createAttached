package org.portality.createattached;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.UUID;

public class PlayerPhysicHandler {
    public static final HashMap<UUID, UUID> sublevelToPlayer = new HashMap<>();

    private static final HashMap<UUID, Float> serverBodyRotations = new HashMap<>();

    private static final double PLAYER_WEIGHT_KPG = 10;
    private static final double MAX_HANDLING_KPG = 20;

    private static final ResourceLocation OVERLOAD_SPEED_ID = ResourceLocation.fromNamespaceAndPath(Createattached.MODID, "overload_speed");
    private static final ResourceLocation OVERLOAD_JUMP_ID = ResourceLocation.fromNamespaceAndPath(Createattached.MODID, "overload_jump");

    public static void put(SubLevel subLevel, Player player){
        sublevelToPlayer.put(subLevel.getUniqueId(), player.getUUID());
    }

    @Nullable
    public static Player get(SubLevel subLevel){
        if(!(subLevel.getLevel() instanceof ServerLevel serverLevel)) {
            return Minecraft.getInstance().player;
        }

        UUID playerId = sublevelToPlayer.get(subLevel.getUniqueId());
        if(playerId == null) return null;
        Entity player = serverLevel.getEntity(playerId);

        if(player instanceof Player realPlayer) return realPlayer;
        return null;
    }

    public static @org.jetbrains.annotations.Nullable SubLevel getAttachedSubLevel(UUID uuid, Level serverLevel) {
        final SubLevelContainer container = SubLevelContainer.getContainer(serverLevel);
        return container.getSubLevel(uuid);
    }

    public static float getBodyRotation(Player player){
        UUID uuid = player.getUUID();
        float headRot = player.getYHeadRot();
        float currentBodyRot = serverBodyRotations.getOrDefault(uuid, headRot); //hashmap

        double movementX = player.getDeltaMovement().x;
        double movementZ = player.getDeltaMovement().z;
        float movementSqr = (float)(movementX * movementX + movementZ * movementZ);

        if (movementSqr > 0.000025F) {
            float moveAngle = (float)(Mth.atan2(movementZ, movementX) * (180F / (float)Math.PI)) - 90.0F;
            float diff = Mth.abs(Mth.wrapDegrees(headRot - moveAngle));

            if (diff > 95.0F) {
                currentBodyRot = headRot - 90.0F;
            } else if (diff < -95.0F) {
                currentBodyRot = headRot + 90.0F;
            } else {
                currentBodyRot = moveAngle;
            }
        } else {
            float diff = Mth.wrapDegrees(headRot - currentBodyRot);
            if (diff < -50.0F) {
                currentBodyRot = headRot + 50.0F;
            } else if (diff > 50.0F) {
                currentBodyRot = headRot - 50.0F;
            }
        }

        currentBodyRot = Mth.wrapDegrees(currentBodyRot);
        serverBodyRotations.put(uuid, currentBodyRot);

        return currentBodyRot;
    }

    public static void applyForceToPlayer(Vector3d localForce, ServerPlayer serverPlayer, Vector3d localGravity, ServerSubLevel serverSubLevel) {
        Vector3d movement = serverSubLevel.logicalPose().orientation().transform(new Vector3d(localForce.x, localForce.y, localForce.z));
        Vector3d movementAfterGravity = serverSubLevel.logicalPose().orientation().transform(new Vector3d(localForce.x, localForce.y, localForce.z).add(localGravity));

        Vec3 resultingAffectingPlayer = new Vec3(0, 0, 0);
        double playerOverload = 0;

        if(movementAfterGravity.y() < 0){
            resultingAffectingPlayer = new Vec3(movement.x(), 0, movement.z());
            playerOverload = -movementAfterGravity.y();
        } else {
            resultingAffectingPlayer = new Vec3(movementAfterGravity.x(), movementAfterGravity.y(), movementAfterGravity.z());
        }

        playerOverload = playerOverload / 10d;

        if (playerOverload > 0) {
            double slowDownCoefficient = playerOverload / MAX_HANDLING_KPG;

            slowDownCoefficient = Math.max(0.0, Math.min(1.0, slowDownCoefficient));

            double modifierValue = -slowDownCoefficient;

            var movementInstance = serverPlayer.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementInstance != null) {
                movementInstance.removeModifier(OVERLOAD_SPEED_ID);

                movementInstance.addTransientModifier(new AttributeModifier(
                        OVERLOAD_SPEED_ID,
                        modifierValue,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ));
            }

            var jumpingInstance = serverPlayer.getAttribute(Attributes.JUMP_STRENGTH);
            if (jumpingInstance != null) {
                jumpingInstance.removeModifier(OVERLOAD_JUMP_ID);

                jumpingInstance.addTransientModifier(new AttributeModifier(
                        OVERLOAD_JUMP_ID,
                        modifierValue,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ));
            }

        } else {
            var movementInstance = serverPlayer.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementInstance != null) movementInstance.removeModifier(OVERLOAD_SPEED_ID);

            var jumpingInstance = serverPlayer.getAttribute(Attributes.JUMP_STRENGTH);
            if (jumpingInstance != null) jumpingInstance.removeModifier(OVERLOAD_JUMP_ID);
        }

        Vec3 scaledForce = resultingAffectingPlayer.scale(1d /((PLAYER_WEIGHT_KPG + serverSubLevel.getMassTracker().getMass()) * 10));

        serverPlayer.addDeltaMovement(scaledForce);
        serverPlayer.hurtMarked = true;


    }
}
