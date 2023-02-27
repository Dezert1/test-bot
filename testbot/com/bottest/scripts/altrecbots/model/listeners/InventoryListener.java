package com.bottest.scripts.altrecbots.model.listeners;

import com.bottest.scripts.altrecbots.model.ActionRecordingContext;

import java.util.Optional;

import l2.gameserver.listener.inventory.OnEquipListener;
import l2.gameserver.model.Playable;
import l2.gameserver.model.items.ItemInstance;

public class InventoryListener extends BasicPlayerListener implements OnEquipListener
{
	private static final InventoryListener instance = new InventoryListener();

	public static InventoryListener getInstance()
	{
		return instance;
	}

	@Override
	public void onEquip(int slot, ItemInstance item, Playable playable)
	{
		Optional<ActionRecordingContext> actionRef = getRecordingContext(playable);
		if(!actionRef.isPresent() || !playable.isPlayer())
			return;

		ActionRecordingContext context = actionRef.get();
		context.onEquip(item, playable.getPlayer());
	}

	@Override
	public void onUnequip(int slot, ItemInstance item, Playable playable)
	{
		Optional<ActionRecordingContext> actionRef = getRecordingContext(playable);
		if(!actionRef.isPresent() || !playable.isPlayer())
			return;

		ActionRecordingContext context = actionRef.get();
		context.onUnequip(slot, playable.getPlayer());
	}
}