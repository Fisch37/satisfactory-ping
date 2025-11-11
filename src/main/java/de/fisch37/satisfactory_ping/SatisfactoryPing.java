package de.fisch37.satisfactory_ping;

import de.fisch37.satisfactory_ping.packets.BlockPingPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SatisfactoryPing implements ModInitializer {
    public static final String MOD_ID = "satisfactory_ping";
    public static final Identifier PING_PACKET_ID = Identifier.of(MOD_ID, "ping");
    public static final Logger LOGGER = LoggerFactory.getLogger(SatisfactoryPing.class);

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(PingCommand::register);
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
