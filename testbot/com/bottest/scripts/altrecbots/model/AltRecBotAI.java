package com.bottest.scripts.altrecbots.model;

import com.bottest.scripts.altrecbots.ThreadPoolManager;

import java.util.Objects;

import l2.commons.threading.RunnableImpl;
import l2.gameserver.ai.PlayerAI;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Класс AltRecBotAI наследуется от PlayerAI и является реализацией ИИ для AltRecBot(ботов)
 */
public class AltRecBotAI extends PlayerAI
{
	private volatile Pair<RunnableImpl, Long> arriveRunnableAndDelay;

	public AltRecBotAI(AltRecBot recBot)
	{
		super(recBot);
	}

	@Override
	public AltRecBot getActor()
	{
		return (AltRecBot) super.getActor();
	}

    /**
     * Метод устанавливает пару значений в поле arriveRunnableAndDelay.
     * Первый аргумент типа RunnableImpl является заданием, которое должно выполниться при прибытии бота в пункт назначения, а второй аргумент - задержка типа long, указывающая, через какое время после прибытия задание должно быть выполнено.
     * @param r
     * @param delay
     * @return
     */
	public AltRecBotAI setArriveRunnable(RunnableImpl r, long delay)
	{
		arriveRunnableAndDelay = Pair.of(r, delay);
		return this;
	}

    /**
     * Метод переопределен для выполнения задания, если оно было установлено через setArriveRunnable().
     * После того, как бот прибывает на место, пара значений в поле arriveRunnableAndDelay проверяется на наличие значения null.
     * Если пара содержит значения, то они удаляются из поля arriveRunnableAndDelay и запускается выполнение RunnableImpl, используя ThreadPoolManager для этого.
     * Если второй аргумент пары не равен 0, то задание будет выполнено через указанное время.
     */
	@Override
	protected void onEvtArrived()
	{
		super.onEvtArrived();
		Pair<RunnableImpl, Long> runnableAndDelay = arriveRunnableAndDelay;
		if(runnableAndDelay != null)
		{
			arriveRunnableAndDelay = null;
			if(Objects.equals(runnableAndDelay.getRight(), 0L))
				ThreadPoolManager.getInstance().execute(runnableAndDelay.getLeft());
			else
				ThreadPoolManager.getInstance().schedule(runnableAndDelay.getLeft(), runnableAndDelay.getRight());
		}
	}
}