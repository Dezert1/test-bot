package com.bottest.scripts.altrecbots.model;

import com.bottest.scripts.altrecbots.utils.BotUtils;
import l2.gameserver.model.GameObjectsStorage;
import l2.gameserver.model.Player;

/**
 * Этот код определяет перечисление BotSpawnStrategy, которое содержит два значения:
 *
 * Constant - стратегия, которая определяет необходимое количество заспавненных ботов на основе переданного аргумента (первого элемента массива строк args).
 * Необходимое количество рассчитывается как разность между этим аргументом и текущим количеством заспавненных ботов, которое может быть получено через метод getSpawnCounter() класса BotSpawnManager.
 *
 * OnlinePercent - стратегия, которая определяет необходимое количество заспавненных ботов на основе процента онлайн-игроков и количества уже заспавненных ботов.
 * Аргументом этой стратегии является процент, на основе которого рассчитывается необходимое количество ботов.
 * Необходимое количество рассчитывается как разность между количеством онлайн-игроков, умноженным на процент (первый элемент массива строк args, переданный при вызове метода getSpawnNeeded()), и количеством уже заспавненных ботов.
 */
public enum BotSpawnStrategy
{
	Constant
    {
        @Override
        public int getSpawnNeeded(String[] args)
        {
            return Math.max(0, Integer.parseInt(args[0]) - BotSpawnManager.getInstance().getSpawnCounter().get());
        }
    },
    OnlinePercent
    {
        @Override
        public int getSpawnNeeded(String[] args)
        {
            int onlinePlayers = 0;
            int onlineBots = 0;
            double mult = Double.parseDouble(args[0]) / 100.;
            for(Player player : GameObjectsStorage.getAllPlayersForIterate())
            {
                if(player != null)
                {
                    if(BotUtils.isBot(player))
                        onlineBots++;
                    else
                        onlinePlayers++;
                }
            }
            return (int) Math.max(0., onlinePlayers * mult - onlineBots);
        }
    };

    /**
     * Метод getSpawnNeeded() определен в каждом значении перечисления и возвращает необходимое количество заспавненных ботов, рассчитанное на основе соответствующей стратегии и переданных аргументов.
     * @param args
     * @return
     */
	public abstract int getSpawnNeeded(String[] args);
}