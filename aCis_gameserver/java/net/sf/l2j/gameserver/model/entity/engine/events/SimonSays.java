package net.sf.l2j.gameserver.model.entity.engine.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.engine.AbstractEvent;
import net.sf.l2j.gameserver.model.entity.engine.EventInformation;
import net.sf.l2j.gameserver.model.entity.engine.EventManager;
import net.sf.l2j.gameserver.model.entity.engine.EventState;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

/**
 * @author Katara
 */
public class SimonSays extends AbstractEvent
{
	private static int _runningTime = 10;
	public static List<Location> SIMON_RESPAWN_SPOTS = new ArrayList<>();
	private int _playersInside = 0;
	private Npc _SimonNpc;
	private String _SimonMessage;
	private Map<Player, String> _playerMessages = new HashMap<>();
	private ScheduledFuture<?> _checkForNoAnswer = null;
	private ScheduledFuture<?> _checkForAnswer = null;
	private int _firstLengthOfRNDSTR = 5;
	private int _roundInGame = 0;
	
	public SimonSays()
	{
		super("SimonSays", 4, _runningTime);

		for (Location location : Config.SIMON_PLAYER_RESPAWN_SPOTS)
			addTeleportLocation(location);

		_eventInfo = new EventInformation(this, "Players Left: %players%");
		_eventInfo.addReplacement("%players%", _playersInside);
	}

	@Override
	public void run()
	{
		EventManager.getInstance().setActiveEvent(this);
		openRegistrations();
		schedule(() -> start(), (Config.EVENT_REGISTRATION_TIME*60)+1);
	}
	
	@Override
	protected void start()
	{
		if (!enoughRegistered(Config.SIMON_MIN_PLAYERS))
		{
			abort();
			return;
		}
		
		_roundInGame = 0;
		_SimonNpc = spawnNpc(50020, Config.SIMON_NPC_RESPAWN_SPOTS, "Simon Beast");

		super.start();
	}
	
	@Override
	protected void begin()
	{
		super.begin();
		
		_roundInGame = 0;
		_eventInfo.addReplacement("%players%", _playersInside);
	}
	
	@Override
	protected void end()
	{
		if (_checkForNoAnswer != null)
			_checkForNoAnswer.cancel(true);
		
		_checkForNoAnswer = null;
		
		if (_checkForAnswer != null)
			_checkForAnswer.cancel(true);
		
		_checkForAnswer = null;
		
		_roundInGame = 0;
		announceTop(1);
		rewardTop(1, Config.SIMON_WINNER_REWARDS);
		super.end();
	}
	
	@Override
	protected void preparePlayers()
	{
		super.preparePlayers();
		_playersInside = _players.size();
	}
	
	@Override
	protected void increaseScore(Player player, int count)
	{
		super.increaseScore(player, count);
	}
	
	@Override
	public boolean isAutoAttackable(Player attacker, Player target)
	{
		return false;
	}
	
	@Override
	public void onKill(Player killer, Player victim)
	{
		increaseScore(killer, 1);
		_eventRes.addPlayer(victim);
	}
	
	@Override
	public boolean canHeal(Player healer, Player target)
	{
		return healer == target;
	}
	
	@Override
	public boolean canAttack(Player attacker, Player target)
	{
		return false;
	}
	
	@Override
	public boolean allowDiePacket(Player player)
	{
		return false;
	}
	
	@Override
	public boolean onSay(Player player, String text)
	{
		if(_playerMessages.containsKey(player)) 
		{
			player.sendMessage("You can't talk until next check of simon.");
			return false;
		}
		
		_playerMessages.put(player, text);
		return true;
	}
	
	@Override
	public void onCheck()
	{
		LOGGER.info("[Simon says] on Check");
		_eventInfo.addReplacement("%players%", _playersInside);
		
		if (_playersInside <= 1)
		{
			cancelCheckTask();
			end();
		}
		else
		{
			String rndText = createNewRandomString(_firstLengthOfRNDSTR + _roundInGame);
			String textToSend = rndText + Config.SIMON_WORDS.get(Rnd.get(0, Config.SIMON_WORDS.size()-1)) + rndText; // if something wrong send the first

			sendToPlayers(textToSend);

			_checkForNoAnswer = schedule(() -> checkForMessages(), Config.SIMON_ROUND_TIME-15); // 15 seconds before check for answers
			_checkForAnswer = schedule(() -> checkTheMessageOfPlayers(), Config.SIMON_ROUND_TIME-1); // 1 second until end round check for answers
		}
	}
	
	private void checkForMessages()
	{
		if (getState() != EventState.RUNNING)
			return;
		
		for (Player player : this.getPlayers())
		{
			if (_playerMessages.containsKey(player))
				continue;
			
			player.sendMessage("You didn't reply to simon. Please reply or you will leave from event");
		}
	}
	
	private void checkTheMessageOfPlayers()
	{
		if (getState() != EventState.RUNNING)
			return;
		
		LOGGER.info("Check Message Of Players");
		
		// First remove all players that answer nothing
		List<Player> _tempList = new ArrayList<>();
		for (Player player : this.getPlayers())
		{
			if (_playerMessages.containsKey(player))
				continue;
			
			_tempList.add(player);
		}
		
		for (Player player : _tempList) 
		{
			player.sendMessage("You lost! You didn't reply to simon.");
			
			removePlayer(player);
			restorePlayer(player);
			
			_playersInside--;
		}

		for (Entry<Player, String> _test : _playerMessages.entrySet()) 
		{
			Player player = _test.getKey();
			String replyOfPlayer = _test.getValue();
			
			if (replyOfPlayer.equalsIgnoreCase(_SimonMessage)) // Correct answer
			{
				player.sendMessage("Your answer was correct. Good job.");
				increaseScore(player, 1);
			}
			else // InCorrect answer
			{
				player.sendMessage("You lost! Your answer was incorrect. Try again next time.");
				
				removePlayer(player);
				restorePlayer(player);
				
				_playersInside--;
			}
		}

		_playerMessages.clear();
		_roundInGame++; // Round ended
	}
	
	@Override
	public String getStartingMsg()
	{
		return "Say excactly as NPC says as fast as possible!";
	}
	
	private void sendToPlayers(String text)
	{
		_SimonMessage = text;
		for (Player player : getPlayers())
			player.sendPacket(new CreatureSay(_SimonNpc.getObjectId(), 1, "Simon", text));
	}
	
	protected static String createNewRandomString(int size)
	{
		String str = "";
		
		for (int i = 0; i < size; i++)
			str += (char) (Rnd.get(26) + 97);
		
		return str;
	}
}