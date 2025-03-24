package de.fisch37.satisfactory_ping.client.config;

import de.fisch37.satisfactory_ping.client.SatisfactoryPingClient;
import de.fisch37.satisfactory_ping.client.config.widgets.DoubleSliderWidget;
import de.fisch37.satisfactory_ping.client.config.widgets.SingleEntryElementList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {
    private static final Text TITLE = Text.translatable("options.satisfactory_ping.title");

    private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);
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
        layout.addHeader(TITLE, this.textRenderer);
        layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, b -> this.close()).build());
        layout.addBody(body = new SingleEntryElementList(
                MinecraftClient.getInstance(),
                width, layout.getContentHeight(),
                layout.getHeaderHeight(), 25
        ));
        body.add(new DoubleSliderWidget(
                0, 0,
                240, 18,
                Config.MIN_ICON_HEIGHT, Config.MAX_ICON_HEIGHT, config.minimumIconHeight.get(),
                value -> Text.translatableWithFallback(
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
                value -> Text.translatableWithFallback(
                        "options.satisfactory_ping.text_height",
                        "Apparent Text Height: %f°".formatted(value), value
                ),
                value -> {
                    config.textHeight.set(value).save();
                    SatisfactoryPingClient.getRenderer().setApparentTextSize(value);
                }
        ));

        layout.forEachChild(this::addDrawableChild);
        layout.refreshPositions();
    }

    @Override
    public void close() {
        assert this.client != null;
        this.client.setScreen(parent);
    }
}
