package com.bottest.scripts.altrecbots.model;

import com.bottest.scripts.altrecbots.Config;
import com.bottest.scripts.altrecbots.ContextHolder;
import com.bottest.scripts.altrecbots.model.actions.Action;
import com.bottest.scripts.altrecbots.model.actions.Attack;
import com.bottest.scripts.altrecbots.model.actions.EquipItem;
import com.bottest.scripts.altrecbots.model.actions.MoveToLocation;
import com.bottest.scripts.altrecbots.model.actions.SkillCast;
import com.bottest.scripts.altrecbots.model.actions.Subclass;
import com.bottest.scripts.altrecbots.model.actions.TeleportTo;
import com.bottest.scripts.altrecbots.model.actions.UnEquipItem;
import com.bottest.scripts.altrecbots.model.listeners.InventoryListener;
import com.bottest.scripts.altrecbots.model.listeners.MoveListener;
import com.bottest.scripts.altrecbots.model.listeners.PlayerListeners;
import com.bottest.scripts.altrecbots.utils.BotUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import l2.gameserver.ai.PlayerAI;
import l2.gameserver.model.Creature;
import l2.gameserver.model.GameObject;
import l2.gameserver.model.Player;
import l2.gameserver.model.Skill;
import l2.gameserver.model.SubClass;
import l2.gameserver.model.base.ClassId;
import l2.gameserver.model.instances.NpcInstance;
import l2.gameserver.model.items.ItemInstance;
import l2.gameserver.templates.item.ItemTemplate;
import l2.gameserver.utils.Location;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Класс ActionRecordingContext представляет собой контекст записи действий игрока.
 * Класс содержит набор методов для записи различных действий, таких как атака, перемещение, использование умения и т. д.
 */
public final class ActionRecordingContext extends BaseContext<Player>
{
    // Объект класса ContextHolder, который содержит список контекстов записи действий игрока.
    private static final ContextHolder<ActionRecordingContext> contexts = new ContextHolder<ActionRecordingContext>();
    private static final long MIN_DURATION = 333L;
    // Флаг, указывающий, закрыт ли контекст записи действий игрока.
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    // Идентификатор объекта игрока в игре.
    private final int objId;
    // Карта, содержащая информацию о подклассах игрока.
    private final Map<Integer, SubClass> subclassesMap = new HashMap<Integer, SubClass>();
    // Список записанных действий игрока.
    private final List<Action> actions = new CopyOnWriteArrayList<Action>();
    // Карта, содержащая информацию об использованных игроком умениях.
    private final Map<Integer, Skill> skills = new HashMap<Integer, Skill>();
    // Карта, содержащая информацию об экипированных предметах игрока.
    private final Map<Integer, Integer> equipedItems = new HashMap<Integer, Integer>();
    // Карта, содержащая информацию о предметах в инвентаре игрока.
    private final Map<ItemTemplate, Pair<Integer, Long>> inventory = new HashMap<ItemTemplate, Pair<Integer, Long>>();
    // Карта, содержащая информацию об опыте, полученном игроком за каждый из подклассов.
    private final Map<Integer, Long> exps = new HashMap<>();
    // Список идентификаторов подклассов игрока.
    private Set<Integer> subclasses = new HashSet<Integer>();
    // Предыдущее время в миллисекундах, когда было записано действие игрока.
    private Long prevTimestamp;
    // Идентификатор первоначального класса игрока.
    private Integer initialClassId;
    // Первоначальная локация игрока.
    private Location initialLoc;
    // Идентификатор активного класса игрока.
    private Optional<Integer> activeClassId = Optional.empty();

    // TODO [V] - поменял с HardReference<Player> на Player, ибо ошибку бьет
    private ActionRecordingContext(Player hardReference)
    {
        super(hardReference);
        objId = hardReference.getObjectId();
    }

    /**
     * Статический метод, возвращающий текущий контекст записи действий игрока, если он существует.
     * @param player
     * @return
     */
    public static Optional<ActionRecordingContext> getRecordingContext(Player player)
    {
        return contexts.getContext(player);
    }

    /**
     * Статический метод, создающий новый контекст записи действий игрока и возвращающий его.
     * @param player
     * @return
     */
    public static ActionRecordingContext openContext(Player player)
    {
        // TODO [V] - с player.getRef() на player, ибо ошибку бьет
        //return contexts.addContext(player, new ActionRecordingContext(player.getRef())).onStart();
        return contexts.addContext(player, new ActionRecordingContext(player)).onStart();
    }

    /**
     * Метод, добавляющий слушателей к игроку для отслеживания действий.
     * @param player
     */
    private static void addListeners(Player player)
    {
        player.getListeners().add(MoveListener.getInstance());
        player.getListeners().add(PlayerListeners.getInstance());
        player.getInventory().getListeners().add(InventoryListener.getInstance());
    }

