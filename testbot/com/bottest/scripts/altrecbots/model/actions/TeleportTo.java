package com.bottest.scripts.altrecbots.model.actions;

import com.bottest.scripts.altrecbots.model.AltRecBot;
import com.bottest.scripts.altrecbots.ThreadPoolManager;
import com.bottest.scripts.altrecbots.model.ActionPlaybackContext;

import java.io.Serializable;
import java.util.Optional;

import l2.commons.threading.RunnableImpl;
import l2.gameserver.Config;
import l2.gameserver.instancemanager.ReflectionManager;
import l2.gameserver.model.World;
import l2.gameserver.utils.Location;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class TeleportTo extends Action<TeleportTo> implements Serializable
{
	private Location location;

	public TeleportTo()
	{
	}

	public TeleportTo(Location location)
	{
		this.location = location;
	}

	public Location getLocation()
	{
		return location;
	}

	public TeleportTo setLocation(Location location)
	{
		this.location = location;
		return this;
	}

	@Override
	public TeleportTo fromLegacy(int[] actions)
	{
		return setLocation(new Location(actions[0], actions[1], actions[2] + Config.CLIENT_Z_SHIFT));
	}

	@Override
	public boolean doItImpl(AltRecBot recBot, final ActionPlaybackContext playbackContext)
	{
		int x = World.validCoordX(getLocation().getX());
		int y = World.validCoordY(getLocation().getY());
		int z = World.validCoordZ(getLocation().getZ());
		recBot.teleToLocation(x, y, z, ReflectionManager.DEFAULT);
		ThreadPoolManager.getInstance().schedule(new RunnableImpl()
		{
		    @Override
			public void runImpl() throws Exception
			{
				Optional<AltRecBot> optBot = playbackContext.getPlayer();
				if(!optBot.isPresent())
					return;
				optBot.get().onTeleported();
			}
		}, 1000L);
		return true;
	}

	@Override
	public long getDuration(ActionPlaybackContext playbackContext)
	{
		return super.getDuration(playbackContext) + 1000L;
	}

	@Override
	public ActionType getActionType()
	{
		return ActionType.TELEPORT_TO;
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
			return true;

		if(o == null || getClass() != o.getClass())
			return false;

		TeleportTo teleportTo = (TeleportTo) o;
		return new EqualsBuilder().append(location, teleportTo.location).isEquals();
	}

	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(17, 37).append(location).toHashCode();
	}

	@Override
	public String toString()
	{
		return "TeleportToParams{location=" + location + '}';
	}
}