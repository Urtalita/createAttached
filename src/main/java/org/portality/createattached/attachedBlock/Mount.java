package org.portality.createattached.attachedBlock;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.portality.createattached.physics.EntityRotationHandler;

import java.util.function.Function;

public enum Mount {
    HEAD(Entity::getRotationVector, livingEntity -> new Vec3(0, 0, 0)),
    BODY(Mount::getBodyRotation, livingEntity -> new Vec3(0, -livingEntity.getEyeHeight() / 2, 0)),
    CAMERA(Entity::getRotationVector, Mount::getCameraOffset)

    ;

    private final Function<LivingEntity, Vec2> getXYDegRotation;
    private final Function<LivingEntity, Vec3> getOffcet;

    public Vec2 getXYDegRotation(LivingEntity entity) {
        return getXYDegRotation.apply(entity);
    }

    public Vec3 getOffset(LivingEntity entity) {
        return getOffcet.apply(entity);
    }

    Mount(Function<LivingEntity, Vec2> getXYDegRotation, Function<LivingEntity, Vec3> offset){
        this.getXYDegRotation = getXYDegRotation;
        this.getOffcet = offset;
    }

    public static Vec2 getBodyRotation(LivingEntity livingEntity){
        if(livingEntity instanceof Player player){
            float yRot = EntityRotationHandler.getBodyRotation(player);
            float xRot = (player.isShiftKeyDown()) ? 30 : 0;

            return new Vec2(xRot, yRot);
        }

        return livingEntity.getRotationVector();
    }

    public static Vec3 getCameraOffset(LivingEntity livingEntity){
        return livingEntity.getViewVector(0).normalize();
    }
}
