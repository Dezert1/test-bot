package com.bottest.scripts.altrecbots.model;

import com.bottest.scripts.altrecbots.Config;
import com.bottest.scripts.altrecbots.ThreadPoolManager;
import com.bottest.scripts.altrecbots.utils.BotUtils;
import java.util.concurrent.CopyOnWriteArrayList;

import l2.commons.threading.RunnableImpl;
import l2.commons.util.Rnd;
import l2.gameserver.dao.CharacterDAO;
import l2.gameserver.model.Creature;
import l2.gameserver.model.Party;
import l2.gameserver.model.Player;
import l2.gameserver.model.Request;
import l2.gameserver.model.items.ItemInstance;
import l2.gameserver.model.items.PcInventory;
import l2.gameserver.model.items.PcWarehouse;
import l2.gameserver.model.items.TradeItem;
import l2.gameserver.network.l2.components.IStaticPacket;
import l2.gameserver.network.l2.components.SystemMsg;
import l2.gameserver.network.l2.s2c.JoinParty;
import l2.gameserver.network.l2.s2c.SystemMessage2;
import l2.gameserver.network.l2.s2c.TradeStart;
import l2.gameserver.templates.PlayerTemplate;
import l2.gameserver.templates.item.ItemTemplate;

/**
 * Класс AltRecBot расширяет класс Player и реализует бота для использования в игре.
 */
public class AltRecBot extends Player
{
    private static final ItemInstance[] EMPTY_ITEM_INSTANCE_ARRAY = new ItemInstance[0];

    private final int botObjId;
    private final AltRecBotInventory botInventory;
    private final AltRecBotWarhouse botWarhouse;
    private ActionPlaybackContext _playbackContext;

    public AltRecBot(int objectId, PlayerTemplate template, String accountName)
    {
        super(objectId, template, accountName);
        this.botObjId = objectId;
        this.botInventory = new AltRecBotInventory(this);
        this.botWarhouse = new AltRecBotWarhouse(this);
        applyMagic(this);
    }

