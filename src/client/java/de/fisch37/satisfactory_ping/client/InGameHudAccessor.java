package de.fisch37.satisfactory_ping.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public interface InGameHudAccessor {
    Pair<Hud.ContextualInfo, ContextualBar> satisfactory_ping$getCurrentBar();

    static Hud.ContextualInfo getCurrentBarType(@NotNull Gui obj) {
        return getCurrentBarType(obj.hud);
    }
    static Hud.ContextualInfo getCurrentBarType(@NotNull Hud obj) {
        return ((InGameHudAccessor)obj).satisfactory_ping$getCurrentBar().getLeft();
    }

    static ContextualBar getCurrentBar(@NotNull Gui obj) {
        return getCurrentBar(obj.hud);
    }
    static ContextualBar getCurrentBar(@NotNull Hud obj) {
        return ((InGameHudAccessor)obj).satisfactory_ping$getCurrentBar().getRight();
    }
}
