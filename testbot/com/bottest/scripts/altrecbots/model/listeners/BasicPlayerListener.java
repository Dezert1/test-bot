package com.bottest.scripts.altrecbots.model.listeners;

import com.bottest.scripts.altrecbots.utils.BotUtils;
import com.bottest.scripts.altrecbots.model.ActionRecordingContext;

import java.util.Optional;

import l2.gameserver.model.Creature;
import l2.gameserver.model.Player;

/**
 * Класс BasicPlayerListener - это простой слушатель игровых событий игрока, используемый для записи действий игрока и создания ботов.
 */
public class BasicPlayerListener
{
	protected Optional<ActionRecordingContext> getRecordingContext(Creature actor)
	{
		if(actor == null || !actor.isPlayer() || BotUtils.isBot(actor))
		{
			return Optional.empty();
		}
		return ActionRecordingContext.getRecordingContext((Player) actor);
	}
}