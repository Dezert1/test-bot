package com.bottest.scripts.altrecbots;

import l2.commons.threading.LoggingRejectedExecutionHandler;
import l2.commons.threading.PriorityThreadFactory;
import l2.commons.threading.RunnableImpl;
import l2.commons.threading.RunnableStatsWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class ThreadPoolManager
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPoolManager.class);
	private static final long MAX_DELAY = TimeUnit.NANOSECONDS.toMillis(Long.MAX_VALUE - System.nanoTime()) / 2;
	private static final ThreadPoolManager _instance = Config.DEDICATED_EXECUTOR ? DedicatedTreadPoolManager.ofDedicated(Config.DEDICATED_EXECUTOR_THREADS, Config.DEDICATED_SCHEDULED_THREADS) : new ThreadPoolManager() {};

	public static ThreadPoolManager getInstance()
	{
		return _instance;
	}

	public ScheduledFuture<?> schedule(RunnableImpl r, long delay)
	{
		return l2.gameserver.ThreadPoolManager.getInstance().schedule(r, delay);
	}

	public void execute(RunnableImpl r)
	{
		l2.gameserver.ThreadPoolManager.getInstance().execute(r);
	}

	private static class DedicatedTreadPoolManager extends ThreadPoolManager
	{
		private final ScheduledThreadPoolExecutor scheduledExecutor;
		private final ThreadPoolExecutor executor;

		private DedicatedTreadPoolManager(ScheduledThreadPoolExecutor scheduledExecutor, ThreadPoolExecutor executor)
		{
			this.scheduledExecutor = scheduledExecutor;
			this.executor = executor;
		}

		private static Runnable wrapper(Runnable r)
		{
			return l2.gameserver.Config.ENABLE_RUNNABLE_STATS ? RunnableStatsWrapper.wrap(r) : r;
		}

		private static long validate(long delay)
		{
			return Math.max(0, Math.min(ThreadPoolManager.MAX_DELAY, delay));
		}

		private static DedicatedTreadPoolManager ofDedicated(int executorSize, int scheduleExecutorSize)
		{
			ThreadPoolExecutor executor = new ThreadPoolExecutor(executorSize, Integer.MAX_VALUE, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("AltRecBotsThreadPoolExecutor", 4), new LoggingRejectedExecutionHandler());
			ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(scheduleExecutorSize, new PriorityThreadFactory("AltRecBotsScheduledThreadPool", 4), new LoggingRejectedExecutionHandler());
			scheduledExecutor.scheduleAtFixedRate(new RunnableImpl()
			{
			    @Override
				public void runImpl() throws Exception
				{
					scheduledExecutor.purge();
					executor.purge();
				}
			}, 15, 15, TimeUnit.MINUTES);
			return new DedicatedTreadPoolManager(scheduledExecutor, executor);
		}

		@Override
		public ScheduledFuture<?> schedule(RunnableImpl r, long delay)
		{
			return scheduledExecutor.schedule(wrapper(r), validate(delay), TimeUnit.MILLISECONDS);
		}

		@Override
		public void execute(RunnableImpl r)
		{
			executor.execute(wrapper(r));
		}
	}
}