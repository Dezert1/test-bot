package com.bottest.scripts.altrecbots.model.listeners;

import com.bottest.scripts.altrecbots.utils.BotUtils;
import com.bottest.scripts.altrecbots.model.ActionRecordingContext;

import java.util.Optional;

import l2.gameserver.listener.actor.OnMoveListener;
import l2.gameserver.model.Creature;
import l2.gameserver.model.Player;
import l2.gameserver.utils.Location;

public class MoveListener extends BasicPlayerListener implements OnMoveListener
{
	private static final MoveListener instance = new MoveListener();

	public static MoveListener getInstance()
	{
		return instance;
	}

	@Override
	public void onMove(Creature actor, Location loc)
	{
		if(actor == null || !actor.isPlayer())
			return;

		Player player = actor.getPlayer();
		Optional<ActionRecordingContext> optContext = getRecordingContext(actor);
		if(!optContext.isPresent())
			return;

		ActionRecordingContext recordingContext = optContext.get();
		if(!BotUtils.testRecordingCondition(player))
			recordingContext.close(player);

		recordingContext.onMove(loc);
	}
}