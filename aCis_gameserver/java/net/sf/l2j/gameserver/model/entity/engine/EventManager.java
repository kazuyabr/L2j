package net.sf.l2j.gameserver.model.entity.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.engine.events.CaptureTheFlag;
import net.sf.l2j.gameserver.model.entity.engine.events.DeathMatch;
import net.sf.l2j.gameserver.model.entity.engine.events.SimonSays;
import net.sf.l2j.gameserver.model.entity.engine.events.TeamVsTeam;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

/**
 * @author Anarchy This class should manage the creation and scheduling of the events, as well as hold information of the participants.
 */
public final class EventManager
{
	private static final CLogger LOGGER = new CLogger(EventManager.class.getName());
	
	private Map<Integer, AbstractEvent> _events = new HashMap<>();
	private List<PlayerEvent> _playersData = new ArrayList<>();
	private List<Integer> _availableEvents = new ArrayList<>();
	private int _nextEvent;
	private AbstractEvent _activeEvent = null;
	
	protected EventManager()
	{
		if (Config.ALLOW_DM_EVENT)
			registerEvent(new DeathMatch());
		
		if (Config.ALLOW_TVT_EVENT)
			registerEvent(new TeamVsTeam());
		
		if (Config.ALLOW_CTF_EVENT)
			registerEvent(new CaptureTheFlag());
		
		if (Config.ALLOW_SIMON_EVENT)
			registerEvent(new SimonSays());
		
		_availableEvents.addAll(_events.keySet());
		_nextEvent = _availableEvents.get(0);
		
		LOGGER.info("Event Manager: Loaded " + _events.size() + " event(s)");
		scheduleNextEvent(true);
	}
	
	public void setActiveEvent(AbstractEvent event)
	{
		_activeEvent = event;
		if (event != null)
			announce("EventManager", "The next event will be " + event.getName() + "!");
	}
	
	@SuppressWarnings("deprecation")
	public void scheduleNextEvent(boolean first)
	{
		if (first)
			ThreadPool.schedule(_events.get(_nextEvent), (Config.TIME_BETWEEN_EVENTS * 1000 * 60) / 2);
		else
			ThreadPool.schedule(_events.get(_nextEvent), Config.TIME_BETWEEN_EVENTS * 1000 * 60);
		
		_availableEvents.remove(new Integer(_nextEvent));
		
		if (_availableEvents.isEmpty())
			_availableEvents.addAll(_events.keySet());
		
		_nextEvent = _availableEvents.get(0);
	}
	
	public int getTotalParticipants()
	{
		return _activeEvent.getPlayers().size();
	}
	
	public void removePlayer(Player player)
	{
		if (_activeEvent == null || _activeEvent.getState() != EventState.REGISTERING)
		{
			player.sendMessage("You cannot unregister now.");
			return;
		}
		
		if (!_activeEvent.isInEvent(player))
		{
			player.sendMessage("You are not registered to the event.");
			return;
		}
		
		_activeEvent.removePlayer(player);
		player.sendMessage("You have successfully unregistered from the event.");
	}
	
	public void registerPlayer(Player player)
	{
		if (_activeEvent == null || _activeEvent.getState() != EventState.REGISTERING)
		{
			player.sendMessage("You cannot register now.");
			return;
		}
		
		if (_activeEvent.isInEvent(player))
		{
			player.sendMessage("You are already registered to the event.");
			return;
		}

		if (player.isFestivalParticipant())
		{
			player.sendMessage("Festival participants cannot register to the event.");
			return;
		}
		
		if (player.isInJail())
		{
			player.sendMessage("Jailed players cannot register to the event.");
			return;
		}
		
		if (player.isDead())
		{
			player.sendMessage("Dead players cannot register to the event.");
			return;
		}

		if (player.isAio())
		{
			player.sendMessage("Aio players cannot register to the event.");
			return;
		}
		
		if (OlympiadManager.getInstance().isRegisteredInComp(player))
		{
			player.sendMessage("Grand Olympiad participants cannot register to the event.");
			return;
		}
		
		if ((player.getLevel() < Config.MIN_LEVEL) || (player.getLevel() > Config.MAX_LEVEL))
		{
			player.sendMessage("You have not reached the appropriate level to join the event.");
			return;
		}
		
		if (player.getClassId() == ClassId.BISHOP || player.getClassId() == ClassId.CARDINAL || player.getClassId() == ClassId.SHILLIEN_ELDER || player.getClassId() == ClassId.SHILLIEN_SAINT || player.getClassId() == ClassId.ELVEN_ELDER || player.getClassId() == ClassId.EVAS_SAINT || player.getClassId() == ClassId.PROPHET || player.getClassId() == ClassId.HIEROPHANT)
		{
			player.sendMessage("Your class are not allowed on event.");
			return;
		}
		
		for (Player registered : _activeEvent.getPlayers())
		{
			if (registered == null)
				continue;
			
			if (registered.getObjectId() == player.getObjectId())
			{
				player.sendMessage("You are already registered in the event.");
				return;
			}
			
			// Check if dual boxing is not allowed
			if (!Config.DUAL_BOX)
			{
				if ((registered.getClient() == null) || (player.getClient() == null))
					continue;
				
				String ip1 = player.getClient().getHwid();
				String ip2 = registered.getClient().getHwid();
				
				if ((ip1 != null) && (ip2 != null) && ip1.equals(ip2))
				{
					player.sendMessage("Your IP is already registered in the event.");
					return;
				}
			}
		}
		
		_activeEvent.registerPlayer(player);
		player.sendMessage("You have successfully registered to the event.");
	}
	
	public void storePlayersData(List<Player> players)
	{
		for (Player player : players)
			_playersData.add(new PlayerEvent(player));
	}
	
	public void restorePlayer(Player player)
	{
		PlayerEvent playerData = null;
		for (PlayerEvent pd : _playersData)
		{
			if (pd.getPlayerId() == player.getObjectId())
			{
				playerData = pd;
				pd.restore(player);
				break;
			}
		}
		
		if (playerData != null)
			_playersData.remove(playerData);
	}
	
	public AbstractEvent getActiveEvent()
	{
		return _activeEvent;
	}
	
	private void registerEvent(AbstractEvent event)
	{
		_events.put(event.getId(), event);
	}
	
	public static void announce(String from, String msg, List<Player> to)
	{
		for (Player player : to)
			player.sendPacket(new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, from, from + ": " + msg));
	}
	
	public static void announce(String from, String msg)
	{
		World.toAllOnlinePlayers(new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, from, from + ": " + msg));
	}
	
	public static EventManager getInstance()
	{
		return SingletonHolder.instance;
	}
	
	private static class SingletonHolder
	{
		protected static final EventManager instance = new EventManager();
	}
}