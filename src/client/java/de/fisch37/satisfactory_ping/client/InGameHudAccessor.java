package de.fisch37.satisfactory_ping.client;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.bar.Bar;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public interface InGameHudAccessor {
    Pair<InGameHud.BarType, Bar> satisfactory_ping$getCurrentBar();

    static InGameHud.BarType getCurrentBarType(@NotNull InGameHud obj) {
        return ((InGameHudAccessor)obj).satisfactory_ping$getCurrentBar().getLeft();
    }

    static Bar getCurrentBar(@NotNull InGameHud obj) {
        return ((InGameHudAccessor)obj).satisfactory_ping$getCurrentBar().getRight();
    }
}
