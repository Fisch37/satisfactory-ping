package de.fisch37.satisfactory_ping.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

import static de.fisch37.satisfactory_ping.SatisfactoryPing.PING_PACKET_ID;

public record BlockPingPayload(RegistryKey<World> dimension, Vec3d pos, UUID cause) implements CustomPayload {
    public static final CustomPayload.Id<BlockPingPayload> ID = new CustomPayload.Id<>(PING_PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, BlockPingPayload> CODEC = PacketCodec.tuple(
            RegistryKey.createPacketCodec(RegistryKeys.WORLD), BlockPingPayload::dimension,
            Vec3d.PACKET_CODEC, BlockPingPayload::pos,
            Uuids.PACKET_CODEC, BlockPingPayload::cause,
            BlockPingPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public BlockPingPayload withPlayer(UUID player) {
        return new BlockPingPayload(dimension, pos, player);
    }
}
