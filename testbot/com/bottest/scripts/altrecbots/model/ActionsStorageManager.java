package com.bottest.scripts.altrecbots.model;

import com.bottest.scripts.altrecbots.model.actions.Action;
import com.bottest.scripts.altrecbots.model.actions.ActionType;
import com.bottest.scripts.altrecbots.utils.BotUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import l2.gameserver.database.DatabaseFactory;
import l2.gameserver.utils.Location;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Класс ActionsStorageManager представляет собой менеджер хранилища действий, содержащего записи действий ботов.
 * Класс содержит методы для выполнения операций с записями ботов в базе данных.
 */
public class ActionsStorageManager
{
    private static final Logger logger = LoggerFactory.getLogger(ActionsStorageManager.class);

    private static final ActionsStorageManager instance = new ActionsStorageManager();

    private static final String SELECT_BOTS = "SELECT `id`, `face`, `hairStyle`, `hairColor`, `sex`, `x`, `y`, `z`, `is_noble` FROM `altrec_bots`";
    private static final String SELECT_SUBCLASSES = "SELECT `bot_id`, `class_id`, `exp`, `active`, `is_base` FROM `altrec_subclasses`";
    private static final String SELECT_SKILLS = "SELECT `bot_id`, `skill_id`, `skill_level` FROM `altrec_skills`";
    private static final String SELECT_ITEMS = "SELECT `bot_id`, `item_type`, `amount`, `is_equipped`, `enchant` FROM `altrec_items`";
    private static final String SELECT_ACTIONS = "SELECT `bot_id`, `ord`, `action_type`, `duration`, `body` FROM `altrec_actions` ORDER BY `bot_id`, `ord` ASC";
    private static final String SELECT_LAST_INSERT_ID = "SELECT LAST_INSERT_ID() AS `id`";
    private static final String SELECT_NAMES = "SELECT `name` FROM `altrec_names` WHERE (`sex` = ?) OR (ISNULL(`sex`))";
    private static final String SELECT_TITLES = "SELECT `title` FROM `altrec_title` WHERE (`sex` = ?) OR (ISNULL(`sex`))";
    private static final String SELECT_PHRASES = "SELECT `text`, `sex`, `x`, `y`, `z` FROM `altrec_phrases`";
    private static final String INSERT_BOT = "INSERT INTO `altrec_bots` (`face`, `hairStyle`, `hairColor`, `sex`, `x`, `y`, `z`, `is_noble`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String REPLACE_BOT = "REPLACE INTO `altrec_bots` (id, face, hairStyle, hairColor, sex, x, y, z, is_noble) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String REPLACE_SUBCLASS = "REPLACE INTO `altrec_subclasses` (bot_id, class_id, exp, active, is_base) VALUES (?, ?, ?, ?, ?)";
    private static final String REPLACE_SKILL = "INSERT INTO `altrec_skills` (bot_id, skill_id, skill_level) VALUES (?, ?, ?)";
    private static final String INSERT_ITEM = "INSERT INTO `altrec_items` (bot_id, item_type, amount, is_equipped, enchant) VALUES (?, ?, ?, ?, ?)";
    private static final String INSERT_ACTION = "INSERT INTO `altrec_actions` (`bot_id`, `ord`, `action_type`, `duration`, `body`) VALUES (?, ?, ?, ?, ?)";
    private static final String INSERT_NAME = "INSERT INTO `altrec_names` (`name`, `sex`) VALUES (?, ?)";
    private static final String INSERT_TITLE = "INSERT INTO `altrec_title` (`title`, `sex`) VALUES (?, ?)";
    private static final String DELETE_BOT = "DELETE FROM `altrec_bots` WHERE `id` = ?";
    private static final String DELETE_BOT_SUBCLASSES = "DELETE FROM `altrec_subclasses` WHERE `bot_id` = ?";
    private static final String DELETE_BOT_SKILLS = "DELETE FROM `altrec_skills` WHERE `bot_id` = ?";
    private static final String DELETE_BOT_ITEMS = "DELETE FROM `altrec_items` WHERE `bot_id` = ?";
    private static final String DELETE_BOT_ACTIONS = "DELETE FROM `altrec_actions` WHERE `bot_id` = ?";

