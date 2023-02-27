package com.bottest.scripts.altrecbots.model.handler.admincommands;

import com.bottest.scripts.altrecbots.model.AltRecBot;
import com.bottest.scripts.altrecbots.model.BotSpawnManager;
import com.bottest.scripts.altrecbots.model.actions.Action;
import com.bottest.scripts.altrecbots.utils.BotUtils;
import com.bottest.scripts.altrecbots.Config;
import com.bottest.scripts.altrecbots.model.ActionPlaybackContext;
import com.bottest.scripts.altrecbots.model.ActionRecord;

import java.util.Objects;
import l2.commons.threading.RunnableImpl;
import l2.gameserver.ThreadPoolManager;
import l2.gameserver.handler.admincommands.IAdminCommandHandler;
import l2.gameserver.model.GameObjectsStorage;
import l2.gameserver.model.Player;
import l2.gameserver.network.l2.s2c.NpcHtmlMessage;

public class AdminAltRecBots implements IAdminCommandHandler
{
    private static final AdminAltRecBots instance = new AdminAltRecBots();

    public static AdminAltRecBots getInstance()
    {
        return instance;
    }

    enum Commands
    {
        admin_altrec1,
        admin_delete_bot_record,
        admin_kick_bot,
        admin_bots_strategy,
        admin_bots_disable
    }

    private boolean deleteBotRecord(Player activeChar, String[] wordList)
    {
        if(wordList.length < 1)
        {
            activeChar.sendMessage("Usage: admin_delete_bot_record <botId>");
            return false;
        }

        int id = Integer.parseInt(wordList[1]);
        boolean deleted = false;
        for(Player player : GameObjectsStorage.getAllPlayersForIterate())
        {
            if(player == null || !BotUtils.isBot(player))
                continue;
            AltRecBot recBot = (AltRecBot) player;
            ActionPlaybackContext playbackContext = recBot.getPlaybackContext();
            if(playbackContext == null)
                continue;
            ActionRecord actionRecord = playbackContext.getActionRecord();
            if(actionRecord == null || !actionRecord.getId().isPresent() || !Objects.equals(actionRecord.getId().get(), id))
                continue;
            if(BotSpawnManager.getInstance().deleteRecord(actionRecord))
            {
                deleted = true;
                playbackContext.finish(0);
                activeChar.sendMessage("'" + recBot.getName() + "'[" + id + "] deleted and kicked.");
            }
        }
        return deleted;
    }

    private boolean kickBot(Player activeChar, String[] wordList)
    {
        if(wordList.length < 1)
        {
            activeChar.sendMessage("Usage: admin_kick_bot <objId>");
            return false;
        }
        int id = Integer.parseInt(wordList[1]);
        for(Player player : GameObjectsStorage.getAllPlayersForIterate())
        {
            if(player == null || !BotUtils.isBot(player))
                continue;
            AltRecBot altRecBot = (AltRecBot) player;
            ActionPlaybackContext playbackContext = altRecBot.getPlaybackContext();
            if(playbackContext == null)
                continue;
            if(altRecBot.getObjectId() != id)
                continue;
            playbackContext.finish(0);
            activeChar.sendMessage("'" + altRecBot.getName() + "'[" + playbackContext.getActionRecord().getId().get() + "] kicked.");
            return true;
        }
        return false;
    }

    @Override
    public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, Player activeChar) {
        Commands commands = (Commands)comm;
        if(commands == Commands.admin_delete_bot_record)
            return deleteBotRecord(activeChar, wordList);
        if(commands == Commands.admin_kick_bot)
            return kickBot(activeChar, wordList);
        if(commands == Commands.admin_bots_strategy)
        {
            if(wordList.length < 3)
                activeChar.sendMessage("//admin_bots_strategy [Constant|OnlinePercent] <num>");
            else
            {
                Config.BOT_COUNT_SUPPLIER = Config.parseStrategy(wordList[1], wordList[2]);
                activeChar.sendMessage("Bot Strategy now is " + wordList[1] + " with param " + wordList[2]);
            }
        }
        else if(commands == Commands.admin_bots_disable)
        {
            Config.BOTS_ENABLED = false;
            for(Player player : GameObjectsStorage.getAllPlayersForIterate())
            {
                if(player == null || !BotUtils.isBot(player))
                    continue;
                AltRecBot recBot = (AltRecBot) player;
                ActionPlaybackContext playbackContext = recBot.getPlaybackContext();
                if(playbackContext != null)
                {
                    playbackContext.finish(1000L);
                }
                else
                {
                    recBot.stopAllTimers();
                    recBot.getInventory().clear();
                    recBot.setIsOnline(false);
                    recBot.deleteMe();
                }
            }
        }
        else if(commands == Commands.admin_altrec1)
        {
            ThreadPoolManager.getInstance().execute(new RunnableImpl()
            {
                @Override
                public void runImpl() throws Exception
                {
                    for(int i = 0; i < 1000; i++)
                    {
                        BotSpawnManager.getInstance().spawnOne();
                        if(i % 100 == 0)
                            System.out.println("Spawned " + BotSpawnManager.getInstance().getSpawnCounter().get());
                    }
                }
            });
        }
        return false;
    }

    public boolean doOnActionShift(Player player, AltRecBot recBot)
    {
        AltRecBot altRecBot = recBot;
        ActionPlaybackContext playbackContext = altRecBot.getPlaybackContext();
        if (playbackContext == null || !player.isGM())
            return false;

        Action<?> action = playbackContext.getCurrentAction();

        NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
        StringBuilder replyMSG = new StringBuilder("<html><body><br>");
        long lifeTime = System.currentTimeMillis() - playbackContext.getCreatedAt();
        replyMSG.append("Action sequence id: ").append(playbackContext.getActionRecord().getId().get()).append("<br>");
        replyMSG.append("Current action: ").append(action != null ? action.toString() : "(null)").append("<br>");
        replyMSG.append("Next action idx: ").append(playbackContext.getActionIdx().get()).append("<br>");
        replyMSG.append("Lifetime: ").append(lifeTime).append("<br>");
        replyMSG.append("<button value=\"Kick this bot\" action=\"bypass -h admin_kick_bot " + altRecBot.getObjectId() + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"><br>");
        replyMSG.append("<button value=\"Remove this bot\" action=\"bypass -h admin_delete_bot_record " + playbackContext.getActionRecord().getId().get() + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"><br>");
        replyMSG.append("<button value=\"Effects\" action=\"bypass -h admin_show_effects\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"><br>");
        replyMSG.append("TODO ...<br>");
        replyMSG.append("<body></html>");
        adminReply.setHtml(replyMSG.toString());
        player.sendPacket(adminReply);
        return true;
    }

    @Override
    public Enum[] getAdminCommandEnum()
    {
        return Commands.values();
    }
}