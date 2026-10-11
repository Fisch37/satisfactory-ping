package de.fisch37.satisfactory_ping.client.rendering;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import de.fisch37.satisfactory_ping.SatisfactoryPing;
import de.fisch37.satisfactory_ping.client.LevelRendererAccessor;
import de.fisch37.satisfactory_ping.client.SatisfactoryPingRenderingHook;
import de.fisch37.satisfactory_ping.packets.BlockPingPayload;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static de.fisch37.satisfactory_ping.client.SatisfactoryPingRenderingHook.LOGGER;
import static de.fisch37.satisfactory_ping.client.rendering.Utilities.lookAtRotations;

public class InWorldRendering {
    public static final float MIN_SCALE = 0.25f;
    private static final RenderPipeline PING_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withColorTargetState(new ColorTargetState(
                            Optional.empty(),
                            GpuFormat.RGBA8_UNORM,
                            ColorTargetState.WRITE_ALL
                    ))
                    .withLocation(Identifier.fromNamespaceAndPath(SatisfactoryPing.MOD_ID, "pipeline/ping"))
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
            .build());
    private static final RenderPipeline TEXT_PIPELINE = RenderPipelines.TEXT;
    private static final StagedVertexBuffer STAGED_BUFFER = new StagedVertexBuffer(() -> "Waypoints Buffer", RenderType.SMALL_BUFFER_SIZE);
    private static final StagedVertexBuffer STAGED_BUFFER_TEXT = new StagedVertexBuffer(() -> "Waypoints Distance Buffer", RenderType.SMALL_BUFFER_SIZE);
    private static final Identifier TEXTURE_ID = Utilities.fromGuiTexture(SatisfactoryPingRenderingHook.TEXTURE_ID);
    private static final Identifier BORDER_TEXTURE = Utilities.fromGuiTexture(SatisfactoryPingRenderingHook.BORDER_TEXTURE);

    private final RenderingConfig config;
    private final Multimap<Identifier, ColTexPing> individualPings = MultimapBuilder
            .hashKeys()
            .arrayListValues()
            .build();
    private final List<PingRenderInfo> renderState = new ArrayList<>();
    private final List<TextRenderInfo> textState = new ArrayList<>();

    public InWorldRendering(RenderingConfig config) {
        this.config = config;
    }

    public void extraction(LevelRenderContext context, Stream<BlockPingPayload> pings) {
        renderState.clear();
        individualPings.clear();
        pings.map(ping -> processPing(context, ping))
                .forEach(info -> {
                    if (info instanceof ColTexPing coltex) {
                        individualPings.put(coltex.textureId, coltex);
                    } else {
                        renderState.add(info);
                    }
                    submitDistanceText(context, info);
                });
    }

    private void submitDistanceText(LevelRenderContext context, PingRenderInfo info) {
        // TODO: This implementation is hacky, does not work, and will most certainly fail to compile in future versions
        //  Find out how to do text rendering properly
        if (true) return;
        /*
        final var fontHeight = 2*distanceToCamera*config.getTextScalingFactor();
        textHelper(context, ping.pos().subtract(0, scale+fontHeight, 0), (long)distanceToCamera + "m", (float)fontHeight);

        final var textRenderer = context.gameRenderer().getMinecraft().font;
        textRenderer.drawInBatch(
                text,
                -textRenderer.width(text)/2f, 0,
                CommonColors.WHITE, false,
                matrix, context.consumers(),
                Font.DisplayMode.SEE_THROUGH,
                0, 255
        );
         */
        final var textRenderer = Minecraft.getInstance().font;
        String text = (long)info.distance + "m";

        final var matrices = context.poseStack();
        final var fontHeight = 2*info.distance*config.getTextScalingFactor();
        final var diff = info.position.subtract(0, info.scale+fontHeight, 0).subtract(getCameraPos(context));
        matrices.translate(diff);
        matrices.translate(0, fontHeight, 0);
        matrices.rotate(Axis.ZP, (float)Math.PI);  // For some reason text is rendered upside-down
        matrices.scale(info.scale, info.scale, info.scale);
        var rotation = lookAtRotations(diff.toVector3f());
        matrices.rotate(Axis.YP, -(float)rotation.y());
        matrices.rotate(Axis.XP, -(float)rotation.x());
        LevelRendererAccessor.getSubmitNodeStorage(Minecraft.getInstance().levelRenderer)
                .submitText(
                        matrices,
                        -textRenderer.width(text)/2f, 0,
                        FormattedCharSequence.forward(text, Style.EMPTY),
                        false,
                        Font.DisplayMode.NORMAL,
                        // TODO: What is "lightCoords"
                        0,
                        CommonColors.WHITE,
                        0,
                        0
                );
    }

    public void drawing(LevelRenderContext context) {
        VertexFormat formatBinding = PING_PIPELINE.getVertexFormatBinding(0);

        assert formatBinding != null;

        var primitive = PING_PIPELINE.getPrimitiveTopology();
        final var compiledPipeline = RenderSystem.getCompiledPipeline(PING_PIPELINE);
        final var causelessTexture = getOrLoadTexture(TEXTURE_ID);
        final var borderTexture = getOrLoadTexture(BORDER_TEXTURE);

        RenderTarget mainTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        GpuTextureView colorTexture = mainTarget.getColorTextureView();
        assert colorTexture != null;

        record DrawInstruction(StagedVertexBuffer.Draw draw, AbstractTexture texture) { }
        final List<DrawInstruction> draws = new ArrayList<>();
        final Function<AbstractTexture, StagedVertexBuffer.Draw> appendDraw = texture -> {
            var draw = STAGED_BUFFER.appendDraw(
                    formatBinding,
                    primitive,
                    primitive == PrimitiveTopology.QUADS ? RenderSystem.getProjectionType().vertexSorting() : null
            );
            draws.add(new DrawInstruction(draw, texture));
            return draw;
        };

        var matrices = context.poseStack();
        matrices.pushPose();
        Vec3 camPos = getCameraPos(context);
        matrices.translate(-(float) camPos.x, -(float) camPos.y, -(float) camPos.z);


        {
            final var draw = appendDraw.apply(causelessTexture);
            final var builder = STAGED_BUFFER.getVertexBuilder(draw);
            for (var task : renderState) {
                matrices.pushPose();
                transformForPing(matrices, task, camPos);
                addCenteredQuad(builder, matrices.last().pose(), task.scale);
                matrices.popPose();
            }
        }

        // Border
        {
            final var draw = appendDraw.apply(borderTexture);
            final var builder = STAGED_BUFFER.getVertexBuilder(draw);
            for (var task : individualPings.values()) {
                matrices.pushPose();
                transformForPing(matrices, task, camPos);
                addCenteredQuad(builder, matrices.last().pose(), task.scale(), task.color);
                matrices.popPose();
            }
        }
        // Head
        for (var textureId : individualPings.keySet()) {
            {
                StagedVertexBuffer.Draw draw = null;
                VertexConsumer builder = null;
                for (var task : Objects.requireNonNull(individualPings.get(textureId))) {
                    if (draw == null) {
                        draw = appendDraw.apply(task.texture);
                        builder = STAGED_BUFFER.getVertexBuilder(draw);
                    }

                    matrices.pushPose();
                    transformForPing(matrices, task, camPos);
                    renderHead(builder, matrices.last().pose(), task.scale());
                    matrices.popPose();
                }
            }
        }

        STAGED_BUFFER.upload();
        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> SatisfactoryPing.MOD_ID + " example render pipeline rendering",
                        colorTexture,
                        Optional.empty(),
                        mainTarget.getDepthTextureView(),
                        OptionalDouble.empty()
                )
        ) {
            final BiConsumer<AbstractTexture, StagedVertexBuffer.Draw> commitDrawPrepared = (texture, draw2) -> commitDraw(
                    renderPass,
                    draw2,
                    compiledPipeline,
                    texture
            );
            draws.forEach(instruction -> commitDrawPrepared.accept(instruction.texture, instruction.draw));
        }
        STAGED_BUFFER.endDraw();
        STAGED_BUFFER.endFrame();

        matrices.popPose();
    }

    private static void transformForPing(PoseStack matrices, PingRenderInfo task, Vec3 camPos) {
        var pos = task.position;
        var diff = camPos.subtract(pos);
        matrices.translate((float)pos.x, (float)pos.y, (float)pos.z);
        // This could have been a simple library call, but that didn't seem to work
        var rotations = lookAtRotations(diff.toVector3f());
        matrices.rotate(Axis.YP, (float)rotations.y());
        matrices.rotate(Axis.XP, (float) rotations.x());
    }

    @Deprecated
    public void renderIrisWorkaround(LevelRenderContext context, BlockPingPayload ping) {
        Vec3 diff = getCameraPos(context).subtract(ping.pos());
        float scale = 0.25f;
        final double distanceToCamera = diff.length();
        double apparentHeight = 2*Math.atan(scale/distanceToCamera);
        if (apparentHeight < config.getSmallestApparentHeight())
            scale = (float) (distanceToCamera*config.getScalingFactor());
        renderText(context, ping, scale, distanceToCamera);
    }

    private PingRenderInfo processPing(final LevelRenderContext context, BlockPingPayload ping) {
        Vec3 camPos = getCameraPos(context);
        var pos = ping.pos();
        var diff = camPos.subtract(pos);

        final double distanceToCamera = diff.length();
        double apparentHeight = 2*Math.atan(MIN_SCALE/distanceToCamera);
        float scale;
        if (apparentHeight < config.getSmallestApparentHeight()) {
            scale = (float) (distanceToCamera * config.getScalingFactor());
        } else {
            scale = MIN_SCALE;
        }

//        if (!config.isIrisWorkaroundEnabled()) renderText(context, causeless, scale, distanceToCamera);

        var creator = ping.cause().flatMap(InWorldRendering::getTextureData)
                .map(data -> (PingRenderInfo.Creator)(a,b,c) -> new ColTexPing(
                        a, b, c, data.getLeft(), data.getRight())
                ).orElse(PingRenderInfo::new);
        return creator.create(
                ping.pos(),
                distanceToCamera,
                scale
        );
    }

    private static @NotNull Optional<Pair<Identifier, Integer>> getTextureData(@Nullable UUID causeUuid) {
        if (causeUuid == null) {
            return Optional.empty();
        }
        final var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            LOGGER.warn("Tried to render causeless without a network connection");
            return Optional.empty();
        }
        final var playerEntry = connection.getPlayerInfo(causeUuid);
        if (playerEntry == null) {
            LOGGER.error("Cannot find player entry for {}, cannot get player skin", causeUuid);
            return Optional.empty();
        }
        int color = Utilities.getPingDataFromEntry(playerEntry).color();
        return Optional.of(Pair.of(
                playerEntry.getSkin().body().texturePath(),
                color
        ));
    }

    private TextRenderInfo prepareText(LevelRenderContext context, PingRenderInfo info) {
        final var fontHeight = 2*info.distance*config.getTextScalingFactor();
        return textHelper(
                context,
                info.position.subtract(0, info.scale+fontHeight, 0),
                (long)info.distance + "m",
                (float)fontHeight
        );
    }
    private void renderText(LevelRenderContext context, BlockPingPayload ping, double scale, double distanceToCamera) {
        final var fontHeight = 2*distanceToCamera*config.getTextScalingFactor();
        textHelper(context, ping.pos().subtract(0, scale+fontHeight, 0), (long)distanceToCamera + "m", (float)fontHeight);
    }

    private void commitDraw(
            RenderPass renderPass,
            StagedVertexBuffer.Draw draw,
            CompiledRenderPipeline pipeline,
            AbstractTexture texture
    ) {
        var info = STAGED_BUFFER.getExecuteInfo(draw);
        if (info == null) {
            // LOGGER.error("Failed to get execution info for draw");
            return;
        }
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(
                        RenderSystem.getModelViewMatrixCopy(),
                        new Vector4f(1f, 1f, 1f, 1f),
                        new Vector3f(),
                        new Matrix4f()
                );

        renderPass.setPipeline(pipeline);

        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.setUniform("DynamicTransforms", dynamicTransforms);

        // Bind texture if applicable:
        // Sampler0 is used for texture inputs in vertices
        renderPass.setUniform("Sampler0", texture.getTextureView(), texture.getSampler());

        renderPass.setVertexBuffer(0, info.vertexBuffer().slice());
        renderPass.setIndexBuffer(info.indexBuffer(), info.indexType());

        // The base vertex is the starting index when we copied the data into the vertex buffer divided by vertex size
        renderPass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
    }

    private void addCenteredQuad(
            VertexConsumer buffer,
            Matrix4f matrix,
            float scale
    ) {
        addCenteredQuad(buffer, matrix, scale, -1);
    }
    private void addCenteredQuad(
            VertexConsumer buffer,
            Matrix4f matrix,
            float scale,
            int color
    ) {
        buffer.addVertex(matrix, -scale, -scale, 0).setUv(0, 1).setColor(color);
        buffer.addVertex(matrix, scale, -scale, 0).setUv(1, 1).setColor(color);
        buffer.addVertex(matrix, scale, scale, 0).setUv(1, 0).setColor(color);
        buffer.addVertex(matrix, -scale, scale, 0).setUv(0, 0).setColor(color);
    }

    private void renderHead(
            VertexConsumer buffer,
            Matrix4f matrix,
            float scale
    ) {
        scale *= (12/16f);
        buffer.addVertex(matrix, -scale, -scale, 0).setUv(8/64f, 16/64f).setColor(CommonColors.WHITE);
        buffer.addVertex(matrix, scale, -scale, 0).setUv(16/64f, 16/64f).setColor(CommonColors.WHITE);
        buffer.addVertex(matrix, scale, scale, 0).setUv(16/64f, 8/64f).setColor(CommonColors.WHITE);
        buffer.addVertex(matrix, -scale, scale, 0).setUv(8/64f, 8/64f).setColor(CommonColors.WHITE);
    }

    private static TextRenderInfo textHelper(LevelRenderContext context, Vec3 bottomCentre, String text, float height) {
        // TODO
        final var textRenderer = Minecraft.getInstance().font;
        final var scale = height/textRenderer.lineHeight;
//        var matrix = new Matrix4f();
//        final var diff = bottomCentre.subtract(getCameraPos(context)).toVector3f();
//        matrix.translate(diff);
//        matrix.translate(0, height, 0);
//        matrix.rotate((float)Math.PI, 0, 0, 1);  // For some reason text is rendered upside-down
//        matrix.scale(scale);
//        var rotation = lookAtRotations(diff);
//        matrix.rotate(-(float)rotation.y(), 0, 1, 0);
//        matrix.rotate(-(float)rotation.x(), 1, 0, 0);
        // matrix.scale(scale);
        return new TextRenderInfo(
                textRenderer.prepareText(
                        text,
                        -textRenderer.width(text)/2f, 0,
                        CommonColors.WHITE, false,
                        0
                ),
                bottomCentre
        );
    }

    @Deprecated
    private static void textHelper(
            LevelRenderContext context,
            VertexConsumer buffer,
            Matrix4f matrix,
            String text
    ) {
        // TODO
        final var textRenderer = Minecraft.getInstance().font;
        textRenderer.prepareText(
                text,
                -textRenderer.width(text)/2f, 0,
                CommonColors.WHITE, false,
                0
        );
    }

    private static AbstractTexture getOrLoadTexture(Identifier textureId) {
        return Minecraft.getInstance().getTextureManager().getTexture(textureId);
    }

    private static Vec3 getCameraPos(LevelRenderContext context) {
        return context.gameRenderer().mainCamera().position();
    }

    /**
     *
     */
    @NullMarked
    private static class PingRenderInfo {
        protected final Vec3 position;
        protected final double distance;
        protected final float scale;

        /**
         * @param position In-world position of the causeless
         * @param distance Distance to the camera (in meters)
         * @param scale    Apparent height of the waypoint in degrees
         */
        private PingRenderInfo(
                Vec3 position,
                double distance,
                float scale
        ) {
            this.position = position;
            this.distance = distance;
            this.scale = scale;
        }

        public final Vec3 position() {
            return position;
        }

        public final double distance() {
            return distance;
        }

        public final float scale() {
            return scale;
        }

        @Override
        public String toString() {
            return "PingRenderInfo[" +
                    "position=" + position + ", " +
                    "distance=" + distance + ", " +
                    "scale=" + scale + "]";
        }

        @FunctionalInterface
        public interface Creator {
            PingRenderInfo create(Vec3 position, double distance, float scale);
        }
    }

    private static class ColTexPing extends PingRenderInfo {
        protected final Identifier textureId;
        protected final AbstractTexture texture;
        protected final int color;

        /**
         * @param position In-world position of the causeless
         * @param distance Distance to the camera (in meters)
         * @param scale    Apparent height of the waypoint in degrees
         */
        private ColTexPing(
                Vec3 position,
                double distance,
                float scale,
                Identifier texture,
                int color
        ) {
            super(position, distance, scale);
            this.textureId = texture;
            this.texture = getOrLoadTexture(texture);
            this.color = color;
        }

        public int color() {
            return color;
        }
    }

    private record TextRenderInfo(Font.PreparedText text, Vec3 bottomCenterPos) { }
}
