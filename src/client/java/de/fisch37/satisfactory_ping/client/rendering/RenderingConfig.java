package de.fisch37.satisfactory_ping.client.rendering;

public class RenderingConfig {
    private boolean irisWorkaround;
    private double smallestApparentHeight, scalingFactor, textScalingFactor;

    public void enableIrisWorkaround() {
        irisWorkaround = true;
    }

    public void setSmallestApparentHeight(double degrees) {
        smallestApparentHeight = Math.toRadians(degrees);
        scalingFactor = Math.tan(smallestApparentHeight / 2d);
    }

    public void setTextApparentHeight(double degrees) {
        textScalingFactor = Math.tan(Math.toRadians(degrees/2));
    }

    public double getSmallestApparentHeight() {
        return smallestApparentHeight;
    }

    public double getScalingFactor() {
        return scalingFactor;
    }

    public double getTextScalingFactor() {
        return textScalingFactor;
    }

    public boolean isIrisWorkaroundEnabled() {
        return irisWorkaround;
    }
}
