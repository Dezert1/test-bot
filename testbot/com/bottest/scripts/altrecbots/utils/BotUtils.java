package com.bottest.scripts.altrecbots.utils;

import com.bottest.scripts.altrecbots.model.AltRecBot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.bottest.scripts.altrecbots.Config;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import l2.commons.lang.ArrayUtils;
import l2.gameserver.ai.PlayerAI;
import l2.gameserver.data.xml.holder.ItemHolder;
import l2.gameserver.instancemanager.ReflectionManager;
import l2.gameserver.model.Creature;
import l2.gameserver.model.Player;
import l2.gameserver.model.Skill;
import l2.gameserver.model.Zone;
import l2.gameserver.model.base.ClassId;
import l2.gameserver.model.base.TeamType;
import l2.gameserver.model.instances.NpcInstance;
import l2.gameserver.model.items.PcInventory;
import l2.gameserver.model.items.Warehouse;
import l2.gameserver.templates.item.ItemTemplate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Класс BotUtils содержит набор утилитарных методов для работы с объектами ботов.
 */
public class BotUtils
{
    private static final Gson gson = new GsonBuilder().create();

    /**
     * Метод возвращает экземпляр класса Gson, который используется для сериализации/десериализации объектов в формат JSON.
     * @return
     */
    public static Gson getGSON()
    {
        return gson;
    }

    /**
     * Метод проверяет, является ли переданный объект игроком-ботом (экземпляром класса AltRecBot).
     * @param player
     * @return
     */
    public static boolean isBot(Player player)
    {
        return (player != null && player instanceof AltRecBot);
    }

    /**
     * Метод проверяет, является ли переданный объект существом-ботом (экземпляром класса AltRecBot).
     * @param actor
     * @return
     */
    public static boolean isBot(Creature actor)
    {
        return (actor != null && actor instanceof AltRecBot);
    }

    /**
     * Метод возвращает объект класса Optional с классом, соответствующим переданному classId, или пустой объект Optional, если такого класса не существует.
     * @param classId
     * @return
     */
    public static Optional<ClassId> toClassId(int classId)
    {
        return Stream.<ClassId>of(ClassId.values()).filter(c -> (c.getId() == classId)).findAny();
    }

    /**
     * Метод устанавливает цель для бота recBot на NPC с идентификатором npcId, находящимся в радиусе Config.BOT_NPC_FIND_RADIUS метров.
     * Если такого NPC не существует, метод возвращает null.
     * @param recBot
     * @param npcId
     * @return
     */
    public static NpcInstance setMyTargetByNpcId(AltRecBot recBot, Integer npcId)
    {
        if(npcId == null)
            return null;
        for(NpcInstance npc : recBot.getAroundNpc(Config.BOT_NPC_FIND_RADIUS, 256))
        {
            if(Objects.equals(npc.getNpcId(), npcId))
            {
                recBot.setTarget(npc);
                return npc;
            }
        }
        return null;
    }

    /**
     * Метод возвращает объект класса Optional с шаблоном предмета, соответствующего переданному itemId, или пустой объект Optional, если такого шаблона не существует.
     * @param itemId
     * @return
     */
    public static Optional<ItemTemplate> getItemTemplate(int itemId)
    {
        return Optional.ofNullable(ArrayUtils.valid(ItemHolder.getInstance().getAllTemplates(), itemId));
    }

