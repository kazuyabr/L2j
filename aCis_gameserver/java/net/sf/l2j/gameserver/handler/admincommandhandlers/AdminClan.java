package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.pledge.Clan;

/**
 * @author Williams
 *
 */
public class AdminClan implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_watchclan",
		"admin_stopwatch",
		"admin_clanfull"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		String clanName = command.substring(16);
		final Clan clan = ClanTable.getInstance().getClanByName(clanName);

		if (command.startsWith("admin_watchclan"))
		{
			if (clan == null)
			{
				activeChar.sendMessage("Usage: //watchclan <clanname>");
				return false;
			}
			
			if (clan.getWatchers().contains(activeChar))
			{
				activeChar.sendMessage("You already watch "+ clanName +" clan.");
				return false;
			}
			
			clan.getWatchers().add(activeChar);
			activeChar.sendMessage("You are now a watcher in "+ clanName +" clan.");
		}
		else if (command.startsWith("admin_stopwatch"))
		{
			if (clan == null)
			{
				activeChar.sendMessage("Usage: //stopwatch <clanname>");
				return false;
			}
			
			if (!clan.getWatchers().contains(activeChar))
			{
				activeChar.sendMessage("You don't watch even "+ clanName +" clan.");
				return false;
			}
			
			clan.getWatchers().remove(activeChar);
			activeChar.sendMessage("You are not a watcher anymore in "+ clanName +" clan.");
		}
		else if (command.startsWith("admin_clanfull"))
		{
			if (clan == null)
			{
				activeChar.sendMessage("Usage: //clanfull <clanname>");
				return false;
			}
			
			for (int i = 370; i <= 391; i++)
				clan.addNewSkill(SkillTable.getInstance().getInfo(i, SkillTable.getInstance().getMaxLevel(i)));            
			
			clan.addReputationScore(30000000);
			clan.changeLevel(8);
			clan.updateClanInDB();
			
			activeChar.sendMessage("O Clan " + clan.getName() +" esta clanfull agora!");  
		}
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}