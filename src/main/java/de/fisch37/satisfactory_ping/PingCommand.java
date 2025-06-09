package de.fisch37.satisfactory_ping;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.fisch37.satisfactory_ping.packets.BlockPingPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.*;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.Collection;
import java.util.Optional;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class PingCommand {
    private static ServerWorld worldFromContext(CommandContext<ServerCommandSource> context) {
        return context.getSource().getWorld();
    }
    private static Vec3d posFromContextWithArg(CommandContext<ServerCommandSource> context) {
        return Vec3ArgumentType.getPosArgument(context, "pos").getPos(context.getSource());
    }

    private static final LiteralArgumentBuilder<ServerCommandSource> COMMAND = literal("satisfactory-ping")
        .requires(source -> source.hasPermissionLevel(2))
        .then(argument("targets", EntityArgumentType.players())
                .executes(context -> execute(context,
                        context.getSource().getPosition(), context.getSource().getWorld())
                )
                .then(argument("pos", Vec3ArgumentType.vec3())
                        .executes(context -> execute(context, 
                                posFromContextWithArg(context), context.getSource().getWorld())
                        )
                        .then(argument("dimension", DimensionArgumentType.dimension())
                                .executes(context -> execute(context,
                                        posFromContextWithArg(context),
                                        DimensionArgumentType.getDimensionArgument(context, "dimension")
                                ))
                        )
                )
        );

    public static void register(
            CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environ
    ) {
        dispatcher.register(COMMAND);
    }

    public static int execute(CommandContext<ServerCommandSource> context, Vec3d pos, ServerWorld dimension)
            throws CommandSyntaxException {

        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "targets");
        var payload = new BlockPingPayload(dimension.getRegistryKey(), pos,
                Optional.ofNullable(context.getSource().getPlayer()).map(Entity::getUuid)
        );
        int sentTo = 0;
        for (var player : targets) {
            ServerPlayNetworking.send(player, payload);
            sentTo++;
        }
        return sentTo;
    }
}
