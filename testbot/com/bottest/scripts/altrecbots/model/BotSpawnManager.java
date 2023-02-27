package com.bottest.scripts.altrecbots.model;

import com.bottest.scripts.altrecbots.utils.BotUtils;
import com.bottest.scripts.altrecbots.Config;
import com.bottest.scripts.altrecbots.ThreadPoolManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import l2.commons.threading.RunnableImpl;
import l2.commons.util.RandomUtils;
import l2.commons.util.Rnd;
import l2.gameserver.GameServer;
import l2.gameserver.dao.CharacterDAO;
import l2.gameserver.model.GameObjectsStorage;
import l2.gameserver.model.Player;
import l2.gameserver.model.base.Experience;
import l2.gameserver.model.items.ItemInstance;
import l2.gameserver.tables.SkillTable;
import l2.gameserver.templates.item.ItemTemplate;
import l2.gameserver.utils.Util;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Класс BotSpawnManager отвечает за создание и управление ботами на сервере.
 * В нем создаются экземпляры ботов и рассчитываются их параметры на основе ранее созданных шаблонов.
 * Класс содержит методы, используемые для взаимодействия с ботами, а также для управления их созданием.
 */
public class BotSpawnManager
{
    private static final Logger logger = LoggerFactory.getLogger(BotSpawnManager.class);

    private static final BotSpawnManager instance = new BotSpawnManager();

    // Карта которая содержит пулы для каждой профессии ботов.
    private final Map<Integer, ClassIdSpawnPool> classIdSpawnPools = new ConcurrentHashMap<Integer, ClassIdSpawnPool>();
    private final NamesPool maleNames = new NamesPool();
    private final NamesPool femaleNames = new NamesPool();
    private final NamesPool maleTitles = new NamesPool();
    private final NamesPool femaleTitles = new NamesPool();
    private final AtomicInteger spawnCounter = new AtomicInteger();
    private final AtomicInteger spawnPendingCounter = new AtomicInteger();

    private BotSpawnManager()
    {

    }

    public static BotSpawnManager getInstance()
    {
        return instance;
    }

    /**
     * Метод, который возвращает имя бота на основе параметров ActionRecord и удаления из списка неактуальных имён.
     * Если нет подходящего имени, возвращается пустое значение.
     * @param record
     * @return
     */
    private Optional<String> acquireName(ActionRecord record)
    {
        return record.getSex() != Player.PLAYER_SEX_MALE ? femaleNames.acquireRecord() : maleNames.acquireRecord();
    }

    /**
     * Метод, который возвращает титул бота на основе параметров ActionRecord и удаления из списка неактуальных титулов.
     * Если нет подходящего титула, возвращается пустое значение.
     * @param record
     * @return
     */
    private Optional<String> acquireTitle(ActionRecord record)
    {
        return record.getSex() != Player.PLAYER_SEX_MALE ? femaleTitles.acquireRecord() : maleTitles.acquireRecord();
    }

    /***
     * Метод, который освобождает имя бота из списка имён, если оно больше не используется.
     * @param name
     * @param record
     * @return
     */
    private boolean releaseName(String name, ActionRecord record)
    {
        return record.getSex() != Player.PLAYER_SEX_MALE ? femaleNames.releaseRecord(name) : maleNames.releaseRecord(name);
    }

    /**
     * Метод, который освобождает титул бота из списка титулов, если он больше не используется.
     * @param title
     * @param record
     * @return
     */
    private boolean releaseTitle(String title, ActionRecord record)
    {
        return record.getSex() != Player.PLAYER_SEX_MALE ? femaleTitles.releaseRecord(title) : maleTitles.releaseRecord(title);
    }

