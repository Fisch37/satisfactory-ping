package de.fisch37.satisfactory_ping.client.rendering;

import de.fisch37.satisfactory_ping.client.InGameHudAccessor;
import de.fisch37.satisfactory_ping.client.SatisfactoryPingRenderingHook;
import de.fisch37.satisfactory_ping.packets.BlockPingPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;

import static de.fisch37.satisfactory_ping.SatisfactoryPing.LOGGER;

public class HudRendering {
    private static final int BAR_WIDTH = 182, PING_WIDTH = 8, PING_HEIGHT = 8;
    private static final double MAX_YAW = 60;

    public static void renderPingOnBar(GuiGraphics context, BlockPingPayload ping, DeltaTracker tickCounter) {
        var client = Minecraft.getInstance();
        if (client.player == null
                || !client.player.level().dimension().equals(ping.dimension()))
            return;
        var barType = InGameHudAccessor.getCurrentBarType(client.gui);
        if (barType != Gui.ContextualInfo.LOCATOR && barType != Gui.ContextualInfo.EXPERIENCE)
            return;

        var bar = InGameHudAccessor.getCurrentBar(client.gui);
        var centerX = bar.left(client.getWindow()) + BAR_WIDTH / 2;
        var centerY = bar.top(client.getWindow());

        // Magic math
        var relativeYaw = Mth.degreesDifference(
                client.gameRenderer.getMainCamera().yaw(),
                (float)Utilities.getYawOfCoord(client.player.getEyePosition().subtract(ping.pos()).rotateClockwise90())
        );
        var fractionalOffset = Mth.clamp(relativeYaw / MAX_YAW, -1, 1);

        int x = centerX + (int)(fractionalOffset * BAR_WIDTH / 2);
        ping.cause().ifPresentOrElse(
                uuid -> {
                    var networkHandler = client.getConnection();
                    PlayerInfo playerListEntry;
                    if (networkHandler != null && (playerListEntry = networkHandler.getPlayerInfo(uuid)) != null) {
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

    private static void drawPlayerlessPing(GuiGraphics context, int x, int y) {
        context.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                SatisfactoryPingRenderingHook.TEXTURE_ID,
                x - PING_WIDTH / 2, y - 2,
                PING_WIDTH, PING_HEIGHT
        );
    }

    private static void drawPing(GuiGraphics context, int x, int y, Utilities.PingRenderData data) {
        context.blit(
                RenderPipelines.GUI_TEXTURED,
                data.texture(),
                x - 8 / 2, y - 2,
                8, 8,
                8, 8,
                64, 64
        );
        context.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                SatisfactoryPingRenderingHook.BORDER_TEXTURE,
                x - 10 / 2, y - 3,
                10, 10,
                data.color()
        );
    }
}
