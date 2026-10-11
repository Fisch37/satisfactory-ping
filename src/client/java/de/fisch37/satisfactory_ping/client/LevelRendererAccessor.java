package de.fisch37.satisfactory_ping.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface LevelRendererAccessor {
    SubmitNodeStorage satisfactory_ping$getSubmitNodeStorage();

    static SubmitNodeStorage getSubmitNodeStorage(LevelRenderer levelRenderer) {
        return ((LevelRendererAccessor)levelRenderer).satisfactory_ping$getSubmitNodeStorage();
    }
}
