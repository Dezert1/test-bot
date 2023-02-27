package com.bottest.scripts.altrecbots.model;

import java.util.Optional;

import l2.gameserver.utils.Location;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Класс BotPhrase описывает фразу, которую может сказать бот.
 */
public class BotPhrase
{
	private final String text;
	private Optional<Integer> sex;
	private Optional<Location> loc;

	public BotPhrase(String text, Optional<Integer> sex, Optional<Location> loc)
	{
		this.text = text;
		this.sex = sex;
		this.loc = loc;
	}

	public BotPhrase(String text, Integer sex)
	{
		this(text, Optional.of(sex), Optional.empty());
	}

	public BotPhrase(String text)
	{
		this(text, Optional.empty(), Optional.empty());
	}

	public String getText()
	{
		return text;
	}

	public Optional<Integer> getSex()
	{
		return sex;
	}

	public BotPhrase setSex(Integer sex)
	{
		this.sex = Optional.ofNullable(sex);
		return this;
	}

	public Optional<Location> getLoc()
	{
		return loc;
	}

	public BotPhrase setLoc(Location loc)
	{
		this.loc = Optional.ofNullable(loc);
		return this;
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
			return true;

		if(o == null || getClass() != o.getClass())
			return false;

		BotPhrase botPhrase = (BotPhrase) o;
		return new EqualsBuilder().append(text, botPhrase.text).append(sex, botPhrase.sex).append(loc, botPhrase.loc).isEquals();
	}

	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(17, 37).append(text).append(sex).append(loc).toHashCode();
	}

	@Override
	public String toString()
	{
		return "BotPhrase{text='" + text + '\'' + ", sex=" + sex + ", loc=" + loc + '}';
	}
}