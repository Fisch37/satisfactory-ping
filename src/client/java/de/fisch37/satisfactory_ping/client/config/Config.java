package de.fisch37.satisfactory_ping.client.config;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.entry.ConfigEntry;

public class Config {
    public static final double MIN_ICON_HEIGHT = 1;
    public static final double MAX_ICON_HEIGHT = 45;
    public static final double DEFAULT_ICON_HEIGHT = 1.5;

    public static final double MIN_TEXT_HEIGHT = 1;
    public static final double DEFAULT_TEXT_HEIGHT = 2;
    public static final double MAX_TEXT_HEIGHT = 20;

    public final ConfigEntry<Double> minimumIconHeight;
    public final ConfigEntry<Double> textHeight;

    public Config(ConfigBuilder builder) {
        builder.header("Configuration for SatisfactoryPing");
        minimumIconHeight = builder.doubleEntry("minimum_icon_height", DEFAULT_ICON_HEIGHT, MIN_ICON_HEIGHT, MAX_ICON_HEIGHT)
                .comment(
                        "The minimum apparent height for the ping icon to have",
                        "Values are specified in degrees occupied in the Field of View"
                );
        textHeight = builder.doubleEntry("text_height", DEFAULT_TEXT_HEIGHT, MIN_TEXT_HEIGHT, MAX_TEXT_HEIGHT)
                .comment("The degrees the distance meter under a ping will occupy in your Field of View (vertically)");
    }
}
