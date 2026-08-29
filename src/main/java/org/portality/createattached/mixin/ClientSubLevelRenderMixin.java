package org.portality.createattached.mixin;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.network.client.SubLevelSnapshotInterpolator;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.portality.createattached.index.AttachedIndex;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(ClientSubLevel.class)
public class ClientSubLevelRenderMixin {

    @Shadow @Final private SubLevelSnapshotInterpolator interpolator;

    @Inject(method = "renderPose(F)Ldev/ryanhcode/sable/companion/math/Pose3dc;", at = @At("HEAD"), cancellable = true, remap = false)
    private void createAttached$renderPose(float partialTick, CallbackInfoReturnable<Pose3dc> cir) {
        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null) return;

        LocalPlayer player = mc.player;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if(!chest.has(AttachedIndex.ATTACHED)) return;

        UUID attachedId = chest.get(AttachedIndex.ATTACHED);
        SubLevel thisSubLevel = (SubLevel)(Object)this;
        if(!thisSubLevel.getUniqueId().equals(attachedId)) return;

        BlockPos anchorPos = chest.get(AttachedIndex.ATTACHED_POS);
        Direction facing = chest.get(AttachedIndex.ATTACHED_FACING);
        if(anchorPos == null || facing == null) return;

        cir.setReturnValue(createAttached$getInterpolatedPose(facing, anchorPos, thisSubLevel, partialTick, player));
    }

    @Unique
    private Pose3dc createAttached$getInterpolatedPose(Direction facing, BlockPos anchorPos, SubLevel thisSubLevel, float pt, LivingEntity entity){
        double px = Mth.lerp(pt, entity.xOld, entity.getX());
        double py = Mth.lerp(pt, entity.yOld, entity.getY());
        double pz = Mth.lerp(pt, entity.zOld, entity.getZ());
        float yBodyRot = Mth.lerp(pt, entity.yBodyRotO, entity.yBodyRot);

        Vector3d target = new Vector3d(px, py + entity.getEyeHeight() - 0.5, pz);

        Vector3d forward = new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        Vector3d up = Math.abs(forward.y) > 0.999D ? new Vector3d(0, 0, 1) : new Vector3d(0, 1, 0);
        Quaterniond initialRot = new Quaterniond().lookAlong(forward, up).rotateY(Math.PI);

        double degRotation = yBodyRot;
        boolean shifting = false;

        if(entity instanceof Player player){
            shifting = player.isShiftKeyDown();
        }

        if(facing.getAxis() == Direction.Axis.Y) degRotation += 180;
        double yawRad = Math.toRadians(Mth.wrapDegrees(-(float)degRotation));
        double xRad = Math.toRadians(shifting ? 30 : 0);
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
