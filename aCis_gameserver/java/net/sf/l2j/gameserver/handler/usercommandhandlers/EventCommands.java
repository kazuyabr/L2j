package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.engine.EventManager;

/**
 * @author Williams
 *
 */
public class EventCommands implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		117, 118
	};
	
	@Override
	public boolean useUserCommand(int id, Player activeChar)
	{
		if (id == 117)
			EventManager.getInstance().registerPlayer(activeChar);
		else if (id == 118)
			EventManager.getInstance().removePlayer(activeChar);
		
		return true;
	}
	
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}