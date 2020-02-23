package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.network.GameClient;

public class GameGuardReply extends L2GameClientPacket
{
	private int _dx;
	
	@Override
	protected void readImpl()
	{
		_dx = readC();
	}
	
	@Override
	protected void runImpl()
	{
		final GameClient client = getClient();
		if (_dx == 104)
			client.setGameGuardOk(true);
		else
			client.setGameGuardOk(false);
	}
}