package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.network.serverpackets.KeyPacket;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;

public final class ProtocolVersion extends L2GameClientPacket
{
	private int _version;
	private byte _data[];
	private String _hwidHdd = "NoHWID-HD";
	private String _hwidMac = "NoHWID-MAC";
	private String _hwidCPU = "NoHWID-CPU";
	
	@Override
	protected void readImpl()
	{
		_version = readD();
		if (Config.ALLOW_GUARD_SYSTEM)
		{
			if (_buf.remaining() > 260) 
			{ 
				_data = new byte[260]; 
				readB(_data);
				
				if (Config.ALLOW_GUARD_SYSTEM)
				{
					_hwidHdd = readS();
					_hwidMac = readS();
					_hwidCPU = readS();
				}
			}
		}
		else if (Config.ALLOW_GUARD_SYSTEM)
			getClient().close(new KeyPacket(getClient().enableCrypt()));
	}
	
	@Override
	protected void runImpl()
	{
		if (Config.ALLOW_GUARD_SYSTEM)
		{
			if (_hwidHdd.equals("NoGuard") && _hwidMac.equals("NoGuard") && _hwidCPU.equals("NoGuard"))
			{
				LOGGER.info("HWID Status: No Client side dlls");
				getClient().close(new KeyPacket(getClient().enableCrypt()));
			}
			
			switch (Config.GET_CLIENT_HWID)
			{
				case 1:
					getClient().setHwid(_hwidHdd);
					break;
				case 2:
					getClient().setHwid(_hwidMac);
					break;
				case 3:
					getClient().setHwid(_hwidCPU);
					break;
			}
		}
		
		switch (_version)
		{
			case 737:
			case 740:
			case 744:
			case 746:
				getClient().sendPacket(new KeyPacket(getClient().enableCrypt()));
				break;
			
			default:
				getClient().close((L2GameServerPacket) null);
				break;
		}
	}
}