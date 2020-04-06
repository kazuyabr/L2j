package net.sf.l2j.gameserver.model.entity.engine;

import java.util.HashMap;
import java.util.Map;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;

/**
 * @author Anarchy
 *
 */
public class EventInformation implements Runnable
{
	private AbstractEvent _event;
	private String _msg;
	private Map<String, Integer> _replacements = new HashMap<>();
	
	public EventInformation(AbstractEvent event, String msg)
	{
		_event = event;
		_msg = msg;
	}
	
	public void setReplacements(Map<String, Integer> val)
	{
		_replacements = val;
	}
	
	public Map<String, Integer> getReplacements()
	{
		return _replacements;
	}
	
	public void addReplacement(String id, int value)
	{
		_replacements.put(id, value);
	}
	
	@Override
	public void run()
	{
		String finalMsg = _msg;
		
		for (String r : _replacements.keySet())
			finalMsg = finalMsg.replaceAll(r, _replacements.get(r) + "");
		
		for (Player player : _event.getPlayers())
			player.sendPacket(new ExShowScreenMessage(1, 0, SMPOS.BOTTOM_RIGHT.ordinal(), false, 0, 0, 0, false, 1500, false, finalMsg));
	}
}