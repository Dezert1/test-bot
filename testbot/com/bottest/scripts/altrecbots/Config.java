package com.bottest.scripts.altrecbots;

import com.bottest.scripts.altrecbots.model.BotSpawnStrategy;
import com.bottest.scripts.altrecbots.utils.BotUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import l2.commons.configuration.ExProperties;
import l2.gameserver.model.Request.L2RequestType;
import l2.gameserver.model.Skill;
import l2.gameserver.tables.SkillTable;
import l2.gameserver.templates.item.ItemTemplate;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Класс хранения конфигов для ботов (настроек).
 */
public class Config
{
	private static final String ALT_REC_BOTS_FILE = "config/altrecbots.properties";

	public static boolean BOTS_ENABLED;
	public static boolean BOTS_LOG_ENABLE;
	public static boolean DEDICATED_EXECUTOR;
	public static int DEDICATED_EXECUTOR_THREADS;
	public static int DEDICATED_SCHEDULED_THREADS;
	public static boolean AUTO_RECORD_PLAYER_ACTIONS;
	public static int AUTO_RECORD_MIN_LVL;
	public static int AUTO_RECORD_MAX_LVL;
	public static boolean AUTO_RECORD_IGNORE_NOBLE;
	public static boolean AUTO_RECORD_IGNORE_HERO;
	public static boolean AUTO_RECORD_IGNORE_GM;
	public static String[] AUTO_RECORD_IGNORE_ZONES;
	public static boolean AUTO_RECORD_IGNORE_TELEPORT;
	public static int RECORD_MIN_LENGTH;
	public static int RECORD_MAX_LENGTH;
	public static long RECORD_MIN_DURATION;
	public static long RECORD_MAX_DURATION;
	public static boolean AUTO_RECORD_INSTANT_NEW_SEQUENCE;
	public static boolean LOOP_PLAYBACK;
	public static long BOT_TTL;
	public static int[] PLAYBACK_IGNORED_ITEM_IDS;
	public static Map<Integer, Double> PLAYBACK_SPAWN_CLASSID_PROBABILITY_MOD = new HashMap<Integer, Double>();
	public static int PLAYBACK_SEQUENCE_SELECTOR_RANDOM_SLOPE_MOD;
	public static double PLAYBACK_SEQUENCE_SELECTOR_RANDOM_THRESHOLD;
	public static int PLAYBACK_SPAWN_POS_RANDOM_RADIUS;
	public static String INITIAL_BOTS_TITLE;
	public static double INDIVIDUAL_BOT_TITLE_CHANCE;
	public static long BOTS_UNSPAWN_INTERVAL_MIN;
	public static long BOTS_UNSPAWN_INTERVAL_MAX;
	public static int BOTS_SPAWN_MIN_LEVEL;
	public static int BOTS_SPAWN_MAX_LEVEL;
	public static long BOTS_SPAWN_INTERVAL_MIN;
	public static long BOTS_SPAWN_INTERVAL_MAX;
	public static long BOTS_FIRST_ACTION_MIN;
	public static long BOTS_FIRST_ACTION_MAX;
	public static long BOTS_SPAWN_CHECK_INTERVAL;
	public static Map<L2RequestType, Double> BOT_ACCEPT_REQUEST_CHANCE;
	public static Map<L2RequestType, Double> BOT_DENY_REQUEST_CHANCE;
	public static long PHRASE_REUSE_TIME;
	public static double BOT_TALK_CHANCE;
	public static double BOT_TALK_CHANCE_SHOUT;
	public static String BOT_ACCOUNT_NAME;
	public static Supplier<Integer> BOT_COUNT_SUPPLIER;
	public static Map<ItemTemplate, Long> BOT_ADDITIONAL_INVENTORY_ITEMS;
	public static List<Skill> BOT_INITIAL_EFFECTS;
	public static int BOT_ITEM_ENCHANT_ANIMATE_LIMIT;
	public static int BOT_NPC_FIND_RADIUS;
	public static List<Pair<Integer, Integer>> BOT_MAGE_BUFF_ON_CHAR_CREATE;
	public static List<Pair<Integer, Integer>> BOT_WARRIOR_BUFF_ON_CHAR_CREATE;

	private Config()
	{
	}

	public static Supplier<Integer> parseStrategy(String strategyName, String strategyParam)
	{
		String[] strategyArgs = StringUtils.isBlank(strategyParam) ? ArrayUtils.EMPTY_STRING_ARRAY : Stream.of(StringUtils.split(strategyParam, ',')).map(StringUtils::trimToEmpty).toArray(String[]::new);
		BotSpawnStrategy spawnStrategy = BotSpawnStrategy.valueOf(strategyName);
		return () -> spawnStrategy.getSpawnNeeded(strategyArgs);
	}

