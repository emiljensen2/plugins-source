package net.runelite.client.plugins.socket.plugins.chat;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.VarClientStr;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.VarClientStrChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
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
import net.runelite.client.util.ColorUtil;
import javax.inject.Inject;
import java.awt.event.KeyEvent;
import org.pf4j.Extension;

@Slf4j
@Extension
@PluginDescriptor(
	name = "Socket - Chat",
	description = "Chat over socket",
	tags = {"Socket", "chat"},
	enabledByDefault = false
)
@PluginDependency(SocketPlugin.class)
public class sChatPlugin extends Plugin implements KeyListener
{
	@Inject
	Client client;

	@Inject
	private KeyManager keyManager;

	@Inject
	sChatConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private SocketPlugin socketPlugin;

	private boolean tradeActive = false;
	private boolean typing = false;
	private boolean lastTypingState = false;

	@Provides
	sChatConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(sChatConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		keyManager.registerKeyListener(this);
	}

	@Inject
	private EventBus eventBus;


	@Override
	public void keyTyped(KeyEvent e)
	{

	}

	@Subscribe
	public void onVarClientStrChanged(VarClientStrChanged event) throws InterruptedException
	{
		removeHotkey();
	}

	private void removeHotkey() throws InterruptedException
	{
		String typedText = client.getVar(VarClientStr.CHATBOX_TYPED_TEXT);
		if (typedText.length() > 0)
		{
			String subTypedText = typedText.substring(0, typedText.length() - 1);
			String x = KeyEvent.getKeyText(config.hotkey().getKeyCode());
			char a = (char) KeyEvent.getExtendedKeyCodeForChar(typedText.substring(typedText.length() - 1).toCharArray()[0]);
			char b = (char) config.hotkey().getKeyCode();
			String y = typedText.substring(typedText.length() - 1);
			if (a == b)
			{
				client.setVar(VarClientStr.CHATBOX_TYPED_TEXT, subTypedText);
			}
		}
	}

	@Subscribe
	private void onBeforeRender(BeforeRender event)
	{
		Widget chatbox = client.getWidget(WidgetInfo.CHATBOX_INPUT);
		if (chatbox == null || chatbox.isHidden())
		{
			return;
		}
		if (!tradeActive && client.getVarcIntValue(41) == 6)
		{
			lastTypingState = typing;
			typing = true;
			tradeActive = true;
		}
		else if (tradeActive && client.getVarcIntValue(41) != 6)
		{
			typing = lastTypingState;
			tradeActive = false;
		}
		if (typing)
		{
			if (!chatbox.getText().startsWith("[SOCKET CHAT] "))
			{
				chatbox.setText("[SOCKET CHAT] " + chatbox.getText());
			}
		}
		else
		{
			if (chatbox.getText().startsWith("[SOCKET CHAT] "))
			{
				chatbox.setText(chatbox.getText().substring(13));
			}
		}
		if (config.overrideTradeButton())
		{
			Widget tradeButton = client.getWidget(162, 27);
			tradeButton.setText("Socket");
		}
		else
		{
			Widget tradeButton = client.getWidget(162, 27);
			tradeButton.setText("Trade");
		}
	}

	//var client int 41 == 5
	//162.31 trade
	@Override
	public void keyPressed(KeyEvent e)
	{
		if (config.hotkey().matches(e))
		{
			typing = !typing;
			clientThread.invokeLater(() ->
			{
				try
				{
					removeHotkey();
				}
				catch (InterruptedException interruptedException)
				{
					interruptedException.printStackTrace();
				}
			});
		}
		if (e.getKeyCode() == KeyEvent.VK_ENTER)
		{
			String typedText = client.getVar(VarClientStr.CHATBOX_TYPED_TEXT);
			if (typing)
			{
				if (typedText.startsWith("/"))
				{
					if (!config.overrideSlash())
					{
						sendMessage(typedText);
						client.setVar(VarClientStr.CHATBOX_TYPED_TEXT, "");
					}
				}
				else
				{
					sendMessage(typedText);
					client.setVar(VarClientStr.CHATBOX_TYPED_TEXT, "");
				}
			}
		}
	}

	private void sendMessage(String msg)
	{
		if (msg.equals(""))
		{
			return;
		}
		JSONArray data = new JSONArray();
		JSONObject jsonmsg = new JSONObject();
		jsonmsg.put("msg", " " + msg);
		jsonmsg.put("sender", client.getLocalPlayer().getName());
		data.put(jsonmsg);
		JSONObject send = new JSONObject();
		send.put("sChat", data);
		eventBus.post(new SocketBroadcastPacket(send));
	}

	@Subscribe
	public void onSocketReceivePacket(SocketReceivePacket event)
	{
		try
		{
			JSONObject payload = event.getPayload();
			if (!payload.has("sChat"))
			{
				return;
			}

			JSONArray data = payload.getJSONArray("sChat");
			JSONObject jsonmsg = data.getJSONObject(0);
			String sender = jsonmsg.getString("sender");
			String msg = jsonmsg.getString("msg");
			ChatMessageType cmt;
			if (config.overrideTradeButton())
			{
				cmt = ChatMessageType.TRADE;
			}
			else
			{
				cmt = ChatMessageType.GAMEMESSAGE;
			}
			this.client.addChatMessage(cmt, "", "[SOCKET CHAT] " + sender + ": " + ColorUtil.prependColorTag(msg, config.messageColor()), null, false);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{

	}
}
