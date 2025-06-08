package de.fisch37.satisfactory_ping.client.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import de.fisch37.satisfactory_ping.packets.BlockPingPayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Colors;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.UUID;

import static de.fisch37.satisfactory_ping.client.SatisfactoryPingRenderingHook.*;
import static de.fisch37.satisfactory_ping.client.rendering.Utilities.lookAtRotations;

public class InWorldRendering {
    public static final float MIN_SCALE = 0.25f;
    private final RenderingConfig config;

    public InWorldRendering(RenderingConfig config) {
        this.config = config;
    }

    public void renderIrisWorkaround(WorldRenderContext context, BlockPingPayload ping) {
        var diff = context.camera().getPos().subtract(ping.pos());
        float scale = 0.25f;
        final double distanceToCamera = diff.length();
        double apparentHeight = 2*Math.atan(scale/distanceToCamera);
        if (apparentHeight < config.getSmallestApparentHeight())
            scale = (float) (distanceToCamera*config.getScalingFactor());
        renderText(context, ping, scale, distanceToCamera);
    }

    public void renderPing(WorldRenderContext context, BlockPingPayload ping) {
        // Running BEFORE_ENTITIES: matrixStack == null
        Matrix4f matrix = new Matrix4f();
        var camPos = context.camera().getPos();
        var pos = ping.pos();
        var diff = camPos.subtract(pos);
        matrix.translate(-(float)camPos.x, -(float)camPos.y, -(float)camPos.z);
        matrix.translate((float)pos.x, (float)pos.y, (float)pos.z);
        // This could have been a simple library call, but that didn't seem to work
        var rotations = lookAtRotations(diff.toVector3f());
        matrix.rotate((float)rotations.y(), 0, 1, 0);
        matrix.rotate((float) rotations.x(), 1, 0, 0);

        final double distanceToCamera = diff.length();
        double apparentHeight = 2*Math.atan(MIN_SCALE/distanceToCamera);
        float scale;
        if (apparentHeight < config.getSmallestApparentHeight()) {
            scale = (float) (distanceToCamera * config.getScalingFactor());
        } else {
            scale = MIN_SCALE;
        }

        ping.cause().ifPresentOrElse(
                cause -> renderHeadWithBorder(context, cause, matrix, scale),
                () -> renderTexture(context, matrix, scale)
        );

        if (!config.isIrisWorkaroundEnabled()) renderText(context, ping, scale, distanceToCamera);
    }

    private void renderText(WorldRenderContext context, BlockPingPayload ping, double scale, double distanceToCamera) {
        final var fontHeight = 2*distanceToCamera*config.getTextScalingFactor();
        textHelper(context, ping.pos().subtract(0, scale+fontHeight, 0), (long)distanceToCamera + "m", (float)fontHeight);
    }

    private void renderTexture(WorldRenderContext context, Matrix4f matrix, float scale) {
        var buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(matrix, -scale, -scale, 0).texture(0, 1);
        buffer.vertex(matrix, scale, -scale, 0).texture(1, 1);
        buffer.vertex(matrix, scale, scale, 0).texture(1, 0);
        buffer.vertex(matrix, -scale, scale, 0).texture(0, 0);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
        RenderSystem.setShaderTexture(0, TEXTURE_ID);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
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
        matrix.rotate(-(float)rotation.y(), 0, 1, 0);
        matrix.rotate(-(float)rotation.x(), 1, 0, 0);
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
