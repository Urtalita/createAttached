package org.portality.createattached.physics;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.portality.createattached.Createattached;
import org.portality.createattached.attachedBlock.AttachedIndex;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerPhysicHandler {
    public static HashMap<UUID, UUID> sublevelToPlayer = new HashMap<>();
    public static final HashMap<UUID, UUID> previouslyAddedMovement = new HashMap<>();

    private static final HashMap<UUID, Float> serverBodyRotations = new HashMap<>();

    private static final double PLAYER_WEIGHT_KPG = 10;
    private static final double MAX_HANDLING_KPG = 20;

    private static final ResourceLocation OVERLOAD_SPEED_ID = ResourceLocation.fromNamespaceAndPath(Createattached.MODID, "overload_speed");
    private static final ResourceLocation OVERLOAD_JUMP_ID = ResourceLocation.fromNamespaceAndPath(Createattached.MODID, "overload_jump");

    public static void put(SubLevel subLevel, Player player){
        sublevelToPlayer.put(subLevel.getUniqueId(), player.getUUID());
    }

    public static boolean isPlayerAttached(Player player){
        return sublevelToPlayer.containsValue(player.getUUID());
    }

    public static boolean isSublevelAttached(SubLevel subLevel){
        return sublevelToPlayer.containsKey(subLevel.getUniqueId());
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

    public static @org.jetbrains.annotations.Nullable ServerSubLevel getAttachedServerSubLevel(UUID uuid, ServerLevel serverLevel) {
        SubLevel subLevel = getAttachedSubLevel(uuid, serverLevel);
        if(subLevel == null) return null;
        return (ServerSubLevel) subLevel;
    }

    public static float getBodyRotation(Player player) {
        UUID uuid = player.getUUID();
        float headRot = player.getYHeadRot();
        float currentBodyRot = serverBodyRotations.getOrDefault(uuid, headRot);

        float diff = Mth.wrapDegrees(headRot - currentBodyRot);
        if (diff < -50.0F) {
            currentBodyRot = headRot + 50.0F;
        } else if (diff > 50.0F) {
            currentBodyRot = headRot - 50.0F;
        }

        currentBodyRot = Mth.wrapDegrees(currentBodyRot);
        serverBodyRotations.put(uuid, currentBodyRot);

        return currentBodyRot;
    }

    public static void syncBodyRotation(ServerPlayer player, float rotation){
        serverBodyRotations.put(player.getUUID(), rotation);
    }

    public static Vec3 predictNextTickPhysics(ServerPlayer player) {
        Vec3 currentPos = player.position();
        Vec3 deltaMovement = player.getDeltaMovement();

        double gravity = player.isNoGravity() ? 0.0D : 0.08D;
        double drag = 0.98D;

        if (player.isInWater()) {
            gravity = 0.02D;
            drag = 0.8D;
        } else if (player.isInLava()) {
            gravity = 0.02D;
            drag = 0.5D;
        }

        double nextMotionY = (deltaMovement.y - gravity) * drag;
        double nextMotionX = deltaMovement.x * drag;
        double nextMotionZ = deltaMovement.z * drag;


        if (player.onGround() && nextMotionY < 0) {
            nextMotionY = 0;
        }

        Vec3 addedVector = new Vec3(nextMotionX, nextMotionY, nextMotionZ);

        return currentPos.add(addedVector);
    }


    public static Vec3 getTarget(ServerPlayer player){
        return predictNextTickPhysics(player).add(0, player.getEyeHeight(), 0).add(0, -0.5f, 0);
    }

    @Nullable
    public static Vec3 getSublevelTarget(ServerPlayer player){
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if(!stack.has(AttachedIndex.ATTACHED)) return null;

        UUID subLevelId = stack.get(AttachedIndex.ATTACHED);
        BlockPos position = stack.get(AttachedIndex.ATTACHED_POS);

        ServerSubLevel subLevel = getAttachedServerSubLevel(subLevelId, player.serverLevel());
        if(subLevel == null) return null;

        return getSublevelTarget(subLevel, position);
    }

    public static Vec3 getSublevelTarget(ServerSubLevel serverSubLevel, BlockPos attachedPosition){
        return serverSubLevel.logicalPose().transformPosition(attachedPosition.getCenter());
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
                        modifierValue + 0.3d,
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

    public static void pullPlayerToContraption(ServerPlayer player){
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if(!stack.has(AttachedIndex.ATTACHED)) return;

        UUID subLevelId = stack.get(AttachedIndex.ATTACHED);
        BlockPos position = stack.get(AttachedIndex.ATTACHED_POS);

        ServerSubLevel subLevel = getAttachedServerSubLevel(subLevelId, player.serverLevel());
        if(subLevel == null) return;

        pullPlayerToContraption(player, subLevel, position);
    }

    public static void pullPlayerToContraption(ServerPlayer player, ServerSubLevel serverSubLevel, BlockPos attachedPosition){
        /*
        Vec3 playerPosition = getTarget(player);
        Vec3 playerGoal = getSublevelTarget(serverSubLevel, attachedPosition);

        Vec3 diff = playerGoal.subtract(playerPosition);
        double diffLength = diff.length();

        if(diffLength <= .25d) return;
        if(diffLength >= 5) return;

        Vec3 addedMovement = diff.scale(.5d);

        player.addDeltaMovement(addedMovement);
        player.hurtMarked = true;

         */
    }
}
