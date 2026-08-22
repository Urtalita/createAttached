package org.portality.createattached.physics;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.portality.createattached.Createattached;
import org.portality.createattached.attachedBlock.AttachedItem;

import java.util.HashMap;
import java.util.UUID;

import static org.portality.createattached.physics.PlayerPhysicHandler.OVERLOAD_JUMP_ID;
import static org.portality.createattached.physics.PlayerPhysicHandler.OVERLOAD_SPEED_ID;

public class LivingEntityPhysicsHandler {
    public static final double DEFAULT_WEIGHT_PER_BLOCK = 10; //weight of entity per block of volume
    public static final double MAX_HANDLING_KPG_PER_BLOCK = 30; // max kpg per block of entity

    public static final HashMap<EntityType<?>, Double> entityWeights = new HashMap<>(){};
    public static final HashMap<EntityType<?>, Double> entityMaxKpg = new HashMap<>(){};

    public static final ResourceLocation OVERLOAD_FLIGHT_ID = ResourceLocation.fromNamespaceAndPath(Createattached.MODID, "overload_flight");

    public static void initWeights(){
        entityWeights.put(EntityType.HORSE, 20D);
        entityWeights.put(EntityType.CAMEL, 70D);
        entityWeights.put(EntityType.GHAST, 15D);
    }

    public static void initMaxKpg(){
        entityWeights.put(EntityType.HORSE, 50D);
        entityWeights.put(EntityType.CAMEL, 70D);
        entityWeights.put(EntityType.GHAST, 140D);
    }

    public static double getVolume(LivingEntity entity){
        AABB aabb = entity.getBoundingBox();
        return aabb.getXsize() * aabb.getYsize() * aabb.getZsize();
    }

    public static double getDefaultWeight(LivingEntity livingEntity){
        if(entityWeights.containsKey(livingEntity.getType())) return entityWeights.get(livingEntity.getType());

        double volume = getVolume(livingEntity);
        return volume * DEFAULT_WEIGHT_PER_BLOCK;
    }

    public static double getDefaultMaxHandlingKpg(LivingEntity livingEntity){
        if(entityMaxKpg.containsKey(livingEntity.getType())) return entityMaxKpg.get(livingEntity.getType());

        double volume = getVolume(livingEntity);
        return volume * MAX_HANDLING_KPG_PER_BLOCK;
    }

    public static double getRotation(LivingEntity entity){
        return entity.getRotationVector().y;
    }

    public static void applyForceToEntity(Vector3d localForce, LivingEntity entity, Vector3d localGravity, ServerSubLevel serverSubLevel) {
        Vector3d movement = serverSubLevel.logicalPose().orientation().transform(new Vector3d(localForce.x, localForce.y, localForce.z));
        Vector3d movementAfterGravity = serverSubLevel.logicalPose().orientation().transform(new Vector3d(localForce.x, localForce.y, localForce.z).add(localGravity));

        Vec3 resultingAffectingPlayer;
        double playerOverload = 0;

        if(movementAfterGravity.y() < 0){
            resultingAffectingPlayer = new Vec3(movement.x(), 0, movement.z());
            playerOverload = -movementAfterGravity.y();
        } else {
            resultingAffectingPlayer = new Vec3(movementAfterGravity.x(), movementAfterGravity.y(), movementAfterGravity.z());
        }

        applyAttributes(entity, playerOverload);

        Vec3 scaledForce = resultingAffectingPlayer.scale(1d /((getDefaultWeight(entity) + serverSubLevel.getMassTracker().getMass()) * 10));

        entity.addDeltaMovement(scaledForce);
        if(scaledForce.length() <= 0.01) return;
        entity.hurtMarked = true;
    }

    public static void applyAttributes(LivingEntity entity, double entityOverload){
        entityOverload = entityOverload / 10d;

        if (entityOverload > 0) {

            double slowDownCoefficient = entityOverload / getDefaultMaxHandlingKpg(entity);

            slowDownCoefficient = Math.max(0.0, Math.min(1.0, slowDownCoefficient));
            double modifierValue = -slowDownCoefficient;

            var movementInstance = entity.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementInstance != null) {
                movementInstance.removeModifier(OVERLOAD_SPEED_ID);

                movementInstance.addTransientModifier(new AttributeModifier(
                        OVERLOAD_SPEED_ID,
                        modifierValue,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ));
            }

            var jumpingInstance = entity.getAttribute(Attributes.JUMP_STRENGTH);
            if (jumpingInstance != null) {
                jumpingInstance.removeModifier(OVERLOAD_JUMP_ID);

                jumpingInstance.addTransientModifier(new AttributeModifier(
                        OVERLOAD_JUMP_ID,
                        modifierValue,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ));
            }

            var flyingInstance = entity.getAttribute(Attributes.FLYING_SPEED);
            if (flyingInstance != null) {
                flyingInstance.removeModifier(OVERLOAD_FLIGHT_ID);

                flyingInstance.addTransientModifier(new AttributeModifier(
                        OVERLOAD_FLIGHT_ID,
                        modifierValue,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ));
            }
        } else {
            var movementInstance = entity.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementInstance != null) movementInstance.removeModifier(OVERLOAD_SPEED_ID);

            var jumpingInstance = entity.getAttribute(Attributes.JUMP_STRENGTH);
            if (jumpingInstance != null) jumpingInstance.removeModifier(OVERLOAD_JUMP_ID);

            var flyingInstance = entity.getAttribute(Attributes.FLYING_SPEED);
            if (flyingInstance != null) flyingInstance.removeModifier(OVERLOAD_FLIGHT_ID);
        }
    }
}
