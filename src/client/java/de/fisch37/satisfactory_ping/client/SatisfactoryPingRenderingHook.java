package de.fisch37.satisfactory_ping.client;

import com.mojang.blaze3d.systems.RenderSystem;
import de.fisch37.satisfactory_ping.packets.BlockPingPayload;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
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

import java.util.concurrent.ConcurrentLinkedQueue;

import static de.fisch37.satisfactory_ping.SatisfactoryPing.MOD_ID;

public class SatisfactoryPingRenderingHook {
    private static final Logger LOGGER = LoggerFactory.getLogger("SatisfactoryPing/Rendering");
    private static final Identifier TEXTURE_ID = Identifier.of(MOD_ID, "textures/gui/location_ping.png");
    private static final double PING_RENDER_TIME = 5_000;

    private final SatisfactoryPingClient mod;
    private final ConcurrentLinkedQueue<Pair<BlockPingPayload, Long>> renderQueue = new ConcurrentLinkedQueue<>();
    public double smallestApparentHeight;
    public double scalingFactor;
    public double textScalingFactor;

    public SatisfactoryPingRenderingHook(SatisfactoryPingClient mod) {
        this.mod = mod;
        WorldRenderEvents.LAST.register(this::renderSequence);
        setMinimumHeight(1.5);
        setApparentTextSize(2.5);
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

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        float scale = 0.25f;
        final double distanceToCamera = diff.length();
        double apparentHeight = 2*Math.atan(scale/distanceToCamera);
        if (apparentHeight < smallestApparentHeight)
            scale = (float) (distanceToCamera*scalingFactor);
        // Write our vertices, Z doesn't really matter since it's on the HUD.
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
