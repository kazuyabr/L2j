package net.sf.l2j.gameserver.model.entity.engine;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.gameserver.enums.MessageType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.entity.Duel.DuelState;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.location.Location;

/**
 * @author Anarchy
 */
public class EventTeam
{
	private String _name;
	private int _color;
	private Location _location;
	private List<Player> _players;
	private int _score = 0;
	
	public EventTeam(String name, int color, Location location)
	{
		_name = name;
		_color = color;
		_location = location;
		_players = new CopyOnWriteArrayList<>();
	}
	
	public void clear()
	{
		_score = 0;
		_players.clear();
	}
	
	public void reward(IntIntHolder reward)
	{
		for (Player player : _players) 
			player.addItem("Event reward.", reward.getId(), reward.getValue(), null, true);
	}
	
	public void teleportTeam()
	{
		for (Player player : _players)
		{
			if (player.isCastingNow())
				player.abortCast();
			
			player.getAppearance().setVisible();
			
			if (player.isDead())
				player.doRevive();
			else
			{
				player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
				player.setCurrentCp(player.getMaxCp());
			}
			
			player.stopCubicsByOthers();
			
			if (player.isMounted())
				player.dismount();
			else
			{
				final Summon summon = player.getSummon();
				
				if (summon instanceof Pet)
					summon.unSummon(player);
				else if (summon != null)
				{
					summon.stopAllEffectsExceptThoseThatLastThroughDeath();
					summon.abortAttack();
					summon.abortCast();
				}
			}
			
			if (player.getParty() != null)
			{
				Party party = player.getParty();
				party.removePartyMember(player, MessageType.EXPELLED);
			}
			
			if (player.isInDuel())
				player.setDuelState(DuelState.INTERRUPTED);
			
			player.teleToLocation(_location);
		}
	}
	
	public Location getLocation()
	{
		return _location;
	}
	
	public int getScore()
	{
		return _score;
	}
	
	public void increaseScore(int count)
	{
		_score += count;
	}
	
	public void removePlayer(Player player)
	{
		_players.remove(player);
	}
	
	public void addPlayer(Player player)
	{
		_players.add(player);
		player.getAppearance().setNameColor(_color);
		player.broadcastUserInfo();
	}
	
	public boolean inTeam(Player player)
	{
		return _players.contains(player);
	}
	
	public String getName()
	{
		return _name;
	}
}