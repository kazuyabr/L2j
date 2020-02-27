package net.sf.l2j.gameserver.model.entity.events;

import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author Williams
 *
 */
public abstract class Event implements Runnable
{
	public static final CLogger LOGGER = new CLogger(Event.class.getName());
	
	public enum EventState
	{
		AWAITING,
		REGISTRATION,
		PROCESSING,
		INACTIVE
	}
	
	public EventState _state = EventState.AWAITING;
	
	public abstract boolean isRegistered(Player player);
	public abstract void autoRegister(Player player);
	public abstract void onDie(Creature player);
	public abstract void onKill(Player player, Player target);
	public abstract void onRevive(Creature player);

	public int _blueTeamKills;
	public int _redTeamKills;

	protected Npc _npc;
	protected int _tick;
	
	public boolean isStarted()
	{
		return _state == EventState.PROCESSING;
	}

	public EventState getEventState()
	{
		return _state;
	}
	
	public void setEventState(EventState state)
	{
		_state = state;
	}

	public void registerPlayer(Player player)
	{
		player.setEvent(this);
	}
	
	public void removePlayer(Player player)
	{
		player.setEvent(null);
	}

	public int getBlueKills()
	{
		return _blueTeamKills;
	}
	
	public int getRedKills()
	{
		return _redTeamKills;
	}
}