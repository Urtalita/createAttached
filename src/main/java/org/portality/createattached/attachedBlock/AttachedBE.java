package org.portality.createattached.attachedBlock;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.index.SimSoundEvents;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.portality.createattached.index.AttachedIndex;
import org.portality.createattached.network.SyncBodyAnglePayload;
import org.portality.createattached.physics.LivingEntityPhysicsHandler;
import org.portality.createattached.physics.PlayerPhysicHandler;
import org.portality.createattached.physics.AttachedConstraint;

import java.util.*;

public class AttachedBE extends SmartBlockEntity implements BlockEntitySubLevelActor {
    private boolean assembled = false;
    public boolean followingPlayer = false;

    @Nullable
    AttachedConstraint constraint = null;

    @NotNull
    private UUID attachedEntity = UUID.randomUUID();

    public void setConstraint(@Nullable AttachedConstraint constraint) {
        this.constraint = constraint;
    }

    public void setAttachedEntity(@NotNull UUID attachedEntity) {
        this.attachedEntity = attachedEntity;
    }

    public AttachedBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }

    @Override
    public void tick() {
        super.tick();

        if(level.isClientSide()){
            Player player = Minecraft.getInstance().player;
            if(player == null) return;

            fixSublevelClientPosition();

            if(!(Minecraft.getInstance().level.getGameTime() % SyncBodyAnglePayload.TICKS_BETWEEN_PACKETS == 0)) return;

            float yBodyRotation = player.yBodyRot;
            PacketDistributor.sendToServer(new SyncBodyAnglePayload(yBodyRotation));
        }
    }

    public void fixSublevelClientPosition(){
        ClientSubLevel clientSubLevel = (ClientSubLevel) Sable.HELPER.getContaining(this);

        if (Minecraft.getInstance().player == null) return;
        Player player = Minecraft.getInstance().player;
        Vec3 playerMovement = player.getDeltaMovement();


    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        SubLevel subLevel = Sable.HELPER.getContaining(this);

        assembled = subLevel != null;
        followingPlayer = tag.getBoolean("following");

        attachedEntity = tag.getUUID("player");

        if(followingPlayer && constraint == null){
            constraint = new AttachedConstraint(attachedEntity, 1, null);
        }
    }

    public void onRemove(){
        PlayerPhysicHandler.sublevelToEntity.remove(attachedEntity);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {

        tag.putUUID("player", attachedEntity);
        tag.putBoolean("following", followingPlayer);

        super.write(tag, registries, clientPacket);
    }

    public ItemInteractionResult clickedOn(Player player) {
        if(player.level().isClientSide) return ItemInteractionResult.SUCCESS;

        if(player.isShiftKeyDown()){

            SubLevel subLevel = Sable.HELPER.getContaining(this);
            if(subLevel == null) return ItemInteractionResult.SUCCESS;
            UUID subLevelId = subLevel.getUniqueId();

            ItemStack putOnStack = new ItemStack(AttachedIndex.ATTACHED_BLOCK.asItem());
            putOnStack.set(AttachedIndex.ATTACHED, subLevelId);
            putOnStack.set(AttachedIndex.ATTACHED_POS, worldPosition);
            putOnStack.set(AttachedIndex.ATTACHED_FACING, getBlockState().getValue(AttachedBlock.FACING));

            player.getInventory().armor.set(EquipmentSlot.CHEST.getIndex(), putOnStack);

            this.attachedEntity = player.getUUID();
            PlayerPhysicHandler.put(subLevel, player);
            followingPlayer = true;

            constraint = new AttachedConstraint(attachedEntity, 1, null);
            sendData();

            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(AttachedBlock.ASSEMBLED, true));

            return ItemInteractionResult.SUCCESS;
        }

        assemble();
        return ItemInteractionResult.SUCCESS;
    }

    private void assemble(){
        BlockPos pos = worldPosition;

        if(!(getLevel() instanceof ServerLevel serverLevel)){return;}
        @org.jetbrains.annotations.Nullable ServerSubLevelContainer container = ServerSubLevelContainer.getContainer(serverLevel);
        if(container == null) return;

        SubLevelAttachedAssemblyHelper helper = new SubLevelAttachedAssemblyHelper(this);
        SubLevelAssemblyHelper.@NotNull GatherResult result = SubLevelAssemblyHelper.gatherConnectedBlocks(pos, serverLevel, 256_000, helper);
        if(result.blocks() == null){return;}
        ArrayList<BlockPos> assemblyBlocks = new ArrayList<>(result.blocks());

        final BoundingBox3d aabb = new BoundingBox3d(new AABB(worldPosition));
        ServerSubLevel addedServerSubLevel = SubLevelAssemblyHelper.assembleBlocks(serverLevel, worldPosition, assemblyBlocks, aabb.expand(5).chunkBoundsFrom());

        final SubLevelPhysicsSystem physicsSystem = container.physicsSystem();

        this.level.playSound(null, pos, SimSoundEvents.SIMULATED_CONTRAPTION_MOVES.event(), SoundSource.BLOCKS, 1.0f, 1.0f);

        assembled = true;
        sendData();
        notifyUpdate();
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        BlockEntitySubLevelActor.super.sable$physicsTick(subLevel, handle, timeStep);

        subLevel.enableIndividualQueuedForcesTracking(true);

        Entity entity = subLevel.getLevel().getEntity(attachedEntity);
        if(entity == null) return;

        if(!PlayerPhysicHandler.isSublevelAttached(subLevel)){
            PlayerPhysicHandler.sublevelToEntity.put(subLevel.getUniqueId(), attachedEntity);
        }

        if(entity instanceof ServerPlayer serverPlayer){
            PlayerPhysicHandler.pullPlayerToContraption(serverPlayer, subLevel, worldPosition, timeStep);
        }

        if(constraint != null){
            constraint.physicsTick(subLevel, handle, worldPosition, getBlockState());
        }
    }

    public Direction getFacing(){
        return getBlockState().getValue(AttachedBlock.FACING);
    }

    public static void onPostPhysicsTick(final SubLevelPhysicsSystem physicsSystem, final double timeStep) {
        if(!(physicsSystem.getLevel() instanceof ServerLevel serverLevel)) return;

        if(PlayerPhysicHandler.sublevelToEntity.isEmpty()) return;

        for(Map.Entry<UUID, UUID> entry : PlayerPhysicHandler.sublevelToEntity.entrySet()){
            UUID subLevel = entry.getKey();

            SubLevel sub = PlayerPhysicHandler.getAttachedSubLevel(subLevel, serverLevel);
            forceToPlayer(physicsSystem, timeStep, entry.getValue(), sub);
        }
    }

    public static void forceToPlayer(final SubLevelPhysicsSystem physicsSystem, final double timeStep, UUID attachedPlayer, SubLevel subLevel) {
        if(!(physicsSystem.getLevel() instanceof ServerLevel serverLevel)) return;

        if(subLevel == null) return;
        if(!(subLevel instanceof ServerSubLevel serverSubLevel)) return;
        Entity entity = serverLevel.getEntity(attachedPlayer);

        final Object2ObjectMap<ForceGroup, QueuedForceGroup> queuedForceGroups = serverSubLevel.getQueuedForceGroups();

        Vector3d resultingMovement = new Vector3d(0, 0, 0);

        if (queuedForceGroups != null) {
            for (final Map.Entry<ForceGroup, QueuedForceGroup> entry : queuedForceGroups.entrySet()) {
                final QueuedForceGroup value = entry.getValue();

                for (final QueuedForceGroup.PointForce pointForce : value.getRecordedPointForces()) {
                    final Vector3dc force = new Vector3d(pointForce.force()).div(timeStep);
                    resultingMovement.add(force);
                }
            }
        }

        final Vector3d localGravity = subLevel.logicalPose().transformNormalInverse(DimensionPhysicsData.getGravity(serverLevel)).mul(serverSubLevel.getMassTracker().getMass());

        if(entity instanceof ServerPlayer serverPlayer){
            PlayerPhysicHandler.applyForceToPlayer(resultingMovement, serverPlayer, localGravity, serverSubLevel);

            if(!PlayerPhysicHandler.validatePlaceInMap(serverSubLevel, serverPlayer))
                PlayerPhysicHandler.sublevelToEntity.remove(serverSubLevel.getUniqueId());

            return;
        }


        if(entity instanceof LivingEntity livingEntity){
            LivingEntityPhysicsHandler.applyForceToEntity(resultingMovement, livingEntity, localGravity, serverSubLevel);
        }
    }

    public void reset() {
        attachedEntity = UUID.randomUUID();
        sendData();
        constraint = null;
        followingPlayer = false;
        level.setBlock(getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
    }

    public static class SubLevelAttachedAssemblyHelper implements SubLevelAssemblyHelper.FrontierPredicate {
        private AttachedBE be;

        SubLevelAttachedAssemblyHelper(AttachedBE be){
            this.be = be;
        }

        @Override
        public boolean isValidConnection(BlockPos originPos, BlockState originState, BlockPos pos, BlockState state, @org.jetbrains.annotations.Nullable Direction directionFrom) {
            return true;
        }
    };
}