    /**
     * Метод, удаляющий слушателей игрока.
     * @param player
     */
    private static void removeListeners(Player player)
    {
        player.getInventory().getListeners().remove(InventoryListener.getInstance());
        player.getListeners().remove(PlayerListeners.getInstance());
        player.getListeners().remove(MoveListener.getInstance());
    }

    /**
     * Метод, возвращающий значение флага isClosed.
     * @return
     */
    private boolean isClosed()
    {
        return isClosed.get();
    }

    /**
     * Метод, возвращающий игрока, ассоциированного с текущим контекстом записи действий.
     * @return
     */
    public Optional<Player> getPlayer()
    {
        if(isClosed())
            return Optional.empty();
        return super.getPlayer();
    }

    /**
     * Иинициализирует контекст записи действий игрока, который используется для сохранения записей в базу данных.
     * В этом методе происходит инициализация начальной позиции игрока, времени последнего действия, классов, инвентаря и оборудования игрока, а также подключение слушателей игрока.
     * @return
     */
    protected ActionRecordingContext onStart()
    {
        Optional<Player> optPlayer = getPlayer();
        if(!optPlayer.isPresent())
            return null;
        
        Player player = optPlayer.get();
        initialLoc = player.getLoc().clone();
        prevTimestamp = System.currentTimeMillis();

        for(Map.Entry<Integer, SubClass> entry : player.getSubClasses().entrySet())
        {
            subclasses.add(entry.getKey());
            subclassesMap.put(entry.getKey(), entry.getValue());
            exps.put(entry.getKey(), entry.getValue().getExp());
        }

        initialClassId = player.getActiveClassId();
        activeClassId = Optional.of(player.getActiveClassId());

        for(ItemInstance item : player.getInventory().getPaperdollItems())
        {
            if(item != null)
            {
                equipedItems.put(item.getItemId(), item.getEnchantLevel());
                inventory.put(item.getTemplate(), Pair.of(item.getEnchantLevel(), item.getCount()));
            }
        }

        addListeners(player);
        return this;
    }

    /**
     * Метод close() - закрывает текущий контекст записи действий игрока, и происходит сохранение всех записанных действий в базу данных, если количество записей удовлетворяет конфигурационным параметрам.
     * @param p
     */
    public void close(Player p)
    {
        if(!isClosed.compareAndSet(false, true))
            return;
        
        Player player = null;
        try
        {
            Optional<Player> optPlayer = getPlayer();
            player = optPlayer.orElse(p);
            if(player != null)
            {
                onClose(player);
                removeListeners(player);
            }
        }
        finally
        {
            if(player != null)
                contexts.removeContext(player);
            else
                contexts.removeContext(objId);
            
            if(player != null && !player.isLogoutStarted() && player.isConnected() && Config.AUTO_RECORD_PLAYER_ACTIONS && Config.AUTO_RECORD_INSTANT_NEW_SEQUENCE)
            {
                if(BotUtils.testRecordingCondition(player))
                    openContext(player);
            }
        }
    }

    /**
     * Создает объект записи действий игрока на основе собранных данных и сохраняет запись в базу данных.
     * В этом методе происходит сохранение текущих действий, подклассов, инвентаря и оборудования игрока.
     * @param player
     */
    protected void onClose(Player player)
    {
        if(initialClassId == null || player == null)
            return;
        ActionRecord actionRecord = new ActionRecord(player.getFace(), player.getHairStyle(), player.getHairColor(), player.getSex(), initialLoc, player.isNoble(), new ArrayList<ActionRecord.SubclassRecord>(), new ArrayList<ActionRecord.SkillRecord>(), new ArrayList<ActionRecord.ItemRecord>());
       
        for(SubClass subClass : subclassesMap.values())
        {
            long exp = exps.getOrDefault(subClass.getClassId(), subClass.getExp());
            actionRecord.getSubclasses().add(new ActionRecord.SubclassRecord(subClass.getClassId(), exp, initialClassId.equals(subClass.getClassId()), subClass.isBase()));
        }
        
        for(Map.Entry<ItemTemplate, Pair<Integer, Long>> entry : inventory.entrySet())
            actionRecord.getItems().add(new ActionRecord.ItemRecord(entry.getKey().getItemId(), entry.getValue().getRight(), entry.getValue().getLeft(), equipedItems.containsKey(entry.getKey().getItemId())));
        
        for(Skill skill : skills.values())
            actionRecord.getSkills().add(new ActionRecord.SkillRecord(skill.getId(), skill.getLevel()));
        
        List<Action> actionList = new ArrayList<Action>(actions);
        actionRecord.setActions(actionList);
        
        if(actionList.size() < Config.RECORD_MIN_LENGTH || actionList.size() > Config.RECORD_MAX_LENGTH)
            return;
        
        long allActionDurations = actionList.stream().mapToLong(action -> action.getDuration()).sum();
        if(allActionDurations > Config.RECORD_MAX_DURATION || allActionDurations < Config.RECORD_MIN_DURATION)
            return;
        
        ActionsStorageManager.getInstance().storeRecord(actionRecord);
    }

