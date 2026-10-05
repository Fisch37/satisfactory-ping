package de.fisch37.satisfactory_ping.client.config.widgets;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

public class SingleEntryElementList extends ContainerObjectSelectionList<SingleEntryElementList.SingleEntry> {
    public SingleEntryElementList(Minecraft minecraftClient, int width, int height, int y, int itemHeight) {
        super(minecraftClient, width, height, y, itemHeight);
    }

    public void add(AbstractWidget widget) {
        addEntry(new SingleEntry(widget));
    }

    static class SingleEntry extends ContainerObjectSelectionList.Entry<SingleEntry> {
        private final AbstractWidget element;

        public SingleEntry(AbstractWidget element) {
            this.element = element;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(element);
        }

        /**
         * Gets a list of all child GUI elements.
         */
        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(element);
        }

        @Override
        public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
            return super.getChildAt(mouseX, mouseY);
        }

        /**
         * Renders an entry in a list.
         */
        @Override
        public void renderContent(GuiGraphics context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            element.setX(getContentX());
            element.setY(getContentY());
            element.render(context,mouseX, mouseY, deltaTicks);
        }
    }
}
