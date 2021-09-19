package net.runelite.client.plugins.socketplayerindicatorsextended;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Objects;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.socket.SocketPlugin;
import net.runelite.client.plugins.socket.packet.SocketPlayerJoin;
import net.runelite.client.plugins.socket.packet.SocketMembersUpdate;
import net.runelite.client.plugins.socket.packet.SocketShutdown;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.pf4j.Extension;

@Extension
@PluginDescriptor(
	name = "Socket - Player Indicator",
	description = "Shows you players who are in your socket",
	tags = {"indicator, socket, player, highlight"},
	enabledByDefault = false
)
@PluginDependency(SocketPlugin.class)
public class PlayerIndicatorsExtendedPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(PlayerIndicatorsExtendedPlugin.class);

	@Inject
	private PlayerIndicatorsExtendedConfig config;

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private SocketPlugin socketPlugin;

	@Inject
	private PlayerIndicatorsExtendedOverlay overlay;

	@Inject
	private PlayerIndicatorsExtendedMinimapOverlay overlayMinimap;

	@Inject
	private ChatIconManager chatIconManager;

	private ArrayList<Actor> players;

	private ArrayList<String> names;

	@Provides
	PlayerIndicatorsExtendedConfig getConfig(ConfigManager configManager)
	{
		return (PlayerIndicatorsExtendedConfig) configManager.getConfig(PlayerIndicatorsExtendedConfig.class);
	}

	public ArrayList<Actor> getPlayers()
	{
		return this.players;
	}

	int activeTick = 0;

	boolean cleared = false;

	protected void startUp()
	{
		this.overlayManager.add(this.overlay);
		this.overlayManager.add(this.overlayMinimap);
		this.players = new ArrayList<>();
		this.names = new ArrayList<>();
	}

	protected void shutDown()
	{
		this.overlayManager.remove(this.overlay);
		this.overlayManager.remove(this.overlayMinimap);
	}

	@Subscribe
	public void onSocketPlayerJoin(SocketPlayerJoin event)
	{
		this.names.add(event.getPlayerName());
		if (event.getPlayerName().equals(Objects.requireNonNull(this.client.getLocalPlayer()).getName()))
		{
			this.names.clear();
		}
	}

	@Subscribe
	public void onSocketMembersUpdate(SocketMembersUpdate event)
	{
		this.names.clear();
		for (String s : event.getMembers())
		{
			if (!s.equals(client.getLocalPlayer().getName()))
			{
				names.add(s);
			}
		}
	}

	@Subscribe
	public void onSocketShutdown(SocketShutdown event)
	{
		names.clear();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		players.clear();
		loop:
		for (Player p : client.getPlayers())
		{
			for (String name : names)
			{
				if (name.equals(p.getName()))
				{
					players.add(p);
					continue loop;
				}
			}
		}
	}
}
