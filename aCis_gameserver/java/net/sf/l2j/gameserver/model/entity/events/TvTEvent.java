package net.sf.l2j.gameserver.model.entity.events;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.MessageType;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.instance.TvTManager;
import net.sf.l2j.gameserver.model.entity.Duel.DuelState;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;

/**
 * @author Williams
 *
 */
public final class TvTEvent extends Event
{
	private List<Player> _registered = new CopyOnWriteArrayList<>();
	private List<Player> _blueTeam = new CopyOnWriteArrayList<>();
	private List<Player> _redTeam = new CopyOnWriteArrayList<>();
	
	private TvTEvent()
	{
		if (Config.TVT_ENABLE)
		{
			_state = EventState.AWAITING;
			ThreadPool.scheduleAtFixedRate(this, 1000, 1000);
			LOGGER.info("TvT Event: Enable.");
		}
		else
		{
			_state = EventState.INACTIVE;
			LOGGER.info("TvT Event: Disabled.");
		}
	}
	
	@Override
	public void run()
	{
		if (_state == EventState.AWAITING)
		{
			final Calendar calendar = Calendar.getInstance();
			final int hour = calendar.get(Calendar.HOUR_OF_DAY);
			final int minute = calendar.get(Calendar.MINUTE);
			
			for (String time : Config.TVT_SCHEDULER_TIMES)
			{
				final String[] splitTime = time.split(":");
				if (Integer.parseInt(splitTime[0]) == hour && Integer.parseInt(splitTime[1]) == minute)
				{
					register();
					break;
				}
			}
		}
		else if (_state == EventState.REGISTRATION)
		{
			switch (_tick)
			{
				case 3600:
					World.announceToOnlinePlayers("TvT Event: " + _tick / 3600 + " hour(s) until registration is closed!", true);
					break;
					
				case 1800: // 30 minutes left
				case 900: // 15 minutes left
				case 600: //  10 minutes left
				case 300: // 5 minutes left
				case 240: // 4 minutes left
				case 180: // 3 minutes left
				case 120: // 2 minutes left
				case 60: // 1 minute left
					
					World.announceToOnlinePlayers("TvT Event: " + _tick / 60 + " minute(s) until registration is closed!", true);
					break;
					
				case 30: // 30 seconds left
				case 10: // 10 seconds left
				case 5:
					World.announceToOnlinePlayers("TvT Event: " + _tick + " second(s) until registration is closed!", true);
					break;
			}
			
			if (_tick == 0)
				prepareTeams();
			
			_tick--;
		}
		else if (_state == EventState.PROCESSING)
		{
			switch (_tick)
			{
				case 3600:
					World.announceToOnlinePlayers("TvT Event: " + _tick / 3600 + " hour(s) until event is finished!", true);
					break;
					
				case 1800: // 30 minutes left
				case 900: // 15 minutes left
				case 600: //  10 minutes left
				case 300: // 5 minutes left
				case 240: // 4 minutes left
				case 180: // 3 minutes left
				case 120: // 2 minutes left
				case 60: // 1 minute left
					
					World.announceToOnlinePlayers("TvT Event: " + _tick / 60 + " minute(s) until the event is finished!", true);
					break;
					
				case 30: // 30 seconds left
				case 10: // 10 seconds left
				case 5:
					
					World.announceToOnlinePlayers("TvT Event: " + _tick + " second(s) until the event is finished!", true);
					break;
			}
			
			if (_tick == 0)
				eventRemovals();
			
			_tick--;
		}
	}
	
	public void register()
	{
		_state = EventState.REGISTRATION;
		
		_npc = new TvTManager(IdFactory.getInstance().getNextId(), NpcData.getInstance().getTemplate(Config.TVT_NPC_ID));
		_npc.spawnMe(Config.TVT_NPC_LOCATION.getX(), Config.TVT_NPC_LOCATION.getY(), Config.TVT_NPC_LOCATION.getZ(), 0);
		_npc.broadcastPacket(new MagicSkillUse(_npc, _npc, 1034, 1, 1, 1));
		_npc.broadcastNpcSay("Hello adventurers come and have fun with me and win prizes.");
		
		World.announceToOnlinePlayers("TvT Event: Recruiting levels: " + Config.MIN_LEVEL + " to " + Config.MAX_LEVEL + ".", true);
		World.announceToOnlinePlayers("TvT Event: Max players in team: "+ Config.MAX_PARTICIOANTS +".", true);
		World.announceToOnlinePlayers("TvT Event: Commands /register /unregister.", true);
		
		for (Player player : World.getInstance().getPlayers())
			autoRegister(player);
		
		for (IntIntHolder reward: Config.TVT_REWARDS)
			World.announceToOnlinePlayers("TvT Event: Reward "+ reward.getValue() + "-" + ItemTable.getInstance().getTemplate(reward.getId()).getName() , true);
		
		_tick = Config.TVT_PARTICIPATION_TIME * 60;
	}
	