	public static void load()
	{
		ExProperties botProperties = l2.gameserver.Config.load(ALT_REC_BOTS_FILE);

		BOTS_ENABLED = botProperties.getProperty("BotsEnabled", false);
		DEDICATED_EXECUTOR = botProperties.getProperty("UseDedicatedExecutor", true);
		DEDICATED_EXECUTOR_THREADS = botProperties.getProperty("DedicatedExecutorThreads", 1);
		DEDICATED_SCHEDULED_THREADS = botProperties.getProperty("DedicatedScheduledThreads", 2);
		AUTO_RECORD_PLAYER_ACTIONS = botProperties.getProperty("AutoRecordPlayerActions", false);
		AUTO_RECORD_MIN_LVL = botProperties.getProperty("AutoRecordMinLvl", 10);
		AUTO_RECORD_MAX_LVL = botProperties.getProperty("AutoRecordMaxLvl", 78);
		AUTO_RECORD_IGNORE_NOBLE = botProperties.getProperty("AutoRecordIgnoreNoble", false);
		AUTO_RECORD_IGNORE_HERO = botProperties.getProperty("AutoRecordIgnoreHero", true);
		AUTO_RECORD_IGNORE_GM = botProperties.getProperty("AutoRecordIgnoreGM", false);
		AUTO_RECORD_IGNORE_ZONES = botProperties.getProperty("AutoRecordIgnoreZones", ArrayUtils.EMPTY_STRING_ARRAY);
		AUTO_RECORD_IGNORE_TELEPORT = botProperties.getProperty("AutoRecordIgnorePeaceTeleport", true);
		RECORD_MIN_LENGTH = botProperties.getProperty("PlayerRecordMinSequenceLength", 10);
		RECORD_MAX_LENGTH = botProperties.getProperty("PlayerRecordMaxSequenceLength", 500);
		RECORD_MIN_DURATION = botProperties.getProperty("PlayerRecordMinSequenceDuration", 10000L);
		RECORD_MAX_DURATION = botProperties.getProperty("PlayerRecordMaxSequenceDuration", 600000L);
		AUTO_RECORD_INSTANT_NEW_SEQUENCE = botProperties.getProperty("AutoRecordNewSequence", true);
		LOOP_PLAYBACK = botProperties.getProperty("LoopPlayback", false);
		BOT_TTL = botProperties.getProperty("PlaybackBotTTL", 600000L);
		BOT_ITEM_ENCHANT_ANIMATE_LIMIT = botProperties.getProperty("BotItemEnchantAnimateLimit", 128);
		PLAYBACK_SPAWN_POS_RANDOM_RADIUS = botProperties.getProperty("PlaybackSpawnPosRandomRadius", 128);
		PLAYBACK_IGNORED_ITEM_IDS = botProperties.getProperty("PlaybackIgnoredItemIds", ArrayUtils.EMPTY_INT_ARRAY);
		PLAYBACK_SEQUENCE_SELECTOR_RANDOM_SLOPE_MOD = botProperties.getProperty("PlaybackSequenceSelectorRandomSlopeMod", 10);
		PLAYBACK_SEQUENCE_SELECTOR_RANDOM_THRESHOLD = botProperties.getProperty("PlaybackSequenceSelectorRandomThreshold", 0.01);
		String playbackClassIdProbMod = botProperties.getProperty("PlaybackClassIdProbabilityMod", "");
        StringTokenizer st = new StringTokenizer(playbackClassIdProbMod, ";");
		while(st.hasMoreTokens())
		{
			String classIdProbPair = st.nextToken();
			if(StringUtils.isBlank(playbackClassIdProbMod))
				continue;

			int colonIndex = playbackClassIdProbMod.indexOf(':');
			if(colonIndex < 0)
				throw new IllegalStateException("Can't parse '" + playbackClassIdProbMod + "'");

			int classId = Integer.parseInt(classIdProbPair.substring(0, colonIndex).trim());
			double probability = Double.parseDouble(classIdProbPair.substring(colonIndex + 1).trim());
			PLAYBACK_SPAWN_CLASSID_PROBABILITY_MOD.put(classId, probability);
		}
		BOT_TALK_CHANCE = botProperties.getProperty("BotTalkChance", 0.5);
		BOT_TALK_CHANCE_SHOUT = botProperties.getProperty("BotTalkChanceShout", 0.2);
		PHRASE_REUSE_TIME = botProperties.getProperty("PhraseReuseTime", 30000L);
		BOT_ACCOUNT_NAME = botProperties.getProperty("BotAccountName", "ololo_bot_account");
		String botSpawnStrategy = botProperties.getProperty("BotSpawnStrategy", "Constant(500)");
		if(!StringUtils.isBlank(botSpawnStrategy))
		{
			int startIndex = botSpawnStrategy.indexOf('(');
			int endIndex = botSpawnStrategy.lastIndexOf(')');
			String strategyName = StringUtils.trimToEmpty(botSpawnStrategy.substring(0, startIndex));
			String strategyParam = botSpawnStrategy.substring(startIndex + 1, endIndex);
			BOT_COUNT_SUPPLIER = Config.parseStrategy(strategyName, strategyParam);
		}
		else
		{
			BOT_COUNT_SUPPLIER = null;
		}
		BOTS_UNSPAWN_INTERVAL_MIN = botProperties.getProperty("BotsUnspawnIntervalMin", 5000);
		BOTS_UNSPAWN_INTERVAL_MAX = botProperties.getProperty("BotsUnspawnIntervalMax", 15000);
		BOTS_SPAWN_MIN_LEVEL = botProperties.getProperty("BotsSpawnLevelMin", 1);
		BOTS_SPAWN_MAX_LEVEL = botProperties.getProperty("BotsSpawnLevelMax", 80);
		BOTS_FIRST_ACTION_MIN = botProperties.getProperty("BotsFirstActionMin", 5000);
		BOTS_FIRST_ACTION_MAX = botProperties.getProperty("BotsFirstActionMax", 5000);
		BOTS_SPAWN_INTERVAL_MIN = botProperties.getProperty("BotsSpawnIntervalMin", 5000);
		BOTS_SPAWN_INTERVAL_MAX = botProperties.getProperty("BotsSpawnIntervalMax", 15000);
		BOTS_SPAWN_CHECK_INTERVAL = botProperties.getProperty("BotsSpawnCheckInterval", 60000);
		BOT_ACCEPT_REQUEST_CHANCE = Stream.of(botProperties.getProperty("BotAcceptRequestChances", ArrayUtils.EMPTY_STRING_ARRAY)).filter(StringUtils::isNotBlank).collect(Collectors.toMap(string -> L2RequestType.valueOf(string.substring(0, string.indexOf(':'))), string -> Double.parseDouble(string.substring(string.indexOf(':') + 1).trim())));
		BOT_DENY_REQUEST_CHANCE = Stream.of(botProperties.getProperty("BotDenyRequestChances", ArrayUtils.EMPTY_STRING_ARRAY)).filter(StringUtils::isNotBlank).collect(Collectors.toMap(string -> L2RequestType.valueOf(string.substring(0, string.indexOf(':'))), string -> Double.parseDouble(string.substring(string.indexOf(':') + 1).trim())));
		BOT_ADDITIONAL_INVENTORY_ITEMS = Stream.of(botProperties.getProperty("BotAdditionalInventoryItems", ArrayUtils.EMPTY_STRING_ARRAY)).filter(StringUtils::isNotBlank).collect(Collectors.toMap(string -> BotUtils.getItemTemplate(Integer.parseInt(string.substring(0, string.indexOf(':')).trim())).get(), string -> Long.parseLong(string.substring(string.indexOf(':') + 1).trim())));
		BOT_INITIAL_EFFECTS = Stream.of(botProperties.getProperty("BotInitialEffects", ArrayUtils.EMPTY_STRING_ARRAY)).filter(StringUtils::isNotBlank).map(string -> SkillTable.getInstance().getInfo(Integer.parseInt(string.substring(0, string.indexOf(':')).trim()), Integer.parseInt(string.substring(string.indexOf(':') + 1).trim()))).collect(Collectors.toList());
		BOT_NPC_FIND_RADIUS = botProperties.getProperty("BotFindNpcAtRadius", 1024);
		INITIAL_BOTS_TITLE = botProperties.getProperty("InitialBotsTitle", "");
		INDIVIDUAL_BOT_TITLE_CHANCE = botProperties.getProperty("IndividualBotTitleChance", 30.0);
		BOT_MAGE_BUFF_ON_CHAR_CREATE = new ArrayList<Pair<Integer, Integer>>();
		for(String s : StringUtils.split(botProperties.getProperty("BotMageBuffList", "1303-1"), ";,"))
		{
			String[] params = StringUtils.split(s, "-:");
			BOT_MAGE_BUFF_ON_CHAR_CREATE.add(Pair.of(Integer.parseInt(params[0]), Integer.parseInt(params[1])));
		}
		BOT_WARRIOR_BUFF_ON_CHAR_CREATE = new ArrayList<Pair<Integer, Integer>>();
		for(String s : StringUtils.split(botProperties.getProperty("BotWarriorBuffList", "1086-1"), ";,"))
		{
			String[] params = StringUtils.split(s, "-:");
			BOT_WARRIOR_BUFF_ON_CHAR_CREATE.add(Pair.of(Integer.parseInt(params[0]), Integer.parseInt(params[1])));
		}
	}
}