    /**
     * Метод addAction() - добавляет новое действие игрока в список текущих действий.
     * @param action
     * @param player
     */
    protected void addAction(Action<?> action, Player player)
    {
        long currentTime = System.currentTimeMillis();
        long duration = Math.max(currentTime - prevTimestamp, MIN_DURATION);
        if(actions.size() >= Config.RECORD_MAX_LENGTH || actions.stream().mapToLong(a -> a.getDuration()).sum() + duration > Config.RECORD_MAX_DURATION)
        {
            close(player);
            return;
        }
        actions.add(action.setDuration(duration));
        prevTimestamp = currentTime;
    }

    /**
     * Метод onMove() - записывает перемещение игрока в список текущих действий, если это перемещение является допустимым (например, игрок не телепортируется, не выходит из игры и не производит заклинаний).
     * @param loc
     */
    public void onMove(Location loc)
    {
        Optional<Player> optPlayer = getPlayer();
        if(!optPlayer.isPresent())
            return;

        Player player = optPlayer.get();
        if(!BotUtils.testRecordingCondition(player))
        {
            close(player);
            return;
        }

        Location playerLoc = player.getLoc();
        if(player.isTeleporting() || player.isLogoutStarted() || player.isCastingNow())
            return;

        MoveToLocation moveToLocation = new MoveToLocation(loc, 0, true);
        moveToLocation.setFromLocation(playerLoc);

        GameObject target = player.getTarget();
        if(target != null && target.isNpc())
        {
            moveToLocation = moveToLocation.setTargetNpc(((NpcInstance) target).getNpcId());
        }

        addAction(moveToLocation, player);
    }

    /**
     * Метод onEquip() - записывает экипировку предмета игроком в список текущих действий, если это действие является допустимым (например, игрок не телепортируется, не выходит из игры и не производит заклинаний).
     * @param item
     * @param player
     */
    public void onEquip(ItemInstance item, Player player)
    {
        if(isClosed() || player == null)
            return;
        if(player.isTeleporting() || player.isLogoutStarted() || player.isCastingNow())
            return;
        EquipItem equipItem = new EquipItem(item.getItemId());
        if(item.getEnchantLevel() > 0)
            equipItem.setEnchant(item.getEnchantLevel());
        inventory.put(item.getTemplate(), Pair.of(item.getEnchantLevel(), item.getCount()));
        addAction(equipItem, player);
    }

    /**
     * Метод onUnequip() - записывает снятие предмета с экипировки игроком в список текущих действий, если это действие является допустимым (например, игрок не телепортируется).
     * @param slot
     * @param player
     */
    public void onUnequip(int slot, Player player)
    {
        if(isClosed())
            return;
        UnEquipItem unEquipItem = new UnEquipItem(slot);
        addAction(unEquipItem, player);
    }

    /**
     * Метод onTeleported() - записывает телепортацию игрока в список текущих действий.
     * @param loc
     * @param player
     */
    public void onTeleported(Location loc, Player player)
    {
        Optional<Player> optPlayer = getPlayer();
        if(!optPlayer.isPresent())
            return;
        TeleportTo teleportTo = new TeleportTo(loc.clone());
        addAction(teleportTo, player);
    }

    /**
     * Метод onSkillCast() - записывает использование игроком заклинания в список текущих действий, если это действие является допустимым (например, игрок не телепортируется, не выходит из игры и не перемещается).
     * @param player
     * @param skill
     * @param target
     */
    public void onSkillCast(Player player, Skill skill, Creature target)
    {
        Optional<Player> optional = getPlayer();
        if(!optional.isPresent() || player == null)
            return;
        if(player.isTeleporting() || player.isLogoutStarted() || player.isMoving())
            return;
        SkillCast skillCast = new SkillCast(skill.getId());
        if(target != null)
        {
            if(target.isNpc())
            {
                PlayerAI ai = player.getAI();

                skillCast.setTargetNpcId(target.getNpcId());
                skillCast.setTargetLoc(target.getLoc());

                Optional<Pair<Skill, Boolean>> skillAndForceUse = BotUtils.getSkillAndForceUseFromPlayableAI(ai);
                if(skillAndForceUse.isPresent())
                {
                    Pair<Skill, Boolean> pair = skillAndForceUse.get();
                    if(pair.getLeft() != null && pair.getLeft().getId() == skill.getId() && pair.getRight() != null)
                        skillCast = skillCast.setForceUse(pair.getRight());
                }
            }
        }

        skills.put(skill.getId(), skill);

        addAction(skillCast, player);
    }