	private void prepareTeams()
	{
		if ((_registered.size() >= Config.MIN_PARTICIOANTS) && (_state != EventState.AWAITING))
		{
			Player player = _registered.get(Rnd.get(_registered.size()));
			
			// First create 2 event teams
			if (_blueTeam.size() > _redTeam.size())
			{
				_redTeam.add(player);
				player.setTeam(TeamType.RED);
			}
			else
			{
				_blueTeam.add(player);
				player.setTeam(TeamType.BLUE);
			}
			
			// Abort casting if player casting
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
			
			// Remove Buffs
			player.stopAllEffectsExceptThoseThatLastThroughDeath();
			
			// stop any cubic that has been given by other player.
			player.stopCubicsByOthers();
			
			// Dismount player, if mounted.
			if (player.isMounted())
				player.dismount();
			// Test summon existence, if any.
			else
			{
				final Summon summon = player.getSummon();
				
				// Unsummon pets directly.
				if (summon instanceof Pet)
					summon.unSummon(player);
				// Remove servitor buffs and cancel animations.
				else if (summon != null)
				{
					summon.stopAllEffectsExceptThoseThatLastThroughDeath();
					summon.abortAttack();
					summon.abortCast();
				}
			}
			
			// Remove player from his party
			if (player.getParty() != null)
			{
				Party party = player.getParty();
				party.removePartyMember(player, MessageType.EXPELLED);
			}
			
			// Remove Duel State
			if (player.isInDuel())
				player.setDuelState(DuelState.INTERRUPTED);
			
			// Close doors
			toggleArenaDoors(false);
			
			// Retrieve the initial Location.
			Location playerCoordinates = new Location(player.getPosition());
			player.setOriginalCoordinates(playerCoordinates);
			
			_registered.remove(player);
			
			_state = EventState.PROCESSING;
			_tick = Config.TVT_RUNNING_TIME * 60;
			
			// Port teams
			for (Player blue : _blueTeam)
				blue.teleToLocation(Config.TVT_BLUE_SPAWN_LOCATION);
			
			for (Player red : _redTeam)
				red.teleToLocation(Config.TVT_RED_SPAWN_LOCATION);
		}
		else
		{
			if (_state == EventState.AWAITING)
				World.announceToOnlinePlayers("TvT Event: Event was cancelled.", true);
			else
				World.announceToOnlinePlayers("TvT Event: Event was cancelled due to lack of participation.", true);
			
			_registered.clear();
			
			if (_npc != null)
			{
				if (_npc.isVisible())
					_npc.deleteMe();
			}
		}
	}
	
	public void eventRemovals()
	{
		// Blue team
		for (Player blue : _blueTeam)
		{
			// Give rewards
			if (_state != EventState.AWAITING && (_blueTeamKills > _redTeamKills || _blueTeamKills == _redTeamKills && Config.REWARD_DIE))
			{
				for (IntIntHolder reward : Config.TVT_REWARDS)
					blue.addItem("TvTReward", reward.getId(), reward.getValue(), null, true);
			}
			
			if (blue.isDead())
				blue.doRevive();
			
			removePlayer(blue);
			blue.teleToLocation(blue.getOriginalCoordinates());
		}
		
		// Red team
		for (Player red : _redTeam)
		{
			// Give rewards
			if (_state != EventState.AWAITING && (_blueTeamKills < _redTeamKills || _blueTeamKills == _redTeamKills && Config.REWARD_DIE))
			{
				for (IntIntHolder reward : Config.TVT_REWARDS)
					red.addItem("TvTReward", reward.getId(), reward.getValue(), null, true);
			}
			
			if (red.isDead())
				red.doRevive();
			
			removePlayer(red);
			red.teleToLocation(red.getOriginalCoordinates());
		}
		
		World.announceToOnlinePlayers("TvT Event: Blue Team kills: " + _blueTeamKills + " , Red Team kills: " + _redTeamKills + ".", true);
		
		// clean kills
		_blueTeamKills = 0;
		_redTeamKills = 0;
		
		// clean teams
		_blueTeam.clear();
		_redTeam.clear();
		
		// open door
		toggleArenaDoors(true);
		
		if (_npc != null)
		{
			if (_npc.isVisible())
				_npc.deleteMe();
		}
		
		_state = EventState.AWAITING;
	}
	
