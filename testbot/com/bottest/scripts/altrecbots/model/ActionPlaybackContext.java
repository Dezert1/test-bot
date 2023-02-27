package com.bottest.scripts.altrecbots.model;

import com.bottest.scripts.altrecbots.Config;
import com.bottest.scripts.altrecbots.model.actions.Action;
import com.bottest.scripts.altrecbots.ThreadPoolManager;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import l2.commons.threading.RunnableImpl;
import l2.commons.util.Rnd;
import l2.gameserver.GameServer;
import l2.gameserver.instancemanager.ReflectionManager;
import l2.gameserver.model.Party;
import l2.gameserver.model.Player;
import l2.gameserver.model.Skill;
import l2.gameserver.model.World;
import l2.gameserver.model.chat.ChatFilters;
import l2.gameserver.model.chat.chatfilter.ChatFilter;
import l2.gameserver.network.l2.components.ChatType;
import l2.gameserver.network.l2.s2c.MagicSkillLaunched;
import l2.gameserver.network.l2.s2c.MagicSkillUse;
import l2.gameserver.network.l2.s2c.Say2;

/**
 * Данный класс ActionPlaybackContext является контекстом проигрывания действий бота в игре.
 * Он содержит информацию о записи действий, которые должны быть воспроизведены, и используется для выполнения каждого действия и планирования следующего действия в соответствии с задержкой и длительностью.
 */
public class ActionPlaybackContext extends BaseContext<AltRecBot>
{
    // Запись действий, которые должны быть воспроизведены
	private final ActionRecord actionRecord;
    // Индекс текущего воспроизводимого действия
	private final AtomicInteger actionIdx = new AtomicInteger(0);
    // Время создания контекста
	private final long createdAt;
    // Объект блокировки, используемый для синхронизации выполнения действий
	private final ReentrantLock lock = new ReentrantLock();
    // Имя контекста
	private final String name;
	// Объект ScheduledFuture, представляющий запланированное выполнение следующего действия
	private ScheduledFuture<?> nextActionFuture;
    // Флаг, указывающий, завершен ли контекст
	private volatile boolean isFinished = false;
	// Текущее выполняемое действие
	private volatile Action<?> currentAction;
    // Время последнего разговора бота.
	private volatile long lastTalk;

	public ActionPlaybackContext(AltRecBot recBot, ActionRecord actionRecord, String name)
	{
		// TODO [V] - пришлось поменять, ибо тут ошибка
		//super(recBot.getRef());
		super(recBot);

		this.name = name;
		this.actionRecord = actionRecord;

		recBot.setPlaybackContext(this);
		createdAt = System.currentTimeMillis();
	}

    /**
     * Метод возвращает имя контекста
     * @return
     */
	public String getName()
	{
		return name;
	}

    /**
     * Метод возвращает запись действий, которые должны быть воспроизведены
     * @return
     */
	public ActionRecord getActionRecord()
	{
		return actionRecord;
	}

    /**
     * Метод возвращает индекс текущего воспроизводимого действия
     * @return
     */
	public AtomicInteger getActionIdx()
	{
		return actionIdx;
	}

    /**
     * Метод возвращает время создания контекста
     * @return
     */
	public long getCreatedAt()
	{
		return createdAt;
	}

    /**
     * Возвращает true, если есть еще действия, которые нужно воспроизвести
     * @return
     */
	public boolean haveNext()
	{
		return actionIdx.get() < actionRecord.getActions().size();
	}

