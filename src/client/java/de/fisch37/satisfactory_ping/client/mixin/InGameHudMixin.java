package de.fisch37.satisfactory_ping.client.mixin;

import de.fisch37.satisfactory_ping.client.InGameHudAccessor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Gui.class)
public class InGameHudMixin implements InGameHudAccessor {
    @Shadow
    private Pair<Gui.ContextualInfo, ContextualBarRenderer> contextualInfoBar;

    @Override
    public Pair<Gui.ContextualInfo, ContextualBarRenderer> satisfactory_ping$getCurrentBar() {
        return contextualInfoBar;
    }
}
