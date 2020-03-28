package net.sf.l2j.gameserver.model.entity.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.enums.skills.AbnormalEffect;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.spawn.L2Spawn;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo.NpcInfo;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

/**
 * @author Anarchy
 *
 */
public abstract class AbstractEvent implements Runnable
{
	protected static final CLogger LOGGER = new CLogger(EventManager.class.getName());
	
	private String _name;
	private int _id;
	private EventState _state = EventState.INACTIVE;
	protected List<Player> _players = new ArrayList<>();
	private Map<Player, Integer> _playerScores = new HashMap<>();
	private List<Location> _teleportLocations = new ArrayList<>();
	protected List<EventTeam> _teams = new ArrayList<>();
	private List<Npc> _spawnedNpcs = new ArrayList<>();
	private ScheduledFuture<?> _endTask = null;
	protected EventResTask _eventRes = null;
	private ScheduledFuture<?> _resTask = null;
	protected EventInformation _eventInfo = null;
	private ScheduledFuture<?> _infoTask = null;
	
	private ScheduledFuture<?> _checkSayTask = null;
	private int _runningMinutes;
	
	protected AbstractEvent(String name, int id, int runningMinutes)
	{
		_name = name;
		_id = id;
		_runningMinutes = runningMinutes;
	}
	
	protected int getTopScore()
	{
		int topScore = 0;
		for (Player player : _playerScores.keySet())
		{
			if (_playerScores.get(player) > topScore)
				topScore = _playerScores.get(player);
		}
		
		return topScore;
	}
	
	protected void rewardTopInDraw(IntIntHolder[] rewards)
	{
		int topScore = 0;
		
		for (EventTeam et : _teams)
		{
			if (et.getScore() > topScore)
				topScore = et.getScore();
		}
		
		List<EventTeam> topTeams = new ArrayList<>();
		
		for (EventTeam et : _teams)
		{
			if (et.getScore() == topScore)
				topTeams.add(et);
		}
		
		for (EventTeam et : topTeams)
		{
			for (IntIntHolder reward : rewards)
				et.reward(reward);
		}
	}
	
	protected boolean draw()
	{
		int topScore = 0;
		EventTeam topTeam = null;
		for (EventTeam et : _teams)
		{
			if (et.getScore() > topScore)
			{
				topScore = et.getScore();
				topTeam = et;
			}
		}
		
		if (topScore == 0 || topTeam == null)
			return false;
		
		for (EventTeam et : _teams)
		{
			if (et.getScore() == topScore && et != topTeam)
				return true;
		}
		
		return false;
	}
	
	protected Npc spawnNpc(int npcId, Location location, String title)
	{
		Npc ret = null;
		NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
		try
		{
			L2Spawn spawn = new L2Spawn(template);
			spawn.setLoc(location.getX(), location.getY(), location.getZ(), 0);
			spawn.setRespawnDelay(10);
			SpawnTable.getInstance().addSpawn(spawn, false);
			spawn.doSpawn(false);
			spawn.setRespawnState(false);
			spawn.getNpc().setTitle(title);
			spawn.getNpc().broadcastPacket(new NpcInfo(spawn.getNpc(), null));
			
			ret = spawn.getNpc();
		}
		catch (Exception e)
		{
			LOGGER.info("Event Manager: Unable to spawn npc with id "+ npcId +".");
		}
		
		if (ret != null)
			_spawnedNpcs.add(ret);
		
		return ret;
	}
	
	protected void addTeam(String name, int color, Location location)
	{
		_teams.add(new EventTeam(name, color, location));
	}
	
	public Location getRandomLocation()
	{
		return _teleportLocations.size() > 1 ? _teleportLocations.get(Rnd.get(_teleportLocations.size())) : _teleportLocations.get(0);
	}
	
	protected void increaseScore(Player player, int count)
	{
		if (_playerScores.containsKey(player))
		{
			int old = _playerScores.get(player);
			_playerScores.put(player, old+count);
		}
		else
			_playerScores.put(player, count);
		
		if (!_teams.isEmpty())
			getTeam(player).increaseScore(1);
	}
	
	protected void abort()
	{
		announce("The event was canceled due to lack of participation.", true);
		cleanUp();
	}
	
	protected boolean enoughRegistered(int count)
	{
		return _players.size() >= count;
	}
	