    /**
     * Инициирует проигрывание записи действий, возвращая true, если операция была успешной
     * @return
     */
	public boolean initiate()
	{
		Optional<AltRecBot> playerOpt = getPlayer();
		if(!playerOpt.isPresent())
			return false;

		AltRecBot recBot = playerOpt.get();
		if(recBot.isDeleted())
			return false;

		Party party = recBot.getParty();
		if(party != null)
			recBot.leaveParty();

		recBot.setCurrentHpMp(recBot.getMaxHp(), recBot.getMaxMp());

		if(!Config.BOT_INITIAL_EFFECTS.isEmpty())
			for(Skill skill : Config.BOT_INITIAL_EFFECTS)
				skill.getEffects(recBot, recBot, false, false);

		if(!recBot.isVisible())
		{
			recBot.setLoc(actionRecord.getLocationRandomized());
			recBot.spawnMe();
			actionIdx.set(0);
			return scheduleNextAction();
		}

		if(actionRecord.getLocation().distance(recBot.getLoc()) > (Config.PLAYBACK_SPAWN_POS_RANDOM_RADIUS + 32))
		{
			if(recBot.isInPeaceZone())
			{
				recBot.decayMe();
				recBot.spawnMe(actionRecord.getLocationRandomized());
				actionIdx.set(0);
				return scheduleNextAction();
			}

			MagicSkillUse msu = new MagicSkillUse(recBot, recBot, 2213, 1, 20000, 0L);
			recBot.broadcastPacket(msu);

			ThreadPoolManager.getInstance().schedule(new RunnableImpl()
			{
			    @Override
				public void runImpl() throws Exception
				{
					Optional<AltRecBot> optionalPlayer = getPlayer();
					if(!optionalPlayer.isPresent())
						return;
					AltRecBot bot = optionalPlayer.get();
					bot.broadcastPacket(new MagicSkillLaunched(bot, 2213, 1, bot));
					bot.teleToLocation(actionRecord.getLocationRandomized().correctGeoZ(), ReflectionManager.DEFAULT);
					bot.onTeleported();
					actionIdx.set(0);
					scheduleNextAction();
				}
			}, 20000L);
		}
		else
		{
			actionIdx.set(0);
			return scheduleNextAction();
		}
		return true;
	}

    /**
     * Возвращает текущее выполняемое действие
     * @return
     */
	public Action<?> getCurrentAction()
	{
		return currentAction;
	}

    /**
     * Завершает проигрывание записи действий через указанное время
     * @param delay : время
     */
	public void finish(long delay)
	{
		isFinished = true;

		BotSpawnManager.getInstance().onPlaybackFinished(this);

		Optional<AltRecBot> playerOpt = getPlayer();
		if(!playerOpt.isPresent())
			return;

		AltRecBot altRecBot = playerOpt.get();
		if(altRecBot.isCastingNow())
			altRecBot.abortCast(true, false);
		if(altRecBot.isMoving())
			altRecBot.stopMove();

		Party party = altRecBot.getParty();
		if(party != null)
			altRecBot.leaveParty();

		RunnableImpl r = new RunnableImpl()
		{
		    @Override
			public void runImpl() throws Exception
			{
				AltRecBot altRecBot = playerOpt.get();
				if(altRecBot == null)
					return;
				altRecBot.stopAllTimers();
				altRecBot.getInventory().clear();
				altRecBot.setIsOnline(false);
				altRecBot.deleteMe();
			}
		};

		if(delay > 0)
			ThreadPoolManager.getInstance().schedule(r, delay);
		else
			r.run();
	}

    /**
     * Завершает проигрывание записи действий
     */
	public void finish()
	{
		finish(Rnd.get(Config.BOTS_UNSPAWN_INTERVAL_MIN, Config.BOTS_UNSPAWN_INTERVAL_MAX));
	}

    /**
     * Останавливает запланированное выполнение следующего действия
     */
	public void stopNextActionTimer()
	{
		ScheduledFuture<?> stopNextActionTask = nextActionFuture;
		if(stopNextActionTask != null)
		{
			nextActionFuture = null;
			stopNextActionTask.cancel(false);
		}
	}

