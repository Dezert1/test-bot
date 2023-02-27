package com.bottest.scripts.altrecbots;

import com.bottest.scripts.altrecbots.model.BaseContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import l2.gameserver.model.Player;

/**
 * Класс ContextHolder представляет собой обертку над Map, позволяющую хранить и получать контексты по игрокам в многопоточной среде.
 * ContextHolder предоставляет механизм хранения и доступа к контексту игрока по его идентификатору (objectId), который является уникальным для каждого игрока в игре.
 * Контекст используется для хранения различных данных и состояний игрока, например, информации о текущем состоянии задачи, выполненных действиях и т.д.
 * @param <CTX>
 */
public class ContextHolder <CTX extends BaseContext>
{
	/** Блокировка для чтения/записи объектов из "кэша" */
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();

	// Объект Map для хранения контекстов игроков по objectId.
	private final Map<Integer, CTX> contextMap = new HashMap<Integer, CTX>();

	/**
	 * Метод получения контекста игрока. Возвращает Optional с контекстом игрока, если он был сохранен, иначе возвращает пустой Optional.
	 * @param player : игрок
	 * @return
	 */
	public Optional<CTX> getContext(Player player)
	{
		readLock.lock();
		try
		{
			Optional<CTX> ctx = Optional.ofNullable(contextMap.get(player.getObjectId()));
			return ctx;
		}
		finally
		{
			readLock.unlock();
		}
	}

	/**
	 * Метод добавления контекста игрока. Сохраняет контекст игрока по его objectId.
	 * @param player : игрок
	 * @param context : контекст который сохраняем
	 * @return
	 */
	public CTX addContext(Player player, CTX context)
	{
		writeLock.lock();
		try
		{
			contextMap.put(player.getObjectId(), Objects.requireNonNull(context));
            return context;
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Метод удаления контекста игрока по objectId.
	 * @param objectId
	 * @return
	 */
    @SuppressWarnings("unchecked")
    public CTX removeContext(int objectId)
	{
		writeLock.lock();
		try
		{
			BaseContext ctx = contextMap.remove(objectId);
			return (CTX) ctx;
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Метод удаления контекста игрока.
	 * @param player
	 * @return
	 */
	public CTX removeContext(Player player)
	{
		return removeContext(player.getObjectId());
	}
}