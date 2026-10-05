package de.fisch37.satisfactory_ping.client.rendering;

import org.joml.Vector3f;

import java.util.Optional;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.phys.Vec3;

public class Utilities {
    public static Identifier fromGuiTexture(Identifier spriteIdentifier) {
        return spriteIdentifier
                .withPrefix("textures/gui/sprites/")
                .withSuffix(".png");
    }

    public static LookingRotation lookAtRotations(Vector3f direction) {
        return new LookingRotation(
                Math.PI/2 - Math.atan2(direction.z, direction.x),
                -Math.atan2(direction.y, Math.sqrt(direction.x*direction.x + direction.z*direction.z))
        );
    }

    /**
     * Gets the yaw of a position relative to the origin.
     * (Hint: Use {@link Vec3#subtract(Vec3)} to normalize to the origin)
     */
    static double getYawOfCoord(Vec3 pos) {
        return Math.toDegrees(Math.atan2(pos.z, pos.x));
    }

    static PingRenderData getPingDataFromEntry(PlayerInfo playerListEntry) {
        int color = Optional.ofNullable(playerListEntry.getTeam())
                .flatMap(team -> Optional.ofNullable(team.getColor().getColor()))
                .orElse(CommonColors.WHITE);
        return new PingRenderData(playerListEntry.getSkin().body().id(), color);
    }

    public record LookingRotation(double y, double x) { }

    public record PingRenderData(Identifier texture, int color) { }
}
