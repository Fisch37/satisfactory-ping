package de.fisch37.satisfactory_ping.client.rendering;

import de.fisch37.satisfactory_ping.client.SatisfactoryPingRenderingHook;
import org.joml.Vector3f;

public class Utilities {
    public static LookingRotation lookAtRotations(Vector3f direction) {
        return new LookingRotation(
                Math.PI/2 - Math.atan2(direction.z, direction.x),
                -Math.atan2(direction.y, Math.sqrt(direction.x*direction.x + direction.z*direction.z))
        );
    }

    public record LookingRotation(double y, double x) { }
}
