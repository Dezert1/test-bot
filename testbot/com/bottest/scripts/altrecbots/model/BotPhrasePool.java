package com.bottest.scripts.altrecbots.model;

import com.bottest.scripts.altrecbots.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import l2.commons.util.RandomUtils;
import l2.gameserver.model.World;
import l2.gameserver.utils.Location;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Класс BotPhrasePool представляет собой пул фраз бота и содержит методы для поиска фраз и загрузки фраз в пул.
 */
public class BotPhrasePool
{
	private static final BotPhrasePool instance = new BotPhrasePool();

	private List<BotPhrasePoolRecord> records = new ArrayList<BotPhrasePoolRecord>();

	private BotPhrasePool()
	{

	}

	public static BotPhrasePool getInstance()
	{
		return instance;
	}

	private static int regionX(int x)
	{
		return (x >> World.SHIFT_BY) + World.OFFSET_X;
	}

	private static int regionY(int y)
	{
		return (y >> World.SHIFT_BY) + World.OFFSET_Y;
	}

    /**
     * findPhrase0(int findSex, Location targetLoc): вспомогательный метод для поиска фразы в пуле по полу и местоположению. Возвращает фразу в виде объекта Optional<BotPhrase>.
     * @param findSex
     * @param targetLoc
     * @return
     */
	private Optional<BotPhrase> findPhrase0(int findSex, Location targetLoc)
	{
		long currentTime = System.currentTimeMillis();

		List<Pair<BotPhrasePoolRecord, Long>> phrasesWithoutLoc = new ArrayList<Pair<BotPhrasePoolRecord, Long>>(records.size());
		List<Pair<BotPhrasePoolRecord, Long>> phrasesWithLoc = new ArrayList<Pair<BotPhrasePoolRecord, Long>>(records.size());

		int targetRegionX = regionX(targetLoc.getX());
		int targetRegionY = regionY(targetLoc.getY());

		for(BotPhrasePoolRecord record : records)
		{
			long lastUse = record.getLastUse();
			if(Math.max(0L, currentTime - lastUse) < Config.PHRASE_REUSE_TIME)
				continue;
			BotPhrase botPhrase = record.getPhrase();
			if(botPhrase.getSex().isPresent() && !Objects.equals(botPhrase.getSex().get(), findSex))
				continue;
			Optional<Location> phraseLoc = botPhrase.getLoc();
			if(phraseLoc.isPresent())
			{
				Location loc = phraseLoc.get();

				if(regionX(loc.getX()) != targetRegionX || regionY(loc.getY()) != targetRegionY)
					continue;

			    phrasesWithLoc.add(Pair.of(record, lastUse));
			}
			else
			    phrasesWithoutLoc.add(Pair.of(record, lastUse));
		}

		Collections.shuffle(phrasesWithoutLoc);

		List<Pair<Pair<BotPhrasePoolRecord, Long>, Double>> allAvailablePhrases = new ArrayList<Pair<Pair<BotPhrasePoolRecord, Long>, Double>>(phrasesWithoutLoc.size() + phrasesWithLoc.size());
		double totalProbability = 0.;
		if(!phrasesWithLoc.isEmpty())
		{
			// TODO [V] - пересмотреть
			Collections.sort(phrasesWithLoc, (loc1, loc2) -> -Double.compare(targetLoc.distance(loc1.getLeft().getPhrase().getLoc().get()), targetLoc.distance(loc2.getLeft().getPhrase().getLoc().get())));

			for(int i = 0; i < phrasesWithLoc.size(); i++)
			{
				Pair<BotPhrasePoolRecord, Long> phrasePair = phrasesWithLoc.get(i);
				double probability = 1. / i;

				totalProbability += probability;
				allAvailablePhrases.add(Pair.of(phrasePair, probability));
				if(i < phrasesWithoutLoc.size())
				{
					allAvailablePhrases.add(Pair.of(phrasesWithoutLoc.get(i), probability));
					totalProbability += probability;
				}
			}
		}
		else
		{
			for(int i = 0; i < phrasesWithoutLoc.size(); i++)
				allAvailablePhrases.add(Pair.of(phrasesWithoutLoc.get(i), 1. / i));
		}

		if(allAvailablePhrases.isEmpty())
			return Optional.empty();

		Pair<BotPhrasePoolRecord, Long> randomPhrase = RandomUtils.pickRandomSortedGroup(allAvailablePhrases, totalProbability);
		if(randomPhrase != null)
		{
			BotPhrasePoolRecord record = randomPhrase.getLeft();
			long lastUseTime = randomPhrase.getRight();
			if(record.casLastUse(lastUseTime, currentTime))
			{
				BotPhrase botPhrase = record.getPhrase();
				return Optional.of(botPhrase);
			}
		}

		return Optional.empty();
	}

    /**
     * Метод для загрузки фраз в пул, используется метод класса ActionsStorageManager, который загружает фразы из файла.
     */
	public void loadPhrases()
	{
		List<BotPhrasePoolRecord> phraseRecords = new ArrayList<BotPhrasePoolRecord>();
		for(BotPhrase botPhrase : ActionsStorageManager.getInstance().loadPhrases())
			phraseRecords.add(new BotPhrasePoolRecord(botPhrase));
		records = phraseRecords;
	}

    /**
     * Метод для поиска фразы в пуле по полу и местоположению. Возвращает фразу в виде объекта Optional<BotPhrase>
     * @param sex
     * @param loc
     * @return
     */
	public Optional<BotPhrase> findPhrase(int sex, Location loc)
	{
		return findPhrase0(sex, loc);
	}

    /**
     * Внутренний класс BotPhrasePoolRecord представляет собой запись в пуле фраз и содержит информацию о фразе и времени ее последнего использования.
     * Он используется в списке records в классе BotPhrasePool.
     */
	private static final class BotPhrasePoolRecord
	{
		private final BotPhrase phrase;
		private final AtomicLong lastUse = new AtomicLong(0L);

		private BotPhrasePoolRecord(BotPhrase phrase)
		{
			this.phrase = phrase;
		}

        /**
         * Метод для получения объекта BotPhrase, связанного с данной записью.
         * @return
         */
		public BotPhrase getPhrase()
		{
			return phrase;
		}

        /**
         * Метод для получения времени последнего использования фразы.
         * @return
         */
		public long getLastUse()
		{
			return lastUse.get();
		}

        /**
         * Метод для обновления времени последнего использования фразы.
         * Обновление происходит только в том случае, если текущее время больше последнего времени использования фразы.
         * @param oldTimeUse
         * @param newTimeUse
         * @return
         */
		public boolean casLastUse(long oldTimeUse, long newTimeUse)
		{
			return lastUse.compareAndSet(oldTimeUse, newTimeUse);
		}

		@Override
		public boolean equals(Object o)
		{
			if(this == o)
				return true;
			if(o == null || getClass() != o.getClass())
				return false;
			BotPhrasePoolRecord record = (BotPhrasePoolRecord) o;
			return new EqualsBuilder().append(lastUse, record.lastUse).append(phrase, record.phrase).isEquals();
		}

		@Override
		public int hashCode()
		{
			return new HashCodeBuilder(17, 37).append(phrase).append(lastUse).toHashCode();
		}

		@Override
		public String toString()
		{
			return "BotPhrasePoolRecord{phrase=" + phrase + ", lastUse=" + lastUse + '}';
		}
	}
}