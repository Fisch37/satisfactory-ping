package de.fisch37.satisfactory_ping.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import de.fisch37.satisfactory_ping.client.SatisfactoryPingClient;
import net.minecraft.client.gui.screen.Screen;

public class ModMenuEntrypoint implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return this::configScreenFactory;
    }

    private ConfigScreen configScreenFactory(Screen other) {
        return new ConfigScreen(other, SatisfactoryPingClient.getConfig());
    }
}
