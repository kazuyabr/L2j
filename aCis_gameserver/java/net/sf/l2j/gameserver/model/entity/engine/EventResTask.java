/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model.entity.engine;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author Anarchy
 *
 */
public class EventResTask implements Runnable
{
	private AbstractEvent _event;
	private List<Player> _players;
	
	public EventResTask(AbstractEvent event)
	{
		_event = event;
		_players = new ArrayList<>();
	}
	
	public void addPlayer(Player player)
	{
		_players.add(player);
		player.sendMessage("You have been added to the ressurection task.");
	}
	
	@Override
	public void run()
	{
		if (_event.getState() != EventState.RUNNING)
			return;
		
		for (Player player : _players)
		{
			if (!player.isDead())
				continue;
			
			_event.autoBuff(player);
			
			player.doRevive();
			player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
			
			if (_event.getTeam(player) == null)
				player.teleToLocation(_event.getRandomLocation());
			else
				player.teleToLocation(_event.getTeam(player).getLocation());
		}
	}
}