	protected void rewardTopTeams(int top, IntIntHolder[] rewards)
	{
		List<EventTeam> temp = new ArrayList<>();
		temp.addAll(_teams);
		
		for (int i = 1; i <= top; i++)
		{
			EventTeam topTeam = null;
			int score = 0;
			
			for (EventTeam team : temp)
			{
				if (team.getScore() > score)
				{
					topTeam = team;
					score = team.getScore();
				}
			}
			
			if (topTeam == null)
				break;
			
			temp.remove(topTeam);
			
			for (IntIntHolder reward : rewards)
				topTeam.reward(reward);
			
			topTeam = null;
			score = 0;
		}
	}
	
	protected void announceTopTeams(int top)
	{
		List<EventTeam> temp = new ArrayList<>();
		temp.addAll(_teams);
		
		announce("The top team(s) of the event:", true);
		
		for (int i = 1; i <= top; i++)
		{
			EventTeam topTeam = null;
			int score = 0;
			
			for (EventTeam team : temp)
			{
				if (team.getScore() > score)
				{
					topTeam = team;
					score = team.getScore();
				}
			}
			
			if (topTeam == null)
				break;
			
			temp.remove(topTeam);
			announce(i+". Team: "+topTeam.getName()+" Score: "+score, true);
			topTeam = null;
			score = 0;
		}
	}
	
	protected void rewardTop(int top, IntIntHolder[] rewards)
	{
		List<Player> temp = new ArrayList<>();
		temp.addAll(_players);
		
		for (int i = 1; i <= top; i++)
		{
			Player topPlayer = null;
			int score = 0;
			
			for (Player player : temp)
			{
				if (!_playerScores.containsKey(player))
					continue;
				
				if (_playerScores.get(player) > score)
				{
					topPlayer = player;
					score = _playerScores.get(player);
				}
			}
			
			if (topPlayer == null)
				break;
			
			temp.remove(topPlayer);
			
			for (IntIntHolder id : rewards)
				topPlayer.addItem("Event reward.", id.getId(), id.getValue(), null, true);
				
			topPlayer = null;
			score = 0;
		}
	}
	
	protected void announceTop(int top)
	{
		List<Player> temp = new ArrayList<>();
		temp.addAll(_players);
		
		announce("The top player(s) of the event:", true);
		
		for (int i = 1; i <= top; i++)
		{
			Player topPlayer = null;
			int score = 0;
			
			for (Player player : temp)
			{
				if (!_playerScores.containsKey(player))
					continue;
				
				if (_playerScores.get(player) > score)
				{
					topPlayer = player;
					score = _playerScores.get(player);
				}
			}
			
			if (topPlayer == null)
				break;
			
			temp.remove(topPlayer);
			announce(i+". Player: "+topPlayer.getName()+" Score: "+score, true);
			topPlayer = null;
			score = 0;
		}
	}
	
	protected void cleanUp()
	{
		_state = EventState.INACTIVE;
		_players.clear();
		_playerScores.clear();
		
		for (EventTeam et : _teams)
			et.clear();
		
		for (Npc spawn : _spawnedNpcs)
			spawn.deleteMe();
		
		_spawnedNpcs.clear();
		
		if (_endTask != null)
			_endTask.cancel(true);
		
		_endTask = null;
		
		if (_checkSayTask != null)
			_checkSayTask.cancel(true);
		
		_checkSayTask = null;
		
		if (_resTask != null)
		{
			_resTask.cancel(true);
			_resTask = null;
		}
		
		if (_infoTask != null)
		{
			_infoTask.cancel(true);
			_infoTask = null;
		}
		
		if (_eventInfo != null)
		{
			Map<String, Integer> temp = new HashMap<>();
			
			for (String key : _eventInfo.getReplacements().keySet())
				temp.put(key, 0);
			
			_eventInfo.setReplacements(temp);
		}
		EventManager.getInstance().setActiveEvent(null);
		EventManager.getInstance().scheduleNextEvent(false);
	}
	
	protected void restorePlayer(Player player)
	{
		if (player.isOnline())
			EventManager.getInstance().restorePlayer(player);
	}
	
	protected void restorePlayers()
	{
		for (Player player : _players)
		{
			if (player.isOnline())
				EventManager.getInstance().restorePlayer(player);
		}
		
		unparalizePlayers();
	}
	
