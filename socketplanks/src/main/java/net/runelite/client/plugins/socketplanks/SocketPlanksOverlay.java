package net.runelite.client.plugins.socketplanks;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

public class SocketPlanksOverlay extends OverlayPanel
{
	private final Client client;

	private final SocketPlanksPlugin plugin;

	private final SocketPlanksConfig config;

	@Inject
	private SocketPlanksOverlay(Client client, SocketPlanksPlugin plugin, SocketPlanksConfig config)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.LOW);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	public Dimension render(Graphics2D graphics)
	{
		if (plugin.planksDropped && !plugin.planksPickedUp)
		{
			if (plugin.planksDroppedTile != null)
			{
				LocalPoint point = LocalPoint.fromWorld(client, plugin.planksDroppedTile);
				renderSpot(graphics, client, point, Color.WHITE);
			}
		}
		return super.render(graphics);
	}

	private void renderSpot(Graphics2D graphics, Client client, LocalPoint point, Color color)
	{
		Polygon poly = Perspective.getCanvasTilePoly(client, point);
		if (poly != null)
		{
			OverlayUtil.renderPolygon(graphics, poly, color);
		}

		Point pt = Perspective.getCanvasTextLocation(client, graphics, point, plugin.nameGotPlanks, 0);
		graphics.setFont(FontManager.getRunescapeBoldFont());
		OverlayUtil.renderTextLocation(graphics, pt, plugin.nameGotPlanks, Color.WHITE);
	}
}