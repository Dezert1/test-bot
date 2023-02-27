package com.bottest.scripts.altrecbots;

import com.bottest.scripts.altrecbots.model.ActionPlaybackContext;
import com.bottest.scripts.altrecbots.model.ActionRecordingContext;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import l2.commons.lang.reference.HardReference;
import l2.gameserver.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Contexts
{
	private static final Logger LOGGER = LoggerFactory.getLogger(Contexts.class);
	private static final Contexts INSTANCE = new Contexts();
	private final Map<HardReference<Player>, ActionPlaybackContext> playbackContexts;
	private final Map<HardReference<Player>, ActionRecordingContext> recordingContexts;

	protected Contexts(Map<HardReference<Player>, ActionPlaybackContext> playbackContexts, Map<HardReference<Player>, ActionRecordingContext> recordingContexts)
	{
		this.playbackContexts = playbackContexts;
		this.recordingContexts = recordingContexts;
	}

	private Contexts()
	{
		this(new ConcurrentHashMap<HardReference<Player>, ActionPlaybackContext>(), new ConcurrentHashMap<HardReference<Player>, ActionRecordingContext>());
	}

	public static Contexts getInstance()
	{
		return INSTANCE;
	}

	public Optional<ActionPlaybackContext> getPlaybackContext(Player player)
	{
		return Optional.ofNullable(playbackContexts.get(player.getRef()));
	}

	public Optional<ActionRecordingContext> getRecordingContext(Player player)
	{
		return Optional.ofNullable(recordingContexts.get(player.getRef()));
	}

	public void putContext(Player player, ActionPlaybackContext playbackContext)
	{
		playbackContexts.put(player.getRef(), playbackContext);
	}

	public void putContext(Player player, ActionRecordingContext recordingContext)
	{
		recordingContexts.put(player.getRef(), recordingContext);
	}
}