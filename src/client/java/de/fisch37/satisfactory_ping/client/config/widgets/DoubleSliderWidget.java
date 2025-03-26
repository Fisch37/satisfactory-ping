package de.fisch37.satisfactory_ping.client.config.widgets;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Function;

public class DoubleSliderWidget extends SliderWidget {
    private final double min, max;
    private final Function<Double, Text> updateFunction;
    private final Consumer<Double> applier;

    public DoubleSliderWidget(
            int x,
            int y,
            int width,
            int height,
            double min,
            double max,
            double initialValue,
            Function<Double, Text> messageUpdate,
            Consumer<Double> applier
    ) {
        super(x, y, width, height, Text.empty(), (initialValue-min)/(max-min));
        this.min = min;
        this.max = max;
        this.updateFunction = messageUpdate;
        this.applier = applier;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(updateFunction.apply(getRealValue()));
    }

    @Override
    protected void applyValue() {
        applier.accept(getRealValue());
    }

    public double getRealValue() {
        return Math.round(10*(this.value * (max-min) + min))/10d;
    }
}
