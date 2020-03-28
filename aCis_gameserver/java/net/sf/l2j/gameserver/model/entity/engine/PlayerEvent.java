package net.sf.l2j.gameserver.model.entity.engine;

import java.util.concurrent.TimeUnit;

import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;

/**
 * @author Anarchy
 *
 * This class should hold all the information of a player
 * before he entered an event. It is used to restore
 * players' status after an event, or after disconnection.
 */
public class PlayerEvent
{
	private int _playerId;
	private int _playerColor;
	private String _playerTitle;
	private Location _playerLocation;
	
	public PlayerEvent(Player player)
	{
		_playerId = player.getObjectId();
		_playerColor = player.getAppearance().getNameColor();
		_playerTitle = player.getTitle();
		_playerLocation = new Location(player.getX(), player.getY(), player.getZ());
	}
	
	public void restore(Player player)
	{
		if (player.isDead())
			player.doRevive();
		
		player.getAppearance().setNameColor(_playerColor);
		player.setTitle(_playerTitle);
		player.setTeam(TeamType.NONE);
		player.teleToLocation(_playerLocation);
		player.sendMessage("Your status has been restored after leaving an event.");
		
		// Increase the participated in tasks
		long remainingTime = player.getMemos().getLong("participated_automatic_events", 0);
		if (remainingTime > 0)
			player.getMemos().set("participated_automatic_events", remainingTime + TimeUnit.MINUTES.toMillis(-1));
	}
	
	public int getPlayerId()
	{
		return _playerId;
	}
	
	public int getPlayerColor()
	{
		return _playerColor;
	}
	
	public String getPlayerTitle()
	{
		return _playerTitle;
	}
}