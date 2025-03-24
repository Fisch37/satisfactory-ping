package de.fisch37.satisfactory_ping.client;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.entry.ConfigEntry;

public class Config {
    public final ConfigEntry<Double> minimumIconHeight;
    public final ConfigEntry<Double> textHeight;

    public Config(ConfigBuilder builder) {
        builder.header("Configuration for SatisfactoryPing");
        minimumIconHeight = builder.doubleEntry("minimum_icon_height", 1.5, 1d, 45d)
                .comment(
                        "The minimum apparent height for the ping icon to have",
                        "Values are specified in degrees occupied in the Field of View"
                );
        textHeight = builder.doubleEntry("text_height", 2d, 1d, 20d)
                .comment("The degrees the distance meter under a ping will occupy in your Field of View (vertically)");
    }
}
