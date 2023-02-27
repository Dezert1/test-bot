package com.bottest.scripts.altrecbots.model.actions;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Это перечисление ActionType, которое перечисляет все возможные действия (Action), которые могут быть выполнены ботами.
 * Каждое действие представлено в перечислении соответствующим классом (actionClazz), который реализует интерфейс Action.
 */
public enum ActionType
{
    MOVE_TO_LOCATION(1, MoveToLocation.class),
    EQUIP_ITEM(2, EquipItem.class),
    UNEQUIP_SLOT(3, UnEquipItem.class),
    ITEM_SET_ENCHANT(4, ItemSetEnchant.class),
    ATTACK(5, Attack.class),
    SKILL_CAST(6, SkillCast.class),
    SUBCLASS(7, Subclass.class),
    GAIN_EXP(8, GainExp.class),
    TELEPORT_TO(9, TeleportTo.class),
    USE_ITEM(10, UseItem.class);

    private static final Map<Integer, ActionType> ACTION_TYPE_LEGACY_ORDS = Stream.of(ActionType.values()).collect(Collectors.toMap(ActionType::getLegacyOrd, Function.identity()));
    private final int legacyOrd;
    private final Class<? extends Action> actionClazz;

    private ActionType(int legacyOrd, Class<? extends Action> actionClazz)
    {
        this.legacyOrd = legacyOrd;
        this.actionClazz = actionClazz;
    }

    public static ActionType getActionTypeByLegacyOrd(int ord)
    {
        return ACTION_TYPE_LEGACY_ORDS.get(ord);
    }

    public int getLegacyOrd()
    {
        return legacyOrd;
    }

    public Class<? extends Action> getActionClass()
    {
        return actionClazz;
    }

    @SuppressWarnings("unchecked")
    public <T extends Action> T newActionInstance()
    {
        try
        {
            Action action = actionClazz.newInstance();
            return (T) action;
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}