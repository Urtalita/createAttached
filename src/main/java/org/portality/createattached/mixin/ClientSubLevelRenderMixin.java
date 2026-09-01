package org.portality.createattached.mixin;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.portality.createattached.attachedBlock.Mount;
import org.portality.createattached.config.CAClient;
import org.portality.createattached.config.ModConfigs;
import org.portality.createattached.index.AttachedIndex;
import org.portality.createattached.physics.LivingEntityPhysicsHandler;
import org.portality.createattached.physics.PlayerPhysicHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(ClientSubLevel.class)
public class ClientSubLevelRenderMixin {

    @Inject(method = "renderPose(F)Ldev/ryanhcode/sable/companion/math/Pose3dc;", at = @At("HEAD"), cancellable = true, remap = false)
    private void createAttached$renderPose(float partialTick, CallbackInfoReturnable<Pose3dc> cir) {
        if(!ModConfigs.client().interpolation.get()) return;
        Pose3dc playerTry = createAttached$tryRenderPlayer(partialTick);
        if(playerTry != null) cir.setReturnValue(playerTry);
        Pose3dc entityTry = createAttached$tryRenderEntity(partialTick);
        if(entityTry != null) cir.setReturnValue(entityTry);
    }

    @Unique
    private Pose3dc createAttached$tryRenderEntity(float pt){
        Minecraft mc = Minecraft.getInstance();
        if(mc.level == null) return null;

        SubLevel thisSubLevel = (SubLevel)(Object)this;
        if(!PlayerPhysicHandler.sublevelToEntity.containsKey(thisSubLevel.getUniqueId())) return null;

        UUID entityID = PlayerPhysicHandler.sublevelToEntity.get(thisSubLevel.getUniqueId());
        Entity entity = createAttached$getEntityByUUID(mc.level, entityID);

        if(!(entity instanceof LivingEntity livingEntity)) return null;

        if(!LivingEntityPhysicsHandler.entityToControllerPos.containsKey(entityID)) return null;
        BlockPos controller = LivingEntityPhysicsHandler.entityToControllerPos.get(entityID);

        if(!LivingEntityPhysicsHandler.entityToControllerPos.containsKey(entityID)) return null;
        Direction controllerFacing = LivingEntityPhysicsHandler.entityToControllerFacing.get(entityID);

        return createAttached$getInterpolatedPose(controllerFacing, controller, thisSubLevel, pt, livingEntity, Mount.BODY);
    }

    @Unique
    @Nullable
    private static Entity createAttached$getEntityByUUID(ClientLevel level, UUID uuid) {
        for (Entity entity : level.entitiesForRendering()) {
            if (entity.getUUID().equals(uuid)) {
                return entity;
            }
        }
        return null;
    }

    @Unique
    private Pose3dc createAttached$tryRenderPlayer(float pt){
        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null) return null;

        LocalPlayer player = mc.player;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if(!chest.has(AttachedIndex.ATTACHED)) return null;

        UUID attachedId = chest.get(AttachedIndex.ATTACHED);
        SubLevel thisSubLevel = (SubLevel)(Object)this;
        if(!thisSubLevel.getUniqueId().equals(attachedId)) return null;

        BlockPos anchorPos = chest.get(AttachedIndex.ATTACHED_POS);
        Direction facing = chest.get(AttachedIndex.ATTACHED_FACING);
        Integer mountIndex = chest.get(AttachedIndex.ATTACHED_MOUNT);
        if(anchorPos == null || facing == null || mountIndex == null) return null;

        Mount mount = Mount.values()[mountIndex];

        return createAttached$getInterpolatedPose(facing, anchorPos, thisSubLevel, pt, player, mount);
    }

    @Unique
    private Pose3dc createAttached$getInterpolatedPose(Direction facing, BlockPos anchorPos, SubLevel thisSubLevel, float pt, LivingEntity entity, Mount mount){
        double px = Mth.lerp(pt, entity.xOld, entity.getX());
        double py = Mth.lerp(pt, entity.yOld, entity.getY());
        double pz = Mth.lerp(pt, entity.zOld, entity.getZ());
        float yBodyRot = Mth.lerp(pt, entity.yBodyRotO, entity.yBodyRot);

        Vec2 rotationalVector = mount.getXYDegRotation(entity);

        if(entity instanceof Player && mount == Mount.BODY) rotationalVector = new Vec2(rotationalVector.x, yBodyRot);

        Vec3 offset = mount.getOffset(entity);
        Vector3d target = new Vector3d(px, py + entity.getEyeHeight(), pz).add(offset.x, offset.y, offset.z);

        Vector3d forward = new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        Vector3d up = Math.abs(forward.y) > 0.999D ? new Vector3d(0, 0, 1) : new Vector3d(0, 1, 0);
        Quaterniond initialRot = new Quaterniond().lookAlong(forward, up).rotateY(Math.PI);

        double degRotation = rotationalVector.y;

        if(facing.getAxis() == Direction.Axis.Y) degRotation += 180;
        double yawRad = Math.toRadians(Mth.wrapDegrees(-(float)degRotation));
        double xRad = Math.toRadians(rotationalVector.x);
        if(facing.getAxis() == Direction.Axis.Y) xRad = -xRad;
        Quaterniond targetOrientation = new Quaterniond().rotateX(-xRad).rotateY(-yawRad).invert().mul(initialRot);

        Vector3d localBlockCenter = new Vector3d(anchorPos.getX() + 0.5, anchorPos.getY() + 0.5, anchorPos.getZ() + 0.5);
        Vector3d rotPoint = new Vector3d(thisSubLevel.logicalPose().rotationPoint());
        Vector3d rotatedOffset = targetOrientation.transform(localBlockCenter.sub(rotPoint, new Vector3d()), new Vector3d());
        Vector3d newPosition = target.sub(rotatedOffset, new Vector3d());

        Vector3d scale = new Vector3d(thisSubLevel.logicalPose().scale());
        return new Pose3d(newPosition, targetOrientation, new Vector3d(rotPoint), scale);
    }
}
