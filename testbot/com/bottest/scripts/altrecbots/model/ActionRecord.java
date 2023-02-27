package com.bottest.scripts.altrecbots.model;

import com.bottest.scripts.altrecbots.Config;
import com.bottest.scripts.altrecbots.model.actions.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import l2.gameserver.utils.Location;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Данный класс "ActionRecord" представляет запись о действии персонажа в игре.
 * Он содержит данные о персонаже, такие как идентификатор, вид персонажа, прическа, цвет волос, пол, местоположение и т.д.
 * Класс также содержит списки подклассов, навыков и предметов, а также список действий, которые может выполнить персонаж.
 * "ActionRecord" обеспечивает удобный интерфейс для получения и установки значений всех полей.
 * Он также имеет методы equals, hashCode и toString для сравнения, хеширования и преобразования в строку объекта.
 * Кроме того, класс содержит методы для получения случайной локации на основе текущего местоположения персонажа.
 */
public class ActionRecord
{
	private Optional<Integer> idOpt;
	private int face;
	private int hairStyle;
	private int hairColor;
	private int sex;
	private Location location;
	private boolean isNoble;
	private List<SubclassRecord> subclasses;
	private List<SkillRecord> skills;
	private List<ItemRecord> items;
	private List<Action> actions = new ArrayList<Action>();

	public ActionRecord(Optional<Integer> idOpt, int face, int hairStyle, int hairColor, int sex, Location location, boolean isNoble, List<SubclassRecord> subclasses, List<SkillRecord> skills, List<ItemRecord> items)
	{
		this.idOpt = idOpt;
		this.face = face;
		this.hairStyle = hairStyle;
		this.hairColor = hairColor;
		this.sex = sex;
		this.location = location;
		this.isNoble = isNoble;
		this.subclasses = subclasses;
		this.skills = skills;
		this.items = items;
	}

	public ActionRecord(int face, int hairStyle, int hairColor, int sex, Location location, boolean isNoble, List<SubclassRecord> subclasses, List<SkillRecord> skills, List<ItemRecord> items)
	{
		this(Optional.empty(), face, hairStyle, hairColor, sex, location, isNoble, subclasses, skills, items);
	}

	public ActionRecord(Integer id, int face, int hairStyle, int hairColor, int sex, Location location, boolean isNoble)
	{
		this(Optional.ofNullable(id), face, hairStyle, hairColor, sex, location, isNoble, new ArrayList<SubclassRecord>(), new ArrayList<SkillRecord>(), new ArrayList<ItemRecord>());
	}

	public List<Action> getActions()
	{
		return actions;
	}

	public ActionRecord setActions(List<Action> actions)
	{
		this.actions = actions;
		return this;
	}

	public Optional<Integer> getId()
	{
		return idOpt;
	}

	public ActionRecord setId(Integer id)
	{
		return setId(Optional.ofNullable(id));
	}

	public ActionRecord setId(Optional<Integer> idOpt)
	{
		this.idOpt = idOpt;
		return this;
	}

	public int getFace()
	{
		return face;
	}

	public ActionRecord setFace(int face)
	{
		this.face = face;
		return this;
	}

	public int getHairStyle()
	{
		return hairStyle;
	}

	public ActionRecord setHairStyle(int hairStyle)
	{
		this.hairStyle = hairStyle;
		return this;
	}

	public int getHairColor()
	{
		return hairColor;
	}

	public ActionRecord setHairColor(int hairColor)
	{
		this.hairColor = hairColor;
		return this;
	}

	public int getSex()
	{
		return sex;
	}

	public ActionRecord setSex(int sex)
	{
		this.sex = sex;
		return this;
	}

	public Location getLocation()
	{
		return location;
	}

	public ActionRecord setLocation(Location location)
	{
		this.location = location;
		return this;
	}

	public Location getLocationRandomized()
	{
		if(Config.PLAYBACK_SPAWN_POS_RANDOM_RADIUS > 0)
		{
			return Location.findPointToStay(getLocation(), Config.PLAYBACK_SPAWN_POS_RANDOM_RADIUS / 3, Config.PLAYBACK_SPAWN_POS_RANDOM_RADIUS);
		}
		return getLocation();
	}

	public boolean isNoble()
	{
		return isNoble;
	}

	public ActionRecord setNoble(boolean noble)
	{
		this.isNoble = noble;
		return this;
	}

	public List<SubclassRecord> getSubclasses()
	{
		return subclasses;
	}

	public ActionRecord setSubclasses(List<SubclassRecord> subclasses)
	{
		this.subclasses = subclasses;
		return this;
	}

	public Optional<SubclassRecord> getBaseSubclass()
	{
		return getSubclasses().stream().filter(SubclassRecord::isBase).findFirst();
	}

	public List<SkillRecord> getSkills()
	{
		return skills;
	}

	public ActionRecord setSkills(List<SkillRecord> skills)
	{
		this.skills = skills;
		return this;
	}

	public List<ItemRecord> getItems()
	{
		return items;
	}

	public ActionRecord setItems(List<ItemRecord> items)
	{
		this.items = items;
		return this;
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
			return true;

		if(o == null || getClass() != o.getClass())
			return false;

		ActionRecord actionRecord = (ActionRecord) o;
		if(idOpt.isPresent() && actionRecord.idOpt.isPresent())
			return new EqualsBuilder().append(idOpt, actionRecord.idOpt).isEquals();

		return new EqualsBuilder().append(idOpt, actionRecord.idOpt).append(face, actionRecord.face).append(hairStyle, actionRecord.hairStyle).append(hairColor, actionRecord.hairColor).append(sex, actionRecord.sex).append(isNoble, actionRecord.isNoble).append(location, actionRecord.location).append(subclasses, actionRecord.subclasses).append(skills, actionRecord.skills).append(items, actionRecord.items).isEquals();
	}

