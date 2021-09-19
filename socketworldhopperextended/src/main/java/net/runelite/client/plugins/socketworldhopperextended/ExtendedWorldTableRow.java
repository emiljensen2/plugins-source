package net.runelite.client.plugins.socketworldhopperextended;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldRegion;
import net.runelite.http.api.worlds.WorldType;

class ExtendedWorldTableRow extends JPanel
{
	private static final ImageIcon FLAG_AUS;

	private static final ImageIcon FLAG_UK;

	private static final ImageIcon FLAG_US;

	private static final ImageIcon FLAG_GER;

	private static final int WORLD_COLUMN_WIDTH = 60;

	private static final int PLAYERS_COLUMN_WIDTH = 40;

	private static final int PING_COLUMN_WIDTH = 35;

	private static final Color CURRENT_WORLD = new Color(66, 227, 17);

	private static final Color DANGEROUS_WORLD = new Color(251, 62, 62);

	private static final Color TOURNAMENT_WORLD = new Color(79, 145, 255);

	private static final Color MEMBERS_WORLD = new Color(210, 193, 53);

	private static final Color FREE_WORLD = new Color(200, 200, 200);

	private static final Color LEAGUE_WORLD = new Color(133, 177, 178);

	static
	{
		FLAG_AUS = new ImageIcon(ImageUtil.loadImageResource(WorldHopperExtendedPlugin.class, "flag_aus.png"));
		FLAG_UK = new ImageIcon(ImageUtil.loadImageResource(WorldHopperExtendedPlugin.class, "flag_uk.png"));
		FLAG_US = new ImageIcon(ImageUtil.loadImageResource(WorldHopperExtendedPlugin.class, "flag_us.png"));
		FLAG_GER = new ImageIcon(ImageUtil.loadImageResource(WorldHopperExtendedPlugin.class, "flag_ger.png"));
	}

	private final JMenuItem favoriteMenuOption = new JMenuItem();

	private JLabel worldField;

	private JLabel playerCountField;

	private JLabel activityField;

	private JLabel pingField;

	private final BiConsumer<World, Boolean> onFavorite;

	private final World world;

	private int updatedPlayerCount;

	private int ping;

	private Color lastBackground;

	public World getWorld()
	{
		return this.world;
	}

	int getUpdatedPlayerCount()
	{
		return this.updatedPlayerCount;
	}

