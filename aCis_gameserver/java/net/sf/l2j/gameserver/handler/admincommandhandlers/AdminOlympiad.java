package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.commons.util.StatsSet;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;

/**
 * This class handles following admin commands:
 * <ul>
 * <li>endoly : ends olympiads manually.</li>
 * <li>sethero : set the target as a temporary hero.</li>
 * <li>setnoble : set the target as a noble.</li>
 * </ul>
 **/
public class AdminOlympiad implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_addolypoints",
		"admin_removeolypoints",
		"admin_endoly",
		"admin_setnoble"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		
		int value = 0;
		if (st.hasMoreTokens())
		{
			try
			{
				value = Integer.parseInt(st.nextToken());
			}
			catch (NumberFormatException nfe)
			{
				activeChar.sendMessage("Invalid number format used: " + nfe);
				return false;
			}
		}
		
		if (command.startsWith("admin_addolypoints"))
		{
			Player target = null;
			if (activeChar.getTarget() instanceof Player)
				target = (Player) activeChar.getTarget();
			else
				target = activeChar;
			
			if (target == null)
			{
				activeChar.sendMessage("Usage: //addolypoints <char_name> [points]");
				return false;
			}
			
			StatsSet playerStat = Olympiad.getInstance().getNobleStats(target.getObjectId());
			if (playerStat == null)
			{
				activeChar.sendMessage(target.getName() + " not yet registered in the Olympics");
				return false;
			}
			
			int oldpoints = Olympiad.getInstance().getNoblePoints(target.getObjectId());
			int points = oldpoints + value;
			if (points > 9999)
			{
				activeChar.sendMessage("You cannot add more than 9999 points.");
				return false;
			}
			playerStat.set("olympiad_points", points);
			
			activeChar.sendMessage(target.getName() + " now it has " + points + " Olympics points.");
		}
		else if (command.startsWith("admin_removeolypoints"))
		{
			Player target = null;
			if (activeChar.getTarget() instanceof Player)
				target = (Player) activeChar.getTarget();
			else
				target = activeChar;
			
			if (target == null)
			{
				activeChar.sendMessage("Usage: //removeolypoints <char_name> [points]");
				return false;
			}
			
			StatsSet playerStat = Olympiad.getInstance().getNobleStats(target.getObjectId());
			if (playerStat == null)
			{
				activeChar.sendMessage(target.getName() + " not yet registered in the Olympics.");
				return false;
			}
			int oldpoints = Olympiad.getInstance().getNoblePoints(target.getObjectId());
			int points = oldpoints - value;
			
			if (points < 0)
				points = 0;
			
			playerStat.set("olympiad_points", points);
			
			activeChar.sendMessage(target.getName() + " now it has " + points + " Olympics points.");
		}
		else if (command.startsWith("admin_endoly"))
		{
			Olympiad.getInstance().manualSelectHeroes();
			activeChar.sendMessage("Heroes have been formed.");
		}
		else if (command.startsWith("admin_setnoble"))
		{
			Player target = null;
			if (activeChar.getTarget() instanceof Player)
				target = (Player) activeChar.getTarget();
			else
				target = activeChar;
			
			target.setNoble(!target.isNoble(), true);
			activeChar.sendMessage("You have modified " + target.getName() + "'s noble status.");
		}
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}