	protected void end()
	{
		_state = EventState.TELEPORTING;
		announce("The event has ended. Players will be teleported back in 10 seconds.", false);
		paralizePlayers();
		toggleArenaDoors(true);
		schedule(() -> restorePlayers(), 10);
		schedule(() -> cleanUp(), 11);
	}

	protected void onCheck()
	{ }

	protected void start()
	{
		_state = EventState.TELEPORTING;
		announce("The registrations have closed. The event has started.", true);
		announce("You will be teleported in 20 seconds. Get ready!", false);
		preparePlayers();
		toggleArenaDoors(false);
		
		schedule(() -> teleportPlayers(), 20);
	}

	protected void toggleArenaDoors(boolean open)
	{
		for (String doorId : Config.DOOR_LIST)
		{
			final Door door = DoorData.getInstance().getDoor(Integer.parseInt(doorId));
			if (door != null)
			{
				if (open)
					door.openMe();
				else
					door.closeMe();
			}
		}
	}
	
	protected void cancelEndTask()
	{
		_endTask.cancel(true);
		_endTask = null;
	}
	
	protected void cancelCheckTask()
	{
		_checkSayTask.cancel(true);
		_checkSayTask = null;
	}
	
	protected void teleportPlayers()
	{
		_state = EventState.RUNNING;
		
		if (!_teams.isEmpty())
		{
			for (EventTeam team : _teams)
				team.teleportTeam();
		}
		else
		{
			for (Player player : _players)
			{
				player.teleToLocation(getRandomLocation());
				autoBuff(player);
			}
		}
		
		paralizePlayers();
		announce("You have been teleported to the event.", false);
		announce("The event will begin in 20 seconds!", false);
		schedule(() -> begin(), 20);
	}
	
	protected void begin()
	{
		if(!getStartingMsg().equals(""))
		{
			for (Player player : _players)
			{
				player.sendPacket(new ExShowScreenMessage(1, -1, 2, false, 0, 0, 0, false, 3000, false, getStartingMsg()));
				player.sendPacket(new CreatureSay(player.getObjectId(), 1, "Simon Event", getStartingMsg()));
			}
			
			System.out.println(getStartingMsg());
		}
		
		unparalizePlayers();
		announce("The event has begun!", false);
		
		if (getId() == 4) // Simon (without warns to end)
			_checkSayTask = ThreadPool.scheduleAtFixedRate(() -> onCheck(), 10*1000, 30*1000);
		else
		{
			warnEventEnd(_runningMinutes*60);
			schedule(() -> warnEventEnd((_runningMinutes *60) /2), (_runningMinutes *60) /2);
			schedule(() -> warnEventEnd(30), (_runningMinutes*60)-30);
			schedule(() -> warnEventEnd(5), (_runningMinutes*60)-5);
			schedule(() -> warnEventEnd(4), (_runningMinutes*60)-4);
			schedule(() -> warnEventEnd(3), (_runningMinutes*60)-3);
			schedule(() -> warnEventEnd(2), (_runningMinutes*60)-2);
			schedule(() -> warnEventEnd(1), (_runningMinutes*60)-1);
			_endTask = schedule(() -> end(), _runningMinutes*60);
		}

		if (_eventRes != null)
			_resTask = ThreadPool.scheduleAtFixedRate(_eventRes, 8*1000, 8*1000);
		
		if (_eventInfo != null)
			_infoTask = ThreadPool.scheduleAtFixedRate(_eventInfo, 1*1000, 1*1000);
	}
	
	private void warnEventEnd(int seconds)
	{
		int mins = seconds / 60;
		int secs = seconds % 60;
		
		announce((mins == 0 ? "" : mins+" minute(s)")+(mins > 0 && secs > 0 ? " and " : "")+(secs == 0 ? "" : secs+" second(s)")+" remaining until the event ends.", false);
	}
	
	protected void unparalizePlayers()
	{
		for (Player player : _players)
		{
			player.setIsParalyzed(false);
			player.stopAbnormalEffect(AbnormalEffect.ROOT);
		}
	}
	
	protected void paralizePlayers()
	{
		for (Player player : _players)
		{
			player.setIsParalyzed(true);
			player.startAbnormalEffect(AbnormalEffect.ROOT);
		}
	}

