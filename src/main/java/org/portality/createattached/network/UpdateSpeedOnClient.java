package org.portality.createattached.network;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.portality.createattached.physics.EntityRotationHandler;

public record UpdateSpeedOnClient(Vector3f updatedSpeed) implements ClientboundPacketPayload {

    public static final StreamCodec<ByteBuf, UpdateSpeedOnClient> STREAM_CODEC = ByteBufCodecs.VECTOR3F.map(
            UpdateSpeedOnClient::new, UpdateSpeedOnClient::updatedSpeed
    );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return AttachedPackets.SYNK_MOVEMENT_PAYLOAD;
    }

    @Override
    public void handle(LocalPlayer player) {
        Vec3 updated = new Vec3(updatedSpeed.x(), updatedSpeed.y(), updatedSpeed.z());
        player.addDeltaMovement(updated);
        player.hurtMarked = false;
    }
}
