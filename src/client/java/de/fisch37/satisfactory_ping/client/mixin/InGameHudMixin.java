package de.fisch37.satisfactory_ping.client.mixin;

import de.fisch37.satisfactory_ping.client.InGameHudAccessor;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.bar.Bar;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(InGameHud.class)
public class InGameHudMixin implements InGameHudAccessor {
    @Shadow
    private Pair<InGameHud.BarType, Bar> currentBar;

    @Override
    public Pair<InGameHud.BarType, Bar> satisfactory_ping$getCurrentBar() {
        return currentBar;
    }
}
