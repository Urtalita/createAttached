package org.portality.createattached.physics;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.portality.createattached.Createattached;
import org.portality.createattached.attachedBlock.AttachedBE;
import org.portality.createattached.attachedBlock.AttachedItem;
import org.portality.createattached.attachedBlock.Mount;
import org.portality.createattached.index.AttachedIndex;
import org.portality.createattached.network.UpdateSpeedOnClient;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerPhysicHandler {
    public static ConcurrentHashMap<UUID, UUID> sublevelToEntity = new ConcurrentHashMap<>(); //safe access from anywhere across sable events
    public static ConcurrentHashMap<UUID, Vector3f> queuedVelocity = new ConcurrentHashMap<>(); //safe access from anywhere across sable events
    static final HashMap<UUID, Boolean> hasTurnedOffSpring = new HashMap<>(); //rotation loop bug fix

    public static double PLAYER_WEIGHT_KPG = 15;
    public static double MAX_HANDLING_KPG = 30;

    public static final ResourceLocation OVERLOAD_SPEED_ID = ResourceLocation.fromNamespaceAndPath(Createattached.MODID, "overload_speed");
    public static final ResourceLocation OVERLOAD_JUMP_ID = ResourceLocation.fromNamespaceAndPath(Createattached.MODID, "overload_jump");

    public static boolean isEntityAttached(Entity entity){
        return sublevelToEntity.containsValue(entity.getUUID());
    }

    public static void put(SubLevel subLevel, Entity entity){
        sublevelToEntity.put(subLevel.getUniqueId(), entity.getUUID());
    }

    public static boolean isSublevelAttached(SubLevel subLevel){
        return sublevelToEntity.containsKey(subLevel.getUniqueId());
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

    @Nullable
    public static BlockPos getPos(ServerPlayer player){
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if(!stack.has(AttachedIndex.ATTACHED)) return null;

        return stack.get(AttachedIndex.ATTACHED_POS);
    }

    @Nullable
    public static Player get(SubLevel subLevel){
        if(!(subLevel.getLevel() instanceof ServerLevel serverLevel)) {
            return Minecraft.getInstance().player;
        }

        UUID playerId = sublevelToEntity.get(subLevel.getUniqueId());
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

    public static Vec3 predictNextTickPhysics(Entity entity) {
        Vec3 currentPos = entity.position();
        Vec3 deltaMovement = entity.getDeltaMovement();

        double gravity = entity.isNoGravity() ? 0.0D : 0.08D;
        double drag = 0.98D;

        if (entity.isInWater()) {
            gravity = 0.02D;
            drag = 0.8D;
        } else if (entity.isInLava()) {
            gravity = 0.02D;
            drag = 0.5D;
        }

        double nextMotionY = (deltaMovement.y - gravity) * drag;
        double nextMotionX = deltaMovement.x * drag;
        double nextMotionZ = deltaMovement.z * drag;


        if (entity.onGround() && nextMotionY < 0) {
            nextMotionY = 0;
        }

        Vec3 addedVector = new Vec3(nextMotionX, nextMotionY, nextMotionZ);

        return currentPos.add(addedVector);
    }


    public static Vec3 getTarget(Entity entity, Mount mount){
        return predictNextTickPhysics(entity).add(0, entity.getEyeHeight(), 0).add(mount.getOffset(entity));
    }

    public static Vec3 getPosition(Entity entity){
        return entity.position().add(0, entity.getEyeHeight(), 0).add(0, -0.5f, 0);
    }

    public static boolean validatePlaceInMap(ServerSubLevel serverSubLevel, ServerPlayer player){
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if(!stack.has(AttachedIndex.ATTACHED_POS)) return false;

        BlockPos position = stack.get(AttachedIndex.ATTACHED_POS);
        BlockEntity entity = null;
        if (position != null) entity = serverSubLevel.getLevel().getBlockEntity(position);

        return entity instanceof AttachedBE;
    }

    public static Vec3 getSublevelTarget(ServerSubLevel serverSubLevel, BlockPos attachedPosition){
        return serverSubLevel.logicalPose().transformPosition(attachedPosition.getCenter());
    }

    public static void applyAttributes(ServerPlayer serverPlayer, double playerOverload){
        playerOverload = playerOverload / 10d;

        if(serverPlayer.getAbilities().flying){
            AttachedItem.cleanAttributes(serverPlayer);
            return;
        }

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
    }

    public static void pullPlayerToContraption(ServerPlayer player, Mount mount){
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if(!stack.has(AttachedIndex.ATTACHED)) return;

        UUID subLevelId = stack.get(AttachedIndex.ATTACHED);
        BlockPos position = stack.get(AttachedIndex.ATTACHED_POS);

        ServerSubLevel subLevel = getAttachedServerSubLevel(subLevelId, player.serverLevel());
        if(subLevel == null) return;

        pullPlayerToContraption(player, subLevel, position, 0.025, mount);
    }

    public static Vector3d calculateMidPoint(ServerPlayer player, ServerSubLevel serverSubLevel, BlockPos attachedPosition, Mount mount) {
        Vec3 targetV3 = getTarget(player, mount);
        Vector3d target = new Vector3d(targetV3.x(), targetV3.y(), targetV3.z());

        Vec3 controllerCenter = attachedPosition.getCenter();
        Vector3d controllerCenterD = new Vector3d(controllerCenter.x(), controllerCenter.y(), controllerCenter.z());
        Vector3d controllerInWorld = serverSubLevel.logicalPose().transformPosition(controllerCenterD);

        double contraptionMass = serverSubLevel.getMassTracker().getMass();
        double playerMass = PLAYER_WEIGHT_KPG;
        double totalMass = playerMass + contraptionMass;

        if (totalMass <= 0) {
            return target;
        }

        Vector3d midPoint = new Vector3d();
        midPoint.add(target.mul(playerMass))
                .add(controllerInWorld.mul(contraptionMass))
                .div(totalMass);

        return midPoint;
    }

    public static void applyForceToPlayer(Vector3d localForce, ServerPlayer serverPlayer, Vector3d localGravity, ServerSubLevel serverSubLevel) {
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

        applyAttributes(serverPlayer, playerOverload);

        Vec3 scaledForce = resultingAffectingPlayer.scale(1d / ((PLAYER_WEIGHT_KPG + serverSubLevel.getMassTracker().getMass()) * 10));

        serverPlayer.addDeltaMovement(scaledForce);
        if(scaledForce.length() <= 0.01) return;

        addQueuedVelocity(scaledForce, serverPlayer);
    }

    public static void pullPlayerToContraption(ServerPlayer player, ServerSubLevel serverSubLevel,
                                               BlockPos attachedPosition, double delta, Mount mount) {
        if(attachedPosition == null) return;

        boolean turnedOffSpring = hasTurnedOffSpring.getOrDefault(player.getUUID(), false);
        if(turnedOffSpring) return;

        Vec3 playerPositionVec = getTarget(player, mount);
        Vector3d playerPosition = new Vector3d(playerPositionVec.x, playerPositionVec.y, playerPositionVec.z);

        Vector3d playerGoal = calculateMidPoint(player, serverSubLevel, attachedPosition, mount);

        Vector3d diff = playerGoal.sub(playerPosition, new Vector3d());
        Vec3 playerMotionF = player.getDeltaMovement();

        Vector3d playerMotion = new Vector3d(playerMotionF.x, playerMotionF.y, playerMotionF.z);

        if(diff.lengthSquared() < 0.001){
            return;
        }

        //spring physics calculations
        Vector3d playerMotionSeconds = playerMotion.mul(20.0);
        Vector3d relativeMotion = playerMotionSeconds.sub(serverSubLevel.latestLinearVelocity);

        Vector3d springDirection = new Vector3d(diff).normalize();
        double springVelocity = relativeMotion.dot(springDirection);

        double stiffnessConstant = 400.0;
        double dampingConstant = 10.0;

        double springForceScalar = diff.length() * stiffnessConstant;
        double dampingForceScalar = springVelocity * dampingConstant;

        double totalForceScalar = springForceScalar - dampingForceScalar;
        Vector3d appliedForce = springDirection.mul(totalForceScalar);

        // A = F / M

        Vector3d addedMovement = appliedForce.div(PLAYER_WEIGHT_KPG).mul(delta);

        double maxForce = 2.0;
        if (addedMovement.length() > maxForce) {
            addedMovement.normalize().mul(maxForce);
        }

        Vec3 mojMovement = new Vec3(addedMovement.x, addedMovement.y, addedMovement.z);
        player.addDeltaMovement(mojMovement);

        if(addedMovement.lengthSquared() > 0.0005){return;}
        player.hurtMarked = true;
        //addQueuedVelocity(mojMovement, player);
    }

    public static void addQueuedVelocity(Vec3 vec3, ServerPlayer serverPlayer){
        if(queuedVelocity.containsKey(serverPlayer.getUUID())){
            queuedVelocity.compute(serverPlayer.getUUID(), (k, existing) -> vec3.toVector3f().add(existing));
            return;
        }

        queuedVelocity.put(serverPlayer.getUUID(), vec3.toVector3f());
    }
}
