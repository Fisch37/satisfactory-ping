package de.fisch37.satisfactory_ping.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public interface InGameHudAccessor {
    Pair<Gui.ContextualInfo, ContextualBarRenderer> satisfactory_ping$getCurrentBar();

    static Gui.ContextualInfo getCurrentBarType(@NotNull Gui obj) {
        return ((InGameHudAccessor)obj).satisfactory_ping$getCurrentBar().getLeft();
    }

    static ContextualBarRenderer getCurrentBar(@NotNull Gui obj) {
        return ((InGameHudAccessor)obj).satisfactory_ping$getCurrentBar().getRight();
    }
}
