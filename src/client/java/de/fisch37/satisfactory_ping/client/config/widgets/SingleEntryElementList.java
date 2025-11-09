package de.fisch37.satisfactory_ping.client.config.widgets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;

import java.util.List;
import java.util.Optional;

public class SingleEntryElementList extends ElementListWidget<SingleEntryElementList.SingleEntry> {
    public SingleEntryElementList(MinecraftClient minecraftClient, int width, int height, int y, int itemHeight) {
        super(minecraftClient, width, height, y, itemHeight);
    }

    public void add(ClickableWidget widget) {
        addEntry(new SingleEntry(widget));
    }

    static class SingleEntry extends ElementListWidget.Entry<SingleEntry> {
        private final ClickableWidget element;

        public SingleEntry(ClickableWidget element) {
            this.element = element;
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return List.of(element);
        }

        /**
         * Gets a list of all child GUI elements.
         */
        @Override
        public List<? extends Element> children() {
            return List.of(element);
        }

        @Override
        public Optional<Element> hoveredElement(double mouseX, double mouseY) {
            return super.hoveredElement(mouseX, mouseY);
        }

        /**
         * Renders an entry in a list.
         *
         * @param context
         * @param index       the index of the entry
         * @param y           the Y coordinate of the entry
         * @param x           the X coordinate of the entry
         * @param entryWidth  the width of the entry
         * @param entryHeight the height of the entry
         * @param mouseX      the X coordinate of the mouse
         * @param mouseY      the Y coordinate of the mouse
         * @param hovered     whether the mouse is hovering over the entry
         * @param tickDelta
         */
        @Override
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            throw new RuntimeException("Not implemented");
//            element.setX(x);
//            element.setY(y);
//            element.setWidth(entryWidth);
//            element.setHeight(entryHeight);
//            element.render(context,mouseX, mouseY, tickDelta);
        }
    }
}
