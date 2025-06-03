package de.fisch37.satisfactory_ping.client.rendering;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import de.fisch37.satisfactory_ping.SatisfactoryPing;
import de.fisch37.satisfactory_ping.packets.BlockPingPayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.UUID;

import static de.fisch37.satisfactory_ping.client.SatisfactoryPingRenderingHook.BORDER_TEXTURE;
import static de.fisch37.satisfactory_ping.client.rendering.Utilities.lookAtRotations;
import static de.fisch37.satisfactory_ping.client.SatisfactoryPingRenderingHook.LOGGER;

public class InWorldRendering {
    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
                    .withLocation(Identifier.of(SatisfactoryPing.MOD_ID, "pipeline/ping"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build());
    private static RenderLayer getRenderLayer(Identifier texture) {
        // why is size this big? no clue! I copied most of this from RenderLayers.GUI_TEXTURED
        return RenderLayer.of("ping", 786432, PIPELINE,
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(new RenderPhase.Texture(texture, TriState.FALSE, false))
                        .build(false));
    }

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

        float scale = 0.25f;
        final double distanceToCamera = diff.length();
        double apparentHeight = 2*Math.atan(scale/distanceToCamera);
        if (apparentHeight < config.getSmallestApparentHeight())
            scale = (float) (distanceToCamera*config.getScalingFactor());

        renderHeadWithBorder(context, ping.cause(), matrix, scale);

        if (!config.isIrisWorkaroundEnabled()) renderText(context, ping, scale, distanceToCamera);
    }

    private void renderText(WorldRenderContext context, BlockPingPayload ping, double scale, double distanceToCamera) {
        final var fontHeight = 2*distanceToCamera*config.getTextScalingFactor();
        textHelper(context, ping.pos().subtract(0, scale+fontHeight, 0), (long)distanceToCamera + "m", (float)fontHeight);
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

        var consumers = context.consumers();
        assert consumers != null;
        var vertexConsumer = consumers.getBuffer(getRenderLayer(BORDER_TEXTURE));
        vertexConsumer.vertex(matrix, -scale, -scale, 0).texture(0, 1).color(color);
        vertexConsumer.vertex(matrix, scale, -scale, 0).texture(1, 1).color(color);
        vertexConsumer.vertex(matrix, scale, scale, 0).texture(1, 0).color(color);
        vertexConsumer.vertex(matrix, -scale, scale, 0).texture(0, 0).color(color);

        renderHead(context, playerEntry, matrix, scale*(12/16f));
    }

    private void renderHead(WorldRenderContext context, PlayerListEntry player, Matrix4f matrix, float scale) {
        var texture = player.getSkinTextures().texture();
        var consumers = context.consumers();
        assert consumers != null;
        var vertexConsumer = consumers.getBuffer(getRenderLayer(texture));

        vertexConsumer.vertex(matrix, -scale, -scale, 0).texture(8/64f, 16/64f).color(-1);
        vertexConsumer.vertex(matrix, scale, -scale, 0).texture(16/64f, 16/64f).color(-1);
        vertexConsumer.vertex(matrix, scale, scale, 0).texture(16/64f, 8/64f).color(-1);
        vertexConsumer.vertex(matrix, -scale, scale, 0).texture(8/64f, 8/64f).color(-1);
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
