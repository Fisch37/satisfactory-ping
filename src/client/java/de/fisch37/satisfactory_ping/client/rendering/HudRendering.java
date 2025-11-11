package de.fisch37.satisfactory_ping.client.rendering;

import de.fisch37.satisfactory_ping.client.InGameHudAccessor;
import de.fisch37.satisfactory_ping.client.SatisfactoryPingRenderingHook;
import de.fisch37.satisfactory_ping.packets.BlockPingPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import static de.fisch37.satisfactory_ping.SatisfactoryPing.LOGGER;
import static de.fisch37.satisfactory_ping.SatisfactoryPing.MOD_ID;

public class HudRendering {
    private static final int BAR_WIDTH = 182, PING_WIDTH = 8, PING_HEIGHT = 8;
    private static final double MAX_YAW = 60;

    public static void renderPingOnBar(DrawContext context, BlockPingPayload ping, RenderTickCounter tickCounter) {
        var client = MinecraftClient.getInstance();
        if (client.player == null
                || !client.player.getWorld().getRegistryKey().equals(ping.dimension()))
            return;
        var barType = InGameHudAccessor.getCurrentBarType(client.inGameHud);
        if (barType != InGameHud.BarType.LOCATOR && barType != InGameHud.BarType.EXPERIENCE)
            return;

        var bar = InGameHudAccessor.getCurrentBar(client.inGameHud);
        var centerX = bar.getCenterX(client.getWindow()) + BAR_WIDTH / 2;
        var centerY = bar.getCenterY(client.getWindow());

        // Magic math
        var relativeYaw = Utilities.getYawOfCoord(client.player.getEyePos().subtract(ping.pos()).rotateYClockwise())
                - client.gameRenderer.getCamera().getCameraYaw();
        var fractionalOffset = MathHelper.clamp(relativeYaw / MAX_YAW, -1, 1);

        int x = centerX + (int)(fractionalOffset * BAR_WIDTH / 2);
        ping.cause().ifPresentOrElse(
                uuid -> {
                    var networkHandler = client.getNetworkHandler();
                    PlayerListEntry playerListEntry;
                    if (networkHandler != null && (playerListEntry = networkHandler.getPlayerListEntry(uuid)) != null) {
                        drawPing(context, x, centerY, Utilities.getPingDataFromEntry(playerListEntry));
                    } else {
                        if (networkHandler == null)
                            LOGGER.warn("Tried to render ping without network handler");
                        else
                            LOGGER.warn("Could not get player list entry for player {}", uuid);
                        drawPlayerlessPing(context, x, centerY);
                    }
                },
                () -> drawPlayerlessPing(context, x, centerY)
        );
    }

    private static void drawPlayerlessPing(DrawContext context, int x, int y) {
        context.drawGuiTexture(
                RenderPipelines.GUI_TEXTURED,
                SatisfactoryPingRenderingHook.TEXTURE_ID,
                x - PING_WIDTH / 2, y - 2,
                PING_WIDTH, PING_HEIGHT
        );
    }

    private static void drawPing(DrawContext context, int x, int y, Utilities.PingRenderData data) {
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                data.texture(),
                x - 8 / 2, y - 2,
                8, 8,
                8, 8,
                64, 64
        );
        context.drawGuiTexture(
                RenderPipelines.GUI_TEXTURED,
                SatisfactoryPingRenderingHook.BORDER_TEXTURE,
                x - 10 / 2, y - 3,
                10, 10,
                data.color()
        );
    }
}
