package com.bottest.scripts.altrecbots.model.actions;

import com.bottest.scripts.altrecbots.model.AltRecBot;
import com.bottest.scripts.altrecbots.Config;
import com.bottest.scripts.altrecbots.model.ActionPlaybackContext;

import java.io.Serializable;

import org.apache.commons.lang3.ArrayUtils;

public class UseItem extends Action<UseItem> implements Serializable
{
	private int itemId;

	public UseItem()
	{
	}

	public UseItem(int itemId)
	{
		this.itemId = itemId;
	}

	public int getItemId()
	{
		return itemId;
	}

	public UseItem setItemId(int itemId)
	{
		this.itemId = itemId;
		return this;
	}

	@Override
	public UseItem fromLegacy(int[] actions)
	{
		itemId = actions[1];
		return this;
	}

	@Override
	public boolean doItImpl(AltRecBot recBot, ActionPlaybackContext playbackContext)
	{
		if(ArrayUtils.contains(Config.PLAYBACK_IGNORED_ITEM_IDS, getItemId()))
			return true;

		useShots(recBot);
		return true;
	}

	@Override
	public ActionType getActionType()
	{
		return ActionType.USE_ITEM;
	}
}