    /**
     * Метод, который создает экземпляр бота на основе параметров ActionRecord.
     * Возвращает Optional экземпляр класса ActionPlaybackContext, если бот успешно создан, иначе возвращает пустое значение.
     * @param actionRecord
     * @return
     */
    private Optional<ActionPlaybackContext> createContext(ActionRecord actionRecord)
    {
        Optional<String> optName = Optional.empty();
        int count = 0;
        do
        {
            Optional<String> acquireName = acquireName(actionRecord);
            if(!acquireName.isPresent())
                return Optional.empty();
            String name = acquireName.get();
            if(!Util.isMatchingRegexp(name, l2.gameserver.Config.CNAME_TEMPLATE))
                continue;
            if(Util.isMatchingRegexp(name.toLowerCase(), l2.gameserver.Config.CNAME_FORBIDDEN_PATTERN))
                continue;
            if(GameObjectsStorage.getPlayer(name) != null || CharacterDAO.getInstance().getObjectIdByName(name) > 0)
                continue;
            if(Stream.of(l2.gameserver.Config.CNAME_FORBIDDEN_NAMES).filter(forbiddenName -> StringUtils.equalsIgnoreCase(forbiddenName, name)).findAny().isPresent())
                continue;

            optName = Optional.of(name);
        }
        while(!optName.isPresent() && count++ < 10);

        if(!optName.isPresent())
            return Optional.empty();

        Optional<String> initialTitle = Optional.of(Config.INITIAL_BOTS_TITLE);
        if(actionRecord.isNoble() && Rnd.chance(Config.INDIVIDUAL_BOT_TITLE_CHANCE))
            initialTitle = acquireTitle(actionRecord);

        AltRecBotBuilder botBuilder = new AltRecBotBuilder().setAccountName(Config.BOT_ACCOUNT_NAME).setHairColor(actionRecord.getHairColor()).setHairStyle(actionRecord.getHairStyle()).setFace(actionRecord.getFace()).setSex(actionRecord.getSex()).setNoble(actionRecord.isNoble());
        botBuilder = botBuilder.setTitle(initialTitle.isPresent() ? initialTitle.get() : "");

        int lvl = l2.gameserver.Config.ALT_MAX_LEVEL;
        for(ActionRecord.SubclassRecord subclassRecord : actionRecord.getSubclasses())
        {
            botBuilder = botBuilder.addClassId(BotUtils.toClassId(subclassRecord.getClassId()).get(), subclassRecord.isBase(), subclassRecord.isActive());
            if(subclassRecord.isActive())
                lvl = Experience.getLevel(subclassRecord.getExp());
        }

        if(lvl > Config.BOTS_SPAWN_MAX_LEVEL || lvl < Config.BOTS_SPAWN_MIN_LEVEL)
        {
            if(optName.isPresent())
                releaseName((String) optName.get(), actionRecord);
            if(initialTitle.isPresent())
                releaseTitle(initialTitle.get(), actionRecord);
            return Optional.empty();
        }

        for(ActionRecord.ItemRecord itemRecord : actionRecord.getItems())
        {
            try
            {
                ItemTemplate template = BotUtils.getItemTemplate(itemRecord.getItemType()).orElse(null);
                if(template != null)
                    botBuilder.addItem(template, itemRecord.getEnchant(), itemRecord.getAmount(), itemRecord.isEquipped());
            }
            catch(Exception e)
            {}
        }

        String name = (String)optName.get();
        botBuilder.setName(name);
        botBuilder.setInitialLvl(lvl);

        AltRecBot recBot = botBuilder.build();
        recBot.setCurrentCp(0);
        recBot.setOnlineStatus(false);

        for(ActionRecord.SkillRecord skillRecord : actionRecord.getSkills())
            recBot.addSkill(SkillTable.getInstance().getInfo(skillRecord.getSkillId(), skillRecord.getSkillLevel()));
        recBot.setOnlineStatus(true);

        for(ActionRecord.ItemRecord itemRecord : actionRecord.getItems())
        {
            if(!itemRecord.isEquipped() || ArrayUtils.contains(Config.PLAYBACK_IGNORED_ITEM_IDS, itemRecord.getItemType()))
                continue;

            ItemInstance item = recBot.getInventory().getItemByItemId(itemRecord.getItemType());
            if(item != null && item.isEquipable())
                recBot.getInventory().equipItem(item);
        }
        recBot.spawnMe();
        return Optional.of(new ActionPlaybackContext(recBot, actionRecord, name));
    }

