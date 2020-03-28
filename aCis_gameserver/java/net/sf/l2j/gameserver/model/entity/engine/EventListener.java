package net.sf.l2j.gameserver.model.entity.engine;

import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author Anarchy
 */
public class EventListener
{
	public static boolean isAutoAttackable(Player attacker, Player target)
	{
		if (EventManager.getInstance().getActiveEvent() == null || !(EventManager.getInstance().getActiveEvent().isInEvent(attacker) && EventManager.getInstance().getActiveEvent().isInEvent(target)))
			return false;
		
		return EventManager.getInstance().getActiveEvent().isAutoAttackable(attacker, target);
	}
	
	public static void onKill(Player killer, Player victim)
	{
		if (EventManager.getInstance().getActiveEvent() == null || !(EventManager.getInstance().getActiveEvent().isInEvent(killer) && EventManager.getInstance().getActiveEvent().isInEvent(victim)))
			return;
		
		EventManager.getInstance().getActiveEvent().onKill(killer, victim);
	}
	
	public static boolean onSay(Player player, String text)
	{
		if (EventManager.getInstance().getActiveEvent() == null || !(EventManager.getInstance().getActiveEvent().isInEvent(player)))
			return true;
		
		if (EventManager.getInstance().getActiveEvent().getState() != EventState.RUNNING)
			return true;
		
		return EventManager.getInstance().getActiveEvent().onSay(player, text);
	}
	
	public static void onInterract(Player player, Npc npc)
	{
		if (EventManager.getInstance().getActiveEvent() == null || !EventManager.getInstance().getActiveEvent().isInEvent(player))
			return;
		
		EventManager.getInstance().getActiveEvent().onInterract(player, npc);
	}
	
	public static boolean canAttack(Player attacker, Player target)
	{
		if (EventManager.getInstance().getActiveEvent() == null || !(EventManager.getInstance().getActiveEvent().isInEvent(attacker) && EventManager.getInstance().getActiveEvent().isInEvent(target)))
			return true;
		
		return EventManager.getInstance().getActiveEvent().canAttack(attacker, target);
	}
	
	public static boolean canHeal(Player healer, Player target)
	{
		if (EventManager.getInstance().getActiveEvent() == null || !(EventManager.getInstance().getActiveEvent().isInEvent(healer) && EventManager.getInstance().getActiveEvent().isInEvent(target)))
			return true;
		
		return EventManager.getInstance().getActiveEvent().canHeal(healer, target);
	}
	
	public static boolean canUseItem(Player player, int itemId)
	{
		if (EventManager.getInstance().getActiveEvent() == null || !EventManager.getInstance().getActiveEvent().isInEvent(player))
			return true;
		
		return EventManager.getInstance().getActiveEvent().canUseItem(player, itemId);
	}
	
	public static boolean allowDiePacket(Player player)
	{
		if (EventManager.getInstance().getActiveEvent() == null || !EventManager.getInstance().getActiveEvent().isInEvent(player))
			return true;
		
		return EventManager.getInstance().getActiveEvent().allowDiePacket(player);
	}
}