    /**
     * Vетод, который применяет автозамену реализации ивентаря и склада к боту. Данный метод используется только внутри класса AltRecBot.
     * @param recBot
     */
    private static void applyMagic(AltRecBot recBot)
    {
        try
        {
            BotUtils.applyInventoryHack(recBot, recBot.botInventory);
            BotUtils.applyWarhouseHack(recBot, recBot.botWarhouse);
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Метод, который использует предметы, влияющие на дальность атаки (стрелы).
     * Этот метод проверяет все предметы в инвентаре бота, и если предмет является предметом стрельбы, то бот автоматически начинает использовать его.
     */
    public void useShots()
    {
        for(ItemInstance item : getInventory().getItems())
        {
            if(item != null)
            {
                ItemTemplate template = item.getTemplate();
                if(template.isShotItem())
                    addAutoSoulShot(template.getItemId());
            }
        }
        autoShot();
    }

    /**
     * Метод, который возвращает контекст воспроизведения действий бота.
     * @return
     */
    public ActionPlaybackContext getPlaybackContext()
    {
        return _playbackContext;
    }

    /**
     * Метод, который устанавливает контекст воспроизведения действий бота.
     * @param playbackContext
     * @return
     */
    public AltRecBot setPlaybackContext(ActionPlaybackContext playbackContext)
    {
        _playbackContext = playbackContext;
        return this;
    }

    /**
     * Переопределенный метод, который устанавливает запрос для бота.
     * Этот метод позволяет боту принимать или отклонять запросы на торговлю и присоединение к группе.
     * @param transaction
     */
    @Override
    public void setRequest(Request transaction)
    {
        super.setRequest(transaction);
        if(transaction == null || transaction.getReciever() != this)
            return;

        if(transaction.isTypeOf(Request.L2RequestType.CUSTOM))
        {
            transaction.cancel();
            return;
        }

        for(Request.L2RequestType type : Config.BOT_ACCEPT_REQUEST_CHANCE.keySet())
        {
            if(transaction.isTypeOf(type))
            {
                if(Rnd.chance(Config.BOT_ACCEPT_REQUEST_CHANCE.get(type)))
                {
                    ThreadPoolManager.getInstance().schedule(new RunnableImpl()
                    {
                        @Override
                        public void runImpl() throws Exception
                        {
                            Player reciever = transaction.getReciever();
                            if (reciever == null || reciever.getRequest() != transaction)
                                return;

                            Player requestor = transaction.getRequestor();
                            switch (type)
                            {
                                case TRADE_REQUEST:
                                    if(requestor == null || !requestor.isInActingRange(reciever))
                                    {
                                        transaction.cancel();
                                        return;
                                    }
                                    try
                                    {
                                        new Request(Request.L2RequestType.TRADE, reciever, requestor);
                                        requestor.setTradeList(new CopyOnWriteArrayList<TradeItem>());
                                        requestor.sendPacket(new SystemMessage2(SystemMsg.YOU_BEGIN_TRADING_WITH_C1).addString(reciever.getName()), new TradeStart(requestor, reciever));
                                    }
                                    finally
                                    {
                                        transaction.done();
                                    }
                                    break;
                                case PARTY:
                                    if(requestor == null || requestor.getRequest() != transaction || requestor.isOlyParticipant() || requestor.isOutOfControl())
                                    {
                                        transaction.cancel();
                                        return;
                                    }
                                    Party party = requestor.getParty();
                                    if(party != null && party.getMemberCount() >= l2.gameserver.Config.ALT_MAX_PARTY_SIZE)
                                    {
                                        transaction.cancel();
                                        requestor.sendPacket(SystemMsg.THE_PARTY_IS_FULL);
                                        requestor.sendPacket(JoinParty.FAIL);
                                        return;
                                    }
                                    IStaticPacket problem = reciever.canJoinParty(requestor);
                                    if(problem != null || reciever.getParty() != null)
                                    {
                                        transaction.cancel();
                                        requestor.sendPacket(JoinParty.FAIL);
                                        return;
                                    }
                                    if(party == null)
                                    {
                                        int itemDistribution = transaction.getInteger("itemDistribution", 0);
                                        party = new Party(requestor, itemDistribution);
                                        requestor.setParty(party);
                                    }
                                    try
                                    {
                                        reciever.joinParty(party);
                                        requestor.sendPacket(JoinParty.SUCCESS);
                                    }
                                    finally
                                    {
                                        transaction.done();
                                    }
                                    break;
                                default:
                                    transaction.done();
                                    break;
                            }
                        }
                    }, 3000 + Rnd.get(6000));
                }
                return;
            }
        }

        for(Request.L2RequestType type : Config.BOT_DENY_REQUEST_CHANCE.keySet())
        {
            if(transaction.isTypeOf(type))
            {
                if(Rnd.chance(Config.BOT_DENY_REQUEST_CHANCE.get(type)))
                {
                    ThreadPoolManager.getInstance().schedule(new RunnableImpl()
                    {
                        @Override
                        public void runImpl() throws Exception
                        {
                            Player reciever = transaction.getReciever();
                            if(reciever == null || reciever.getRequest() != transaction)
                                return;
                            Player requestor = transaction.getRequestor();
                            transaction.cancel();
                            switch(type)
                            {
                                case PARTY:
                                    if(requestor != null)
                                        requestor.sendPacket(JoinParty.FAIL);
                                    break;
                                case TRADE_REQUEST:
                                    requestor.sendPacket(new SystemMessage2(SystemMsg.C1_HAS_DENIED_YOUR_REQUEST_TO_TRADE).addString(reciever.getName()));
                                    break;
                            }
                        }
                    }, 3000 + Rnd.get(6000));
                    return;
                }
            }
        }
    }

    /**
     * Метод, который возвращает objectId бота.
     * @return
     */
    public int getBotObjId()
    {
        return botObjId;
    }

    @Override
    public void store(boolean fast)
    {}

    @Override
    public void storeDisableSkills()
    {}

    @Override
    public void storeCharSubClasses()
    {}

    @Override
    public AltRecBotInventory getInventory()
    {
        return botInventory;
    }

    @Override
    public AltRecBotWarhouse getWarehouse()
    {
        return botWarhouse;
    }

    @Override
    protected void onDelete()
    {
        super.onDelete();
        getInventory().clear();
        getWarehouse().clear();
        CharacterDAO.getInstance().deleteCharacterDataByObjId(getBotObjId());
    }

    /**
     * Метод, который вызывается, когда бот умирает. В данном случае, метод завершает контекст воспроизведения действий бота.
     * @param killer
     */
    @Override
    protected void onDeath(Creature killer)
    {
        super.onDeath(killer);
        ActionPlaybackContext playbackContext = getPlaybackContext();
        if(playbackContext == null)
            return;
        playbackContext.finish();
    }

    @Override
    public void stopAllTimers()
    {
        super.stopAllTimers();
        ActionPlaybackContext playbackContext = getPlaybackContext();
        if(playbackContext == null)
            return;
        playbackContext.stopNextActionTimer();
    }

    public static class AltRecBotWarhouse extends PcWarehouse
    {
        public AltRecBotWarhouse(Player owner)
        {
            super(owner);
        }

        public AltRecBotWarhouse(int ownerId)
        {
            super(ownerId);
        }

        @Override
        public ItemInstance[] getItems(ItemTemplate.ItemClass itemClass)
        {
            return AltRecBot.EMPTY_ITEM_INSTANCE_ARRAY;
        }

        @Override
        public long getCountOfAdena()
        {
            return 0;
        }

        @Override
        protected void onAddItem(ItemInstance item)
        {
            item.delete();
        }

        @Override
        protected void onModifyItem(ItemInstance item)
        {
            item.delete();
        }

        @Override
        protected void onRemoveItem(ItemInstance item)
        {
            item.delete();
        }

        @Override
        protected void onDestroyItem(ItemInstance item)
        {
            item.delete();
        }

        @Override
        public void restore()
        {}

        @Override
        public void clear()
        {
            writeLock();
            try
            {
                for(ItemInstance item : getItems())
                    destroyItem(item);
            }
            finally
            {
                writeUnlock();
            }
            super.clear();
        }
    }

    public static class AltRecBotInventory extends PcInventory
    {
        public AltRecBotInventory(AltRecBot owner)
        {
            super(owner);
        }

        @Override
        public void restore()
        {}

        @Override
        public void store() {}

        @Override
        protected void onAddItem(ItemInstance item)
        {
            item.setOwnerId(getOwnerId());
            item.setLocation(getBaseLocation());
            item.setLocData(findSlot());
            item.setCustomFlags(47);
        }

        @Override
        protected void onModifyItem(ItemInstance item) {}

        @Override
        protected void onDestroyItem(ItemInstance item)
        {
            if(item == null || (item.getCount() == 0 && item.getLocData() == -1))
                return;
            item.setCount(0);
            item.setLocData(-1);
            item.delete();
        }

        @Override
        protected void onRestoreItem(ItemInstance item)
        {
            _totalWeight += item.getTemplate().getWeight() * item.getCount();
        }

        @Override
        protected void onRemoveItem(ItemInstance item)
        {
            if(item.isEquipped())
                unEquipItem(item);
            item.setCount(0);
            item.setLocData(-1);
            item.delete();
        }

        @Override
        protected void onEquip(int slot, ItemInstance item)
        {
            item.setLocation(getEquipLocation());
            item.setLocData(slot);
            item.setEquipped(true);
            sendModifyItem(item);
            item.setLocation(getEquipLocation());
            item.setLocData(slot);
            if(item.isWeapon() || item.isArmor() || item.isAccessory())
                getListeners().onEquip(slot, item);
        }

        @Override
        protected void onUnequip(int slot, ItemInstance item)
        {
            if(item.isWeapon() || item.isArmor() || item.isAccessory())
                getListeners().onUnequip(slot, item);
            item.setLocation(getBaseLocation());
            item.setLocData(findSlot());
            item.setEquipped(false);
            item.setChargedSpiritshot(0);
            item.setChargedSoulshot(0);
        }

        @Override
        public void clear()
        {
            writeLock();
            try
            {
                for(ItemInstance item : getItems())
                    destroyItem(item);
            }
            finally
            {
                writeUnlock();
            }
            super.clear();
        }

        /**
         * Находит и возвращает пустой слот в инвентаре.
         */
        private int findSlot()
        {
            ItemInstance item;
            int slot = 0;
            loop: for(slot = 0; slot < _items.size(); slot++)
            {
                for(int i = 0; i < _items.size(); i++)
                {
                    item = _items.get(i);
                    if(item.isEquipped() || item.getTemplate().isQuest()) // игнорируем надетое и квестовые вещи
                        continue;
                    if(item.getEquipSlot() == slot) // слот занят?
                        continue loop;
                }
                break;
            }
            return slot; // слот не занят, возвращаем
        }
    }
}
