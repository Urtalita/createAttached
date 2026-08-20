package org.portality.createattached.mixin;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.PacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.portality.createattached.physics.PlayerPhysicHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision.CollisionInfo;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision.FirstCollisionInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision.class)
public class SubLevelEntityCollision {
    /*
    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private static void onCollide(Entity entity, Vec3 collisionMotionMoj, Vec3 velocityMotionMoj,
                                  LevelReusedVectors sink,
                                  CallbackInfoReturnable<dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision.CollisionInfo> cir) {

        if(entity instanceof Player player){
            if(PlayerPhysicHandler.isPlayerAttached(player)){
                CollisionInfo collisionInfo = new CollisionInfo();

                collisionInfo.horizontalCollision = false;
                collisionInfo.verticalCollision = false;
                collisionInfo.verticalCollisionBelow = false;
                collisionInfo.subLevelHorizontalCollision = false;
                collisionInfo.minorHorizontalCollision = false;
                collisionInfo.motion = collisionMotionMoj;
                collisionInfo.inheritedMotion = new Vec3(0, 0, 0);
                collisionInfo.firstCollisions = new HashMap<>();
                collisionInfo.trackingSubLevel = null;
                collisionInfo.preTrackingSubLevel = null;

                cir.setReturnValue(collisionInfo);
            }
        }
    }

     */

    @Redirect(
            method = "collide",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/ObjectSet;add(Ljava/lang/Object;)Z",
                    ordinal = 0 // Первая инструкция вызова .add() в методе collide
            )
    )
    private static boolean redirectObjectSetAdd(ObjectSet<SubLevel> instance, Object element) {
        SubLevel subLevel = (SubLevel) element;

        if (PlayerPhysicHandler.isSublevelAttached(subLevel)) {
            return false;
        }

        // Если элемент проходит проверку, вызываем оригинальный метод добавления
        return instance.add(subLevel);
    }
}
