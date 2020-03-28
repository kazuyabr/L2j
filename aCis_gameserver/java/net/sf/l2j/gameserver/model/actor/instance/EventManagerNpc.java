package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.entity.engine.EventManager;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Anarchy
 *
 */
public class EventManagerNpc extends Folk
{
	public EventManagerNpc(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.equals("register"))
			EventManager.getInstance().registerPlayer(player);
		else if (command.equals("remove"))
			EventManager.getInstance().removePlayer(player);
		else
			super.onBypassFeedback(player, command);
	}
	
	private static String getActiveEvent()
	{
		String s = "---";
		
		if (EventManager.getInstance().getActiveEvent() != null)
			s = EventManager.getInstance().getActiveEvent().getName();
		
		return s;
	}
	
	private static int getCurrentReg()
	{
		int i = 0;
		
		if (EventManager.getInstance().getActiveEvent() != null)
			i = EventManager.getInstance().getTotalParticipants();
		
		return i;
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		String name = "data/html/eventmanager/" + getNpcId() + ".htm";
		if (val != 0)
			name = "data/html/eventmanager/" + getNpcId() + "-" + val + ".htm";
		
		NpcHtmlMessage htm = new NpcHtmlMessage(getObjectId());
		htm.setFile(name);
		htm.replace("%activeevent%", getActiveEvent());
		htm.replace("%currentreg%", getCurrentReg());
		htm.replace("%minlevel%", String.valueOf(Config.MIN_LEVEL));
		htm.replace("%maxlevel%", String.valueOf(Config.MAX_LEVEL));
		htm.replace("%objectId%", getObjectId());
		player.sendPacket(htm);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}