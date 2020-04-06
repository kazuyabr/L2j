package net.sf.l2j.gameserver.model.entity.engine.events;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.engine.AbstractEvent;
import net.sf.l2j.gameserver.model.entity.engine.EventInformation;
import net.sf.l2j.gameserver.model.entity.engine.EventManager;
import net.sf.l2j.gameserver.model.entity.engine.EventResTask;
import net.sf.l2j.gameserver.model.entity.engine.EventState;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.location.Location;

/**
 * @author Anarchy
 *
 */
public class DeathMatch extends AbstractEvent
{
	public DeathMatch()
	{
		super("DeathMatch", 2, Config.DM_RUNNING_TIME);
		
		for (Location location : Config.DM_RESPAWN_SPOTS)
			addTeleportLocation(location);
		
		_eventRes = new EventResTask(this);
		_eventInfo = new EventInformation(this, "Top kills: %top%");
		_eventInfo.addReplacement("%top%", getTopScore());
	}

	@Override
	public void run()
	{
		EventManager.getInstance().setActiveEvent(this);
		openRegistrations();
		schedule(() -> start(), (Config.EVENT_REGISTRATION_TIME*60));
	}
	
	@Override
	protected void start()
	{
		if (!enoughRegistered(Config.DM_MIN_PLAYERS))
		{
			abort();
			return;
		}
		
		super.start();
	}
	
	@Override
	protected void end()
	{
		announceTop(1);
		rewardTop(1, Config.DM_WINNER_REWARDS);
		super.end();
	}
	
	@Override
	protected void preparePlayers()
	{
		super.preparePlayers();
		for (Player player : _players)
			player.setTitle("Kills: 0");
	}
	
	@Override
	protected void increaseScore(Player player, int count)
	{
		super.increaseScore(player, count);
		
		for (IntIntHolder itemId : Config.DM_ON_KILL_REWARDS)
			player.addItem("Event reward.", itemId.getId(), itemId.getValue(), null, true);
		
		player.setTitle("Kills: "+getScore(player));
		player.broadcastUserInfo();
		_eventInfo.addReplacement("%top%", getTopScore());
	}
	
	@Override
	public boolean isAutoAttackable(Player attacker, Player target)
	{
		return true;
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
		return getState() == EventState.RUNNING;
	}
	
	@Override
	public boolean allowDiePacket(Player player)
	{
		return false;
	}
}