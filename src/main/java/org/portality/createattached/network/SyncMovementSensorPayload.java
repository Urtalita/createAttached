package org.portality.createattached.network;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.portality.createattached.movementSensor.MovementSensorBe;
import org.portality.createattached.physics.PlayerPhysicHandler;

public record SyncMovementSensorPayload(BlockPos blockPos, Boolean isOn) implements ServerboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMovementSensorPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SyncMovementSensorPayload::blockPos,
            ByteBufCodecs.BOOL,
            SyncMovementSensorPayload::isOn,
            SyncMovementSensorPayload::new
    );

    @Override
    public void handle(ServerPlayer player) {
        ServerLevel serverLevel = (ServerLevel) player.level();
        BlockEntity be = serverLevel.getBlockEntity(blockPos);

        if(be instanceof MovementSensorBe movementSensorBe){
            movementSensorBe.activate();
        }
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return AttachedPackets.SYNK_MOVEMENT_SENSOR_PAYLOAD;
    }
}