    /**
     * Метод onAttack() - записывает атаку игрока в список текущих действий, если это действие является допустимым (например, игрок не телепортируется, не выходит из игры и не производит заклинаний).
     * @param player
     * @param target
     */
    public void onAttack(Player player, Creature target)
    {
        Optional<Player> optPlayer = getPlayer();
        if(!optPlayer.isPresent() || player == null)
            return;
        if(player.isTeleporting() || player.isLogoutStarted() || player.isCastingNow())
            return;
        if(target == null || !target.isNpc())
            return;
        Attack attack = new Attack(target.getNpcId(), target.getLoc());
        addAction(attack, player);
    }

    /**
     * Метод onSetClass() - записывает изменение класса игрока в список текущих действий, если это действие является допустимым (например, игрок не телепортируется, не выходит из игры и не производит заклинаний).
     * @param p
     * @param newClassId
     */
    public void onSetClass(Player p, int newClassId)
    {
        Optional<Player> optPlayer = getPlayer();
        if(!optPlayer.isPresent())
            return;

        Player player = optPlayer.get();
        Set<Integer> subClassIds = new HashSet<Integer>();
        for(Map.Entry<Integer, SubClass> entry : player.getSubClasses().entrySet())
        {
            subClassIds.add(entry.getKey());
            subclassesMap.put(entry.getKey(), entry.getValue());
        }

        Subclass subclass = new Subclass();
        Set<Integer> newClassIds = new HashSet<Integer>();
        for(Integer id : subClassIds)
        {
            if(!subclasses.contains(id))
                newClassIds.add(id);
        }

        Set<Integer> oldClassIds = new HashSet<Integer>();
        for(Integer id : subclasses)
        {
            if(!subClassIds.contains(id))
                oldClassIds.add(id);
        }

        if(oldClassIds.isEmpty() && !newClassIds.isEmpty())
        {
            Optional<ClassId> optNewClassId = BotUtils.toClassId(newClassIds.stream().findFirst().get());
            if(optNewClassId.isPresent())
            {
                Subclass newSubClass = subclass.setSubclassActionType(Subclass.SubclassActionType.AddNew).setClassId(optNewClassId.get());
                GameObject target = player.getTarget();
                if(target != null && target.isNpc())
                    newSubClass = newSubClass.setTargetNpcId(((NpcInstance) target).getNpcId()).setLocation(target.getLoc());
                subclasses = subClassIds;
                addAction(newSubClass, p);
                return;
            }
        }
        else if(!oldClassIds.isEmpty() && !newClassIds.isEmpty())
        {
            Optional<ClassId> optNewClassId = BotUtils.toClassId(newClassIds.stream().findFirst().get());
            Optional<ClassId> optOldClassId = BotUtils.toClassId(oldClassIds.stream().findFirst().get());
            if(optOldClassId.isPresent() && optNewClassId.isPresent())
            {
                Subclass newSubClass = subclass.setSubclassActionType(Subclass.SubclassActionType.Replace).setClassId(optOldClassId.get()).setNewClassId(optNewClassId.get());
                GameObject target = player.getTarget();
                if(target != null && target.isNpc())
                    newSubClass = newSubClass.setTargetNpcId(((NpcInstance) target).getNpcId()).setLocation(target.getLoc());
                addAction(newSubClass, p);
                subclasses = subClassIds;
                return;
            }
        }

        if(activeClassId.isPresent() && !Objects.equals(activeClassId.get(), player.getActiveClassId()))
        {
            Optional<ClassId> optChangeClassId = BotUtils.toClassId(player.getActiveClassId());
            if(optChangeClassId.isPresent())
            {
                Subclass newSubClass = subclass.setSubclassActionType(Subclass.SubclassActionType.Change).setClassId(optChangeClassId.get());
                GameObject target = player.getTarget();
                if(target != null && target.isNpc())
                    newSubClass = newSubClass.setTargetNpcId(((NpcInstance) target).getNpcId()).setLocation(target.getLoc());
                addAction(newSubClass, p);
                activeClassId = Optional.of(player.getActiveClassId());
            }
            return;
        }

        if(subclasses.contains(newClassId))
        {
            Optional<ClassId> optionalNewClassId = BotUtils.toClassId(newClassId);
            if(optionalNewClassId.isPresent())
            {
                Subclass newSubClass = subclass.setSubclassActionType(Subclass.SubclassActionType.SetNew).setClassId(optionalNewClassId.get());
                GameObject target = player.getTarget();
                if(target != null && target.isNpc())
                    newSubClass = newSubClass.setTargetNpcId(((NpcInstance) target).getNpcId()).setLocation(target.getLoc());
                addAction(newSubClass, p);
            }
        }
    }
}