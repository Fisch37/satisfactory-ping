package de.fisch37.satisfactory_ping.client.mixin;

import de.fisch37.satisfactory_ping.client.InGameHudAccessor;
import de.fisch37.satisfactory_ping.client.rendering.Utilities;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Hud.class, remap = false)
public class InGameHudMixin implements InGameHudAccessor {
    @Shadow
    private com.mojang.datafixers.util.Pair<Hud.ContextualInfo, ContextualBar> contextualInfoBar;

    @Override
    public Pair<Hud.ContextualInfo, ContextualBar> satisfactory_ping$getCurrentBar() {
        return Utilities.convert(contextualInfoBar);
    }
}
