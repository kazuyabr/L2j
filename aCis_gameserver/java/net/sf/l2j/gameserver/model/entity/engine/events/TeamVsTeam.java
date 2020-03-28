package net.sf.l2j.gameserver.model.entity.engine.events;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.engine.AbstractEvent;
import net.sf.l2j.gameserver.model.entity.engine.EventInformation;
import net.sf.l2j.gameserver.model.entity.engine.EventManager;
import net.sf.l2j.gameserver.model.entity.engine.EventResTask;
import net.sf.l2j.gameserver.model.entity.engine.EventState;

/**
 * @author Anarchy
 *
 */
public class TeamVsTeam extends AbstractEvent
{
	public TeamVsTeam()
	{
		super("TvT", 3, Config.TVT_RUNNING_TIME);
		
		addTeam(Config.TVT_TEAM_1_NAME, Config.TVT_TEAM_1_COLOR, Config.TVT_TEAM_1_LOCATION);
		addTeam(Config.TVT_TEAM_2_NAME, Config.TVT_TEAM_2_COLOR, Config.TVT_TEAM_2_LOCATION);
		
		_eventRes = new EventResTask(this);
		_eventInfo = new EventInformation(this, Config.TVT_TEAM_1_NAME +": %team1Score% | "+ Config.TVT_TEAM_2_NAME +": %team2Score%");
		_eventInfo.addReplacement("%team1Score%", _teams.get(0).getName().equals(Config.TVT_TEAM_1_NAME) ? _teams.get(0).getScore() : _teams.get(1).getScore());
		_eventInfo.addReplacement("%team2Score%", _teams.get(0).getName().equals(Config.TVT_TEAM_2_NAME) ? _teams.get(0).getScore() : _teams.get(1).getScore());
	}
	
	@Override
	public void run()
	{
		EventManager.getInstance().setActiveEvent(this);
		openRegistrations();
		schedule(() -> start(), (Config.EVENT_REGISTRATION_TIME* 60));
	}
	
	@Override
	protected void start()
	{
		if (!enoughRegistered(Config.TVT_MIN_PLAYERS))
		{
			abort();
			return;
		}
		
		super.start();
	}
	
	@Override
	protected void end()
	{
		if (!draw())
		{
			announceTopTeams(1);
			rewardTopTeams(1, Config.TVT_WINNER_REWARDS);
		}
		else
		{
			announce("The event ended in a draw.", true);
			rewardTopInDraw(Config.TVT_DRAW_REWARDS);
		}
		
		super.end();
	}
	
	@Override
	protected void preparePlayers()
	{
		for (Player player : _players)
			player.setTitle("Kills: 0");
		
		super.preparePlayers();
	}
	
	@Override
	protected void increaseScore(Player player, int count)
	{
		player.setTitle("Kills: " + getScore(player));
		player.broadcastUserInfo();
		_eventInfo.addReplacement("%team1Score%", _teams.get(0).getName().equals(Config.TVT_TEAM_1_NAME) ? _teams.get(0).getScore() : _teams.get(1).getScore());
		_eventInfo.addReplacement("%team2Score%", _teams.get(0).getName().equals(Config.TVT_TEAM_2_NAME) ? _teams.get(0).getScore() : _teams.get(1).getScore());
	
		super.increaseScore(player, count);
	}
	
	@Override
	public boolean isAutoAttackable(Player attacker, Player target)
	{
		return getTeam(attacker) != getTeam(target);
	}
	
	@Override
	public void onKill(Player killer, Player victim)
	{
		if (getTeam(killer) != getTeam(victim))
			increaseScore(killer, 1);
		
		_eventRes.addPlayer(victim);
	}
	
	@Override
	public boolean canHeal(Player healer, Player target)
	{
		return getTeam(healer) == getTeam(target);
	}
	
	@Override
	public boolean canAttack(Player attacker, Player target)
	{
		return getTeam(attacker) != getTeam(target) && getState() == EventState.RUNNING;
	}
	
	@Override
	public boolean allowDiePacket(Player player)
	{
		return false;
	}
}