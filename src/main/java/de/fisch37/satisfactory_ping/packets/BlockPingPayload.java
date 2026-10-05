package de.fisch37.satisfactory_ping.packets;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

import static de.fisch37.satisfactory_ping.SatisfactoryPing.PING_PACKET_ID;

public record BlockPingPayload(ResourceKey<Level> dimension, Vec3 pos, Optional<UUID> cause) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BlockPingPayload> ID = new CustomPacketPayload.Type<>(PING_PACKET_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockPingPayload> CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), BlockPingPayload::dimension,
            Vec3.STREAM_CODEC, BlockPingPayload::pos,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), BlockPingPayload::cause,
            BlockPingPayload::new
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public BlockPingPayload withPlayer(@Nullable UUID player) {
        return new BlockPingPayload(dimension, pos, Optional.ofNullable(player));
    }
}