	@Override
	public int hashCode()
	{
		if(idOpt.isPresent())
		{
			return new HashCodeBuilder(17, 37).append(idOpt.get()).toHashCode();
		}
		return new HashCodeBuilder(17, 37).append(face).append(hairStyle).append(hairColor).append(sex).append(location).append(isNoble).append(subclasses).append(skills).append(items).toHashCode();
	}

	@Override
	public String toString()
	{
		return "ActionRecord{id=" + idOpt.orElse(null) + ", face=" + face + ", hairStyle=" + hairStyle + ", hairColor=" + hairColor + ", sex=" + sex + ", location=" + location + ", isNoble=" + isNoble + ", subclasses=" + subclasses + ", skills=" + skills + ", items=" + items + '}';
	}

	public static class SubclassRecord
	{
		private int classId;
		private long exp;
		private boolean isActive;
		private boolean isBase;

		public SubclassRecord(int classId, long exp, boolean isActive, boolean isBase)
		{
			this.classId = classId;
			this.exp = exp;
			this.isActive = isActive;
			this.isBase = isBase;
		}

		public int getClassId()
		{
			return classId;
		}

		public SubclassRecord setClassId(int classId)
		{
			this.classId = classId;
			return this;
		}

		public long getExp()
		{
			return exp;
		}

		public SubclassRecord setExp(long exp)
		{
			this.exp = exp;
			return this;
		}

		public boolean isActive()
		{
			return isActive;
		}

		public SubclassRecord setActive(boolean isActive)
		{
			this.isActive = isActive;
			return this;
		}

		public boolean isBase()
		{
			return isBase;
		}

		public SubclassRecord setBase(boolean isBase)
		{
			this.isBase = isBase;
			return this;
		}

		@Override
		public boolean equals(Object o)
		{
			if(this == o)
				return true;

			if(o == null || getClass() != o.getClass())
				return false;

			SubclassRecord subclassRecord = (SubclassRecord) o;
			return new EqualsBuilder().append(classId, subclassRecord.classId).append(exp, subclassRecord.exp).append(isActive, subclassRecord.isActive).append(isBase, subclassRecord.isBase).isEquals();
		}

		@Override
		public int hashCode()
		{
			return new HashCodeBuilder(17, 37).append(classId).append(exp).append(isActive).append(isBase).toHashCode();
		}

		@Override
		public String toString()
		{
			return "SubclassRecord{classId=" + classId + ", exp=" + exp + ", isActive=" + isActive + ", isBase=" + isBase + '}';
		}
	}

	public static class ItemRecord
	{
		private int itemType;
		private long amount;
		private int enchant;
		private boolean isEquipped;

		public ItemRecord(int itemType, long amount, int enchant, boolean isEquipped)
		{
			this.itemType = itemType;
			this.amount = amount;
			this.enchant = enchant;
			this.isEquipped = isEquipped;
		}

		public int getItemType()
		{
			return itemType;
		}

		public ItemRecord setItemType(int itemType)
		{
			this.itemType = itemType;
			return this;
		}

		public long getAmount()
		{
			return amount;
		}

		public ItemRecord setAmount(long amount)
		{
			this.amount = amount;
			return this;
		}

		public int getEnchant()
		{
			return enchant;
		}

		public ItemRecord setEnchant(int enchant)
		{
			this.enchant = enchant;
			return this;
		}

		public boolean isEquipped()
		{
			return isEquipped;
		}

		public ItemRecord setEquipped(boolean isEquipped)
		{
			this.isEquipped = isEquipped;
			return this;
		}

		@Override
		public boolean equals(Object o)
		{
			if(this == o)
				return true;

			if(o == null || getClass() != o.getClass())
				return false;

			ItemRecord itemRecord = (ItemRecord) o;
			return new EqualsBuilder().append(itemType, itemRecord.itemType).append(amount, itemRecord.amount).append(enchant, itemRecord.enchant).append(isEquipped, itemRecord.isEquipped).isEquals();
		}

		@Override
		public int hashCode()
		{
			return new HashCodeBuilder(17, 37).append(itemType).append(amount).append(enchant).append(isEquipped).toHashCode();
		}

		@Override
		public String toString()
		{
			return "ItemRecord{itemType=" + itemType + ", amount=" + amount + ", enchant=" + enchant + ", isEquipped=" + isEquipped + '}';
		}
	}

	public static class SkillRecord
	{
		private int skillId;
		private int skillLevel;

		public SkillRecord(int skillId, int skillLevel)
		{
			this.skillId = skillId;
			this.skillLevel = skillLevel;
		}

		public int getSkillId()
		{
			return skillId;
		}

		public SkillRecord setSkillId(int skillId)
		{
			this.skillId = skillId;
			return this;
		}

		public int getSkillLevel()
		{
			return skillLevel;
		}

		public SkillRecord setSkillLevel(int skillLevel)
		{
			this.skillLevel = skillLevel;
			return this;
		}

		@Override
		public boolean equals(Object o)
		{
			if(this == o)
				return true;

			if(o == null || getClass() != o.getClass())
				return false;

			SkillRecord skillRecord = (SkillRecord) o;
			return new EqualsBuilder().append(skillId, skillRecord.skillId).append(skillLevel, skillRecord.skillLevel).isEquals();
		}

		@Override
		public int hashCode()
		{
			return new HashCodeBuilder(17, 37).append(skillId).append(skillLevel).toHashCode();
		}

		@Override
		public String toString()
		{
			return "SkillRecord{skillId=" + skillId + ", skillLevel=" + skillLevel + '}';
		}
	}
}