    /**
     * Метод, который инициализирует класс.
     * В нем загружаются все записи ActionRecord, и добавляются в пул соответствующего класса.
     * Также в методе загружаются имена и титулы ботов.
     */
    public void init()
    {
        List<ActionRecord> actionRecords = ActionsStorageManager.getInstance().getActionRecords();
        for(ActionRecord actionRecord : actionRecords)
            addActionRecord(actionRecord);
        maleNames.addAll(ActionsStorageManager.getInstance().loadNames(Player.PLAYER_SEX_MALE));
        femaleNames.addAll(ActionsStorageManager.getInstance().loadNames(Player.PLAYER_SEX_FEMALE));
        maleTitles.addAll(ActionsStorageManager.getInstance().loadTitles(Player.PLAYER_SEX_MALE));
        femaleTitles.addAll(ActionsStorageManager.getInstance().loadTitles(Player.PLAYER_SEX_FEMALE));
    }

    /**
     * Метод, который создает одного бота случайной профессии, используя пул для соответствующего класса.
     * @return
     */
    public boolean spawnOne()
    {
        Optional<ClassIdSpawnPool> randomClassIdSpawnPool = getRandomClassIdSpawnPool();
        if(!randomClassIdSpawnPool.isPresent())
            return false;
        ClassIdSpawnPool classIdSpawnPool = randomClassIdSpawnPool.get();
        Optional<ActionRecord> optRecord = classIdSpawnPool.acquireRecord();
        if(!optRecord.isPresent())
            return false;
        ActionRecord actionRecord = optRecord.get();
        Optional<ActionPlaybackContext> context = createContext(actionRecord);
        if(!context.isPresent())
        {
            classIdSpawnPool.releaseRecord(actionRecord);
            return false;
        }
        if(!context.get().initiate())
        {
            classIdSpawnPool.releaseRecord(actionRecord);
            return false;
        }
        spawnCounter.incrementAndGet();
        return true;
    }

    /**
     * Метод возвращает объект класса AtomicInteger, который используется для подсчета количества созданных ботов.
     * @return
     */
    public AtomicInteger getSpawnCounter()
    {
        return spawnCounter;
    }

    /**
     * Метод добавляет запись об экшене для класса ClassIdSpawnPool с использованием первого подкласса, содержащегося в переданном экшене.
     * Если первый подкласс отсутствует, метод просто возвращает управление.
     * В противном случае метод вычисляет или создает объект ClassIdSpawnPool для переданного класса персонажа и добавляет экшен в пул.
     * @param actionRecord
     */
    public void addActionRecord(ActionRecord actionRecord)
    {
        Optional<ActionRecord.SubclassRecord> baseSubclass = actionRecord.getBaseSubclass();
        if(!baseSubclass.isPresent())
            return;
        ActionRecord.SubclassRecord subclassRecord = baseSubclass.get();
        ClassIdSpawnPool classIdSpawnPool = classIdSpawnPools.computeIfAbsent(subclassRecord.getClassId(), classId -> new ClassIdSpawnPool(classId));
        classIdSpawnPool.add(actionRecord);
    }

