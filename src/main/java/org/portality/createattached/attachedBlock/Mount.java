package org.portality.createattached.attachedBlock;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.portality.createattached.physics.EntityRotationHandler;

import java.util.function.Function;

public enum Mount implements INamedIconOptions {
    BODY(Mount::getBodyRotation, livingEntity -> new Vec3(0, -0.5f, 0)),
    HEAD(Mount::getHeadRotation, livingEntity -> new Vec3(0, 0, 0)),
    CAMERA(Mount::getCameraRotation, Mount::getCameraOffset)

    ;

    private final Function<LivingEntity, Vec2> getXYDegRotation;
    private final Function<Entity, Vec3> getOffcet;

    public Vec2 getXYDegRotation(LivingEntity entity) {
        return getXYDegRotation.apply(entity);
    }

    public Vec3 getOffset(Entity entity) {
        return getOffcet.apply(entity);
    }

    Mount(Function<LivingEntity, Vec2> getXYDegRotation, Function<Entity, Vec3> offset){
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

    public static Vec2 getHeadRotation(LivingEntity livingEntity){
        Vec2 vec2 = livingEntity.getRotationVector();
        vec2 = vec2.add(new Vec2(90, 0));
        return vec2;
    }

    public static Vec2 getCameraRotation(LivingEntity livingEntity){
        Vec2 vec2 = livingEntity.getRotationVector();
        vec2 = vec2.add(new Vec2(0, 180));
        return vec2;
    }

    public static Vec3 getCameraOffset(Entity livingEntity){
        return livingEntity.getViewVector(0).normalize().scale(2);
    }

    @Override
    public AllIcons getIcon() {
        return AllIcons.I_ATTACHED;
    }

    @Override
    public String getTranslationKey() {
        return this.toString();
    }
}
