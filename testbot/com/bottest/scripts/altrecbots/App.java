package com.bottest.scripts.altrecbots;

import com.bottest.scripts.altrecbots.model.ActionsStorageManager;
import com.bottest.scripts.altrecbots.model.AltRecBot;
import com.bottest.scripts.altrecbots.model.BotPhrasePool;
import com.bottest.scripts.altrecbots.model.BotSpawnManager;
import com.bottest.scripts.altrecbots.model.handler.admincommands.AdminAltRecBots;
import com.bottest.scripts.altrecbots.model.listeners.PlayerEnterListener;
import com.bottest.scripts.altrecbots.model.listeners.ZoneListener;

import java.util.Collection;

import l2.commons.threading.RunnableImpl;
import l2.gameserver.GameServer;
import l2.gameserver.ThreadPoolManager;
import l2.gameserver.handler.admincommands.AdminCommandHandler;
import l2.gameserver.instancemanager.ReflectionManager;
import l2.gameserver.listener.game.OnStartListener;
import l2.gameserver.model.GameObject;
import l2.gameserver.model.Player;
import l2.gameserver.model.Zone;
import l2.gameserver.model.actor.listener.PlayerListenerList;
import l2.gameserver.scripts.ScriptFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Класс представляет собой точку входа в приложение AltRecBots, реализующее ботов в игре, которые запоминают и воспроизводят действия
 */
public class App implements OnStartListener, ScriptFile
{
	private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
	protected static boolean isLegacyImport = false;

	public static void main(String... args) throws Exception
	{

	}

	@Override
	public void onLoad()
	{
		Config.load();
		if(!Config.BOTS_ENABLED)
			return;

		AdminCommandHandler.getInstance().registerAdminCommandHandler(AdminAltRecBots.getInstance());
		GameServer.getInstance().getListeners().add(this);
		ActionsStorageManager.getInstance().init();
		BotSpawnManager.getInstance().init();
		BotPhrasePool.getInstance().loadPhrases();
		if(isLegacyImport)
			LegacyTrashImporter.onLoad();
	}

	private void registerListeners()
	{
		PlayerListenerList.addGlobal(PlayerEnterListener.getInstance());

		Collection<Zone> zones = ReflectionManager.DEFAULT.getZones();
		for(Zone zone : zones)
		{
			if(zone.isType(Zone.ZoneType.peace_zone))
				zone.addListener(ZoneListener.getInstance());
		}
	}

	@Override
	public void onStart()
	{
		if(isLegacyImport)
		{
			LegacyTrashImporter.onStart();
			return;
		}

		registerListeners();

		if(Config.BOTS_ENABLED && Config.BOT_COUNT_SUPPLIER != null)
		{
			ThreadPoolManager.getInstance().scheduleAtFixedRate(new RunnableImpl()
			{
				@Override
				public void runImpl() throws Exception
				{
					BotSpawnManager.getInstance().trySpawn();
				}
			}, Config.BOTS_SPAWN_CHECK_INTERVAL, Config.BOTS_SPAWN_CHECK_INTERVAL);
		}
	}

	@Override
	public void onReload()
	{
		l2.gameserver.Config.load();
	}

	@Override
	public void onShutdown()
	{}

	public boolean OnActionShift_AltRecBot(Player player, GameObject target)
	{
		if(player == null || target == null || !(player.getPlayerAccess()).CanViewChar)
			return false;
		if(target instanceof AltRecBot)
			return AdminAltRecBots.getInstance().doOnActionShift(player, (AltRecBot) target);
		return false;
	}
}