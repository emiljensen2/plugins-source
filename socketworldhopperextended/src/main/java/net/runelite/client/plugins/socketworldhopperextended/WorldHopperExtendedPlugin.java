package net.runelite.client.plugins.socketworldhopperextended;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ObjectArrays;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.ChatPlayer;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.FriendsChatMember;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NameableContainer;
import net.runelite.api.Player;
import net.runelite.api.Varbits;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WorldListLoad;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.WorldService;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.socket.SocketPlugin;
import net.runelite.client.plugins.socket.org.json.JSONArray;
import net.runelite.client.plugins.socket.org.json.JSONObject;
import net.runelite.client.plugins.socket.packet.SocketBroadcastPacket;
import net.runelite.client.plugins.socket.packet.SocketReceivePacket;
import net.runelite.client.plugins.worldhopperextended.ping.Ping;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ExecutorServiceExceptionLogger;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.Text;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.pf4j.Extension;

@Extension
@PluginDescriptor(
	name = "Socket - World Hopper",
	description = "Allows you to quickly hop worlds",
	enabledByDefault = false
)
@PluginDependency(SocketPlugin.class)
public class WorldHopperExtendedPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(WorldHopperExtendedPlugin.class);

	private static final int WORLD_FETCH_TIMER = 10;

	private static final int REFRESH_THROTTLE = 60000;

	private static final int TICK_THROTTLE = (int) Duration.ofMinutes(10L).toMillis();

	private static final int DISPLAY_SWITCHER_MAX_ATTEMPTS = 3;

	private static final String HOP_TO = "Hop-to";

	private static final String KICK_OPTION = "Kick";

	private static final ImmutableList<String> BEFORE_OPTIONS = ImmutableList.of("Add friend", "Remove friend", "Kick");

	private static final ImmutableList<String> AFTER_OPTIONS = ImmutableList.of("Message");

	public static boolean allowHopping = true;

	@Inject
	private EventBus eventBus;

	@Inject
	private Client client;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private SocketPlugin socketPlugin;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private WorldService worldService;

	@Inject
	private ScheduledExecutorService executorService;

	@Inject
	private WorldHopperExtendedConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WorldHopperExtendedPingOverlay worldHopperOverlay;

	private ScheduledExecutorService hopperExecutorService;

	private ScheduledExecutorService hopBlocked;

	private NavigationButton navButton;

	private ExtendedWorldSwitcherPanel panel;

	private net.runelite.api.World quickHopTargetWorld;

	private int displaySwitcherAttempts = 0;

	private int lastWorld;

	private int favoriteWorld1;

	private int favoriteWorld2;

	private ScheduledFuture<?> worldResultFuture;

	private ScheduledFuture<?> pingFuture;

	private ScheduledFuture<?> currPingFuture;

	private WorldResult worldResult;

	private int currentWorld;

	private Instant lastFetch;

	private boolean firstRun;

	private String customWorlds;

	public int getLastWorld()
	{
		return this.lastWorld;
	}

	private int logOutNotifTick = -1;

	private long hopDelay = 0L;

	private long hopDelayMS = 0L;

	private boolean allowedToHop = true;

	private int currentPing;

	int getCurrentPing()
	{
		return this.currentPing;
	}

	private final Map<Integer, Integer> storedPings = new HashMap<>();

	private final HotkeyListener previousKeyListener = new HotkeyListener(() -> this.config.previousKey())
	{
		public void hotkeyPressed()
		{
			WorldHopperExtendedPlugin.this.hop(true);
		}
	};

	private final HotkeyListener nextKeyListener = new HotkeyListener(() -> this.config.nextKey())
	{
		public void hotkeyPressed()
		{
			WorldHopperExtendedPlugin.this.hop(false);
		}
	};

	@Provides
	WorldHopperExtendedConfig getConfig(ConfigManager configManager)
	{
		return (WorldHopperExtendedConfig) configManager.getConfig(WorldHopperExtendedConfig.class);
	}

	protected void startUp() throws Exception
	{
		BufferedImage icon;
		this.allowedToHop = true;
		this.hopDelay = 0L;
		this.firstRun = true;
		this.currentPing = -1;
		this.customWorlds = this.config.customWorldCycle();
		this.keyManager.registerKeyListener((KeyListener) this.previousKeyListener);
		this.keyManager.registerKeyListener((KeyListener) this.nextKeyListener);
		this.panel = new ExtendedWorldSwitcherPanel(this);
		synchronized (ImageIO.class)
		{
			icon = ImageIO.read(WorldHopperExtendedPlugin.class.getResourceAsStream("icon.png"));
		}
		this

			.navButton = NavigationButton.builder().tooltip("Socket - World Switcher").icon(icon).priority(3).panel(this.panel).build();
		if (this.config.showWorldHopperSidebar())
		{
			this.clientToolbar.addNavigation(this.navButton);
		}
		this.overlayManager.add(this.worldHopperOverlay);
		this.panel.setFilterMode(this.config.subscriptionFilter());
		this.panel.setRegionFilterMode(config.regionFilter());
		this.hopperExecutorService = (ScheduledExecutorService) new ExecutorServiceExceptionLogger(Executors.newSingleThreadScheduledExecutor());
		this.hopBlocked = (ScheduledExecutorService) new ExecutorServiceExceptionLogger(Executors.newSingleThreadScheduledExecutor());
		this.worldResultFuture = this.executorService.scheduleAtFixedRate(this::tick, 0L, 10L, TimeUnit.MINUTES);
		this.pingFuture = this.hopperExecutorService.scheduleWithFixedDelay(this::pingNextWorld, 15L, 3L, TimeUnit.SECONDS);
		this.currPingFuture = this.hopperExecutorService.scheduleWithFixedDelay(this::pingCurrentWorld, 15L, 1L, TimeUnit.SECONDS);
	}

	protected void shutDown() throws Exception
	{
		this.allowedToHop = true;
		this.hopDelay = 0L;
		this.pingFuture.cancel(true);
		this.pingFuture = null;
		this.currPingFuture.cancel(true);
		this.currPingFuture = null;
		this.overlayManager.remove(this.worldHopperOverlay);
		this.keyManager.unregisterKeyListener((KeyListener) this.previousKeyListener);
		this.keyManager.unregisterKeyListener((KeyListener) this.nextKeyListener);
		this.worldResultFuture.cancel(true);
		this.worldResultFuture = null;
		this.worldResult = null;
		this.lastFetch = null;
		this.clientToolbar.removeNavigation(this.navButton);
		this.hopperExecutorService.shutdown();
		this.hopperExecutorService = null;
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		long x = System.currentTimeMillis() - this.hopDelay;
		if (x > 11000L)
		{
			this.allowedToHop = true;
		}
		else
		{
			this.allowedToHop = false;
		}
		this.hopDelayMS = 11000L - x;
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		String name = event.getActor().getName();
		if (name != null &&
			event.getActor().getAnimation() != 829 && name.equals(((Player) Objects.<Player>requireNonNull(this.client.getLocalPlayer())).getName()))
		{
			this.hopDelay = System.currentTimeMillis();
			this.hopDelayMS = 11000L;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("socketworldhopper"))
		{
			String s;
			JSONArray data;
			JSONObject jsonwp;
			JSONObject payload;
			switch (event.getKey())
			{
				case "showSidebar":
					if (this.config.showWorldHopperSidebar())
					{
						this.clientToolbar.addNavigation(this.navButton);
						break;
					}
					this.clientToolbar.removeNavigation(this.navButton);
					break;
				case "ping":
					if (this.config.worldPing())
					{
						SwingUtilities.invokeLater(() -> this.panel.showPing());
						break;
					}
					SwingUtilities.invokeLater(() -> this.panel.hidePing());
					break;
				case "subscriptionFilter":
					this.panel.setFilterMode(this.config.subscriptionFilter());
					updateList();
					break;
				case "regionFilter":
					panel.setRegionFilterMode(config.regionFilter());
					updateList();
					break;
				case "customWorldCycle":
					this.customWorlds = this.config.customWorldCycle();
					s = this.config.customWorldCycle();
					String chatMessage = (new ChatMessageBuilder()).append(Color.decode("0xB4281E"), "Custom world list: " + s).build();
					this.chatMessageManager.queue(QueuedMessage.builder()
						.type(ChatMessageType.CONSOLE)
						.runeLiteFormattedMessage(chatMessage)
						.build());
					data = new JSONArray();
					jsonwp = new JSONObject();
					jsonwp.put("worlds", s);
					data.put(jsonwp);
					payload = new JSONObject();
					payload.put("worldhopper-extended", data);
					this.eventBus.post(new SocketBroadcastPacket(payload));
					break;
			}
		}
	}

	@Subscribe
	public void onSocketReceivePacket(SocketReceivePacket event)
	{
		try
		{
			JSONObject payload = event.getPayload();
			if (!payload.has("worldhopper-extended"))
			{
				return;
			}
			JSONArray data = payload.getJSONArray("worldhopper-extended");
			JSONObject jsonwp = data.getJSONObject(0);
			String worlds = jsonwp.getString("worlds");
			this.customWorlds = worlds;
			config.setCustomWorldCycle(worlds);
			System.out.println(worlds);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void setFavoriteConfig(int world)
	{
		this.configManager.setConfiguration("socketworldhopper", "favorite_" + world, Boolean.TRUE);
	}

	private boolean isFavoriteConfig(int world)
	{
		Boolean favorite = (Boolean) this.configManager.getConfiguration("socketworldhopper", "favorite_" + world, Boolean.class);
		return (favorite != null && favorite);
	}

	private void clearFavoriteConfig(int world)
	{
		this.configManager.unsetConfiguration("socketworldhopper", "favorite_" + world);
	}

	boolean isFavorite(World world)
	{
		int id = world.getId();
		return (id == this.favoriteWorld1 || id == this.favoriteWorld2 || isFavoriteConfig(id));
	}

	int getCurrentWorld()
	{
		return this.client.getWorld();
	}

	void hopTo(World world)
	{
		hop(world.getId());
	}

	void addToFavorites(World world)
	{
		log.debug("Adding world {} to favorites", world.getId());
		setFavoriteConfig(world.getId());
		this.panel.updateFavoriteMenu(world.getId(), true);
	}

	void removeFromFavorites(World world)
	{
		log.debug("Removing world {} from favorites", world.getId());
		clearFavoriteConfig(world.getId());
		this.panel.updateFavoriteMenu(world.getId(), false);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged varbitChanged)
	{
		int old1 = this.favoriteWorld1;
		int old2 = this.favoriteWorld2;
		this.favoriteWorld1 = this.client.getVar(Varbits.WORLDHOPPER_FAVROITE_1);
		this.favoriteWorld2 = this.client.getVar(Varbits.WORLDHOPPER_FAVROITE_2);
		if (old1 != this.favoriteWorld1 || old2 != this.favoriteWorld2)
		{
			Objects.requireNonNull(this.panel);
			SwingUtilities.invokeLater(this.panel::updateList);
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!this.config.menuOption())
		{
			return;
		}
		final int componentId = event.getActionParam1();
		int groupId = WidgetInfo.TO_GROUP(componentId);
		String option = event.getOption();

		if (groupId == WidgetInfo.FRIENDS_LIST.getGroupId() || groupId == WidgetInfo.FRIENDS_CHAT.getGroupId()
			|| componentId == WidgetInfo.CLAN_MEMBER_LIST.getId() || componentId == WidgetInfo.CLAN_GUEST_MEMBER_LIST.getId())
		{
			boolean after;
			if (AFTER_OPTIONS.contains(option))
			{
				after = true;
			}
			else if (BEFORE_OPTIONS.contains(option))
			{
				after = false;
			}
			else
			{
				return;
			}
			ChatPlayer player = getChatPlayerFromName(event.getTarget());
			WorldResult worldResult = this.worldService.getWorlds();
			if (player == null || player.getWorld() == 0 || player.getWorld() == this.client.getWorld() || worldResult == null)
			{
				return;
			}
			World currentWorld = worldResult.findWorld(this.client.getWorld());
			World targetWorld = worldResult.findWorld(player.getWorld());
			if (targetWorld == null || currentWorld == null || (
				!currentWorld.getTypes().contains(WorldType.PVP) && targetWorld.getTypes().contains(WorldType.PVP)))
			{
				return;
			}
			MenuEntry hopTo = new MenuEntry();
			hopTo.setOption("Hop-to");
			hopTo.setTarget(event.getTarget());
			hopTo.setType(MenuAction.RUNELITE.getId());
			hopTo.setParam0(event.getActionParam0());
			hopTo.setParam1(event.getActionParam1());
			insertMenuEntry(hopTo, this.client.getMenuEntries(), after);
		}
	}

	private void insertMenuEntry(MenuEntry newEntry, MenuEntry[] entries, boolean after)
	{
		MenuEntry[] newMenu = (MenuEntry[]) ObjectArrays.concat((Object[]) entries, newEntry);
		if (after)
		{
			int menuEntryCount = newMenu.length;
			ArrayUtils.swap((Object[]) newMenu, menuEntryCount - 1, menuEntryCount - 2);
		}
		this.client.setMenuEntries(newMenu);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuAction() != MenuAction.RUNELITE || !event.getMenuOption().equals("Hop-to"))
		{
			return;
		}
		ChatPlayer player = getChatPlayerFromName(event.getMenuTarget());
		if (player != null)
		{
			hop(player.getWorld());
		}
	}

	@Subscribe
	public void onPlayerDespawned(PlayerDespawned playerDespawned)
	{
		if (playerDespawned.getPlayer().equals(this.client.getLocalPlayer()))
		{
			return;
		}
		SetHopAbility(playerDespawned.getPlayer().getName().toLowerCase(), true);
	}

	@Subscribe
	public void onPlayerSpawned(PlayerSpawned playerSpawned)
	{
		if (playerSpawned.getPlayer().equals(this.client.getLocalPlayer()))
		{
			return;
		}
		SetHopAbility(playerSpawned.getPlayer().getName().toLowerCase(), false);
	}

	void SetHopAbility(String name, boolean enabled)
	{
		if (!name.isEmpty() && (
			name.equals(this.config.getHopperName().toLowerCase()) || name.equals(this.config.getHopperName2().toLowerCase())))
		{
			this.logOutNotifTick = enabled ? this.client.getTickCount() : -1;
			allowHopping = enabled;
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (this.config.showWorldHopperSidebar() && gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			if (this.lastWorld != this.client.getWorld())
			{
				int newWorld = this.client.getWorld();
				this.panel.switchCurrentHighlight(newWorld, this.lastWorld);
				this.lastWorld = newWorld;
			}
		}
	}

	@Subscribe
	public void onWorldListLoad(WorldListLoad worldListLoad)
	{
		if (!this.config.showWorldHopperSidebar())
		{
			return;
		}
		Map<Integer, Integer> worldData = new HashMap<>();
		for (net.runelite.api.World w : worldListLoad.getWorlds())
		{
			worldData.put(w.getId(), w.getPlayerCount());
		}
		this.panel.updateListData(worldData);
		this.lastFetch = Instant.now();
	}

	private void tick()
	{
		Instant now = Instant.now();
		if (this.lastFetch != null && now.toEpochMilli() - this.lastFetch.toEpochMilli() < TICK_THROTTLE)
		{
			log.debug("Throttling world refresh tick");
			return;
		}
		fetchWorlds();
		if (this.firstRun)
		{
			this.firstRun = false;
			this.hopperExecutorService.execute(this::pingInitialWorlds);
		}
	}

	void refresh()
	{
		Instant now = Instant.now();
		if (this.lastFetch != null && now.toEpochMilli() - this.lastFetch.toEpochMilli() < 60000L)
		{
			log.debug("Throttling world refresh");
			return;
		}
		fetchWorlds();
	}

	private void fetchWorlds()
	{
		log.debug("Fetching worlds");
		WorldResult worldResult = this.worldService.getWorlds();
		if (worldResult != null)
		{
			worldResult.getWorlds().sort(Comparator.comparingInt(World::getId));
			this.worldResult = worldResult;
			this.lastFetch = Instant.now();
			updateList();
		}
	}

	private void updateList()
	{
		SwingUtilities.invokeLater(() -> this.panel.populate(this.worldResult.getWorlds()));
	}

	private void hop(boolean previous)
	{
		if (this.worldResult == null || this.client.getGameState() != GameState.LOGGED_IN || !allowHopping)
		{
			return;
		}
		World currentWorld = this.worldResult.findWorld(this.client.getWorld());
		if (currentWorld == null)
		{
			return;
		}
		EnumSet<WorldType> currentWorldTypes = currentWorld.getTypes().clone();
		if (this.config.quickhopOutOfDanger())
		{
			currentWorldTypes.remove(WorldType.PVP);
			currentWorldTypes.remove(WorldType.HIGH_RISK);
		}
		currentWorldTypes.remove(WorldType.BOUNTY);
		currentWorldTypes.remove(WorldType.SKILL_TOTAL);
		currentWorldTypes.remove(WorldType.LAST_MAN_STANDING);
		List<World> worlds = this.worldResult.getWorlds();
		int worldIdx = worlds.indexOf(currentWorld);
		int totalLevel = this.client.getTotalLevel();
		boolean customCyclePresent = (this.customWorlds.length() > 0);
		if (customCyclePresent)
		{
			String[] customWorldCycleStr = this.customWorlds.split(",");
			List<Integer> customWorldCycleInt = new ArrayList<>();
			for (String world : customWorldCycleStr)
			{
				try
				{
					int parsedWorld = Integer.parseInt(world);
					customWorldCycleInt.add(parsedWorld);
				}
				catch (Exception exception)
				{
				}
			}
			int currentIdx = -1;
			for (int i = 0; i < customWorldCycleInt.size(); i++)
			{
				if (customWorldCycleInt.get(i) == currentWorld.getId())
				{
					currentIdx = i;
					break;
				}
			}
			if (currentIdx != -1)
			{
				if (previous)
				{
					if (--currentIdx <= -1)
					{
						currentIdx = customWorldCycleInt.size() - 1;
					}
				}
				else if (++currentIdx >= customWorldCycleInt.size())
				{
					currentIdx = 0;
				}
			}
			int temp = (currentIdx == -1) ? 0 : currentIdx;
			if (this.config.combatHop())
			{
				if (this.allowedToHop)
				{
					hop((Integer) customWorldCycleInt.get((currentIdx == -1) ? 0 : currentIdx));
				}
				else
				{
					this.hopBlocked.submit(() -> {
						try
						{
							if (this.hopDelayMS > 0L)
							{
								Thread.sleep(this.hopDelayMS);
							}
						}
						catch (InterruptedException e)
						{
							e.printStackTrace();
						}
						hop((Integer) customWorldCycleInt.get(temp));
					});
				}
			}
			else
			{
				hop((Integer) customWorldCycleInt.get(temp));
			}
		}
		else
		{
			World world;
			do
			{
				if (previous)
				{
					worldIdx--;
					if (worldIdx < 0)
					{
						worldIdx = worlds.size() - 1;
					}
				}
				else
				{
					worldIdx++;
					if (worldIdx >= worlds.size())
					{
						worldIdx = 0;
					}
				}
				world = worlds.get(worldIdx);
				if (config.quickHopRegionFilter() != RegionFilterMode.NONE && world.getRegion() != config.quickHopRegionFilter().getRegion())
				{
					continue;
				}
				EnumSet<WorldType> types = world.getTypes().clone();
				types.remove(WorldType.BOUNTY);
				types.remove(WorldType.LAST_MAN_STANDING);
				if (types.contains(WorldType.SKILL_TOTAL))
				{
					try
					{
						int totalRequirement = Integer.parseInt(world.getActivity().substring(0, world.getActivity().indexOf(" ")));
						if (totalLevel >= totalRequirement)
						{
							types.remove(WorldType.SKILL_TOTAL);
						}
					}
					catch (NumberFormatException ex)
					{
						log.warn("Failed to parse total level requirement for target world", ex);
					}
				}
				if (currentWorldTypes.equals(types))
				{
					break;
				}
			} while (world != currentWorld);
			if (world == currentWorld)
			{
				String chatMessage = (new ChatMessageBuilder()).append(ChatColorType.NORMAL).append("Couldn't find a world to quick-hop to.").build();
				this.chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(chatMessage)
					.build());
			}
			else if (this.config.combatHop())
			{
				if (this.allowedToHop)
				{
					hop(world.getId());
				}
				else
				{
					World finalWorld = world;
					this.hopBlocked.submit(() -> {
						try
						{
							if (this.hopDelayMS > 0L)
							{
								Thread.sleep(this.hopDelayMS);
							}
						}
						catch (InterruptedException e)
						{
							e.printStackTrace();
						}
						hop(finalWorld.getId());
					});
				}
			}
			else
			{
				hop(world.getId());
			}
		}
	}

	private void hop(int worldId)
	{
		World world = this.worldResult.findWorld(worldId);
		if (world == null)
		{
			return;
		}
		net.runelite.api.World rsWorld = this.client.createWorld();
		rsWorld.setActivity(world.getActivity());
		rsWorld.setAddress(world.getAddress());
		rsWorld.setId(world.getId());
		rsWorld.setPlayerCount(world.getPlayers());
		rsWorld.setLocation(world.getLocation());
		rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));
		if (this.client.getGameState() == GameState.LOGIN_SCREEN)
		{
			this.client.changeWorld(rsWorld);
			return;
		}
		if (this.config.showWorldHopMessage())
		{
			String chatMessage = (new ChatMessageBuilder()).append(ChatColorType.NORMAL).append("Quick-hopping to World ").append(ChatColorType.HIGHLIGHT).append(Integer.toString(world.getId())).append(ChatColorType.NORMAL).append("..").build();
			this.chatMessageManager
				.queue(QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(chatMessage)
					.build());
		}
		this.quickHopTargetWorld = rsWorld;
		this.displaySwitcherAttempts = 0;
		this.hopDelay = 0L;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		this.currentWorld = this.client.getWorld();
		if (this.client.getTickCount() == this.logOutNotifTick)
		{
			this.logOutNotifTick = -1;
			if (this.config.playSound())
			{
				this.client.playSoundEffect(80);
			}
		}
		if (this.quickHopTargetWorld == null)
		{
			return;
		}
		if (this.client.getWidget(WidgetInfo.WORLD_SWITCHER_LIST) == null)
		{
			this.client.openWorldHopper();
			if (++this.displaySwitcherAttempts >= 3)
			{
				String chatMessage = (new ChatMessageBuilder()).append(ChatColorType.NORMAL).append("Failed to quick-hop after ").append(ChatColorType.HIGHLIGHT).append(Integer.toString(this.displaySwitcherAttempts)).append(ChatColorType.NORMAL).append(" attempts.").build();
				this.chatMessageManager
					.queue(QueuedMessage.builder()
						.type(ChatMessageType.CONSOLE)
						.runeLiteFormattedMessage(chatMessage)
						.build());
				resetQuickHopper();
			}
		}
		else
		{
			this.client.hopToWorld(this.quickHopTargetWorld);
			resetQuickHopper();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}
		if (event.getMessage().equals("Please finish what you're doing before using the World Switcher."))
		{
			resetQuickHopper();
		}
	}

	private void resetQuickHopper()
	{
		this.displaySwitcherAttempts = 0;
		this.quickHopTargetWorld = null;
	}

	private ChatPlayer getChatPlayerFromName(String name)
	{
		String cleanName = Text.removeTags(name);

		// Search friends chat members first, because we can always get their world;
		// friends worlds may be hidden if they have private off. (#5679)
		FriendsChatManager friendsChatManager = client.getFriendsChatManager();
		if (friendsChatManager != null)
		{
			FriendsChatMember member = friendsChatManager.findByName(cleanName);
			if (member != null)
			{
				return member;
			}
		}

		ClanChannel clanChannel = client.getClanChannel();
		if (clanChannel != null)
		{
			ClanChannelMember member = clanChannel.findMember(cleanName);
			if (member != null)
			{
				return member;
			}
		}

		clanChannel = client.getGuestClanChannel();
		if (clanChannel != null)
		{
			ClanChannelMember member = clanChannel.findMember(cleanName);
			if (member != null)
			{
				return member;
			}
		}

		NameableContainer<Friend> friendContainer = client.getFriendContainer();
		if (friendContainer != null)
		{
			return friendContainer.findByName(cleanName);
		}

		return null;
	}

	private void pingInitialWorlds()
	{
		WorldResult worldResult = this.worldService.getWorlds();
		if (worldResult == null || !this.config.showWorldHopperSidebar() || !this.config.worldPing())
		{
			return;
		}
		Stopwatch stopwatch = Stopwatch.createStarted();
		for (World world : worldResult.getWorlds())
		{
			int ping = Ping.ping(world);
			SwingUtilities.invokeLater(() -> this.panel.updatePing(world.getId(), ping));
		}
		stopwatch.stop();
		log.debug("Done pinging worlds in {}", stopwatch.elapsed());
	}

	private void pingNextWorld()
	{
		if (this.worldResult == null || !this.config.showWorldHopperSidebar() || !this.config.worldPing())
		{
			return;
		}
		List<World> worlds = this.worldResult.getWorlds();
		if (worlds.isEmpty())
		{
			return;
		}
		if (this.currentWorld >= worlds.size())
		{
			this.currentWorld = 0;
		}
		World world = worlds.get(this.currentWorld++);
		boolean displayPing = (this.config.displayCurrentPing() && this.client.getGameState() == GameState.LOGGED_IN);
		if (displayPing && this.client.getWorld() == world.getId())
		{
			return;
		}
		int ping = Ping.ping(world);
		log.trace("Ping for world {} is: {}", world.getId(), ping);
		SwingUtilities.invokeLater(() -> this.panel.updatePing(world.getId(), ping));
	}

	private void pingCurrentWorld()
	{
		if (this.worldResult == null || !this.config.displayCurrentPing() || this.client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		World currentWorld = this.worldResult.findWorld(this.client.getWorld());
		if (currentWorld == null)
		{
			log.debug("unable to find current world: {}", this.client.getWorld());
			return;
		}
		this.currentPing = Ping.ping(currentWorld);
		log.trace("Ping for current world is: {}", this.currentPing);
		SwingUtilities.invokeLater(() -> this.panel.updatePing(currentWorld.getId(), this.currentPing));
	}

	Integer getStoredPing(World world)
	{
		if (!this.config.worldPing())
		{
			return null;
		}
		return this.storedPings.get(world.getId());
	}

	private int ping(World world)
	{
		int ping = Ping.ping(world);
		this.storedPings.put(world.getId(), ping);
		return ping;
	}
}
