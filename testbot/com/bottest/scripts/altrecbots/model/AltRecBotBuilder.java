package com.bottest.scripts.altrecbots.model;

import com.bottest.scripts.altrecbots.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import l2.gameserver.ai.PlayerAI;
import l2.gameserver.data.xml.holder.CharacterTemplateHolder;
import l2.gameserver.data.xml.holder.ItemHolder;
import l2.gameserver.idfactory.IdFactory;
import l2.gameserver.model.Player;
import l2.gameserver.model.Skill;
import l2.gameserver.model.SubClass;
import l2.gameserver.model.base.ClassId;
import l2.gameserver.model.base.Experience;
import l2.gameserver.model.items.ItemInstance;
import l2.gameserver.tables.SkillTable;
import l2.gameserver.templates.PlayerTemplate;
import l2.gameserver.templates.item.ItemTemplate;
import l2.gameserver.utils.ItemFunctions;
import l2.gameserver.utils.Location;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.Builder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

/**
 * Данный класс представляет собой строителя бота, который позволяет задавать различные параметры для создания игрока-бота.
 * Метод build() создает экземпляр класса AltRecBot, заполняя его переданными параметрами.
 */
public class AltRecBotBuilder implements Builder<AltRecBot>
{
	private static final int defaultNameColor = 0xFFFFFF;
	private static final int defaultTitleColor = 0xFFFF77;

	private static final Location defaultLocation = new Location(-113360, -244676, -15536);
	private final List<ClassId> classIds = new ArrayList<ClassId>();
	private int objId;
	private ClassId activeClassId;
	private ClassId baseClassId;
	private String accountName;
	private int face;
	private int hairStyle;
	private int hairColor;
	private int sex;
	private String name;
	private int nameColor = defaultNameColor;
	private String title;
	private int titleColor = defaultTitleColor;
	private int initialLvl;
	private boolean isNoble;
	private PlayerAI playerAI;
	private List<Triple<ItemTemplate, Integer, Long>> inventory = new ArrayList<Triple<ItemTemplate, Integer, Long>>();
	private List<ItemTemplate> equipment = new ArrayList<ItemTemplate>();

	public List<Triple<ItemTemplate, Integer, Long>> getInventory()
	{
		return this.inventory;
	}

	public AltRecBotBuilder setInventory(List<Triple<ItemTemplate, Integer, Long>> inventory)
	{
		this.inventory = inventory;
		return this;
	}

	public AltRecBotBuilder addItem(ItemTemplate template, int itemId, long itemCount, boolean equip)
	{
		getInventory().add(Triple.of(template, itemId, itemCount));
		if(equip && template.isEquipable())
			equipment.add(template);
		return this;
	}

	public AltRecBotBuilder addItem(int itemId, long itemCount, boolean equip)
	{
		return addItem(ItemHolder.getInstance().getTemplate(itemId), itemId, itemCount, equip);
	}

	public int getObjId()
	{
		return objId;
	}

	public AltRecBotBuilder setObjId(int objectId)
	{
		this.objId = objectId;
		return this;
	}

	public AltRecBotBuilder addClassId(ClassId classId, boolean baseClass, boolean activeClass)
	{
		classIds.add(classId);
		if(baseClassId == null || baseClass)
			baseClassId = classId;
		if(activeClassId == null || activeClass)
			activeClassId = classId;
		return this;
	}

	public String getAccountName()
	{
		return accountName;
	}

	public AltRecBotBuilder setAccountName(String accountName)
	{
		this.accountName = accountName;
		return this;
	}

	public int getSex()
	{
		return sex;
	}

	public AltRecBotBuilder setSex(int sex)
	{
		this.sex = sex;
		return this;
	}

	public int getFace()
	{
		return face;
	}

	public AltRecBotBuilder setFace(int face)
	{
		this.face = face;
		return this;
	}

	public int getHairStyle()
	{
		return hairStyle;
	}

	public AltRecBotBuilder setHairStyle(int hairStyle)
	{
		this.hairStyle = hairStyle;
		return this;
	}

	public int getHairColor()
	{
		return hairColor;
	}

	public AltRecBotBuilder setHairColor(int hairColor)
	{
		this.hairColor = hairColor;
		return this;
	}

	public String getName()
	{
		return name;
	}

	public AltRecBotBuilder setName(String name)
	{
		this.name = name;
		return this;
	}

	public int getNameColor()
	{
		return nameColor;
	}

	public AltRecBotBuilder setNameColor(int nameColor)
	{
		this.nameColor = nameColor;
		return this;
	}

	public String getTitle()
	{
		return title;
	}

	public AltRecBotBuilder setTitle(String title)
	{
		this.title = title;
		return this;
	}

	public int getTitleColor()
	{
		return titleColor;
	}

	public AltRecBotBuilder setTitleColor(int titleColor)
	{
		this.titleColor = titleColor;
		return this;
	}

	public int getInitialLvl()
	{
		return initialLvl;
	}

