package de.fisch37.satisfactory_ping.client.mixin;

import de.fisch37.satisfactory_ping.client.LevelRendererAccessor;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin implements LevelRendererAccessor {
    @Shadow @Final
    private SubmitNodeStorage submitNodeStorage;

    @Override
    public @NonNull SubmitNodeStorage satisfactory_ping$getSubmitNodeStorage() {
        return submitNodeStorage;
    }
}
