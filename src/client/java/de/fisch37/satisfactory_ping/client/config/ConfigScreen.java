package de.fisch37.satisfactory_ping.client.config;

import de.fisch37.satisfactory_ping.client.SatisfactoryPingClient;
import de.fisch37.satisfactory_ping.client.config.widgets.DoubleSliderWidget;
import de.fisch37.satisfactory_ping.client.config.widgets.SingleEntryElementList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private static final Component TITLE = Component.translatable("options.satisfactory_ping.title");

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private SingleEntryElementList body;
    private final Screen parent;
    private final Config config;

    protected ConfigScreen(Screen parent, Config config) {
        super(TITLE);
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        layout.addTitleHeader(TITLE, this.font);
        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose()).build());
        layout.addToContents(body = new SingleEntryElementList(
                Minecraft.getInstance(),
                width, layout.getContentHeight(),
                layout.getHeaderHeight(), 25
        ));
        body.add(new DoubleSliderWidget(
                0, 0,
                240, 18,
                Config.MIN_ICON_HEIGHT, Config.MAX_ICON_HEIGHT, config.minimumIconHeight.get(),
                value -> Component.translatableWithFallback(
                        "options.satisfactory_ping.icon_height",
                        "Minimum Apparent Icon Height: %f°".formatted(value),value
                ),
                value -> {
                    config.minimumIconHeight.set(value).save();
                    SatisfactoryPingClient.getRenderer().setMinimumHeight(value);
                }
        ));
        body.add(new DoubleSliderWidget(
                0, 0,
                240, 18,
                Config.MIN_TEXT_HEIGHT, Config.MAX_TEXT_HEIGHT, config.textHeight.get(),
                value -> Component.translatableWithFallback(
                        "options.satisfactory_ping.text_height",
                        "Apparent Text Height: %f°".formatted(value), value
                ),
                value -> {
                    config.textHeight.set(value).save();
                    SatisfactoryPingClient.getRenderer().setApparentTextSize(value);
                }
        ));

        layout.visitWidgets(this::addRenderableWidget);
        layout.arrangeElements();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }
}
