package com.bottest.scripts.altrecbots.model.actions;

import com.bottest.scripts.altrecbots.model.AltRecBot;
import com.bottest.scripts.altrecbots.model.ActionPlaybackContext;

import java.io.Serializable;

import l2.gameserver.model.items.Inventory;
import l2.gameserver.model.items.ItemInstance;
import l2.gameserver.model.items.PcInventory;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class UnEquipItem extends Action<UnEquipItem> implements Serializable
{
	private int slot;

	public UnEquipItem()
	{
	}

	public UnEquipItem(int slot)
	{
		this.slot = slot;
	}

	public int getSlot()
	{
		return slot;
	}

	public UnEquipItem setSlot(int slot)
	{
		this.slot = slot;
		return this;
	}

	@Override
	public UnEquipItem fromLegacy(int[] actions)
	{
		int slopdIdx = PcInventory.getPaperdollIndex(actions[0]);
		if(slopdIdx >= 0)
			return setSlot(slopdIdx);
		return this;
	}

	@Override
	public boolean doItImpl(AltRecBot recBot, ActionPlaybackContext playbackContext)
	{
		AltRecBot.AltRecBotInventory inventory = recBot.getInventory();
		ItemInstance item = inventory.getPaperdollItem(Math.max(0, Math.min(getSlot(), Inventory.PAPERDOLL_MAX - 1)));
		if(item == null)
			return true;

		recBot.getInventory().unEquipItem(item);
		recBot.broadcastCharInfo();
		return true;
	}

	@Override
	public ActionType getActionType()
	{
		return ActionType.UNEQUIP_SLOT;
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
			return true;

		if(o == null || getClass() != o.getClass())
			return false;

		UnEquipItem unEquipItem = (UnEquipItem) o;
		return new EqualsBuilder().append(slot, unEquipItem.slot).isEquals();
	}

	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(17, 37).append(slot).toHashCode();
	}

	@Override
	public String toString()
	{
		return "UnEquipItemParams{slot=" + slot + '}';
	}
}