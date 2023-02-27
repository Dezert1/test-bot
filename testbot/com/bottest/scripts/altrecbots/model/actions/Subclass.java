package com.bottest.scripts.altrecbots.model.actions;

import com.bottest.scripts.altrecbots.model.AltRecBot;
import com.bottest.scripts.altrecbots.utils.BotUtils;
import com.bottest.scripts.altrecbots.ThreadPoolManager;
import com.bottest.scripts.altrecbots.model.ActionPlaybackContext;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import l2.commons.lang.reference.HardReference;
import l2.commons.threading.RunnableImpl;
import l2.gameserver.Config;
import l2.gameserver.model.GameObjectsStorage;
import l2.gameserver.model.Player;
import l2.gameserver.model.base.ClassId;
import l2.gameserver.model.base.Experience;
import l2.gameserver.model.instances.NpcInstance;
import l2.gameserver.network.l2.s2c.MagicSkillUse;
import l2.gameserver.network.l2.s2c.SocialAction;
import l2.gameserver.utils.Location;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Subclass extends Action<Subclass> implements Serializable
{
	private SubclassActionType subclassActionType;
	private Integer targetNpcId;
	private Location location;
	private ClassId classId;
	private ClassId newClassId;
	private Long exp;

	public Subclass()
	{
	}

	public Subclass(SubclassActionType subclassActionType, Integer targetNpcId, ClassId classId, ClassId newClassId)
	{
		this.subclassActionType = subclassActionType;
		this.targetNpcId = targetNpcId;
		this.classId = classId;
		this.newClassId = newClassId;
	}

	private static final Location getLegacyLocation(int x, int y, Integer npcId)
	{
		if(npcId != null && npcId > 0)
		{
			NpcInstance npcInstance = null;
			long minDist = Long.MAX_VALUE;
			for(NpcInstance npc : GameObjectsStorage.getAllByNpcId(npcId, true))
			{
				long distance = npc.getSqDistance(x, y);
				if(distance < minDist)
				{
					npcInstance = npc;
					minDist = distance;
				}
			}

			if(npcInstance != null)
				return new Location(x, y, npcInstance.getZ() + Config.CLIENT_Z_SHIFT).correctGeoZ();
		}
		return new Location(x, y, 32767).correctGeoZ();
	}

	public SubclassActionType getSubclassActionType()
	{
		return subclassActionType;
	}

	public Subclass setSubclassActionType(SubclassActionType subclassActionType)
	{
		this.subclassActionType = subclassActionType;
		return this;
	}

	public Integer getTargetNpcId()
	{
		return targetNpcId;
	}

	public Subclass setTargetNpcId(Integer targetNpcId)
	{
		this.targetNpcId = targetNpcId;
		return this;
	}

	public ClassId getClassId()
	{
		return classId;
	}

	public Subclass setClassId(ClassId classId)
	{
		this.classId = Objects.requireNonNull(classId);
		return this;
	}

	public ClassId getNewClassId()
	{
		return newClassId;
	}

	public Subclass setNewClassId(ClassId newClassId)
	{
		this.newClassId = Objects.requireNonNull(newClassId);
		return this;
	}

	public Long getExp()
	{
		return exp;
	}

	public Subclass setExp(Long exp)
	{
		this.exp = exp;
		return this;
	}

	public Location getLocation()
	{
		return location;
	}

	public Subclass setLocation(Location location)
	{
		this.location = location;
		return this;
	}

	@Override
	public Subclass fromLegacy(int[] actions)
	{
		switch(actions[0])
		{
			case 0:
				return setSubclassActionType(SubclassActionType.SetNew).setClassId(BotUtils.toClassId(actions[1]).get()).setTargetNpcId(actions[3] != 0 ? actions[3] : null).setLocation(Subclass.getLegacyLocation(actions[4], actions[5], actions[3]));
			case 1:
				return setSubclassActionType(SubclassActionType.Change).setClassId(BotUtils.toClassId(actions[1]).get()).setExp(Experience.getExpForLevel(actions[2])).setTargetNpcId(actions[3] != 0 ? actions[3] : null).setLocation(Subclass.getLegacyLocation(actions[4], actions[5], actions[3]));
			case 2:
				return setSubclassActionType(SubclassActionType.AddNew).setClassId(BotUtils.toClassId(actions[1]).get()).setTargetNpcId(actions[3] != 0 ? actions[3] : null).setLocation(Subclass.getLegacyLocation(actions[4], actions[5], actions[3]));
			case 3:
				return setSubclassActionType(SubclassActionType.Replace).setClassId(BotUtils.toClassId(actions[1]).get()).setNewClassId(BotUtils.toClassId(actions[2]).get()).setTargetNpcId(actions[3] != 0 ? actions[3] : null).setLocation(Subclass.getLegacyLocation(actions[4], actions[5], actions[3]));
		}
		return null;
	}

	@Override
	public boolean doItImpl(AltRecBot recBot, ActionPlaybackContext playbackContext)
	{
		final HardReference<Player> playerRef = recBot.getRef();
		RunnableImpl r = new RunnableImpl()
		{
            @Override
			public void runImpl() throws Exception
			{
				Player player = playerRef.get();
				if(player == null || !(player instanceof AltRecBot))
					return;

				AltRecBot bot = (AltRecBot) player;

				NpcInstance npc = null;
				if(getTargetNpcId() != null)
				{
					npc = BotUtils.setMyTargetByNpcId(bot, getTargetNpcId());
				}

				switch(getSubclassActionType())
				{
					case SetNew:
					{
						player.setClassId(classId.getId(), true, false);
						if(npc != null && bot.getDistance(npc) < npc.getActingRange() + 128)
                        {
                            bot.broadcastPacket(new SocialAction(bot.getObjectId(), 16));
                            bot.broadcastPacket(new SocialAction(bot.getObjectId(), 3));
                        }
						break;
					}
					case Change:
					{
						bot.setActiveSubClass(getClassId().getId(), false);
						if(getExp() != null)
                        {
                            bot.addExpAndSp(getExp() - bot.getExp(), 0L, false, false);
                        }
						break;
					}
					case AddNew:
					{
						bot.setActiveSubClass(getClassId().getId(), false);
						if(npc != null && bot.getDistance(npc) < npc.getActingRange() && getExp() != null)
                        {
                            bot.addExpAndSp(getExp() - bot.getExp(), 0, false, false);
                        }
						break;
					}
					case Replace:
					{
						bot.setActiveSubClass(getClassId().getId(), false);
						if(npc != null && bot.getDistance(npc) < npc.getActingRange())
                        {
                            player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
                            player.broadcastPacket(new MagicSkillUse(player, player, 4339, 1, 0, 0));
                        }
						break;
					}
				}

				int activeClassId = bot.getActiveClassId();
				ClassId c = Stream.of(ClassId.values()).filter(classId -> classId.getId() == activeClassId).findAny().get();
				int level = Math.max(bot.getLevel(), Player.EXPERTISE_LEVELS[c.getLevel()]);
				bot.addExpAndSp(Experience.getExpForLevel(level) - bot.getExp(), 0L, false, false);
				player.broadcastCharInfo();
			}
		};

		if(recBot.isSitting())
		{
			recBot.standUp();
		}

		NpcInstance npc = null;
		if(getTargetNpcId() != null)
		{
            npc = BotUtils.setMyTargetByNpcId(recBot, getTargetNpcId());
		    if(npc != null)
            {
                double distance = 0.0;
                distance = recBot.getDistance(npc);
                if(distance > npc.getActingRange())
                {
                    recBot.moveToLocation(npc.getLoc(), Math.max(48, npc.getActingRange() / 2), true);
                    ThreadPoolManager.getInstance().schedule(r, (long) (distance / (double) recBot.getRunSpeed() * 1000.0));
                    return true;
                }
            }
		}
		r.run();
		return true;
	}

	@Override
	public long getDuration(ActionPlaybackContext playbackContext)
	{
		Optional<AltRecBot> optBot = playbackContext.getPlayer();
		if(!optBot.isPresent())
			return super.getDuration(playbackContext);

		AltRecBot bot = optBot.get();
		NpcInstance npc = null;
		if(getTargetNpcId() != null)
		{
            npc = BotUtils.setMyTargetByNpcId(bot, getTargetNpcId());
		    if(npc != null)
            {
                double distance = 0.;
                distance = bot.getDistance(npc);
                if(distance > npc.getActingRange())
                    return (long) (distance / bot.getRunSpeed() * 1000.0) + super.getDuration();
            }
		}
		return super.getDuration(playbackContext);
	}

	@Override
	public ActionType getActionType()
	{
		return ActionType.SUBCLASS;
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
			return true;

		if(o == null || getClass() != o.getClass())
			return false;

		Subclass subclass = (Subclass) o;
		return new EqualsBuilder().append(subclassActionType, subclass.subclassActionType).append(targetNpcId, subclass.targetNpcId).append(classId, subclass.classId).append(newClassId, subclass.newClassId).append(exp, subclass.exp).isEquals();
	}

	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(17, 37).append(subclassActionType).append(targetNpcId).append(classId).append(newClassId).append(exp).toHashCode();
	}

	@Override
	public String toString()
	{
		return "SubclassParams{subclassActionType=" + subclassActionType + ", targetNpcId=" + targetNpcId + ", classId=" + classId + ", newClassId=" + newClassId + ", exp=" + exp + '}';
	}

	public static enum SubclassActionType
	{
		SetNew,
        Change,
        AddNew,
        Replace
	}
}