    /**
     * Метод извлекает из объекта playerAI (AI игрока) объект класса Skill и флаг forceUse, который указывает, нужно ли использовать навык на цель без проверки условий (например, без проверки наличия маны).
     * @param playerAI
     * @return
     */
    public static Optional<Pair<Skill, Boolean>> getSkillAndForceUseFromPlayableAI(PlayerAI playerAI)
    {
        try
        {
            return AccessController.doPrivileged((PrivilegedExceptionAction<Optional<Pair<Skill, Boolean>>>) () ->
            {
                Boolean forceUse = null;
                Skill skill = null;
                for(Field field : playerAI.getClass().getSuperclass().getDeclaredFields())
                {
                    boolean accessible;
                    if(StringUtils.equalsIgnoreCase(field.getName(), "_forceUse") && field.getType() == Boolean.TYPE)
                    {
                        accessible = field.isAccessible();
                        try
                        {
                            if(!accessible)
                            {
                                field.setAccessible(true);
                            }
                            forceUse = field.getBoolean(playerAI);
                        }
                        finally
                        {
                            if(!accessible && field.isAccessible())
                            {
                                field.setAccessible(false);
                            }
                        }
                    }
                    else if(StringUtils.equalsIgnoreCase(field.getName(), "_skill") && field.getType() == Skill.class)
                    {
                        accessible = field.isAccessible();
                        try
                        {
                            if(!accessible)
                            {
                                field.setAccessible(true);
                            }
                            skill = (Skill) field.get(playerAI);
                        }
                        finally
                        {
                            if(!accessible && field.isAccessible())
                            {
                                field.setAccessible(false);
                            }
                        }
                    }
                }

                if(skill == null || forceUse == null)
                    return Optional.empty();
                else
                    return Optional.of(Pair.of(skill, forceUse));
            });
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Метод заменяет инвентарь бота recBot на объект botInventory.
     * Для этого метод использует рефлексию, перебирая все поля объекта recBot и заменяя значения всех полей, имеющих тип PcInventory.
     * @param recBot
     * @param botInventory
     * @return
     * @throws Exception
     */
    public static boolean applyInventoryHack(AltRecBot recBot, AltRecBot.AltRecBotInventory botInventory) throws Exception
    {
        try
        {
            return !ReflectionUtils.forEachField(recBot, (field, mutableField) ->
            {
                if(TypeUtils.isAssignable(field.getType(), PcInventory.class))
                {
                    mutableField.setValue(botInventory);
                    return false;
                }
                return true;
            });
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Метод заменяет склад бота на объект botWarhouse.
     * Для этого метод использует рефлексию, перебирая все поля объекта bot и заменяя значения всех полей, имеющих тип Warehouse.
     * @param recBot
     * @param botWarhouse
     * @return
     * @throws Exception
     */
    public static boolean applyWarhouseHack(AltRecBot recBot, AltRecBot.AltRecBotWarhouse botWarhouse) throws Exception
    {
        try
        {
            return !ReflectionUtils.forEachField(recBot, (field, mutableField) ->
            {
                if(TypeUtils.isAssignable(field.getType(), Warehouse.class))
                {
                    mutableField.setValue(botWarhouse);
                    return false;
                }
                return true;
            });
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Метод проверяет, подходит ли переданный объект игрока для записи действий.
     * Проверка выполняется на основе конфигурационных параметров из класса Config, например, игнорировать запись для игроков с определенным уровнем, героями, с аурой проклятого оружия и т.д.
     * @param player
     * @return
     */
    public static boolean testRecordingCondition(Player player)
    {
        if(Config.AUTO_RECORD_IGNORE_GM && player.isGM())
            return false;
        if(Config.AUTO_RECORD_IGNORE_HERO && player.isHero() || Config.AUTO_RECORD_IGNORE_NOBLE && player.isNoble())
            return false;
        if(player.getLevel() < Config.AUTO_RECORD_MIN_LVL || player.getLevel() > Config.AUTO_RECORD_MAX_LVL)
            return false;
        if(player.isMounted() || player.isInvisible())
            return false;
        if(player.getReflection() != null && player.getReflection() != ReflectionManager.DEFAULT)
            return false;
        if(player.isOlyParticipant() || player.isInObserverMode() || player.getTeam() != TeamType.NONE || player.isInOfflineMode() || !player.isOnline() || player.isLogoutStarted())
            return false;
        if(player.isCursedWeaponEquipped() || player.isFishing() || player.isFalling())
            return false;
        if(!player.isInZone(Zone.ZoneType.peace_zone) && (!Config.AUTO_RECORD_IGNORE_TELEPORT || !player.isTeleporting()))
            return false;
        for(String zone : Config.AUTO_RECORD_IGNORE_ZONES)
            if(player.isInZone(zone))
                return false;
        return true;
    }
}