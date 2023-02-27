package com.bottest.scripts.altrecbots.model.actions;

import com.bottest.scripts.altrecbots.model.AltRecBot;
import com.bottest.scripts.altrecbots.utils.BotUtils;
import com.bottest.scripts.altrecbots.model.ActionPlaybackContext;

import java.io.Serializable;

import l2.gameserver.model.instances.NpcInstance;
import l2.gameserver.utils.Location;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Attack extends Action<Attack> implements Serializable
{
	private int targetNpcId;
	private Location targetLoc;

	public Attack()
	{
	}

	public Attack(int targetNpcId, Location targetLoc)
	{
		this.targetNpcId = targetNpcId;
		this.targetLoc = targetLoc;
	}

	public int getTargetNpcId()
	{
		return targetNpcId;
	}

	public Attack setTargetNpcId(int targetNpcId)
	{
		this.targetNpcId = targetNpcId;
		return this;
	}

	public Location getTargetLoc()
	{
		return targetLoc;
	}

	public Attack setTargetLoc(Location targetLoc)
	{
		this.targetLoc = targetLoc;
		return this;
	}

	@Override
	public Attack fromLegacy(int[] actions)
	{
		return setTargetNpcId(actions[0]).setTargetLoc(new Location(actions[1], actions[2], 32767).correctGeoZ());
	}

	@Override
	public boolean doItImpl(AltRecBot recBot, ActionPlaybackContext playbackContext)
	{
		if(recBot.isMoving())
			recBot.stopMove();

		if(getTargetNpcId() > 0)
		{
			NpcInstance npc = BotUtils.setMyTargetByNpcId(recBot, getTargetNpcId());
			if(npc != null)
				recBot.doAttack(npc);
		}
		return true;
	}

	@Override
	public ActionType getActionType()
	{
		return ActionType.ATTACK;
	}

	@Override
	public boolean equals(Object object)
	{
		if(this == object)
		{
			return true;
		}
		if(object == null || getClass() != object.getClass())
		{
			return false;
		}
		Attack attack = (Attack) object;
		return new EqualsBuilder().append(targetNpcId, attack.targetNpcId).append(targetLoc, attack.targetLoc).isEquals();
	}

	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(17, 37).append(targetNpcId).append(targetLoc).toHashCode();
	}

	@Override
	public String toString()
	{
		return "AttackParams{targetNpcId=" + targetNpcId + ", targetLoc=" + targetLoc + '}';
	}
}