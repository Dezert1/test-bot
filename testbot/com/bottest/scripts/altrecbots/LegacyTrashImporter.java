package com.bottest.scripts.altrecbots;

import com.bottest.scripts.altrecbots.model.ActionRecord;
import com.bottest.scripts.altrecbots.model.ActionsStorageManager;
import com.bottest.scripts.altrecbots.model.actions.Action;
import com.bottest.scripts.altrecbots.model.actions.ActionType;
import com.bottest.scripts.altrecbots.model.actions.EquipItem;
import com.bottest.scripts.altrecbots.model.actions.ItemSetEnchant;
import com.bottest.scripts.altrecbots.model.actions.SkillCast;
import com.bottest.scripts.altrecbots.model.actions.Subclass;
import com.bottest.scripts.altrecbots.utils.BotUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import l2.gameserver.Config;
import l2.gameserver.GameServer;
import l2.gameserver.Shutdown;
import l2.gameserver.model.Player;
import l2.gameserver.model.base.ClassId;
import l2.gameserver.model.base.Experience;
import l2.gameserver.tables.SkillTable;
import l2.gameserver.templates.item.ItemTemplate;
import l2.gameserver.utils.Location;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Класс LegacyTrashImporter реализует импорт данных из старого формата в новый. В качестве входных данных он принимает список файлов в формате txt, содержащих информацию об игроках, предметах и действиях в игре.
 * Основной метод этого класса – main, который запускает процесс импорта данных.
 */
