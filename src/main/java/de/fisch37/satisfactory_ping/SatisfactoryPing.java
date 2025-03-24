package de.fisch37.satisfactory_ping;

import de.fisch37.satisfactory_ping.packets.BlockPingPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public class SatisfactoryPing implements ModInitializer {
    public static final String MOD_ID = "satisfactory_ping";
    public static final Identifier PING_PACKET_ID = Identifier.of(MOD_ID, "ping");

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(BlockPingPayload.ID, BlockPingPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BlockPingPayload.ID, BlockPingPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(BlockPingPayload.ID, (payload, context) ->
                context.server().getPlayerManager()
                    .getPlayerList()
                    .forEach(player -> ServerPlayNetworking.send(
                            player,
                            payload.withPlayer(context.player().getUuid())
                    )));
    }
}