    private List<ActionRecord> actionRecords = new ArrayList<ActionRecord>();

    private ActionsStorageManager()
    {

    }

    /**
     * Возвращает единственный экземпляр класса (реализация паттерна Singleton).
     * @return
     */
    public static ActionsStorageManager getInstance()
    {
        return instance;
    }

    /**
     * Инициализирует менеджер хранилища, загружая все записи из базы данных в список объектов ActionRecord.
     */
    public void init()
    {
        actionRecords = loadRecords();
        logger.info("AltRecBots: Loaded " + actionRecords.size() + " action sequence(s).");
    }

    /**
     * Возвращает список всех записей действий ботов.
     * @return
     */
    public List<ActionRecord> getActionRecords()
    {
        return Collections.unmodifiableList(actionRecords);
    }

    /**
     * Удаляет запись бота из базы данных, включая все связанные записи подклассов, умений, предметов и действий.
     * @param botId
     */
    public void deleteBotRecord(int botId)
    {
        try(Connection con = DatabaseFactory.getInstance().getConnection())
        {
            try(PreparedStatement statement = con.prepareStatement(DELETE_BOT_ACTIONS))
            {
                statement.setInt(1, botId);
                statement.executeUpdate();
            }
            try(PreparedStatement statement = con.prepareStatement(DELETE_BOT_ITEMS))
            {
                statement.setInt(1, botId);
                statement.executeUpdate();
            }
            try(PreparedStatement statement = con.prepareStatement(DELETE_BOT_SKILLS))
            {
                statement.setInt(1, botId);
                statement.executeUpdate();
            }
            try(PreparedStatement statement = con.prepareStatement(DELETE_BOT_SUBCLASSES))
            {
                statement.setInt(1, botId);
                statement.executeUpdate();
            }
            try(PreparedStatement statement = con.prepareStatement(DELETE_BOT))
            {
                statement.setInt(1, botId);
                statement.executeUpdate();
            }
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Загружает список имен для заданного пола из таблицы altrec_names.
     * @param sex
     * @return
     */
    public List<String> loadNames(int sex)
    {
        List<String> names = new ArrayList<String>();
        try(Connection con = DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement(SELECT_NAMES))
        {
            statement.setInt(1, sex);
            try(ResultSet rset = statement.executeQuery())
            {
                while(rset.next())
                    names.add(rset.getString("name"));
            }
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        return names;
    }

    /**
     * Загружает список титулов для заданного пола из таблицы altrec_title.
     * @param sex
     * @return
     */
    public List<String> loadTitles(int sex)
    {
        List<String> titles = new ArrayList<String>();
        try(Connection con = DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement(SELECT_TITLES))
        {
            statement.setInt(1, sex);
            try(ResultSet rset = statement.executeQuery())
            {
                while(rset.next())
                    titles.add(rset.getString("title"));
            }
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        return titles;
    }

    /**
     * Загружает список фраз и их координат из таблицы altrec_phrases.
     * @return
     */
    public List<BotPhrase> loadPhrases()
    {
        List<BotPhrase> phrases = new ArrayList<BotPhrase>();
        try(Connection con = DatabaseFactory.getInstance().getConnection();
            Statement statement = con.createStatement();
            ResultSet rset = statement.executeQuery(SELECT_PHRASES))
        {
            while(rset.next())
            {
                String text = rset.getString("text");
                int sex = rset.getInt("sex");
                BotPhrase botPhrase = new BotPhrase(text, sex);
                int x = rset.getInt("x");
                int y = rset.getInt("y");
                int z = rset.getInt("z");
                if(!rset.wasNull() && x != 0 && y != 0 && z != 0)
                    botPhrase = botPhrase.setLoc(new Location(x, y, z));
                phrases.add(botPhrase);
            }
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        return phrases;
    }

    /**
     * Добавляет список имен для заданного пола в таблицу altrec_names.
     * @param names
     * @param sex
     */
    public void addNames(Set<String> names, int sex)
    {
        try(Connection con = DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement(INSERT_NAME))
        {
            for(String name : names)
            {
                statement.setString(1, name);
                statement.setInt(2, sex);
                statement.addBatch();
            }
            statement.executeBatch();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Добавляет список титулов для заданного пола в таблицу altrec_title.
     * @param titles
     * @param sex
     */
    public void addTitles(Set<String> titles, int sex)
    {
        try(Connection con = DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement(INSERT_TITLE))
        {
            for(String title : titles)
            {
                statement.setString(1, title);
                statement.setInt(2, sex);
                statement.addBatch();
            }
            statement.executeBatch();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Загружает все записи действий ботов из таблицы altrec_bots, altrec_subclasses, altrec_skills, altrec_items и altrec_actions в список объектов ActionRecord.
     * @return
     */
    public List<ActionRecord> loadRecords()
    {
        try(Connection con = DatabaseFactory.getInstance().getConnection())
        {
            Map<Integer, ActionRecord> records = new HashMap<Integer, ActionRecord>();
            try(Statement statement = con.createStatement();
                ResultSet rset = statement.executeQuery(SELECT_BOTS))
            {
                while(rset.next())
                {
                    int id = rset.getInt("id");
                    records.put(id, new ActionRecord(id, rset.getInt("face"), rset.getInt("hairStyle"), rset.getInt("hairColor"), rset.getInt("sex"), new Location(rset.getInt("x"), rset.getInt("y"), rset.getInt("z")), rset.getBoolean("is_noble")));
                }
            }
            try(Statement statement = con.createStatement();
                ResultSet rset = statement.executeQuery(SELECT_SUBCLASSES))
            {
                while(rset.next())
                {
                    int id = rset.getInt("bot_id");
                    ActionRecord actionRecord = records.get(id);
                    if(actionRecord == null)
                        continue;
                    actionRecord.getSubclasses().add(new ActionRecord.SubclassRecord(rset.getInt("class_id"), rset.getLong("exp"), rset.getBoolean("active"), rset.getBoolean("is_base")));
                }
            }
            try(Statement statement = con.createStatement();
                ResultSet rset = statement.executeQuery(SELECT_SKILLS))
            {
                while(rset.next())
                {
                    int id = rset.getInt("bot_id");
                    ActionRecord actionRecord = records.get(id);
                    if(actionRecord == null)
                        continue;
                    actionRecord.getSkills().add(new ActionRecord.SkillRecord(rset.getInt("skill_id"), rset.getInt("skill_level")));
                }
            }
            try(Statement statement = con.createStatement();
                ResultSet rset = statement.executeQuery(SELECT_ITEMS))
            {
                while(rset.next())
                {
                    int id = rset.getInt("bot_id");
                    ActionRecord actionRecord = records.get(id);
                    if(actionRecord == null)
                        continue;
                    actionRecord.getItems().add(new ActionRecord.ItemRecord(rset.getInt("item_type"), rset.getLong("amount"), rset.getInt("enchant"), rset.getBoolean("is_equipped")));
                }
            }
            try(Statement statement = con.createStatement();
                ResultSet rset = statement.executeQuery(SELECT_ACTIONS))
            {
                while(rset.next())
                {
                    int id = rset.getInt("bot_id");
                    ActionRecord actionRecord = records.get(id);
                    if(actionRecord == null)
                        continue;
                    int ord = rset.getInt("ord");
                    String actionTypeStr = rset.getString("action_type");
                    ActionType actionType = Objects.<ActionType>requireNonNull(ActionType.valueOf(actionTypeStr), "Unknown action type '" + actionTypeStr + "'");
                    long duration = rset.getLong("duration");
                    String text = Objects.<String>requireNonNull(StringUtils.trimToNull(rset.getString("body")), "Body is empty for " + id + ":" + ord);
                    Action<?> action = ((Action<?>) Objects.<Object>requireNonNull(BotUtils.getGSON().fromJson(text, actionType.getActionClass()), "Body is null for " + id + ":" + ord)).setDuration(duration);
                    actionRecord.getActions().add(action);
                }
            }
            return new ArrayList<ActionRecord>(records.values());
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Данный код относится к методу, который сохраняет данные объекта типа ActionRecord в базу данных. Метод имеет название storeRecord и возвращает объект типа ActionRecord.
     * @param actionRecord
     * @return
     */
    public ActionRecord storeRecord(ActionRecord actionRecord)
    {
        actionRecord = storeRecord0(actionRecord);
        actionRecord.getId().orElseThrow(() -> new RuntimeException("'id' not specified"));
        actionRecords.add(actionRecord);
        BotSpawnManager.getInstance().addActionRecord(actionRecord);
        return actionRecord;
    }

    /**
     * Данный код относится к методу, который сохраняет данные объекта типа ActionRecord в базу данных. Метод имеет название storeRecord и возвращает объект типа ActionRecord.
     * @param actionRecord
     * @return
     */
    public ActionRecord storeRecord0(ActionRecord actionRecord)
    {
        try(Connection con = DatabaseFactory.getInstance().getConnection())
        {
            Location loc = Objects.<Location>requireNonNull(actionRecord.getLocation(), "'location' is null");
            if(actionRecord.getId().isPresent())
            {
                try(PreparedStatement statement = con.prepareStatement(REPLACE_BOT))
                {
                    statement.setInt(1, actionRecord.getId().get());
                    statement.setInt(2, actionRecord.getFace());
                    statement.setInt(3, actionRecord.getHairStyle());
                    statement.setInt(4, actionRecord.getHairColor());
                    statement.setInt(5, actionRecord.getSex());
                    statement.setInt(6, loc.getX());
                    statement.setInt(7, loc.getY());
                    statement.setInt(8, loc.getZ());
                    statement.setBoolean(9, actionRecord.isNoble());
                    statement.executeUpdate();
                }
            }
            else
            {
                try(PreparedStatement statement = con.prepareStatement(INSERT_BOT))
                {
                    statement.setInt(1, actionRecord.getFace());
                    statement.setInt(2, actionRecord.getHairStyle());
                    statement.setInt(3, actionRecord.getHairColor());
                    statement.setInt(4, actionRecord.getSex());
                    statement.setInt(5, loc.getX());
                    statement.setInt(6, loc.getY());
                    statement.setInt(7, loc.getZ());
                    statement.setBoolean(8, actionRecord.isNoble());
                    statement.execute();
                }
                try(Statement statement = con.createStatement();
                    ResultSet rset = statement.executeQuery(SELECT_LAST_INSERT_ID))
                {
                    if(rset.next())
                        actionRecord.setId(Optional.of(rset.getInt("id")));
                }
            }

            int id = actionRecord.getId().orElseThrow(() -> new RuntimeException("'id' not specified"));
            try(PreparedStatement statement = con.prepareStatement(REPLACE_SUBCLASS))
            {
                for(ActionRecord.SubclassRecord subclassRecord : actionRecord.getSubclasses())
                {
                    statement.setInt(1, id);
                    statement.setInt(2, subclassRecord.getClassId());
                    statement.setLong(3, subclassRecord.getExp());
                    statement.setBoolean(4, subclassRecord.isActive());
                    statement.setBoolean(5, subclassRecord.isBase());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            try(PreparedStatement statement = con.prepareStatement(REPLACE_SKILL))
            {
                for(ActionRecord.SkillRecord skillRecord : actionRecord.getSkills())
                {
                    statement.setInt(1, id);
                    statement.setInt(2, skillRecord.getSkillId());
                    statement.setInt(3, skillRecord.getSkillLevel());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            try(PreparedStatement statement = con.prepareStatement(INSERT_ITEM))
            {
                for(ActionRecord.ItemRecord itemRecord : actionRecord.getItems())
                {
                    statement.setInt(1, id);
                    statement.setInt(2, itemRecord.getItemType());
                    statement.setLong(3, itemRecord.getAmount());
                    statement.setBoolean(4, itemRecord.isEquipped());
                    statement.setInt(5, itemRecord.getEnchant());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            try(PreparedStatement statement = con.prepareStatement(INSERT_ACTION))
            {
                List<Action> actions = actionRecord.getActions();
                for(int i = 0; i < actions.size(); i++)
                {
                    Action action = actions.get(i);
                    statement.setInt(1, id);
                    statement.setInt(2, i);
                    statement.setString(3, action.getActionType().toString());
                    statement.setLong(4, action.getDuration());
                    statement.setString(5, BotUtils.getGSON().toJson(action));
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        return actionRecord;
    }
}