	protected void autoBuff(Player player)
	{
		for (int buffId : PlayerData.getInstance().getTemplate(player.getClassId()).getBuffIds())
			SkillTable.getInstance().getInfo(buffId, SkillTable.getInstance().getMaxLevel(buffId)).getEffects(player, player);
	}
	
	protected void addTeleportLocation(Location location)
	{
		_teleportLocations.add(location);
	}
	
	protected void addTeleportLocation(int x, int y, int z)
	{
		_teleportLocations.add(new Location(x, y, z));
	}
	
	protected void preparePlayers()
	{
		EventManager.getInstance().storePlayersData(_players);
		if (!_teams.isEmpty())
			splitToTeams();
	}
	
	protected void splitToTeams()
	{
		int i = 0;
		for (Player player : _players)
		{
			_teams.get(i++).addPlayer(player);
			if (i > _teams.size()-1)
				i = 0;
		}
	}
	
	protected void openRegistrations()
	{	
		_state = EventState.REGISTERING;
		
		spawnNpc(Config.NPC_REGISTER, Config.NPC_REGISTER_LOC, getName());
		
		for (Player player : World.getInstance().getPlayers())
			autoRegister(player);
		
		warnRegistrations(Config.EVENT_REGISTRATION_TIME*60);
		schedule(() -> warnRegistrations((Config.EVENT_REGISTRATION_TIME*60)/2), (Config.EVENT_REGISTRATION_TIME*60)/2);
		schedule(() -> warnRegistrations(30), (Config.EVENT_REGISTRATION_TIME*60)-30);
		schedule(() -> warnRegistrations(5), (Config.EVENT_REGISTRATION_TIME*60)-5);
	}

	public void autoRegister(Player player)
	{
		if (getState() != EventState.REGISTERING)
			return;
		else if (getState() == EventState.REGISTERING)
			player.sendPacket(new ConfirmDlg(SystemMessageId.S1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT_EVENT.getId()).addString(getName()).addTime(15000));
	}
	
	private void warnRegistrations(int seconds)
	{
		int mins = seconds / 60;
		int secs = seconds % 60;
		
		announce("The registrations will close in "+(mins == 0 ? "" : mins+" minute(s)")+(mins > 0 && secs > 0 ? " and " : "")+(secs == 0 ? "" : +secs+" second(s)")+".", true);
	}
	
	protected ScheduledFuture<?> schedule(Runnable task, int seconds)
	{
		return ThreadPool.schedule(task, seconds*1000);
	}
	
	protected void announce(String msg, boolean global)
	{
		if (global)
			EventManager.announce(getName(), msg);
		else
			EventManager.announce(getName(), msg, _players);
	}
	
	public boolean isAutoAttackable(Player attacker, Player target)
	{
		return false;
	}
	
	public void onKill(Player killer, Player victim)
	{ }
	
	public boolean onSay(Player player, String text)
	{ 
		return true;
	}

	public boolean onInterract(Player player, Npc npc)
	{
		return false;
	}
	
	public boolean canAttack(Player attacker, Player target)
	{
		return true;
	}
	
	public boolean canHeal(Player healer, Player target)
	{
		return true;
	}
	
	public boolean canUseItem(Player player, int itemId)
	{
		return true;
	}
	
	public boolean allowDiePacket(Player player)
	{
		return true;
	}
	
	public boolean isDisguisedEvent()
	{
		return false;
	}
	
	public EventTeam getTeam(Player player)
	{
		EventTeam ret = null;
		for (EventTeam team : _teams)
		{
			if (team.inTeam(player))
			{
				ret = team;
				break;
			}
		}
		
		return ret;
	}
	
	protected int getScore(Player player)
	{
		return _playerScores.containsKey(player) ? _playerScores.get(player) : 0;
	}
	
	public boolean isEventNpc(Npc npc)
	{
		return _spawnedNpcs.contains(npc);
	}
	
	public boolean isInEvent(Player player)
	{
		return _players.contains(player);
	}
	
	public List<Player> getPlayers()
	{
		return _players;
	}
	
	public void registerPlayer(Player player)
	{
		_players.add(player);
	}
	
	public void removePlayer(Player player)
	{
		_players.remove(player);
	}
	
	public EventState getState()
	{
		return _state;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public String getStartingMsg()
	{
		return "";
	}
}