	@Override
	public void autoRegister(Player player)
	{
		if (_state != EventState.REGISTRATION)
			return;
		else if (_state == EventState.REGISTRATION)
			player.sendPacket(new ConfirmDlg(SystemMessageId.S1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPTT.getId()).addString("TvT Event").addTime(15000));
	}
	
	@Override
	public void registerPlayer(Player player)
	{
		if (_state != EventState.REGISTRATION)
		{
			player.sendMessage("TvT Registration is not in progress.");
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
		
		if (_registered.size() == Config.MAX_PARTICIOANTS)
		{
			player.sendMessage("There is no more room for you to register to the event.");
			return;
		}
		
		for (Player registered : _registered)
		{
			if (registered == null)
				continue;
			
			if (registered.getObjectId() == player.getObjectId())
			{
				player.sendMessage("You are already registered in the TvT event.");
				return;
			}
			
			// Check if dual boxing is not allowed
			if (!Config.DUAL_BOX)
			{
				if ((registered.getClient() == null) || (player.getClient() == null))
					continue;
				
				String ip1 = player.getClient().getConnection().getInetAddress().getHostAddress();
				String ip2 = registered.getClient().getConnection().getInetAddress().getHostAddress();
				if ((ip1 != null) && (ip2 != null) && ip1.equals(ip2))
				{
					player.sendMessage("Your IP is already registered in the TvT event.");
					return;
				}
			}
		}
		
		_registered.add(player);
		
		player.sendMessage("You have registered to participate in the TvT Event.");
		
		super.registerPlayer(player);
	}
	
	@Override
	public void removePlayer(Player player)
	{
		if (_registered.contains(player))
		{
			_registered.remove(player);
			player.sendMessage("You have been removed from the TvT Event registration list.");
		}
		else if (player.getTeam() == TeamType.BLUE)
			_blueTeam.remove(player);
		else if (player.getTeam() == TeamType.RED)
			_redTeam.remove(player);
		
		// If no participants left, abort event
		if ((player.getTeam().getId() > 0) && (_blueTeam.size() == 0) && (_redTeam.size() == 0))
			_state = EventState.AWAITING;
		
		// Now, remove team status
		player.setTeam(TeamType.NONE);
		
		super.removePlayer(player);
	}
	
	private static void toggleArenaDoors(boolean open)
	{
		for (String doorId : Config.TVT_DOOR_LIST)
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
	
	@Override
	public boolean isRegistered(Player player)
	{
		return _registered.contains(player);
	}
	
	public List<Player> getBlueTeam()
	{
		return _blueTeam;
	}
	
	public List<Player> getRedTeam()
	{
		return _redTeam;
	}
	
	public List<Player> getRegistered()
	{
		return _registered;
	}
	
	@Override
	public void onDie(Creature killer)
	{
		if (killer instanceof Player)
		{
			final Player player = ((Player) killer);
			
			ThreadPool.schedule(() -> respawnCharacter(player), Config.PLAYER_RESPAWN_DELAY * 1000);
			killer.sendPacket(new ExShowScreenMessage("You will be revived in " + Config.PLAYER_RESPAWN_DELAY + " second(s).", 5000));
		}
	}
	
	private static void respawnCharacter(Player player)
	{
		if (player.isDead())
		{
			player.doRevive();
			
			if (player.getTeam() == TeamType.BLUE)
				player.teleToLocation(Config.TVT_BLUE_SPAWN_LOCATION);
			else if (player.getTeam() == TeamType.RED)
				player.teleToLocation(Config.TVT_RED_SPAWN_LOCATION);
			else
				player.teleportTo(MapRegionData.TeleportType.TOWN);
		}
	}
	
	@Override
	public void onKill(Player player, Player target)
	{
		if (player == null || target == null)
			return;
		
		// Increase kills only if victim belonged to enemy team
		if (player.getTeam() == TeamType.BLUE && target.getTeam() == TeamType.RED)
			_blueTeamKills++;
		else if (player.getTeam() == TeamType.RED && target.getTeam() == TeamType.BLUE)
			_redTeamKills++;
		
		player.broadcastTitleInfo();
		player.broadcastUserInfo();
	}
	
	@Override
	public void onRevive(Creature killer)
	{
		if (killer == null)
			return;
		
		// Heal Player fully
		killer.setCurrentHpMp(killer.getMaxHp(), killer.getMaxMp());
		killer.setCurrentCp(killer.getMaxCp());	
	}
	
	public static final TvTEvent getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TvTEvent INSTANCE = new TvTEvent();
	}
}