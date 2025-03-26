package de.fisch37.satisfactory_ping.client.socials.widgets;

import net.minecraft.util.Identifier;

public record ToggleableButtonTextures(
        Identifier enabled, Identifier disabled,
        Identifier enabledFocused, Identifier disabledFocused
) {
    public Identifier get(boolean isEnabled, boolean isFocused) {
        return isEnabled
                ? (isFocused ? enabledFocused : enabled)
                : (isFocused ? disabledFocused : disabled);
    }
}
