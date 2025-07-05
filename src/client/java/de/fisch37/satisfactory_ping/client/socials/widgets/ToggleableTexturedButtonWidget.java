package de.fisch37.satisfactory_ping.client.socials.widgets;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.screen.ScreenTexts;

public class ToggleableTexturedButtonWidget extends ButtonWidget {
    private final ToggleableButtonTextures textures;
    private boolean state;
    private final ToggleableButtonPressAction onPress;

    public ToggleableTexturedButtonWidget(
            int width, int height, ToggleableButtonTextures textures, ToggleableButtonPressAction onPress
    ) {
        this(0, 0, width, height, textures, onPress);
    }

    public ToggleableTexturedButtonWidget(
            int x, int y, int width, int height,
            ToggleableButtonTextures textures, ToggleableButtonPressAction onPress
    ) {
        super(x, y, width, height, ScreenTexts.EMPTY, b -> { }, DEFAULT_NARRATION_SUPPLIER);
        this.onPress = onPress;
        this.textures = textures;
    }

    public boolean getState() {
        return state;
    }

    public ToggleableTexturedButtonWidget setState(boolean state) {
        this.state = state;
        return this;
    }

    @Override
    public void onPress() {
        state = !state;
        this.onPress.onPress(this);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawGuiTexture(
                RenderPipelines.GUI_TEXTURED, textures.get(getState(), isHovered()),
                getX(), getY(), width, height
        );
    }

    @FunctionalInterface
    public interface ToggleableButtonPressAction {
        void onPress(ToggleableTexturedButtonWidget button);
    }
}
