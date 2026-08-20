package org.portality.createattached;

import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.block.BlockWithSubLevelCollisionCallback;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.index.SableTags;
import dev.ryanhcode.sable.physics.callback.ExplosiveBlockCallback;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlock;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlockEntity;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlock;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.util.SimLevelUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

public class AttachedBE extends SmartBlockEntity implements BlockEntitySubLevelActor {
    private boolean assembled = false;

    private static final Vector3d frictionForce = new Vector3d();
    private static final Vector3d frictionTorque = new Vector3d();
    private static final Vector3d localLinearVelocity = new Vector3d();
    private static final Vector3d localAngularVelocity = new Vector3d();
    private static final Vector3d expectedVelocity = new Vector3d();
    private static final Vector3d localDampingPointForce = new Vector3d();


    @NotNull
    private UUID attachedPlayer = UUID.randomUUID();

    public AttachedBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        SubLevel subLevel = Sable.HELPER.getContaining(this);

        assembled = subLevel != null;

        attachedPlayer = tag.getUUID("player");
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {

        tag.putUUID("player", attachedPlayer);

        super.write(tag, registries, clientPacket);
    }

    public ItemInteractionResult clickedOn(Player player) {
        if(player.level().isClientSide) return ItemInteractionResult.SUCCESS;

        if(player.isShiftKeyDown()){
                          if(!assembled) return ItemInteractionResult.SUCCESS;
            if(getBlockState().getValue(AttachedBlock.ASSEMBLED)) return ItemInteractionResult.SUCCESS;

            this.getLevel().setBlockAndUpdate(this.getBlockPos(), this.getBlockState().setValue(AttachedBlock.ASSEMBLED, true));

            ItemStack putOnStack = new ItemStack(AttachedIndex.ATTACHED_BLOCK.asItem());
            player.getInventory().armor.set(EquipmentSlot.CHEST.getIndex(), putOnStack);

            this.attachedPlayer = player.getUUID();
            sendData();

            return ItemInteractionResult.SUCCESS;
        }

        assemble();
        return ItemInteractionResult.SUCCESS;
    }

