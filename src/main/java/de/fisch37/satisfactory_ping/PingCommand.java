package de.fisch37.satisfactory_ping;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.fisch37.satisfactory_ping.packets.BlockPingPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import java.util.Collection;
import java.util.Optional;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class PingCommand {
    private static ServerLevel worldFromContext(CommandContext<CommandSourceStack> context) {
        return context.getSource().getLevel();
    }
    private static Vec3 posFromContextWithArg(CommandContext<CommandSourceStack> context) {
        return Vec3Argument.getCoordinates(context, "pos").getPosition(context.getSource());
    }

    private static final LiteralArgumentBuilder<CommandSourceStack> COMMAND = literal("satisfactory-ping")
        .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
        .then(argument("targets", EntityArgument.players())
                .executes(context -> execute(context,
                        context.getSource().getPosition(), context.getSource().getLevel())
                )
                .then(argument("pos", Vec3Argument.vec3())
                        .executes(context -> execute(context, 
                                posFromContextWithArg(context), context.getSource().getLevel())
                        )
                        .then(argument("dimension", DimensionArgument.dimension())
                                .executes(context -> execute(context,
                                        posFromContextWithArg(context),
                                        DimensionArgument.getDimension(context, "dimension")
                                ))
                        )
                )
        );

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ignored,
            Commands.CommandSelection ignored1
    ) {
        dispatcher.register(COMMAND);
    }

    public static int execute(CommandContext<CommandSourceStack> context, Vec3 pos, ServerLevel dimension)
            throws CommandSyntaxException {

        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        var payload = new BlockPingPayload(dimension.dimension(), pos,
                Optional.ofNullable(context.getSource().getPlayer()).map(Entity::getUUID)
        );
        int sentTo = 0;
        for (var player : targets) {
            ServerPlayNetworking.send(player, payload);
            sentTo++;
        }
        return sentTo;
    }
}
