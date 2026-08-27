package org.portality.createattached.network;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import org.portality.createattached.physics.PlayerPhysicHandler;

public record SyncBodyAnglePayload(float yBodyRot) implements ServerboundPacketPayload {

    public static final int TICKS_BETWEEN_PACKETS = 5;

    public static final StreamCodec<ByteBuf, SyncBodyAnglePayload> STREAM_CODEC = ByteBufCodecs.FLOAT.map(
            SyncBodyAnglePayload::new, SyncBodyAnglePayload::yBodyRot
    );


    @Override
    public void handle(ServerPlayer player) {
        if(!Float.isFinite(yBodyRot)) return;
        if(Float.isNaN(yBodyRot)) return;

        PlayerPhysicHandler.syncBodyRotation(player, Mth.wrapDegrees(yBodyRot));
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return AttachedPackets.SYNK_ROTATION_PAYLOAD;
    }
}
