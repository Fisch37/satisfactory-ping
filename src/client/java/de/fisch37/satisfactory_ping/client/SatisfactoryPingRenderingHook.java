package de.fisch37.satisfactory_ping.client;

import de.fisch37.satisfactory_ping.client.config.Config;
import de.fisch37.satisfactory_ping.client.rendering.InWorldRendering;
import de.fisch37.satisfactory_ping.client.rendering.RenderingConfig;
import de.fisch37.satisfactory_ping.packets.BlockPingPayload;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

import static de.fisch37.satisfactory_ping.SatisfactoryPing.MOD_ID;

public class SatisfactoryPingRenderingHook {
    public static final Logger LOGGER = LoggerFactory.getLogger("SatisfactoryPing/Rendering");
    public static final Identifier TEXTURE_ID = Identifier.of(MOD_ID, "textures/gui/location_ping.png");
    public static final Identifier BORDER_TEXTURE = Identifier.of(MOD_ID, "textures/gui/ping_border.png");
    private static final double PING_RENDER_TIME = 5_000;

    private final SatisfactoryPingClient mod;
    private final InWorldRendering inWorld;
    private final RenderingConfig config;
    private final ConcurrentLinkedQueue<Pair<BlockPingPayload, Long>> renderQueue = new ConcurrentLinkedQueue<>();

    public SatisfactoryPingRenderingHook(SatisfactoryPingClient mod) {
        this.mod = mod;
        config = new RenderingConfig();
        inWorld = new InWorldRendering(config);
        WorldRenderEvents.LAST.register(this::renderSequence);
        setMinimumHeight(Config.DEFAULT_ICON_HEIGHT);
        setApparentTextSize(Config.DEFAULT_TEXT_HEIGHT);
    }

    void enableIrisWorkaround() {
        // Iris does some insane things to rendering that leads to my text always appearing <em>behind</em> the world, when Iris is installed,
        // but only if I use the LAST render event. I need LAST tho because otherwise my icons appear behind translucent materials, which looks shit.
        // I don't understand what Iris could possibly be doing to cause this, but since it is a very popular mod, I implemented this bullshit workaround
        // I hope I'll come back and replace this with something that actually solves the issue at some point.

        // This workaround isn't perfect either. For one, text now disappears up to one frame after the icon,
        // but also since we're now rendering text before the translucency layer is drawn, text will be partially obscured by translucent blocks such as glass or water.
        LOGGER.warn("Applied a workaround to fix text not rendering when Iris shaders is installed. You may encounter slight visual oddities with distance markers.");
        config.enableIrisWorkaround();
        WorldRenderEvents.AFTER_ENTITIES.register(context ->
            renderQueue.forEach(pair -> inWorld.renderIrisWorkaround(context, pair.getLeft()))
        );
    }

    public void add(BlockPingPayload ping) {
        renderQueue.add(new Pair<>(ping, Util.getMeasuringTimeMs()));
    }

    /**
     * Sets the smallest allowed apparent height for a ping marker to have
     * @param degrees The apparent height in degrees
     */
    public void setMinimumHeight(double degrees) {
        config.setSmallestApparentHeight(degrees);
    }

    /**
     * Sets the apparent size of the distance label
     * @param degrees The apparent size of the text in degrees
     */
    public void setApparentTextSize(double degrees) {
        config.setTextApparentHeight(degrees);
    }

    private void renderSequence(WorldRenderContext context) {
        long millis = Util.getMeasuringTimeMs();
        for (var item : renderQueue) {
            if (millis - item.getRight() <= PING_RENDER_TIME) {
                inWorld.renderPing(context, item.getLeft());
            } else {
                renderQueue.remove(item);
            }
        }
    }
}
