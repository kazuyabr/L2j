package net.sf.l2j.gameserver.taskmanager;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.engine.EventManager;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.zone.type.MultiZone;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;

/**
 * @author Williams
 *
 */
public final class ZoneTaskManager implements Runnable
{
	private int _currentZoneId;
	private int _timer;
	
	public ZoneTaskManager()
	{
		if (getZonesCount() > 1)
			ThreadPool.scheduleAtFixedRate(this, 10000, 1000);
	}
	
	@Override
	public void run()
	{
		switch (_timer)
		{
			case 0:
				selectNextZone();
				break;
				
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 10:
			case 20:
			case 30:
				World.announceToOnlinePlayers("Random zone will change in " + _timer + " second(s).", true);
				break;
				
			case 60:
			case 120:
			case 180:
			case 240:
			case 300:
			case 600:
			case 1200:
			case 1800:
				World.announceToOnlinePlayers("Random zone will change in " + (_timer / 60) + " minute(s).", true);
				break;
				
			case 3600:
			case 7200:
				World.announceToOnlinePlayers("Random zone will change in " + (_timer / 60 / 60) + " hour(s).", true);
				break;
		}
		_timer--;
	}
	
	public void selectNextZone()
	{
		_currentZoneId = _currentZoneId == getZonesCount() ? 1 : ++_currentZoneId;
		
		_timer = getCurrentZone().getTime() * 60;
		
		World.announceToOnlinePlayers("New Random Zone: " + getCurrentZone().getName() + ". Duration: " + getCurrentZone().getTime() + " min(s).", true);
		World.getInstance().getPlayers().stream().filter(player -> player.isInsideZone(ZoneId.MULTI) && getCurrentZone().isActive()).forEach(player -> player.teleportTo(getCurrentZone().getLoc(), 25));
		
		for (Player player : World.getInstance().getPlayers())
			onEnter(player);
	}
	
	public final int getCurrentZoneId()
	{
		return _currentZoneId;
	}
	
	public void onEnter(Player player)
	{
		if (EventManager.getInstance().getActiveEvent() != null && EventManager.getInstance().getActiveEvent().isInEvent(player))
		{
			player.sendMessage("You cannot teleport while in an event.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (player.isFestivalParticipant())
		{
			player.sendMessage("Festival participants cannot teleport.");
			return;
		}
		
		if (player.isInJail())
		{
			player.sendMessage("Jailed players cannot teleport.");
			return;
		}

		if (OlympiadManager.getInstance().isRegisteredInComp(player))
		{
			player.sendMessage("Grand Olympiad participants cannot teleport.");
			return;
		}
		
		if (player.isDead())
			player.doRevive();
		
		if (_currentZoneId > 0)
			player.sendPacket(new ConfirmDlg(SystemMessageId.S1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT_ZONE.getId()).addString(getCurrentZone().getName()).addTime(15000));
	}
	
	private static final int getZonesCount()
	{
		return ZoneManager.getInstance().getAllZones(MultiZone.class).size();
	}
	
	public final MultiZone getCurrentZone()
	{
		return ZoneManager.getInstance().getAllZones(MultiZone.class).stream().filter(t -> t.getId() == _currentZoneId).findFirst().orElse(null);
	}
	
	public static final ZoneTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ZoneTaskManager INSTANCE = new ZoneTaskManager();
	}
}