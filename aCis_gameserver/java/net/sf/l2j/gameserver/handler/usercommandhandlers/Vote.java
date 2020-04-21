package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Williams
 *
 */
public class Vote implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		120
	};
	
	@Override
	public boolean useUserCommand(int id, Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		final StringBuilder sb1 = new StringBuilder();
		final StringBuilder sb2 = new StringBuilder();
		final StringBuilder sb3 = new StringBuilder();
		
		if (activeChar.eligibleToVoteHop())
			sb1.append("<a action=\"bypass -h vote hopzone\"> I have voted on hopzone</a><br1>");
		else
			sb1.append(String.format("Vote again in: %s <br1>", activeChar.getVoteCountdownHop()));
		
		if (activeChar.eligibleToVoteTop())
			sb2.append("<a action=\"bypass -h vote topzone\"> I have voted on topzone</a><br1>");
		else
			sb2.append(String.format("Vote again in: %s <br1>", activeChar.getVoteCountdownTop()));
		
		if (activeChar.eligibleToVoteNet())
			sb3.append("<a action=\"bypass -h vote network\"> I have voted on network</a><br1>");
		else
			sb3.append(String.format("Vote again in: %s <br1>", activeChar.getVoteCountdownNet()));
				
		html.setFile("data/html/vote.htm");
		html.replace("%VoteTopZone%", sb1.toString());
		html.replace("%VoteHopZone%", sb2.toString());
		html.replace("%VoteNetwork%", sb3.toString());
		html.replace("%name%", activeChar.getName());
		
		for (IntIntHolder reward : Config.VOTE_REWARD)
			html.replace("%reward%", ItemTable.getInstance().getTemplate(reward.getId()).getName() +", "+ reward.getValue());
		
		html.replace("%website%", Config.WEB_SERVE);
		activeChar.sendPacket(html);
		
		return true;
	}


	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}