	ExtendedWorldTableRow(final World world, boolean current, boolean favorite, Integer ping, final Consumer<World> onSelect, BiConsumer<World, Boolean> onFavorite)
	{
		this.world = world;
		this.onFavorite = onFavorite;
		this.updatedPlayerCount = world.getPlayers();
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(2, 0, 2, 0));
		addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent mouseEvent)
			{
				if (mouseEvent.getClickCount() == 2)
				{
					if (onSelect != null)
					{
						onSelect.accept(world);
					}
				}
			}

			public void mousePressed(MouseEvent mouseEvent)
			{
				if (mouseEvent.getClickCount() == 2)
				{
					ExtendedWorldTableRow.this.setBackground(ExtendedWorldTableRow.this.getBackground().brighter());
				}
			}

			public void mouseReleased(MouseEvent mouseEvent)
			{
				if (mouseEvent.getClickCount() == 2)
				{
					ExtendedWorldTableRow.this.setBackground(ExtendedWorldTableRow.this.getBackground().darker());
				}
			}

			public void mouseEntered(MouseEvent mouseEvent)
			{
				ExtendedWorldTableRow.this.lastBackground = ExtendedWorldTableRow.this.getBackground();
				ExtendedWorldTableRow.this.setBackground(ExtendedWorldTableRow.this.getBackground().brighter());
			}

			public void mouseExited(MouseEvent mouseEvent)
			{
				ExtendedWorldTableRow.this.setBackground(ExtendedWorldTableRow.this.lastBackground);
			}
		});
		setFavoriteMenu(favorite);
		JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.setBorder(new EmptyBorder(5, 5, 5, 5));
		popupMenu.add(this.favoriteMenuOption);
		setComponentPopupMenu(popupMenu);
		JPanel leftSide = new JPanel(new BorderLayout());
		JPanel rightSide = new JPanel(new BorderLayout());
		leftSide.setOpaque(false);
		rightSide.setOpaque(false);
		JPanel worldField = buildWorldField();
		worldField.setPreferredSize(new Dimension(60, 0));
		worldField.setOpaque(false);
		JPanel pingField = buildPingField(ping);
		pingField.setPreferredSize(new Dimension(35, 0));
		pingField.setOpaque(false);
		JPanel playersField = buildPlayersField();
		playersField.setPreferredSize(new Dimension(40, 0));
		playersField.setOpaque(false);
		JPanel activityField = buildActivityField();
		activityField.setBorder(new EmptyBorder(5, 5, 5, 5));
		activityField.setOpaque(false);
		recolour(current);
		leftSide.add(worldField, "West");
		leftSide.add(playersField, "Center");
		rightSide.add(activityField, "Center");
		rightSide.add(pingField, "East");
		add(leftSide, "West");
		add(rightSide, "Center");
	}

	void setFavoriteMenu(boolean favorite)
	{
		String favoriteAction = favorite ? ("Remove " + this.world.getId() + " from favorites") : ("Add " + this.world.getId() + " to favorites");
		this.favoriteMenuOption.setText(favoriteAction);
		for (ActionListener listener : this.favoriteMenuOption.getActionListeners())
		{
			this.favoriteMenuOption.removeActionListener(listener);
		}
		this.favoriteMenuOption.addActionListener(e -> this.onFavorite.accept(this.world, Boolean.valueOf(!favorite)));
	}

	void updatePlayerCount(int playerCount)
	{
		this.updatedPlayerCount = playerCount;
		this.playerCountField.setText(playerCountString(playerCount));
	}

	private static String playerCountString(int playerCount)
	{
		return (playerCount < 0) ? "OFF" : Integer.toString(playerCount);
	}

	void setPing(int ping)
	{
		this.ping = ping;
		this.pingField.setText((ping <= 0) ? "-" : Integer.toString(ping));
	}

	void hidePing()
	{
		this.pingField.setText("-");
	}

	void showPing()
	{
		setPing(this.ping);
	}

	int getPing()
	{
		return this.ping;
	}

	public void recolour(boolean current)
	{
		this.playerCountField.setForeground(current ? CURRENT_WORLD : Color.WHITE);
		this.pingField.setForeground(current ? CURRENT_WORLD : Color.WHITE);
		if (current)
		{
			this.activityField.setForeground(CURRENT_WORLD);
			this.worldField.setForeground(CURRENT_WORLD);
			return;
		}
		if (this.world.getTypes().contains(WorldType.PVP) || this.world
			.getTypes().contains(WorldType.HIGH_RISK) || this.world
			.getTypes().contains(WorldType.DEADMAN))
		{
			this.activityField.setForeground(DANGEROUS_WORLD);
		}
		else if (this.world.getTypes().contains(WorldType.SEASONAL))
		{
			this.activityField.setForeground(LEAGUE_WORLD);
		}
		else if (this.world.getTypes().contains(WorldType.TOURNAMENT))
		{
			this.activityField.setForeground(TOURNAMENT_WORLD);
		}
		else
		{
			this.activityField.setForeground(Color.WHITE);
		}
		this.worldField.setForeground(this.world.getTypes().contains(WorldType.MEMBERS) ? MEMBERS_WORLD : FREE_WORLD);
	}

	private JPanel buildPlayersField()
	{
		JPanel column = new JPanel(new BorderLayout());
		column.setBorder(new EmptyBorder(0, 5, 0, 5));
		this.playerCountField = new JLabel(playerCountString(this.world.getPlayers()));
		this.playerCountField.setFont(FontManager.getRunescapeSmallFont());
		column.add(this.playerCountField, "West");
		return column;
	}

	private JPanel buildPingField(Integer ping)
	{
		JPanel column = new JPanel(new BorderLayout());
		column.setBorder(new EmptyBorder(0, 5, 0, 5));
		this.pingField = new JLabel("-");
		this.pingField.setFont(FontManager.getRunescapeSmallFont());
		column.add(this.pingField, "East");
		if (ping != null)
		{
			setPing(ping.intValue());
		}
		return column;
	}

	private JPanel buildActivityField()
	{
		JPanel column = new JPanel(new BorderLayout());
		column.setBorder(new EmptyBorder(0, 5, 0, 5));
		this.activityField = new JLabel(this.world.getActivity());
		this.activityField.setFont(FontManager.getRunescapeSmallFont());
		column.add(this.activityField, "West");
		return column;
	}

	private JPanel buildWorldField()
	{
		JPanel column = new JPanel(new BorderLayout(7, 0));
		column.setBorder(new EmptyBorder(0, 5, 0, 5));
		this.worldField = new JLabel(this.world.getId() + "");
		ImageIcon flagIcon = getFlag(this.world.getRegion());
		if (flagIcon != null)
		{
			JLabel flag = new JLabel(flagIcon);
			column.add(flag, "West");
		}
		column.add(this.worldField, "Center");
		return column;
	}

	private static ImageIcon getFlag(WorldRegion region)
	{
		if (region == null)
		{
			return null;
		}
		switch (region)
		{
			case UNITED_STATES_OF_AMERICA:
				return FLAG_US;
			case UNITED_KINGDOM:
				return FLAG_UK;
			case AUSTRALIA:
				return FLAG_AUS;
			case GERMANY:
				return FLAG_GER;
		}
		return null;
	}
}
