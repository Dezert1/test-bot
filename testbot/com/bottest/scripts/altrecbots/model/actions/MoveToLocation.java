package com.bottest.scripts.altrecbots.model.actions;

import com.bottest.scripts.altrecbots.model.AltRecBot;
import com.bottest.scripts.altrecbots.utils.BotUtils;
import com.bottest.scripts.altrecbots.ThreadPoolManager;
import com.bottest.scripts.altrecbots.model.ActionPlaybackContext;

import java.io.Serializable;
import java.util.Optional;

import l2.commons.lang.reference.HardReference;
import l2.commons.threading.RunnableImpl;
import l2.gameserver.Config;
import l2.gameserver.model.Player;
import l2.gameserver.model.instances.NpcInstance;
import l2.gameserver.utils.Location;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class MoveToLocation extends Action<MoveToLocation> implements Serializable
{
	private Location location;
	private int offset;
	private boolean pathfinding;
	private Integer targetNpc;
	private Location fromLocation;

	public MoveToLocation()
	{
	}

	public MoveToLocation(Location location, int offset, boolean pathfinding)
	{
		this.location = location;
		this.offset = offset;
		this.pathfinding = pathfinding;
	}

	public Location getLocation()
	{
		return location;
	}

	public MoveToLocation setLocation(Location location)
	{
		this.location = location;
		return this;
	}

	public int getOffset()
	{
		return offset;
	}

	public MoveToLocation setOffset(int offset)
	{
		this.offset = offset;
		return this;
	}

	public Integer getTargetNpc()
	{
		return targetNpc;
	}

	public MoveToLocation setTargetNpc(Integer targetNpc)
	{
		this.targetNpc = targetNpc;
		return this;
	}

	public Location getFromLocation()
	{
		return fromLocation;
	}

	public MoveToLocation setFromLocation(Location fromLocation)
	{
		this.fromLocation = fromLocation;
		return this;
	}

	public boolean isPathfinding()
	{
		return pathfinding;
	}

	public MoveToLocation setPathfinding(boolean pathfinding)
	{
		this.pathfinding = pathfinding;
		return this;
	}

	@Override
	public MoveToLocation fromLegacy(int[] actions)
	{
		return setLocation(new Location(actions[0], actions[1], actions[2] + Config.CLIENT_Z_SHIFT).correctGeoZ()).setOffset(Math.min(actions[3], 150)).setPathfinding(actions[4] != 0);
	}

	@Override
	public boolean doItImpl(AltRecBot recBot, ActionPlaybackContext playbackContext)
	{
		NpcInstance npc = getTargetNpc() != null ? BotUtils.setMyTargetByNpcId(recBot, getTargetNpc()) : null;
		if(recBot.isSitting())
		{
			recBot.standUp();
		}
		final HardReference<Player> playerRef = recBot.getRef();
		RunnableImpl r = new RunnableImpl()
		{
            @Override
			public void runImpl() throws Exception
			{
				Player player = playerRef.get();
				if(player == null)
					return;

				Location location = getLocation().clone();
				int offset = getOffset();
				if(npc != null)
				{
					location = npc.getLoc().clone();
					offset = Math.max(32, npc.getActingRange() - 16);
				}

				Location finalDestination = player.getFinalDestination();
				if(player.isMoving() && finalDestination.equalsGeo(location))
					return;

				player.moveToLocation(location, offset, true);
			}
		};

		if(!recBot.isInPeaceZone())
		{
			playbackContext.finish();
			return false;
		}

		if(recBot.isAttackingNow() || recBot.isCastingNow())
		{
			long animationEndTime = recBot.getAnimationEndTime();
			if(animationEndTime > 0)
			{
				ThreadPoolManager.getInstance().schedule(r, Math.max(333, animationEndTime - System.currentTimeMillis()));
			}
			return false;
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

		AltRecBot recBot = optBot.get();
		if(recBot.isCastingNow() && recBot.getAnimationEndTime() > 0L)
		{
			return super.getDuration(playbackContext) + Math.max(333L, recBot.getAnimationEndTime() - System.currentTimeMillis());
		}
		return super.getDuration(playbackContext);
	}

	@Override
	public ActionType getActionType()
	{
		return ActionType.MOVE_TO_LOCATION;
	}

	@Override
	public String toString()
	{
		return "MoveToLocationParams{location=" + location + ", offset=" + offset + ", pathfinding=" + pathfinding + '}';
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
			return true;

		if(o == null || getClass() != o.getClass())
			return false;

		MoveToLocation moveToLocation = (MoveToLocation) o;
		return new EqualsBuilder().append(offset, moveToLocation.offset).append(pathfinding, moveToLocation.pathfinding).append(location, moveToLocation.location).isEquals();
	}

	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(17, 37).append(location).append(offset).append(pathfinding).toHashCode();
	}
}