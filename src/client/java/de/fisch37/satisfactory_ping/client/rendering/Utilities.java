package de.fisch37.satisfactory_ping.client.rendering;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.Optional;

public class Utilities {
    public static Identifier fromGuiTexture(Identifier spriteIdentifier) {
        return spriteIdentifier
                .withPrefixedPath("textures/gui/sprites/")
                .withSuffixedPath(".png");
    }

    public static LookingRotation lookAtRotations(Vector3f direction) {
        return new LookingRotation(
                Math.PI/2 - Math.atan2(direction.z, direction.x),
                -Math.atan2(direction.y, Math.sqrt(direction.x*direction.x + direction.z*direction.z))
        );
    }

    /**
     * Gets the yaw of a position relative to the origin.
     * (Hint: Use {@link Vec3d#subtract(Vec3d)} to normalize to the origin)
     */
    static double getYawOfCoord(Vec3d pos) {
        return Math.toDegrees(Math.atan2(pos.z, pos.x));
    }

    static PingRenderData getPingDataFromEntry(PlayerListEntry playerListEntry) {
        int color = Optional.ofNullable(playerListEntry.getScoreboardTeam())
                .flatMap(team -> Optional.ofNullable(team.getColor().getColorValue()))
                .orElse(Colors.WHITE);
        return new PingRenderData(playerListEntry.getSkinTextures().body().id(), color);
    }

    public record LookingRotation(double y, double x) { }

    public record PingRenderData(Identifier texture, int color) { }
}
