package org.portality.createattached.network;

import com.simibubi.create.Create;
import com.simibubi.create.CreateBuildInfo;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Locale;

public enum AttachedPackets implements BasePacketPayload.PacketTypeProvider {
    SYNK_ROTATION_PAYLOAD(SyncBodyAnglePayload.class, SyncBodyAnglePayload.STREAM_CODEC),
    SYNK_MOVEMENT_PAYLOAD(UpdateSpeedOnClient.class, UpdateSpeedOnClient.STREAM_CODEC),
    SYNK_MOVEMENT_SENSOR_PAYLOAD(SyncMovementSensorPayload.class, SyncMovementSensorPayload.STREAM_CODEC)
    ;

    private final CatnipPacketRegistry.PacketType<?> type;

    <T extends BasePacketPayload> AttachedPackets(Class<T> clazz, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        String name = this.name().toLowerCase(Locale.ROOT);
        this.type = new CatnipPacketRegistry.PacketType<>(
                new CustomPacketPayload.Type<>(Create.asResource(name)),
                clazz, codec
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getType() {
        return (CustomPacketPayload.Type<T>) this.type.type();
    }

    public static void register() {
        CatnipPacketRegistry packetRegistry = new CatnipPacketRegistry(Create.ID, CreateBuildInfo.VERSION);
        for (AttachedPackets packet : AttachedPackets.values()) {
            packetRegistry.registerPacket(packet.type);
        }
        packetRegistry.registerAllPackets();
    }
}