    /**
     * Планирует выполнение следующего действия в соответствии с задержкой и длительностью, возвращая true, если операция была успешной.
     * @return
     */
	public boolean scheduleNextAction()
	{
		if(isFinished)
			return false;

		int actionId = actionIdx.getAndIncrement();
		long currentTime = System.currentTimeMillis();

		if(GameServer.getInstance().getPendingShutdown().get() || !Config.BOTS_ENABLED || currentTime - createdAt >= Config.BOT_TTL)
		{
			finish();
			return false;
		}

		if(actionId >= actionRecord.getActions().size())
		{
			if(!actionRecord.getActions().isEmpty())
			{
				if(!Config.LOOP_PLAYBACK)
				{
					finish();
					return false;
				}
				return initiate();
			}
			finish();
			return false;
		}

		Action action = actionRecord.getActions().get(actionId);

		long durationAction = action.getDuration(this);
		if(actionId == 0)
			durationAction += Rnd.get(Config.BOTS_FIRST_ACTION_MIN, Config.BOTS_FIRST_ACTION_MAX);
		long delay = Math.max(32, Math.min(durationAction, Config.BOT_TTL - (currentTime - createdAt)));
		nextActionFuture = ThreadPoolManager.getInstance().schedule(new ActionRunner(this, action), delay);

		if(delay > 1000 && currentTime - lastTalk > Config.PHRASE_REUSE_TIME)
		{
			boolean chance = false;
			if(Rnd.chance(Config.BOT_TALK_CHANCE) || (chance = Rnd.chance(Config.BOT_TALK_CHANCE_SHOUT)))
			{
				lastTalk = currentTime;
				ThreadPoolManager.getInstance().schedule(new BotSpeak(this, chance), delay / 2);
			}
		}
		return true;
	}

    /**
     * Используется для выполнения действий
     */
    private static class ActionRunner extends RunnableImpl
	{
		private final ActionPlaybackContext playbackCtx;
		private final Action<?> action;

		private ActionRunner(ActionPlaybackContext playbackCtx, Action<?> action)
		{
			this.playbackCtx = playbackCtx;
			this.action = action;
		}

		@Override
		public void runImpl() throws Exception
		{
			playbackCtx.lock.lock();
			try
			{
				playbackCtx.currentAction = action;
				try
				{
					Optional<AltRecBot> optPlayer = playbackCtx.getPlayer();
					if(!optPlayer.isPresent())
						return;
					AltRecBot recBot = optPlayer.get();
					action.doIt(recBot, playbackCtx);
				}
				finally
				{
					playbackCtx.scheduleNextAction();
				}
			}
			finally
			{
				playbackCtx.lock.unlock();
			}
		}
	}

    /**
     * Используется для речи бота в чате игры.
     */
	public static class BotSpeak extends RunnableImpl
	{
		private final ActionPlaybackContext playbackCtx;
		private final boolean isShout;

		public BotSpeak(ActionPlaybackContext playbackCtx, boolean isShout)
		{
			this.playbackCtx = playbackCtx;
			this.isShout = isShout;
		}

		@Override
		public void runImpl() throws Exception
		{
			Optional<AltRecBot> playerOpt = playbackCtx.getPlayer();
			if(playbackCtx.isFinished || !playerOpt.isPresent())
				return;

			AltRecBot recBot = playerOpt.get();
			Optional<BotPhrase> optionalPhrase = BotPhrasePool.getInstance().findPhrase(recBot.getSex(), recBot.getLoc());
			if(!optionalPhrase.isPresent())
				return;

			BotPhrase phrase = optionalPhrase.get();
			ChatType chatType = isShout ? ChatType.SHOUT : ChatType.ALL;
			for(ChatFilter filter : ChatFilters.getinstance().getFilters())
			{
				if(filter.isMatch(recBot, chatType, phrase.getText(), null) && filter.getAction() != ChatFilter.ACTION_NONE)
					return;
			}

			Say2 cs = new Say2(recBot.getObjectId(), chatType, recBot.getName(), phrase.getText());
			List<Player> players = World.getAroundPlayers(recBot);
			if(players != null)
            {
                for(Player player : players)
                {
                    if(player == recBot || player.getReflection() != recBot.getReflection() || player.isBlockAll() || player.isInBlockList(recBot))
                        continue;
                    player.sendPacket(cs);
                }
            }
		}
	}
}