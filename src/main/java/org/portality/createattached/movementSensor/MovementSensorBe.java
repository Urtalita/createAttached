package org.portality.createattached.movementSensor;

import com.simibubi.create.AllKeys;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.index.SimIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3d;
import org.portality.createattached.index.AttachedIndex;
import org.portality.createattached.network.SyncMovementSensorPayload;

import java.util.List;

public class MovementSensorBe extends SmartBlockEntity {

    private int activatedTicks = -1;
    protected ScrollOptionBehaviour<MovementSensorKeys> key;

    public MovementSensorBe(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void activate(){
        activatedTicks = 10;
        change(true);
    }

    public void deactivate(){
        activatedTicks = -1;
        change(false);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        key = new ScrollOptionBehaviour<>(MovementSensorKeys.class,
                CreateLang.translateDirect("configure button"), this, new MovementSensorSlotTransform(
                (state -> state.getValue(MovementSensorBlock.FACING)), 8, 5
        ));
        key.withCallback($ -> onKeyChanged());
        behaviours.add(key);
    }

    private void onKeyChanged() {

    }

    public void activateFromClient(Player player){
        Vec3 end = getActualInWorldPos();
        Vec3 start = player.position();
        double distance = start.distanceTo(end);

        if(distance > 10){
            return;
        }

        PacketDistributor.sendToServer(new SyncMovementSensorPayload(worldPosition, true));
        change(true);
    }

    public Vec3 getActualInWorldPos(){
        SubLevel subLevel = Sable.HELPER.getContaining(this);
        BlockPos pos = getBlockPos();
        if(subLevel == null) return pos.getCenter();
        Vec3 center = pos.getCenter();

        Vector3d centerD = new Vector3d(center.x, center.y, center.z);
        Vector3d transformed = subLevel.logicalPose().transformPosition(centerD);

        return new Vec3(transformed.x, transformed.y, transformed.z);
    }

    @Override
    public void tick() {
        super.tick();

        if(level.isClientSide()){
            boolean isPressed = false;
            LocalPlayer player = Minecraft.getInstance().player;

            switch (key.get()){

                case ALT -> {
                    isPressed = AllKeys.altDown();
                }
                case CTRL -> {
                    isPressed = AllKeys.ctrlDown();
                }
                case LEFT_CLICK -> {
                    isPressed = AllKeys.isMouseButtonDown(0);
                }
                case RIGHT_CLICK -> {
                    isPressed = AllKeys.isMouseButtonDown(1);
                }
                case SHIFT -> {
                    isPressed = player.isShiftKeyDown();
                }
                case SPACE -> {
                    isPressed = player.input.jumping;
                }
                case FORWARD -> {
                    isPressed = player.input.hasForwardImpulse();
                }
                case RIGHT -> {
                    isPressed = player.input.right;
                }
                case LEFT -> {
                    isPressed = player.input.left;
                }
                default -> {

                }
            }

            if(activatedTicks < 4 && isPressed){
                activateFromClient(player);
            }
        }

        if(activatedTicks >= 0){
            activatedTicks -= 1;

            if(activatedTicks < 0) change(false);
        }
    }

    public void change(boolean activated) {
        BlockState state = getBlockState();
        state = state.setValue(MovementSensorBlock.POWERED, activated);

        level.setBlock(getBlockPos(), state, 3);
        this.updateNeighbours(state, level, getBlockPos());

        sendData();
    }

    private void updateNeighbours(BlockState state, Level level, BlockPos pos) {
        level.updateNeighborsAt(pos, AttachedIndex.MOVEMENT_SENSOR.get());

        level.updateNeighborsAt(pos.relative(getConnectedDirection(state).getOpposite()), AttachedIndex.MOVEMENT_SENSOR.get());
    }

    private Direction getConnectedDirection(BlockState blockState) {
        return Direction.NORTH;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        tag.putInt("openTicks", activatedTicks);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        activatedTicks = tag.getInt("openTicks");

        super.write(tag, registries, clientPacket);
    }

    public enum MovementSensorKeys implements INamedIconOptions {

        ALT("Alt"),
        CTRL( "Ctrl"),
        LEFT_CLICK("left_click"),
        RIGHT_CLICK("right_click"),
        SHIFT("shift"),
        SPACE("space"),
        FORWARD("W"),
        RIGHT("D"),
        LEFT("A")

        ;

        private String translationKey;
        private AllIcons icon;

        private MovementSensorKeys(String name) {
            this.icon = AllIcons.I_ATTACHED;
            translationKey = name;
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }

    }
}
