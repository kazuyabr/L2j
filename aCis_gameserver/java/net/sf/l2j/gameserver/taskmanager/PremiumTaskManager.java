package net.sf.l2j.gameserver.taskmanager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author williams
 *
 */
public final class PremiumTaskManager implements Runnable
{
	private final Map<Player, Long> _players = new ConcurrentHashMap<>();
	
	protected PremiumTaskManager()
	{
		ThreadPool.scheduleAtFixedRate(this, 1000, 1000);
	}
	
	public final void add(Player player)
	{
		_players.put(player, System.currentTimeMillis());
	}
	
	public final void remove(Player player)
	{
		_players.remove(player);
	}
	
	@Override
	public final void run()
	{
		if (_players.isEmpty())
			return;
		
		for (Map.Entry<Player, Long> entry : _players.entrySet())
		{
			final Player player = entry.getKey();
			
			// AIO
			if (player.isAio())
			{
				if (player.getMemos().getLong("aioTime") < System.currentTimeMillis())
				{
					player.setAio(false);
					remove(player);
				}
			}
			
			// HERO
			if (player.isHero())
			{
				if (player.getMemos().getLong("heroTime") < System.currentTimeMillis())
				{
					player.setHero(false);
					remove(player);
				}
			}
			
			// VIP
			if (player.isVip())
			{
				if (player.getMemos().getLong("vipTime") < System.currentTimeMillis())
				{
					player.setVip(false);
					remove(player);
				}
			}
		}
	}
	
	public static final PremiumTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final PremiumTaskManager _instance = new PremiumTaskManager();
	}
}