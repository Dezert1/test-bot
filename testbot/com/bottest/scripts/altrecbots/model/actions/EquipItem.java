package com.bottest.scripts.altrecbots.model.actions;

import com.bottest.scripts.altrecbots.model.AltRecBot;
import com.bottest.scripts.altrecbots.utils.BotUtils;
import com.bottest.scripts.altrecbots.Config;
import com.bottest.scripts.altrecbots.model.ActionPlaybackContext;

import java.io.Serializable;

import l2.gameserver.model.items.ItemInstance;
import l2.gameserver.templates.item.ItemTemplate;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class EquipItem extends Action<EquipItem> implements Serializable
{
	private int itemId;
	private Integer enchant;

	public EquipItem()
	{
	}

	public EquipItem(int itemId)
	{
		this.itemId = itemId;
	}

	public int getItemId()
	{
		return itemId;
	}

	public EquipItem setItemId(int itemId)
	{
		this.itemId = itemId;
		return this;
	}

	public Integer getEnchant()
	{
		return enchant;
	}

	public EquipItem setEnchant(Integer enchant)
	{
		this.enchant = enchant;
		return this;
	}

	@Override
	public EquipItem fromLegacy(int[] actions)
	{
		return setItemId(actions[0]).setEnchant(Math.min(actions[1], l2.gameserver.Config.ENCHANT_MAX));
	}

	@Override
	public boolean doItImpl(AltRecBot recBot, ActionPlaybackContext playbackContext)
	{
		ItemTemplate itemTemplate = BotUtils.getItemTemplate(getItemId()).orElse(null);
		if(itemTemplate == null || ArrayUtils.contains(Config.PLAYBACK_IGNORED_ITEM_IDS, itemTemplate.getItemId()))
			return true;

		ItemInstance item = recBot.getInventory().getItemByItemId(itemTemplate.getItemId());
		if(item != null && item.isEquipable())
		{
			recBot.getInventory().equipItem(item);
			useShots(recBot);
		}
		recBot.broadcastUserInfo(true);
		return true;
	}

	@Override
	public ActionType getActionType()
	{
		return ActionType.EQUIP_ITEM;
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
		EquipItem equipItem = (EquipItem) object;
		return new EqualsBuilder().append(itemId, equipItem.itemId).append(enchant, equipItem.enchant).isEquals();
	}

	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(17, 37).append(itemId).append(enchant).toHashCode();
	}

	@Override
	public String toString()
	{
		return "EquipItemParams{itemId=" + itemId + ", enchant=" + enchant + '}';
	}
}