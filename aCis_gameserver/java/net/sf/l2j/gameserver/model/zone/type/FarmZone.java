package net.sf.l2j.gameserver.model.zone.type;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.zone.SpawnZoneType;

/**
 * @author Williams
 *s
 */
public class FarmZone extends SpawnZoneType
{
	private final static List<String> _playerIps = new ArrayList<>();
	
	private String _name;
	private boolean _isNoStore;
	
	public FarmZone(int id)
	{
		super(id);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("name"))
			_name = value;
		else if (name.equals("isNoStore"))
			_isNoStore = Boolean.parseBoolean(value);
	}
	
	@Override
	protected void onEnter(Creature character)
	{
		character.setInsideZone(ZoneId.FARM, true);
		
		if (_isNoStore)
			character.setInsideZone(ZoneId.NO_STORE, true);
		
		if (character instanceof Player)
		{
			final Player playerX = (Player) character;
			
			World.getInstance().getPlayers().stream().filter(playerY -> playerY != null && !playerY.getClient().isDetached()).forEach(playerY ->
			{	
				// check for HIWD
				final String ip1 = playerX.getClient().getHwid();
				final String ip2 = playerY.getClient().getHwid();
				
				// Check players conditions.
				if (!_playerIps.contains(ip1))
					_playerIps.add(ip1);
				else
				{
					_playerIps.remove(ip2);
					playerY.teleportTo(TeleportType.TOWN);
				}
				
				ThreadPool.schedule(() -> playerX.sendMessage("Farm Zone : "+ getCharacters().size() + " players in " + _name), 10 * 1000);
			});
		}
	}
	
	@Override
	protected void onExit(Creature character)
	{
		character.setInsideZone(ZoneId.FARM, false);
		
		if (_isNoStore)
			character.setInsideZone(ZoneId.NO_STORE, false);
		
		if (character instanceof Player)
		{
			final Player player = (Player) character;
		}
		
		_playerIps.clear();
	}
	
	public String getName()
	{
		return _name;
	}
}