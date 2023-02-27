package com.bottest.scripts.altrecbots.model.listeners;

import l2.gameserver.listener.actor.player.OnPlayerEnterListener;
import l2.gameserver.model.Player;

public class PlayerEnterListener implements OnPlayerEnterListener
{
	private static final PlayerEnterListener instance = new PlayerEnterListener();

	public static PlayerEnterListener getInstance()
	{
		return instance;
	}

	@Override
	public void onPlayerEnter(Player player)
	{
	}
}