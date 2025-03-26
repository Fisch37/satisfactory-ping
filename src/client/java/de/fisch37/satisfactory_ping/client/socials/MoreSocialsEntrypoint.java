package de.fisch37.satisfactory_ping.client.socials;

import de.fisch37.moresocialinteractions.api.MenuRegistry;
import de.fisch37.moresocialinteractions.api.MoreSocialsInitializer;
import de.fisch37.satisfactory_ping.client.socials.widgets.ToggleableButtonTextures;
import de.fisch37.satisfactory_ping.client.socials.widgets.ToggleableTexturedButtonWidget;
import net.minecraft.util.Identifier;

import static de.fisch37.satisfactory_ping.SatisfactoryPing.MOD_ID;
import static de.fisch37.satisfactory_ping.client.SatisfactoryPingClient.getDisabledCauses;
import static de.fisch37.satisfactory_ping.client.SatisfactoryPingClient.scheduleDisabledCausesSave;

public class MoreSocialsEntrypoint implements MoreSocialsInitializer {
    private static final ToggleableButtonTextures PING_EN_TEXTURES = new ToggleableButtonTextures(
            widgetId("ping_enabled"), widgetId("ping_disabled"),
            widgetId("ping_enabled_highlighted"), widgetId("ping_disabled_highlighted")
    );

    @Override
    public void onMoreSocialsInitialized(MenuRegistry registry) {
        registry.registerButton(entry ->
            new ToggleableTexturedButtonWidget(20, 20, PING_EN_TEXTURES, b -> {
                if (b.getState())
                    getDisabledCauses().remove(entry.uuid());
                else
                    getDisabledCauses().add(entry.uuid());
                scheduleDisabledCausesSave();
            }).setState(!getDisabledCauses().contains(entry.uuid()))
        );
    }

    private static Identifier widgetId(String id) {
        return Identifier.of(MOD_ID, "widget/"+id);
    }
}