    /**
     * Метод возвращает опциональный ClassIdSpawnPool с использованием алгоритма взвешенной случайной выборки.
     * Возвращаемый ClassIdSpawnPool должен содержать экшены, и его вероятность выборки должна быть настроена с помощью параметра PLAYBACK_SPAWN_CLASSID_PROBABILITY_MOD в файле конфигурации.
     * Если ни один из пулов не содержит экшены, метод возвращает Optional.empty().
     * @return
     */
    private Optional<ClassIdSpawnPool> getRandomClassIdSpawnPool()
    {
        List<Pair<ClassIdSpawnPool, Double>> availableClasses = new ArrayList<Pair<ClassIdSpawnPool, Double>>(classIdSpawnPools.size());
        double totalProbability = 0.;
        for(ClassIdSpawnPool classIdSpawnPool : classIdSpawnPools.values())
        {
            if(!classIdSpawnPool.isFilled())
                continue;

            double probability = Config.PLAYBACK_SPAWN_CLASSID_PROBABILITY_MOD.getOrDefault(classIdSpawnPool.getClassId(), 1.);
            availableClasses.add(Pair.of(classIdSpawnPool, probability));
            totalProbability += probability;
        }
        Collections.sort(availableClasses, RandomUtils.DOUBLE_GROUP_COMPARATOR);
        ClassIdSpawnPool randomClassId = RandomUtils.pickRandomSortedGroup(availableClasses, totalProbability);
        if(randomClassId == null)
            return Optional.empty();
        return Optional.of(randomClassId);
    }

    /**
     * Метод вызывается при завершении воспроизведения действия бота.
     * Метод освобождает используемое экшеном имя и возвращает экшен обратно в соответствующий пул ClassIdSpawnPool.
     * @param playbackContext
     */
    public void onPlaybackFinished(ActionPlaybackContext playbackContext)
    {
        ActionRecord actionRecord = playbackContext.getActionRecord();
        Optional<ActionRecord.SubclassRecord> baseSubclass = actionRecord.getBaseSubclass();
        if(!baseSubclass.isPresent())
            return;
        ActionRecord.SubclassRecord subclassRecord = baseSubclass.get();
        ClassIdSpawnPool classIdSpawnPool = classIdSpawnPools.computeIfAbsent(subclassRecord.getClassId(), classId -> new ClassIdSpawnPool(classId));
        if(classIdSpawnPool.releaseRecord(actionRecord))
            spawnCounter.decrementAndGet();
        releaseName(playbackContext.getName(), actionRecord);
    }

    /**
     * Метод осуществляет попытку создания новых ботов.
     * Если параметр BOTS_ENABLED равен false, метод не делает ничего.
     * В противном случае метод вычисляет разницу между запланированным количеством ботов и количеством текущих ботов и запускает один или несколько потоков для создания новых ботов.
     * Количество потоков равно разнице между запланированным количеством ботов и количеством текущих ботов.
     */
    public void trySpawn()
    {
        if(!Config.BOTS_ENABLED)
            return;

        int count = Config.BOT_COUNT_SUPPLIER.get();
        int diffCount = count - spawnPendingCounter.get();
        if(diffCount > 0)
        {
            logger.info("AltRecBots: Spawning {} bot(s)...", diffCount);
        }
        else
        {
            logger.info("AltRecBots: Skip spawning. Pending: {}.", spawnPendingCounter.get());
        }

        while(diffCount-- >= 0)
        {
            spawnPendingCounter.incrementAndGet();
            ThreadPoolManager.getInstance().schedule(new RunnableImpl()
            {
                @Override
                public void runImpl() throws Exception
                {
                    if(GameServer.getInstance().getPendingShutdown().get() || !Config.BOTS_ENABLED)
                        return;
                    try
                    {
                        spawnOne();
                    }
                    finally
                    {
                        spawnPendingCounter.decrementAndGet();
                    }
                }
            }, Rnd.get(Config.BOTS_SPAWN_INTERVAL_MIN, Config.BOTS_SPAWN_INTERVAL_MAX));
        }
    }

    /**
     * Метод удаляет переданный экшен бота из всех пулов ClassIdSpawnPool, если он существует, и возвращает true.
     * Если экшен не найден, метод возвращает false.
     * Экшен также удаляется из хранилища экшенов.
     * @param actionRecord
     * @return
     */
    public boolean deleteRecord(ActionRecord actionRecord)
    {
        int botId = actionRecord.getId().get();
        ActionsStorageManager.getInstance().deleteBotRecord(botId);
        for(ClassIdSpawnPool classIdSpawnPool : classIdSpawnPools.values())
        {
            if(classIdSpawnPool.deleteRecord(actionRecord))
                return true;
        }
        return false;
    }

