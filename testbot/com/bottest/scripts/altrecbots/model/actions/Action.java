package com.bottest.scripts.altrecbots.model.actions;

import com.bottest.scripts.altrecbots.model.AltRecBot;
import com.bottest.scripts.altrecbots.model.ActionPlaybackContext;

import java.io.Serializable;

import l2.gameserver.model.Player;
import l2.gameserver.model.items.ItemInstance;
import l2.gameserver.templates.item.ItemTemplate;

/**
 * Класс Action является абстрактным базовым классом для всех возможных действий бота.
 * @param <SelfT>
 */
public abstract class Action <SelfT extends Action> implements Serializable
{
	private transient long duration;

    /**
     * Абстрактный метод, который должен быть реализован в каждом дочернем классе, и который конвертирует устаревшие (legacy) действия в объект данного класса.
     * @param actions
     * @return
     */
	public abstract SelfT fromLegacy(int[] actions);

    /**
     * Метод, который вызывает абстрактный doItImpl с передачей recBot и playbackContext.
     * @param recBot
     * @param playbackContext
     * @return
     */
	public boolean doIt(AltRecBot recBot, ActionPlaybackContext playbackContext)
	{
		return doItImpl(recBot, playbackContext);
	}

    /**
     * Абстрактный метод, который должен быть реализован в каждом дочернем классе, и который выполняет действие на боте, используя переданный playbackContext.
     * @param recBot
     * @param playbackContext
     * @return
     */
	public abstract boolean doItImpl(AltRecBot recBot, ActionPlaybackContext playbackContext);

    /**
     * Возвращает длительность данного действия.
     * @return
     */
	public long getDuration()
	{
		return duration;
	}

    /**
     * Устанавливает длительность данного действия и возвращает ссылку на объект этого действия.
     * @param duration
     * @return
     */
	public SelfT setDuration(long duration)
	{
		this.duration = duration;
		return (SelfT) this;
	}

    /**
     * Возвращает длительность данного действия с учетом переданного playbackContext.
     * @param playbackContext
     * @return
     */
	public long getDuration(ActionPlaybackContext playbackContext)
	{
		return this.duration;
	}

    /**
     * Возвращает тип данного действия.
     * @return
     */
	public abstract ActionType getActionType();

    /**
     * Проверяет наличие сосков у игрока и использует их.
     * @param player
     */
	protected void useShots(Player player)
	{
		for(ItemInstance item : player.getInventory().getItems())
		{
			if(item == null)
				continue;
			ItemTemplate template = item.getTemplate();
			if(template.isShotItem())
				player.addAutoSoulShot(template.getItemId());
		}
		player.autoShot();
	}
}