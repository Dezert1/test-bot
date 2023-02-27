package com.bottest.scripts.altrecbots.model.actions;

import com.bottest.scripts.altrecbots.model.AltRecBot;
import com.bottest.scripts.altrecbots.model.ActionPlaybackContext;

import java.io.Serializable;

import l2.gameserver.model.base.Experience;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class GainExp extends Action<GainExp> implements Serializable
{
	private Long exp;

	public GainExp()
	{
	}

	public GainExp(Long exp)
	{
		this.exp = exp;
	}

	public Long getExp()
	{
		return exp;
	}

	public GainExp setExp(Long exp)
	{
		this.exp = exp;
		return this;
	}

	@Override
	public GainExp fromLegacy(int[] actions)
	{
		return setExp(Experience.getExpForLevel(actions[0]));
	}

	@Override
	public boolean doItImpl(AltRecBot recBot, ActionPlaybackContext playbackContext)
	{
		int level = recBot.getLevel();
		recBot.addExpAndSp(getExp() - recBot.getExp(), 0L, false, false);
		return true;
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
			return true;

		if(o == null || getClass() != o.getClass())
			return false;

		GainExp gainExp = (GainExp) o;
		return new EqualsBuilder().append(exp, gainExp.exp).isEquals();
	}

	@Override
	public String toString()
	{
		return "GainExp{exp=" + exp + '}';
	}

	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(17, 37).append(exp).toHashCode();
	}

	@Override
	public ActionType getActionType()
	{
		return ActionType.GAIN_EXP;
	}
}