public class LegacyTrashImporter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyTrashImporter.class);

    private static File AI_FILE = null;
    private static File MALE_NAMES_FILE = null;
    private static File FEMALE_NAMES_FILE = null;
    private static File MALE_PHRASES_FILE = null;
    private static File FEMALE_PHRASES_FILE = null;
    private static File MALE_TITLES_FILE = null;
    private static File FEMALE_TITLES_FILE = null;

    private static final void printUsage()
    {
        System.out.println("Usage: LegacyImporter -ai <ai_txt_file> -male_names <male_names_file> -female_names <female_names_file> -male_phrases <male_phrases_file> -female_phrases <male_female_phrases> -male_titles <male_titles_file> -female_titles <female_titles_file>");
    }

    /**
     * Метод main начинается с проверки аргументов командной строки.
     * Если аргументы отсутствуют, выводится сообщение об использовании приложения и программа завершается.
     * Если аргументы присутствуют, их значения сохраняются в соответствующих переменных.
     * Если указанные файлы отсутствуют, выводится сообщение об ошибке и программа завершается.
     * Если все файлы присутствуют, запускается метод GameServer.main(), который инициализирует игровой сервер и запускает процесс импорта данных.
     * @param args
     * @throws Exception
     */
    public static void main(String... args) throws Exception
    {
        App.isLegacyImport = true;
        if(args.length < 0)
        {
            printUsage();
            System.exit(-1);
            return;
        }
        for(int i = 0; i < args.length; i++)
        {
            String str = args[i];
            if("-ai".equalsIgnoreCase(str) && i + 1 < args.length)
            {
                AI_FILE = new File(args[i + 1]);
                i++;
            }
            else if("-male_names".equalsIgnoreCase(str) && i + 1 < args.length)
            {
                MALE_NAMES_FILE = new File(args[i + 1]);
                i++;
            }
            else if("-female_names".equalsIgnoreCase(str) && i + 1 < args.length)
            {
                FEMALE_NAMES_FILE = new File(args[i + 1]);
                i++;
            }
            else if("-male_phrases".equalsIgnoreCase(str) && i + 1 < args.length)
            {
                MALE_PHRASES_FILE = new File(args[i + 1]);
                i++;
            }
            else if("-female_phrases".equalsIgnoreCase(str) && i + 1 < args.length)
            {
                FEMALE_PHRASES_FILE = new File(args[i + 1]);
                i++;
            }
            else if("-male_title".equalsIgnoreCase(str) && i + 1 < args.length)
            {
                MALE_TITLES_FILE = new File(args[i + 1]);
                i++;
            }
            else if("-female_title".equalsIgnoreCase(str) && i + 1 < args.length)
            {
                FEMALE_TITLES_FILE = new File(args[i + 1]);
                i++;
            }
        }
        if ((AI_FILE == null && MALE_NAMES_FILE == null && FEMALE_NAMES_FILE == null && MALE_PHRASES_FILE == null && FEMALE_PHRASES_FILE == null && MALE_TITLES_FILE == null && FEMALE_TITLES_FILE == null)
                || (AI_FILE != null && !AI_FILE.exists())
                || (MALE_NAMES_FILE != null && !MALE_NAMES_FILE.exists())
                || (FEMALE_NAMES_FILE != null && !FEMALE_NAMES_FILE.exists())
                || (MALE_PHRASES_FILE != null && !MALE_PHRASES_FILE.exists())
                || (FEMALE_PHRASES_FILE != null && !FEMALE_PHRASES_FILE.exists())
                || (MALE_TITLES_FILE != null && !MALE_TITLES_FILE.exists())
                || (FEMALE_TITLES_FILE != null && !FEMALE_TITLES_FILE.exists()))
        {
            printUsage();
            System.exit(-1);
            return;
        }
        GameServer.main(ArrayUtils.EMPTY_STRING_ARRAY);
    }

    /**
     * Метод parseLegacyTrash осуществляет разбор данных из входящего потока в формате txt.
     * Возвращает список объектов типа List<ActionRecord>.
     * Каждый объект ActionRecord содержит информацию об игроке, его действиях, подклассах, навыках и предметах.
     * Класс ActionRecord содержит вложенные классы ItemRecord, SkillRecord и SubclassRecord для хранения информации о предметах, навыках и сабклассах соответственно.
     *
     * Метод parseLegacyTrash() считывает данные из входного потока, обрабатывает каждую строку в соответствии с форматом данных и создает объекты ItemRecord, SkillRecord и SubclassRecord, добавляя их в созданный объект ActionRecord.
     * Метод также вызывает метод newActionInstance() для создания объекта Action и сохраняет его в список действий объекта ActionRecord.
     * @param is
     * @return
     * @throws Exception
     */
    public static final List<ActionRecord> parseLegacyTrash(InputStream is) throws Exception
    {
        List<ActionRecord> actionRecords = new ArrayList<ActionRecord>();
        try(InputStreamReader inr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(inr))
        {
            int lineNumber = 0;
            loop: for(String line = StringUtils.trim(br.readLine()); line != null; line = StringUtils.trim(br.readLine()), lineNumber++)
            {
                if(StringUtils.isBlank(line) || StringUtils.startsWith(line, "#"))
                    continue;

                int firstIndex = StringUtils.indexOf(line, 58);
                if(firstIndex == -1)
                {
                    LOGGER.info("Malformed player set line location {}: \"{}\"", lineNumber, line);
                    continue;
                }

                int secondIndex = StringUtils.indexOf(line, 58, firstIndex + 1);
                Location loc = Location.parseLoc(StringUtils.substring(line, 0, firstIndex));
                if(secondIndex == -1)
                {
                    LOGGER.info("Malformed player set line args {}: \"{}\"", lineNumber, line);
                    continue;
                }

                String[] params = StringUtils.split(StringUtils.trimToEmpty(StringUtils.substring(line, firstIndex + 1, secondIndex)), ',');
                if(params.length < 10)
                {
                    LOGGER.info("Malformed player set line args {}: \"{}\"", lineNumber, line);
                    continue;
                }

                List<ActionRecord.ItemRecord> itemRecords = new ArrayList<ActionRecord.ItemRecord>();
                List<ActionRecord.SubclassRecord> subclassRecords = new ArrayList<ActionRecord.SubclassRecord>();
                List<ActionRecord.SkillRecord> skillRecords = new ArrayList<ActionRecord.SkillRecord>();
                List<Action> actions = new ArrayList<Action>();

                int itemStartIndex = StringUtils.indexOf(line, 58, secondIndex + 1);
                int itemEndIndex = StringUtils.lastIndexOf(line, 58);
                if(itemStartIndex != -1 && itemEndIndex > secondIndex)
                {
                    String itemStr = StringUtils.trimToEmpty(StringUtils.substring(line, secondIndex + 1, itemStartIndex));
                    for(String item : StringUtils.split(itemStr, ';'))
                    {

                        item = StringUtils.trimToEmpty(item);
                        int itemDelimIdx = StringUtils.indexOf(item, 44);
                        if(item.isEmpty() || itemDelimIdx == -1)
                            continue;

                        int itemId = Integer.parseInt(StringUtils.trim(StringUtils.substring(item, 0, itemDelimIdx)));
                        int enchant = Integer.parseInt(StringUtils.trim(StringUtils.substring(item, itemDelimIdx + 1)));

                        itemRecords.add(new ActionRecord.ItemRecord(itemId, 1, enchant, true));
                    }
                }

                int baseClassId = Integer.parseInt(params[0]);
                int sex = Integer.parseInt(params[1]);
                int hairStyle = Integer.parseInt(params[2]);
                int hairColor = Integer.parseInt(params[3]);
                int face = Integer.parseInt(params[4]);
                int classId = Integer.parseInt(params[5]);
                int baseLvl = Integer.parseInt(params[6]);
                int lvl = Integer.parseInt(params[7]);
                int noble = Integer.parseInt(params[8]);

                subclassRecords.add(new ActionRecord.SubclassRecord(baseClassId, Experience.getExpForLevel(baseLvl), classId == baseClassId, true));
                if(classId != baseClassId)
                    subclassRecords.add(new ActionRecord.SubclassRecord(classId, Experience.getExpForLevel(lvl), true, false));

                String actionStr = StringUtils.trimToEmpty(StringUtils.substring(line, itemEndIndex + 1));
                if(StringUtils.isBlank(actionStr))
                    continue;

                ActionRecord actionRecord = new ActionRecord(face, hairStyle, hairColor, sex, loc, noble != 0, subclassRecords, skillRecords, itemRecords);
                for(String an : StringUtils.split(actionStr, ';'))
                {
                    String[] actionParams = StringUtils.split(an, ',');
                    if(actionParams.length != 8)
                    {
                        LOGGER.info("Malformed player set line args {}: \"{}\"", lineNumber, line);
                        continue loop;
                    }

                    ActionType actionType = ActionType.getActionTypeByLegacyOrd(Integer.parseInt(actionParams[0]));
                    if(actionType == null)
                    {
                        LOGGER.info("Unknown player action {}. Line args {}: \"{}\"", new String[]{actionStr, String.valueOf(lineNumber), line});
                        continue loop;
                    }

                    Action<?> action = actionType.newActionInstance();
                    action = action.fromLegacy(new int[] {
                        Integer.parseInt(actionParams[2]),
                        Integer.parseInt(actionParams[3]),
                        Integer.parseInt(actionParams[4]),
                        Integer.parseInt(actionParams[5]),
                        Integer.parseInt(actionParams[6]),
                        Integer.parseInt(actionParams[7])
                    });

                    if(action != null)
                    {
                        action.setDuration(Long.parseLong(actionParams[1]));
                        actions.add(action);

                        switch(actionType)
                        {
                            case SKILL_CAST:
                                int skillId = ((SkillCast) action).getSkillId();
                                int skillLevel = SkillTable.getInstance().getMaxLevel(skillId);
                                if(skillLevel > 0)
                                    skillRecords.add(new ActionRecord.SkillRecord(skillId, skillLevel));
                                break;
                            case EQUIP_ITEM:
                            case ITEM_SET_ENCHANT:
                                Optional<ItemTemplate> itemOptional = BotUtils.getItemTemplate(actionType == ActionType.EQUIP_ITEM ? ((EquipItem) action).getItemId() : ((ItemSetEnchant) action).getItemId());
                                if(itemOptional.isPresent())
                                {
                                    ItemTemplate item = itemOptional.get();
                                    itemRecords.add(new ActionRecord.ItemRecord(item.getItemId(), 1, 0, false));
                                }
                                break;
                            case SUBCLASS:
                                ClassId oldClassId = ((Subclass) action).getClassId();
                                ClassId newClassId = ((Subclass) action).getNewClassId();
                                if(oldClassId != null && subclassRecords.stream().filter(subclassRecord -> (subclassRecord.getClassId() != oldClassId.getId())).findAny().isPresent())
                                {
                                    subclassRecords.add(new ActionRecord.SubclassRecord(oldClassId.getId(),
                                            Experience.getExpForLevel(Math.max(Player.EXPERTISE_LEVELS[oldClassId.getLevel()], lvl)), false, false));
                                }
                                if(newClassId != null && subclassRecords.stream().filter(subclassRecord -> (subclassRecord.getClassId() != newClassId.getId())).findAny().isPresent())
                                {
                                    subclassRecords.add(new ActionRecord.SubclassRecord(newClassId.getId(),
                                            Experience.getExpForLevel(Math.max(Player.EXPERTISE_LEVELS[newClassId.getLevel()], lvl)), false, false));
                                }
                                break;
                        }
                    }
                }
                if(!actions.isEmpty())
                {
                    actionRecords.add(actionRecord
                        .setSkills(actionRecord.getSkills().stream().distinct().collect(Collectors.toList()))
                        .setSubclasses(actionRecord.getSubclasses().stream().distinct().collect(Collectors.toList()))
                        .setItems(actionRecord.getItems().stream().distinct().collect(Collectors.toList()))
                        .setActions(actions));
                }
            }
        }
        return actionRecords;
    }

    /**
     * Метод устанавливает порты игры в пустой массив.
     */
    protected static void onLoad()
    {
        Config.PORTS_GAME = ArrayUtils.EMPTY_INT_ARRAY;
    }

    /**
     * Метод отвечает за импорт записей действий игроков из файла AI_FILE (если он задан), который должен быть в формате, соответствующем определенному в parseLegacyTrash().
     * Если файл не существует, то метод завершит работу без импорта записей.
     */
    private static void importAi()
    {
        if(AI_FILE == null || !AI_FILE.exists())
        {
            LOGGER.info("LegacyImporter: Skip legacy ai.");
            return;
        }
        LOGGER.info("LegacyImporter: Importing legacy ai from \"{}\"...", AI_FILE);
        try
        {
            try(FileInputStream fis = new FileInputStream(AI_FILE))
            {
                List<ActionRecord> actionRecords = parseLegacyTrash(fis);
                LOGGER.info("LegacyImporter: Parsed " + actionRecords.size() + " record(s) from \"" + AI_FILE.toString() + "\".");
                LOGGER.info("LegacyImporter: Storing player action record(s) ...");
                int i;
                for(i = 0; i < actionRecords.size(); i++)
                {
                    ActionsStorageManager.getInstance().storeRecord(actionRecords.get(i));
                    if(i % 1000 == 0)
                        LOGGER.info("LegacyImporter: Stored " + i + " records.");
                }
                LOGGER.info("LegacyImporter: Stored " + i + " records.");
            }
            LOGGER.info("LegacyImporter: \"" + AI_FILE.toString() + "\" done.");
        }
        catch(Exception e)
        {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Метод импортируют список мужских из соответствующих файлов, если они существуют.
     * В методе создается FileInputStream, который открывает поток чтения из соответствующего файла, затем происходит чтение каждой строки файла с помощью BufferedReader.
     */
    private static void importMaleNames()
    {
        if(MALE_NAMES_FILE == null || !MALE_NAMES_FILE.exists())
        {
            LOGGER.info("LegacyImporter: Skip legacy male names.");
            return;
        }
        LOGGER.info("LegacyImporter: Importing legacy male names from \"{}\"...", MALE_NAMES_FILE);
        try(FileInputStream fis = new FileInputStream(MALE_NAMES_FILE);
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr))
        {
            List<String> names = new ArrayList<String>();
            names.addAll(ActionsStorageManager.getInstance().loadNames(Player.PLAYER_SEX_MALE));
            names.addAll(ActionsStorageManager.getInstance().loadNames(Player.PLAYER_SEX_FEMALE));

            Set<String> maleNames = new LinkedHashSet<String>();
            for(String line = br.readLine(); line != null; line = br.readLine())
            {
                String nameLine = StringUtils.trimToEmpty(line);
                if(StringUtils.isBlank(nameLine) || nameLine.charAt(0) == '#')
                    continue;

                if(!names.stream().filter(name -> StringUtils.equalsIgnoreCase(name, nameLine)).findAny().isPresent())
                    maleNames.add(nameLine);
            }
            ActionsStorageManager.getInstance().addNames(maleNames, Player.PLAYER_SEX_MALE);
            LOGGER.info("LegacyImporter: Imported " + maleNames.size() + " new names from \"" + MALE_NAMES_FILE.toString() + "\" done.");
        }
        catch(Exception e)
        {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Метод импортируют список женских имен из соответствующих файлов, если они существуют.
     * В каждом методе создается FileInputStream, который открывает поток чтения из соответствующего файла, затем происходит чтение каждой строки файла с помощью BufferedReader.
     */
    private static void importFemaleNames()
    {
        if(FEMALE_NAMES_FILE == null || !FEMALE_NAMES_FILE.exists())
        {
            LOGGER.info("LegacyImporter: Skip legacy female names.");
            return;
        }
        LOGGER.info("LegacyImporter: Importing legacy female names from \"{}\"...", FEMALE_NAMES_FILE);
        try(FileInputStream fis = new FileInputStream(FEMALE_NAMES_FILE);
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr))
        {
            List<String> names = new ArrayList<String>();
            names.addAll(ActionsStorageManager.getInstance().loadNames(Player.PLAYER_SEX_MALE));
            names.addAll(ActionsStorageManager.getInstance().loadNames(Player.PLAYER_SEX_FEMALE));

            Set<String> femaleNames = new LinkedHashSet<String>();
            for(String line = br.readLine(); line != null; line = br.readLine())
            {
                String nameLine = StringUtils.trimToEmpty(line);
                if(StringUtils.isBlank(nameLine) || nameLine.charAt(0) == '#')
                    continue;

                if(!names.stream().filter(name -> StringUtils.equalsIgnoreCase(name, nameLine)).findAny().isPresent())
                    femaleNames.add(nameLine);
            }
            ActionsStorageManager.getInstance().addNames(femaleNames, Player.PLAYER_SEX_FEMALE);
            LOGGER.info("LegacyImporter: Imported " + femaleNames.size() + " new names from \"" + MALE_NAMES_FILE.toString() + "\" done.");
        }
        catch(Exception e)
        {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Метод импортируют список мужских титулов из соответствующих файлов, если они существуют.
     * В каждом методе создается FileInputStream, который открывает поток чтения из соответствующего файла, затем происходит чтение каждой строки файла с помощью BufferedReader.
     */
    private static void importMaleTitles()
    {
        if(MALE_TITLES_FILE == null || !MALE_TITLES_FILE.exists())
        {
            LOGGER.info("LegacyImporter: Skip legacy male titles.");
            return;
        }
        LOGGER.info("LegacyImporter: Importing legacy male titles from \"{}\"...", MALE_TITLES_FILE);
        try(FileInputStream fis = new FileInputStream(MALE_TITLES_FILE);
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr))
        {
            List<String> titles = new ArrayList<String>();
            titles.addAll(ActionsStorageManager.getInstance().loadTitles(Player.PLAYER_SEX_MALE));
            titles.addAll(ActionsStorageManager.getInstance().loadTitles(Player.PLAYER_SEX_FEMALE));

            Set<String> maleTitles = new LinkedHashSet<String>();
            for(String line = br.readLine(); line != null; line = br.readLine())
            {
                String titleLine = StringUtils.trimToEmpty(line);
                if(StringUtils.isBlank(titleLine) || titleLine.charAt(0) == '#')
                    continue;

                if(!titles.stream().filter(title -> StringUtils.equalsIgnoreCase(title, titleLine)).findAny().isPresent())
                    maleTitles.add(titleLine);
            }
            ActionsStorageManager.getInstance().addTitles(maleTitles, Player.PLAYER_SEX_MALE);
            LOGGER.info("LegacyImporter: Imported " + maleTitles.size() + " new titles from \"" + MALE_TITLES_FILE.toString() + "\" done.");
        }
        catch(Exception e)
        {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Метод импортируют список женских титулов из соответствующих файлов, если они существуют.
     * В каждом методе создается FileInputStream, который открывает поток чтения из соответствующего файла, затем происходит чтение каждой строки файла с помощью BufferedReader.
     */
    private static void importFemaleTitles()
    {
        if(FEMALE_TITLES_FILE == null || !FEMALE_TITLES_FILE.exists())
        {
            LOGGER.info("LegacyImporter: Skip legacy female titles.");
            return;
        }
        LOGGER.info("LegacyImporter: Importing legacy female titles from \"{}\"...", FEMALE_TITLES_FILE);
        try(FileInputStream fis = new FileInputStream(FEMALE_TITLES_FILE);
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr))
        {
            List<String> titles = new ArrayList<String>();
            titles.addAll(ActionsStorageManager.getInstance().loadTitles(Player.PLAYER_SEX_MALE));
            titles.addAll(ActionsStorageManager.getInstance().loadTitles(Player.PLAYER_SEX_FEMALE));

            Set<String> femaleTitles = new LinkedHashSet<String>();

            for(String line = br.readLine(); line != null; line = br.readLine())
            {
                String titleLine = StringUtils.trimToEmpty(line);
                if(StringUtils.isBlank(titleLine) || titleLine.charAt(0) == '#')
                    continue;

                if(!titles.stream().filter(title -> StringUtils.equalsIgnoreCase(title, titleLine)).findAny().isPresent())
                    femaleTitles.add(titleLine);
            }
            ActionsStorageManager.getInstance().addTitles(femaleTitles, Player.PLAYER_SEX_FEMALE);
            LOGGER.info("LegacyImporter: Imported " + femaleTitles.size() + " new titles from \"" + FEMALE_TITLES_FILE.toString() + "\" done.");
        }
        catch(Exception e)
        {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Данный метод вызывает методы импорта информации и планирует закрытие сервера после завершения импорта.
     */
    protected static void onStart()
    {
        importAi();
        importMaleNames();
        importFemaleNames();
        importMaleTitles();
        importFemaleTitles();
        LOGGER.info("LegacyImporter: Shutdown ...");
        Shutdown.getInstance().schedule(0, 0);
    }
}