    private void teleportToPlayer(ServerSubLevel subLevel, Vec3 pos){
        Entity entity = subLevel.getLevel().getEntity(attachedPlayer);
        if(!(entity instanceof Player player)) return;

        BlockPos anchor = worldPosition;

        subLevel.logicalPose().position().set(pos.x, pos.y, pos.z);

        subLevel.logicalPose().rotationPoint()
                .set(anchor.getX() + 0.5, anchor.getY() + 0.5, anchor.getZ() + 0.5);

        subLevel.logicalPose().orientation().set(Axis.YN.rotationDegrees(player.getRotationVector().y));

        subLevel.updateLastPose();
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

        final LevelPlot plot = addedServerSubLevel.getPlot();

        final ChunkPos center = plot.getCenterChunk();
        plot.newEmptyChunk(center);

        final Pose3d pose = new Pose3d();
        pose.position().set(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        final ChunkPos centerChunk = plot.getCenterChunk();
        plot.newEmptyChunk(centerChunk);

        final BlockPos plotAnchor = plot.getCenterBlock();
        final Vector3dc centerOfMass = addedServerSubLevel.getMassTracker().getCenterOfMass();
        final Vector3d subLevelCenter = JOMLConversion.atLowerCornerOf(pos);

        if (centerOfMass != null) {
            subLevelCenter.add(centerOfMass.x() - plotAnchor.getX(), centerOfMass.y() - plotAnchor.getY(), centerOfMass.z() - plotAnchor.getZ());
        } else {
            addedServerSubLevel.logicalPose().rotationPoint()
                    .set(plotAnchor.getX() + 0.5, plotAnchor.getY() + 0.5, plotAnchor.getZ() + 0.5);
        }

        addedServerSubLevel.logicalPose().position().set(subLevelCenter.x, subLevelCenter.y, subLevelCenter.z);

        final SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
        final PhysicsPipeline pipeline = physicsSystem.getPipeline();

        pipeline.teleport(addedServerSubLevel, addedServerSubLevel.logicalPose().position(), addedServerSubLevel.logicalPose().orientation());
        addedServerSubLevel.updateLastPose();

        this.level.playSound(null, pos, SimSoundEvents.SIMULATED_CONTRAPTION_MOVES.event(), SoundSource.BLOCKS, 1.0f, 1.0f);

        assembled = true;
        sendData();
        notifyUpdate();
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        BlockEntitySubLevelActor.super.sable$physicsTick(subLevel, handle, timeStep);
        if(subLevel == null) return;
        Entity entity = subLevel.getLevel().getEntity(attachedPlayer);
        if(!(entity instanceof Player player)) return;

        // Target position (player's center/eyes context)
        Vec3 target = player.getEyePosition().add(0, -0.5f, 0).add(player.getDeltaMovement());

        BlockPos anchor = worldPosition;
        if(anchor == null) return;
        Vec3 vecAnchor = new Vec3(anchor.getX() + 0.5, anchor.getY() + 0.5, anchor.getZ() + 0.5);

        // ВАЖНО: rotationPoint лучше задавать при создании подуровня.
        // Если нужно здесь, оставляем, но это может давать микро-дергания.
        subLevel.logicalPose().rotationPoint().set(vecAnchor.toVector3f());

        Vec3 anchorInWorld = subLevel.logicalPose().transformPosition(vecAnchor);
        Vec3 diff = target.subtract(anchorInWorld);

        // Извлекаем текущие углы поворота подуровня
        Vector3d anglesInRadians = new Vector3d();
        subLevel.logicalPose().orientation().getEulerAnglesYXZ(anglesInRadians);

        double currentSubLevelPitch = anglesInRadians.x; // X
        double currentSubLevelYaw   = anglesInRadians.y; // Y
        double currentSubLevelRoll  = anglesInRadians.z; // Z

        // Целевые углы (Yaw игрока инвертирован для соответствия JOML, Pitch/Roll в ноль)
        double targetPlayerYaw = Math.toRadians(-player.getYRot());
        double targetPlayerPitch = 0.0;
        double targetPlayerRoll = 0.0;

        // Вычисляем кратчайшие дельты углов (Shortest path)
        double yawDiff   = Math.atan2(Math.sin(targetPlayerYaw - currentSubLevelYaw), Math.cos(targetPlayerYaw - currentSubLevelYaw));
        double pitchDiff = Math.atan2(Math.sin(targetPlayerPitch - currentSubLevelPitch), Math.cos(targetPlayerPitch - currentSubLevelPitch));
        double rollDiff  = Math.atan2(Math.sin(targetPlayerRoll - currentSubLevelRoll), Math.cos(targetPlayerRoll - currentSubLevelRoll));

        // Переводим дельты в скорости, обязательно деля на timeStep!
        Vector3d angularVelocity = new Vector3d(pitchDiff / timeStep, yawDiff / timeStep, rollDiff / timeStep);

        // КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: Линейную скорость ТОЖЕ нужно делить на timeStep,
        // чтобы объект покрывал расстояние diff ровно за один физический шаг.
        Vector3d linearVelocity = new Vector3d(diff.toVector3f()).div(timeStep);

        // Гасим предыдущие скорости, чтобы избежать накопления кинетической энергии (орбитального дрейфа)
        Vector3d lastLinearVelocity = subLevel.latestLinearVelocity;
        Vector3d lastAngularVelocity = subLevel.latestAngularVelocity;
        handle.addLinearAndAngularVelocity(lastLinearVelocity.mul(-1, -1, -1), lastAngularVelocity.mul(-1, -1, -1));

        // Применяем новые скорректированные скорости
        handle.addLinearAndAngularVelocity(linearVelocity, angularVelocity);

        subLevel.applyQueuedForces(SubLevelPhysicsSystem.get(subLevel.getLevel()), handle, 1);
        subLevel.updateLastPose();
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
