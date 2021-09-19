
package net.runelite.client.plugins.socket.plugins.worldhopperextended;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

class WorldHopperExtendedPingOverlay extends Overlay {
    private static final int Y_OFFSET = 11;

    private static final int X_OFFSET = 1;

    private final Client client;

    private final WorldHopperExtendedPlugin worldHopperPlugin;

    private final WorldHopperExtendedConfig worldHopperConfig;

    @Inject
    private WorldHopperExtendedPingOverlay(Client client, WorldHopperExtendedPlugin worldHopperPlugin, WorldHopperExtendedConfig worldHopperConfig) {
        this.client = client;
        this.worldHopperPlugin = worldHopperPlugin;
        this.worldHopperConfig = worldHopperConfig;
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
        setPosition(OverlayPosition.DYNAMIC);
    }

    public Dimension render(Graphics2D graphics) {
        if (!this.worldHopperConfig.displayCurrentPing())
            return null;
        int ping = this.worldHopperPlugin.getCurrentPing();
        if (ping < 0)
            return null;
        String text = ping + " ms";
        int textWidth = graphics.getFontMetrics().stringWidth(text);
        int textHeight = graphics.getFontMetrics().getAscent() - graphics.getFontMetrics().getDescent();
        Widget logoutButton = this.client.getWidget(WidgetInfo.RESIZABLE_MINIMAP_LOGOUT_BUTTON);
        int xOffset = 1;
        if (logoutButton != null && !logoutButton.isHidden())
            xOffset += logoutButton.getWidth();
        int width = (int)this.client.getRealDimensions().getWidth();
        Point point = new Point(width - textWidth - xOffset, textHeight + 11);
        OverlayUtil.renderTextLocation(graphics, point, text, Color.YELLOW);
        return null;
    }
}