	public AltRecBotBuilder setInitialLvl(int initialLvl)
	{
		this.initialLvl = initialLvl;
		return this;
	}

	public boolean isNoble()
	{
		return isNoble;
	}

	public AltRecBotBuilder setNoble(boolean isNoble)
	{
		this.isNoble = isNoble;
		return this;
	}

	public List<ItemTemplate> getEquipment()
	{
		return equipment;
	}

	public AltRecBotBuilder setEquipment(List<ItemTemplate> equipment)
	{
		this.equipment = equipment;
		return this;
	}

	public PlayerAI getPlayerAI()
	{
		return playerAI;
	}

	public AltRecBotBuilder setPlayerAI(PlayerAI playerAI)
	{
		this.playerAI = playerAI;
		return this;
	}

	private void initInventoryItems(AltRecBot recBot)
	{
		AltRecBot.AltRecBotInventory botInventory = recBot.getInventory();
		List<Triple<ItemTemplate, Integer, Long>> addInventoryItems = new ArrayList<Triple<ItemTemplate, Integer, Long>>(inventory);
		for(Map.Entry<ItemTemplate, Long> entry : Config.BOT_ADDITIONAL_INVENTORY_ITEMS.entrySet())
		{
			addInventoryItems.add(Triple.of(entry.getKey(), 0, entry.getValue()));
		}
		for(Triple<ItemTemplate, Integer, Long> triple : addInventoryItems)
		{
			ItemTemplate template = triple.getLeft();
			if(ArrayUtils.contains(Config.PLAYBACK_IGNORED_ITEM_IDS, template.getItemId()))
				continue;
			ItemInstance item = ItemFunctions.createItem(triple.getLeft().getItemId());
			item.setCustomFlags(47);
			if(template.getCrystalType() != ItemTemplate.Grade.NONE && triple.getMiddle() > 0 && item.canBeEnchanted(false))
				item.setEnchantLevel(Math.min(Config.BOT_ITEM_ENCHANT_ANIMATE_LIMIT, triple.getMiddle()));
			item.setCount(triple.getRight());
			botInventory.addItem(item);
			if(equipment.contains(template))
			    recBot.getInventory().equipItem(item);
		}
		recBot.useShots();
	}

	@Override
	public AltRecBot build()
	{
		int objectId = objId != 0 ? objId : (objId = IdFactory.getInstance().getNextId());
		PlayerTemplate playerTemplate = CharacterTemplateHolder.getInstance().getTemplate(baseClassId, sex == 0);
		AltRecBot recBot = new AltRecBot(objectId, playerTemplate, Objects.requireNonNull(accountName, "'accountName' is null"));
		recBot.setRace(0, activeClassId.getRace().ordinal());
		for(ClassId classId : classIds)
		{
            SubClass subClass = new SubClass();
			subClass.setClassId(classId.getId());
			int lvl = Math.max(initialLvl, Player.EXPERTISE_LEVELS[classId.getLevel()]);
			subClass.setExp(Experience.getExpForLevel(lvl));
			subClass.setSp(0L);
			if(classId == activeClassId)
			{
				subClass.setActive(true);
				subClass.setBase(classId == baseClassId);
			}
			if(classId == baseClassId)
			{
				recBot.setBaseClass(classId.getId());
			}
			recBot.getSubClasses().put(classId.getId(), subClass);
			if(classId == activeClassId)
            {
                recBot.setActiveClass(subClass);
                recBot.setClassId(classId.getId(), true, false);
            }
		}
		recBot.setFace(face);
		recBot.setHairStyle(hairStyle);
		recBot.setHairColor(hairColor);
		recBot.setNameColor(nameColor);
		recBot.setName(Objects.requireNonNull(StringUtils.trimToNull(name)));
		recBot.setTitleColor(titleColor);
		recBot.setDisconnectedTitleColor(titleColor);
		if(!StringUtils.isBlank(title))
		{
			recBot.setTitle(title);
			recBot.setDisconnectedTitle(title);
		}
		else
		{
			recBot.setTitle("");
			recBot.setDisconnectedTitle("");
		}
		recBot.setOnlineStatus(true);
		recBot.entering = false;
		recBot.setAI(playerAI != null ? playerAI : new AltRecBotAI(recBot));
		recBot.setLoc(defaultLocation);
		recBot.setCurrentHpMp(recBot.getMaxHp(), recBot.getMaxMp());
		recBot.setRunning();
		recBot.stopAutoSaveTask();
		initInventoryItems(recBot);
		for(Pair<Integer, Integer> pair : recBot.isMageClass() ? Config.BOT_MAGE_BUFF_ON_CHAR_CREATE : Config.BOT_WARRIOR_BUFF_ON_CHAR_CREATE)
		{
			Skill skill = SkillTable.getInstance().getInfo(pair.getLeft(), pair.getRight());
			skill.getEffects(recBot, recBot, false, false);
		}
		return recBot;
	}
}