package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.engine.EventManager;

/**
 * @author Williams
 *
 */
public class BuffVip implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		119
	};
	
	@Override
	public boolean useUserCommand(int id, Player player)
	{
		if ((EventManager.getInstance().getActiveEvent() != null && EventManager.getInstance().getActiveEvent().isInEvent(player)))
		{
			player.sendMessage("You cannot use this command in Event.");
			return false;
		}
		
		if (player.isFestivalParticipant())
		{
			player.sendMessage("Festival participants cannot use this command.");
			return false;
		}
		
		if (player.isInJail())
		{
			player.sendMessage("Jailed players cannot use this command.");
			return false;
		}
		
		if (player.isDead())
		{
			player.sendMessage("Dead players cannot use this command.");
			return false;
		}
		
		if (player.isInOlympiadMode())
		{
			player.sendMessage("Olympics players cannot use this command.");
			return false;
		}
		
		if (player.getPvpFlag() == 1)
		{
			player.sendMessage("Players with Flag status cannot use this command.");
			return false;
		}

		if (player.isVip())
		{
			for (int buffId : PlayerData.getInstance().getTemplate(player.getClassId()).getBuffIds())
				SkillTable.getInstance().getInfo(buffId, SkillTable.getInstance().getMaxLevel(buffId)).getEffects(player, player);
		}
		else 
			player.sendMessage("Only VIP players can use this command.");
		
		return true;
	}
	
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}	
}