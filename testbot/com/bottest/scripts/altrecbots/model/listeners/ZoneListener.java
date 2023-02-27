package com.bottest.scripts.altrecbots.model.listeners;

import com.bottest.scripts.altrecbots.utils.BotUtils;
import com.bottest.scripts.altrecbots.Config;
import com.bottest.scripts.altrecbots.model.ActionRecordingContext;

import java.util.Optional;

import l2.gameserver.listener.zone.OnZoneEnterLeaveListener;
import l2.gameserver.model.Creature;
import l2.gameserver.model.Player;
import l2.gameserver.model.Zone;

public class ZoneListener implements OnZoneEnterLeaveListener
{
	private static final ZoneListener instance = new ZoneListener();

	public static ZoneListener getInstance()
	{
		return instance;
	}

	@Override
	public void onZoneEnter(Zone zone, Creature actor)
	{
		if(!Config.AUTO_RECORD_PLAYER_ACTIONS || actor == null || !actor.isPlayer() || BotUtils.isBot(actor))
			return;

		Player player = actor.getPlayer();
		if(!BotUtils.testRecordingCondition(player))
		{
			Optional<ActionRecordingContext> recordingContext = ActionRecordingContext.getRecordingContext(player);
			if(recordingContext.isPresent())
			{
				recordingContext.get().close(player);
				return;
			}
			return;
		}

		Optional<ActionRecordingContext> recordingContext = ActionRecordingContext.getRecordingContext(player);
		if(!recordingContext.isPresent())
			ActionRecordingContext.openContext(player);
	}

	@Override
	public void onZoneLeave(Zone zone, Creature actor)
	{
		if(!Config.AUTO_RECORD_PLAYER_ACTIONS || actor == null || !actor.isPlayer() || BotUtils.isBot(actor))
			return;

        Player player = actor.getPlayer();
        if(!BotUtils.testRecordingCondition(player))
        {
            Optional<ActionRecordingContext> recordingContext = ActionRecordingContext.getRecordingContext(player);
            if(recordingContext.isPresent())
                recordingContext.get().close(player);
        }
	}
}