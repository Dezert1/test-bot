package com.bottest.scripts.altrecbots.model.actions;

import com.bottest.scripts.altrecbots.model.AltRecBot;
import com.bottest.scripts.altrecbots.utils.BotUtils;
import com.bottest.scripts.altrecbots.ThreadPoolManager;
import com.bottest.scripts.altrecbots.model.ActionPlaybackContext;

import java.io.Serializable;
import java.util.Optional;

import l2.commons.lang.reference.HardReference;
import l2.commons.threading.RunnableImpl;
import l2.gameserver.model.Creature;
import l2.gameserver.model.Player;
import l2.gameserver.model.Skill;
import l2.gameserver.model.instances.NpcInstance;
import l2.gameserver.tables.SkillTable;
import l2.gameserver.utils.Location;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class SkillCast extends Action<SkillCast> implements Serializable
{
	private int skillId;
	private Integer targetNpcId;
	private Location targetLoc;
	private boolean forceUse;

	public SkillCast()
	{
	}

	public SkillCast(int skillId)
	{
		this.skillId = skillId;
	}

	public SkillCast(int skillId, int targetNpcId, Location targetLoc, boolean forceUse)
	{
		this.skillId = skillId;
		this.targetNpcId = targetNpcId;
		this.targetLoc = targetLoc;
		this.forceUse = forceUse;
	}

	public int getSkillId()
	{
		return skillId;
	}

	public SkillCast setSkillId(int skillId)
	{
		this.skillId = skillId;
		return this;
	}

	public Integer getTargetNpcId()
	{
		return targetNpcId;
	}

	public SkillCast setTargetNpcId(Integer targetNpcId)
	{
		this.targetNpcId = targetNpcId;
		return this;
	}

	public Location getTargetLoc()
	{
		return targetLoc;
	}

	public SkillCast setTargetLoc(Location targetLoc)
	{
		this.targetLoc = targetLoc;
		return this;
	}

	public boolean isForceUse()
	{
		return forceUse;
	}

	public SkillCast setForceUse(boolean forceUse)
	{
		this.forceUse = forceUse;
		return this;
	}

	@Override
	public SkillCast fromLegacy(int[] actions)
	{
		return setSkillId(actions[0]).setTargetNpcId(actions[1] > 0 ? actions[1] : null).setForceUse(actions[2] != 0).setTargetLoc(actions[3] != 0 && actions[4] != 0 ? new Location(actions[3], actions[4], 32767) : null);
	}

	@Override
	public boolean doItImpl(AltRecBot recBot, ActionPlaybackContext playbackContext)
	{
		if(recBot.isSitting())
			recBot.standUp();

		int level = SkillTable.getInstance().getMaxLevel(getSkillId());
		if(level < 1)
			return true;

		Skill skill = SkillTable.getInstance().getInfo(getSkillId(), level);
		if(skill == null)
			return true;

		if(!skill.altUse() && recBot.isMoving())
			recBot.stopMove();

		HardReference<Player> botRef = recBot.getRef();
		RunnableImpl r = new RunnableImpl()
		{
            @Override
			public void runImpl() throws Exception
			{
				AltRecBot bot = (AltRecBot) botRef.get();
				if(bot == null)
					return;

				try
				{
					if(!skill.altUse() && bot.isMoving())
						bot.stopMove();

					Creature target = null;
					if(getTargetNpcId() != null)
					{
						NpcInstance npc = BotUtils.setMyTargetByNpcId(bot, getTargetNpcId());
						if(npc != null)
							target = skill.getAimingTarget(bot, npc);
					}
					else
					{
						bot.setTarget(bot);
						target = skill.getAimingTarget(bot, bot.getTarget());
					}

					if(skill.altUse())
					{
						bot.altUseSkill(skill, target);
					}
					else if(skill.checkCondition(bot, target, isForceUse(), false, true))
					{
						if(target.getEffectList().containEffectFromSkills(skill.getId()))
						{
							target.getEffectList().stopEffect(skill);
						}
						bot.doCast(skill, target, isForceUse());
					}
				}
				catch(Exception e)
				{

				}
			}
		};

		boolean execute = true;
		if(getTargetNpcId() != null && !skill.altUse() && getTargetNpcId() != null)
		{
		    NpcInstance npc = BotUtils.setMyTargetByNpcId(recBot, getTargetNpcId());
		    if(npc != null)
            {
                double distance = 0.0;
                distance = recBot.getDistance(npc);
                if(distance > npc.getActingRange())
                {
                    long delay = (long) (distance / recBot.getRunSpeed() * 1000.);
                    ThreadPoolManager.getInstance().schedule(r, delay + 333);
                    execute = false;
                }
            }
		}

		if(execute)
			ThreadPoolManager.getInstance().execute(r);
		return true;
	}

	@Override
	public long getDuration(ActionPlaybackContext playbackContext)
	{
		Optional<AltRecBot> optPlayer = playbackContext.getPlayer();
		if(!optPlayer.isPresent())
			return super.getDuration(playbackContext);

		AltRecBot recBot = optPlayer.get();
		NpcInstance npc = null;
		if(getTargetNpcId() != null)
		{
            npc = BotUtils.setMyTargetByNpcId(recBot, getTargetNpcId());
		    if(npc != null)
            {
                double distance = 0.0;
                distance = recBot.getDistance(npc);
                if(distance > npc.getActingRange())
                    return (long) (distance / recBot.getRunSpeed() * 1000.0) + super.getDuration() + 500;
            }
		}
		return super.getDuration(playbackContext);
	}

	@Override
	public ActionType getActionType()
	{
		return ActionType.SKILL_CAST;
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
			return true;

		if(o == null || getClass() != o.getClass())
			return false;

		SkillCast skillCast = (SkillCast) o;
		return new EqualsBuilder().append(skillId, skillCast.skillId).append(targetNpcId, skillCast.targetNpcId).append(forceUse, skillCast.forceUse).append(targetLoc, skillCast.targetLoc).isEquals();
	}

	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(17, 37).append(skillId).append(targetNpcId).append(targetLoc).append(forceUse).toHashCode();
	}

	@Override
	public String toString()
	{
		return "SkillCastParams{skillId=" + skillId + ", targetNpcId=" + targetNpcId + ", targetLoc=" + targetLoc + ", forceUse=" + forceUse + '}';
	}
}