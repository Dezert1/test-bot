package com.bottest.scripts.altrecbots.model;

import java.util.Optional;

import l2.commons.lang.reference.HardReference;
import l2.gameserver.model.Player;

/**
 * Класс BaseContext<P> является абстрактным классом, который реализует общий функционал для контекстов игрока, которые используются в скриптах.
 * Контекст игрока представляет собой объект, который содержит информацию о состоянии игрока.
 * Он может использоваться в скриптах для передачи информации между различными функциями или заданиями, которые выполняются для игрока.
 * В данном классе используется параметризованный тип P, который ограничен типом Player из игрового движка для игры.
 * @param <P>
 * TODO [V] - пришлось переделать, потому что бьет ошибку HardReference<P> на просто P
 */
public abstract class BaseContext <P extends Player>
{
	private final P playerRef;

	protected BaseContext(P hardReference)
	{
		playerRef = hardReference;
	}

    /**
     * Метод, который возвращает объект игрока, связанный с данным контекстом.
     * Возвращает объект типа Optional, который может содержать или не содержать ссылку на игрока.
     * В данном методе используется метод get() для получения ссылки на игрока из поля playerRef.
     * @return
     */
	public Optional<P> getPlayer()
	{
		return Optional.ofNullable(playerRef);
	}
}