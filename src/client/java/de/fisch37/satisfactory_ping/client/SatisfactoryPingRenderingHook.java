package de.fisch37.satisfactory_ping.client;

import com.mojang.blaze3d.systems.RenderSystem;
import de.fisch37.satisfactory_ping.client.config.Config;
import de.fisch37.satisfactory_ping.packets.BlockPingPayload;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.*;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static de.fisch37.satisfactory_ping.SatisfactoryPing.MOD_ID;

public class SatisfactoryPingRenderingHook {
    private static final Logger LOGGER = LoggerFactory.getLogger("SatisfactoryPing/Rendering");
    private static final Identifier TEXTURE_ID = Identifier.of(MOD_ID, "textures/gui/location_ping.png");
    private static final Identifier BORDER_TEXTURE = Identifier.of(MOD_ID, "textures/gui/ping_border.png");
    private static final double PING_RENDER_TIME = 5_000;

    private final SatisfactoryPingClient mod;
    private boolean irisWorkaroundEnabled = false;
    private final ConcurrentLinkedQueue<Pair<BlockPingPayload, Long>> renderQueue = new ConcurrentLinkedQueue<>();
    public double smallestApparentHeight;
    public double scalingFactor;
    public double textScalingFactor;

    public SatisfactoryPingRenderingHook(SatisfactoryPingClient mod) {
        this.mod = mod;
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
        irisWorkaroundEnabled = true;
        WorldRenderEvents.AFTER_ENTITIES.register(context ->
            renderQueue.forEach(pair -> {
                var ping = pair.getLeft();
                var diff = context.camera().getPos().subtract(ping.pos());
                float scale = 0.25f;
                final double distanceToCamera = diff.length();
                double apparentHeight = 2*Math.atan(scale/distanceToCamera);
                if (apparentHeight < smallestApparentHeight)
                    scale = (float) (distanceToCamera*scalingFactor);
                renderText(context, ping, scale, distanceToCamera);
            })
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
        smallestApparentHeight = Math.toRadians(degrees);
        scalingFactor = Math.tan(smallestApparentHeight / 2d);
    }

    /**
     * Sets the apparent size of the distance label
     * @param degrees The apparent size of the text in degrees
     */
    public void setApparentTextSize(double degrees) {
        textScalingFactor = Math.tan(Math.toRadians(degrees/2));
    }

    private void renderSequence(WorldRenderContext context) {
        long millis = Util.getMeasuringTimeMs();
        for (var item : renderQueue) {
            if (millis - item.getRight() <= PING_RENDER_TIME) {
                renderPing(context, item.getLeft());
            } else {
                renderQueue.remove(item);
            }
        }
    }

    private void renderPing(WorldRenderContext context, BlockPingPayload ping) {
        // Running BEFORE_ENTITIES: matrixStack == null
        Matrix4f matrix = new Matrix4f();
        var camPos = context.camera().getPos();
        var pos = ping.pos();
        var diff = camPos.subtract(pos);
        matrix.translate(-(float)camPos.x, -(float)camPos.y, -(float)camPos.z);
        matrix.translate((float)pos.x, (float)pos.y, (float)pos.z);
        // This could have been a simple library call, but that didn't seem to work
        var rotations = lookAtRotations(diff.toVector3f());
        matrix.rotate((float)rotations.y, 0, 1, 0);
        matrix.rotate((float) rotations.x, 1, 0, 0);

        float scale = 0.25f;
        final double distanceToCamera = diff.length();
        double apparentHeight = 2*Math.atan(scale/distanceToCamera);
        if (apparentHeight < smallestApparentHeight)
            scale = (float) (distanceToCamera*scalingFactor);

        renderHeadWithBorder(context, ping.cause(), matrix, scale);

        if (!irisWorkaroundEnabled) renderText(context, ping, scale, distanceToCamera);
    }

    private void renderHeadWithBorder(WorldRenderContext context, UUID player, Matrix4f matrix, float scale) {
        int color;
        // It's good that this is a personal project, because I feel like if I did this at a job, I'd get fired
        final var networkHandler = context.gameRenderer().getClient().getNetworkHandler();
        if (networkHandler == null) {
            LOGGER.error("Network handler not available, cannot get player skin");
            return;
        }
        final var playerEntry = networkHandler.getPlayerListEntry(player);
        if (playerEntry == null) {
            LOGGER.error("Cannot find player entry for {}, cannot get player skin", player.toString());
            return;
        }
        var team = playerEntry.getScoreboardTeam();
        // This is ridiculous
        Integer c = team == null ? null : team.getColor().getColorValue();
        color = c == null ? -1 : c;


        var buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(matrix, -scale, -scale, 0).texture(0, 1);
        buffer.vertex(matrix, scale, -scale, 0).texture(1, 1);
        buffer.vertex(matrix, scale, scale, 0).texture(1, 0);
        buffer.vertex(matrix, -scale, scale, 0).texture(0, 0);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
        RenderSystem.setShaderTexture(0, BORDER_TEXTURE);
        RenderSystem.setShaderColor(
                ((color >> 16) & 0xff) / 255f,
                ((color >> 8) & 0xff) / 255f,
                (color & 0xff) / 255f,
                (color >> 24) / 255f
        );

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        renderHead(context, playerEntry, matrix, scale*(12/16f));
    }

    private void renderHead(WorldRenderContext context, PlayerListEntry player, Matrix4f matrix, float scale) {
        var texture = player.getSkinTextures().texture();

        var buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(matrix, -scale, -scale, 0).texture(8/64f, 16/64f);
        buffer.vertex(matrix, scale, -scale, 0).texture(16/64f, 16/64f);
        buffer.vertex(matrix, scale, scale, 0).texture(16/64f, 8/64f);
        buffer.vertex(matrix, -scale, scale, 0).texture(8/64f, 8/64f);

        // Make sure the correct shader for your chosen vertex format is set!
        // I did! (It took 2 hours of debugging, but I did!)
        // You can find all the shaders in the ShaderProgramKeys class.
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void renderIcon(WorldRenderContext context, UUID player, Matrix4f matrix, float scale) {
        var buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(matrix, -scale, -scale, 0).texture(0, 1);
        buffer.vertex(matrix, scale, -scale, 0).texture(1, 1);
        buffer.vertex(matrix, scale, scale, 0).texture(1, 0);
        buffer.vertex(matrix, -scale, scale, 0).texture(0, 0);

        // Make sure the correct shader for your chosen vertex format is set!
        // I did! (It took 2 hours of debugging, but I did!)
        // You can find all the shaders in the ShaderProgramKeys class.
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
        RenderSystem.setShaderTexture(0, TEXTURE_ID);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Draw the buffer onto the screen.
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void renderText(WorldRenderContext context, BlockPingPayload ping, double scale, double distanceToCamera) {
        final var fontHeight = 2*distanceToCamera*textScalingFactor;
        textHelper(context, ping.pos().subtract(0, scale+fontHeight, 0), (long)distanceToCamera + "m", (float)fontHeight);
    }

    private static LookingRotation lookAtRotations(Vector3f direction) {
        return new LookingRotation(
                Math.PI/2 - Math.atan2(direction.z, direction.x),
                -Math.atan2(direction.y, Math.sqrt(direction.x*direction.x + direction.z*direction.z))
        );
    }

    private static void rotateAround(Matrix4f matrix, Vector3f point, LookingRotation rotation) {
        matrix.translate(point)
                .rotate((float)rotation.y, 0, 1, 0)
                .rotate((float)rotation.x, 1, 0, 0)
                .translate(point.negate());
    }

    private record LookingRotation(double y, double x) { }

    private static void textTest(WorldRenderContext context) {
        textHelper(context, new Vec3d(8.5, -60, 8.5), "Hello World!", 0.25f);
    }

    private static void textHelper(WorldRenderContext context, Vec3d bottomCentre, String text, float height) {
        final var textRenderer = context.gameRenderer().getClient().textRenderer;
        final var scale = height/textRenderer.fontHeight;
        var matrix = new Matrix4f();
        final var diff = bottomCentre.subtract(context.camera().getPos()).toVector3f();
        matrix.translate(diff);
        matrix.translate(0, height, 0);
        matrix.rotate((float)Math.PI, 0, 0, 1);  // For some reason text is rendered upside-down
        matrix.scale(scale);
        var rotation = lookAtRotations(diff);
        matrix.rotate(-(float)rotation.y, 0, 1, 0);
        matrix.rotate(-(float)rotation.x, 1, 0, 0);
        // matrix.scale(scale);
        textHelper(context, matrix, text);
    }

    private static void textHelper(WorldRenderContext context, Matrix4f matrix, String text) {
        final var textRenderer = context.gameRenderer().getClient().textRenderer;
        textRenderer.draw(
                text,
                -textRenderer.getWidth(text)/2f, 0,
                Colors.WHITE, false,
                matrix, context.consumers(),
                TextRenderer.TextLayerType.SEE_THROUGH,
                0, 255
        );
    }
}
