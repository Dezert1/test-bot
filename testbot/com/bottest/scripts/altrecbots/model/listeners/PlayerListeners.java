package com.bottest.scripts.altrecbots.model.listeners;

import com.bottest.scripts.altrecbots.utils.BotUtils;
import com.bottest.scripts.altrecbots.model.ActionRecordingContext;

import java.util.Optional;

import l2.gameserver.listener.actor.OnAttackListener;
import l2.gameserver.listener.actor.OnMagicUseListener;
import l2.gameserver.listener.actor.player.OnSetClassListener;
import l2.gameserver.listener.actor.player.OnTeleportListener;
import l2.gameserver.model.Creature;
import l2.gameserver.model.Player;
import l2.gameserver.model.Skill;
import l2.gameserver.model.entity.Reflection;
import l2.gameserver.utils.Location;

public class PlayerListeners extends BasicPlayerListener implements OnAttackListener, OnMagicUseListener, OnSetClassListener, OnTeleportListener
{
	private static final PlayerListeners instance = new PlayerListeners();

	public static PlayerListeners getInstance()
	{
		return instance;
	}

	@Override
	public void onSetClass(Player player, int newClassId)
	{
		Optional<ActionRecordingContext> contextRef = getRecordingContext(player);
		if(!contextRef.isPresent())
			return;

		ActionRecordingContext context = contextRef.get();
		context.onSetClass(player, newClassId);
	}

	@Override
	public void onTeleport(Player player, int x, int y, int z, Reflection ref)
	{
		Optional<ActionRecordingContext> contextRef = getRecordingContext(player);
		if(!contextRef.isPresent())
			return;

		ActionRecordingContext context = contextRef.get();
		if(!BotUtils.testRecordingCondition(player))
		{
			context.close(player);
			return;
		}
		context.onTeleported(new Location(x, y, z), player);
	}

	@Override
	public void onMagicUse(Creature creature, Skill skill, Creature target, boolean alt)
	{
		Optional<ActionRecordingContext> contextRef = getRecordingContext(creature);
		if(!contextRef.isPresent() || !creature.isPlayer())
			return;

		ActionRecordingContext context = contextRef.get();
		context.onSkillCast(creature.getPlayer(), skill, target);
	}

	@Override
	public void onAttack(Creature actor, Creature target)
	{
		Optional<ActionRecordingContext> contextRef = getRecordingContext(actor);
		if(!contextRef.isPresent() || !actor.isPlayer())
			return;

		ActionRecordingContext context = contextRef.get();
		context.onAttack(actor.getPlayer(), target);
	}
}