    private static class NamesPool
    {
        private final Lock lock = new ReentrantLock();
        private final List<String> names = new LinkedList<String>();
        private final Set<String> inUse = new HashSet<String>();

        private NamesPool()
        {}

        public void addAll(List<String> nameList)
        {
            lock.lock();
            try
            {
                for(String name : nameList)
                {
                    if(inUse.contains(name) || names.contains(name))
                        continue;
                    names.add(name);
                }
            }
            finally
            {
                lock.unlock();
            }
        }

        public Optional<String> acquireRecord()
        {
            lock.lock();
            try
            {
                if(names.isEmpty())
                    return Optional.empty();
                String name = names.remove(0);
                inUse.add(name);
                names.remove(name);
                return Optional.of(name);
            }
            finally
            {
                lock.unlock();
            }
        }

        public boolean releaseRecord(String newName)
        {
            if(StringUtils.isBlank(newName))
                return false;
            lock.lock();
            try
            {
                if(!inUse.contains(newName))
                    return false;
                inUse.remove(newName);
                names.add(newName);
                return true;
            }
            finally
            {
                lock.unlock();
            }
        }
    }

    private static class ClassIdSpawnPool
    {
        private final int classId;
        private final Lock lock = new ReentrantLock();
        private final List<ActionRecord> records = new LinkedList<ActionRecord>();
        private final Set<ActionRecord> inUse = new HashSet<ActionRecord>();

        private ClassIdSpawnPool(int classId)
        {
            this.classId = classId;
        }

        public int getClassId()
        {
            return classId;
        }

        public void add(ActionRecord actionRecord)
        {
            if(!actionRecord.getId().isPresent())
                throw new IllegalArgumentException("Undefined 'id' of an action sequence");
            lock.lock();
            try
            {
                if(inUse.contains(actionRecord) || records.contains(actionRecord))
                    return;
                records.add(actionRecord);
            }
            finally
            {
                lock.unlock();
            }
        }

        public boolean isFilled()
        {
            return !records.isEmpty();
        }

        public Optional<ActionRecord> acquireRecord()
        {
            lock.lock();
            try
            {
                if(records.isEmpty())
                    return Optional.empty();

                List<Pair<ActionRecord, Double>> availableRecords = new ArrayList<Pair<ActionRecord, Double>>(records.size());
                double totalProbability = 0.;
                for(int i = 0; i < records.size(); i++)
                {
                    ActionRecord record = records.get(i);
                    if(!inUse.contains(record))
                    {
                        double probability = 1. / ((i / Config.PLAYBACK_SEQUENCE_SELECTOR_RANDOM_SLOPE_MOD) + 1.);
                        if(probability < 0.01)
                            break;
                        availableRecords.add(Pair.of(record, probability));
                        totalProbability += probability;
                    }
                }

                Collections.sort(availableRecords, RandomUtils.DOUBLE_GROUP_COMPARATOR);

                ActionRecord actionRecord = RandomUtils.pickRandomSortedGroup(availableRecords, totalProbability);
                if(actionRecord == null)
                    return Optional.empty();

                records.remove(actionRecord);
                inUse.add(actionRecord);
                return Optional.of(actionRecord);
            }
            finally
            {
                lock.unlock();
            }
        }

        public boolean releaseRecord(ActionRecord actionRecord)
        {
            if(actionRecord == null)
                return false;
            lock.lock();
            try
            {
                if(!inUse.contains(actionRecord))
                    return false;
                inUse.remove(actionRecord);
                records.add(actionRecord);
                return true;
            }
            finally
            {
                lock.unlock();
            }
        }

        public boolean deleteRecord(ActionRecord actionRecord)
        {
            if(actionRecord == null)
                return false;
            lock.lock();
            try
            {
                if(inUse.contains(actionRecord))
                {
                    inUse.remove(actionRecord);
                    return true;
                }
                if(records.contains(actionRecord))
                {
                    records.remove(actionRecord);
                    return true;
                }
                return false;
            }
            finally
            {
                lock.unlock();
            }
        }
    }
}