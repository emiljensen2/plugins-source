package net.runelite.client.plugins.socket.plugins.sockethealing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

public class SocketHealingOverlay extends OverlayPanel
{
	private final Client client;

	private final SocketHealingPlugin plugin;

	private final SocketHealingConfig config;

	private ModelOutlineRenderer modelOutlineRenderer;

	@Inject
	private SocketHealingOverlay(Client client, SocketHealingPlugin plugin, SocketHealingConfig config, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.modelOutlineRenderer = modelOutlineRenderer;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.HIGH);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	public Dimension render(Graphics2D graphics)
	{
		if (config.healingFontType() == SocketHealingConfig.SocketFontType.CUSTOM)
		{
			graphics.setFont(new Font(FontManager.getRunescapeFont().toString(), 1, this.config.fontSize()));
		}
		else if (config.healingFontType() == SocketHealingConfig.SocketFontType.REGULAR)
		{
			graphics.setFont(FontManager.getRunescapeFont());
		}
		else if (config.healingFontType() == SocketHealingConfig.SocketFontType.BOLD)
		{
			graphics.setFont(FontManager.getRunescapeBoldFont());
		}
		else if (config.healingFontType() == SocketHealingConfig.SocketFontType.SMALL)
		{
			graphics.setFont(FontManager.getRunescapeSmallFont());
		}
		Map<String, Point> locations = new HashMap<>();
		Iterator<Player> clientPlayers = this.client.getPlayers().iterator();
		while (clientPlayers.hasNext())
		{
			Player player = clientPlayers.next();
			locations.put(player.getName(), player.getCanvasTextLocation(graphics, "", player.getLogicalHeight() + this.config.overlayOffset() * 10));
		}
		for (String playerName : this.plugin.getPartyMembers().keySet())
		{
			for (String locationName : locations.keySet())
			{
				if (playerName.equalsIgnoreCase(locationName))
				{
					SocketHealingPlayer player = this.plugin.getPartyMembers().get(playerName);
					int health = player.getHealth();
					Color highlightColor = Color.WHITE;
					Color textColor = Color.WHITE;
					if (health > this.config.greenZone())
					{
						highlightColor = new Color(this.config.greenZoneColor().getRed(), this.config.greenZoneColor().getGreen(), this.config.greenZoneColor().getBlue(), config.opacity());
					}
					textColor = config.greenZoneColor();
					if (health <= this.config.greenZone() && health > this.config.orangeZone())
					{
						highlightColor = new Color(this.config.orangeZoneColor().getRed(), this.config.orangeZoneColor().getGreen(), this.config.orangeZoneColor().getBlue(), config.opacity());
						textColor = config.orangeZoneColor();
					}
					else if (health <= this.config.orangeZone())
					{
						highlightColor = new Color(this.config.redZoneColor().getRed(), this.config.redZoneColor().getGreen(), this.config.redZoneColor().getBlue(), config.opacity());
						textColor = config.redZoneColor();
					}
					if (this.config.displayHealth())
					{
						Point point = locations.get(locationName);
						int xOffset = this.config.getIndicatorXOffset();
						int yOffset = this.config.getIndicatorYOffset();
						Point point2 = new Point(point.getX() + xOffset, point.getY() - yOffset);
						OverlayUtil.renderTextLocation(graphics, point2, String.valueOf(health), textColor);
					}
					else if (!this.config.hpPlayerNames().equals(""))
					{
						if (this.plugin.playerNames.contains(playerName.toLowerCase()))
						{
							Point point = locations.get(locationName);
							int xOffset = this.config.getIndicatorXOffset();
							int yOffset = this.config.getIndicatorYOffset();
							Point point2 = new Point(point.getX() + xOffset, point.getY() - yOffset);
							OverlayUtil.renderTextLocation(graphics, point2, String.valueOf(health), textColor);
						}
					}
					if (config.highlightedPlayerNames().toLowerCase().contains(playerName.toLowerCase()))
					{
						List<Player> playerList = this.client.getPlayers();
						for (Player playerIndex : playerList)
						{
							if (playerName.toLowerCase().equals(playerIndex.getName().toLowerCase()))
							{
								if (config.highlightOutline())
								{
									this.modelOutlineRenderer.drawOutline(playerIndex, config.hpThiCC(), highlightColor, config.glow());
								}
								else if (config.highlightHull())
								{
									Shape poly = playerIndex.getConvexHull();
									if (poly != null)
									{
										graphics.setColor(new Color(highlightColor.getRed(), highlightColor.getGreen(), highlightColor.getBlue(), config.opacity()));
										graphics.setStroke(new BasicStroke(this.config.hpThiCC()));
										graphics.draw(poly);
										graphics.setColor(new Color(highlightColor.getRed(), highlightColor.getGreen(), highlightColor.getBlue(), 0));
										graphics.fill(poly);
									}
								}
							}
						}
					}
				}
			}
		}
		return null;
	}
}
