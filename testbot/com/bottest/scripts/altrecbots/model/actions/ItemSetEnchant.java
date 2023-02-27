package com.bottest.scripts.altrecbots.model.actions;

import com.bottest.scripts.altrecbots.model.AltRecBot;
import com.bottest.scripts.altrecbots.Config;
import com.bottest.scripts.altrecbots.model.ActionPlaybackContext;

import java.io.Serializable;

import l2.gameserver.model.items.ItemInstance;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ItemSetEnchant extends Action<ItemSetEnchant> implements Serializable
{
	private int itemId;
	private int enchant;

	public ItemSetEnchant()
	{
	}

	public ItemSetEnchant(int itemId, int enchant)
	{
		this.itemId = itemId;
		this.enchant = enchant;
	}

	public int getItemId()
	{
		return itemId;
	}

	public ItemSetEnchant setItemId(int itemId)
	{
		this.itemId = itemId;
		return this;
	}

	public int getEnchant()
	{
		return enchant;
	}

	public ItemSetEnchant setEnchant(int enchant)
	{
		this.enchant = enchant;
		return this;
	}

	@Override
	public ItemSetEnchant fromLegacy(int[] actions)
	{
		return setItemId(actions[0]).setEnchant(Math.min(actions[1], l2.gameserver.Config.ENCHANT_MAX));
	}

	@Override
	public boolean doItImpl(AltRecBot recBot, ActionPlaybackContext playbackContext)
	{
		if(ArrayUtils.contains(Config.PLAYBACK_IGNORED_ITEM_IDS, getItemId()))
			return true;

		ItemInstance item = recBot.getInventory().getItemByItemId(getItemId());
		if(item == null)
			return true;

		item.setEnchantLevel(Math.min(Config.BOT_ITEM_ENCHANT_ANIMATE_LIMIT, getEnchant()));
		useShots(recBot);
		recBot.broadcastCharInfo();
		return true;
	}

	@Override
	public ActionType getActionType()
	{
		return ActionType.ITEM_SET_ENCHANT;
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
			return true;

		if(o == null || getClass() != o.getClass())
			return false;

		ItemSetEnchant itemSetEnchant = (ItemSetEnchant) o;
		return new EqualsBuilder().append(itemId, itemSetEnchant.itemId).append(enchant, itemSetEnchant.enchant).isEquals();
	}

	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(17, 37).append(itemId).append(enchant).toHashCode();
	}

	@Override
	public String toString()
	{
		return "ItemSetEnchantParams{itemId=" + itemId + ", enchant=" + enchant + '}';
	}
}