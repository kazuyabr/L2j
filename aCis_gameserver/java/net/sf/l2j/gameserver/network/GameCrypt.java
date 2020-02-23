package net.sf.l2j.gameserver.network;

import net.sf.l2j.Config;
import net.sf.l2j.gameguard.crypt.L2Client;
import net.sf.l2j.gameguard.crypt.L2Server;
import net.sf.l2j.gameguard.crypt.VMPC;
import net.sf.l2j.gameserver.network.serverpackets.ProtectionCrypt;

public class GameCrypt
{
	private ProtectionCrypt _client;
	private ProtectionCrypt _server;
	private boolean _isEnabled;
	private boolean _isProtected;

	public void setProtected(boolean state)
	{
		_isProtected = state;
	}

	public void setKey(byte[] key)
	{
		if (_isProtected)
		{
			_client = new VMPC();
			_client.setup(key, Config.GUARD_CLIENT_CRYPT);
			_server = new L2Server();
			_server.setup(key, null);
			_server = new VMPC();
			_server.setup(key, Config.GUARD_SERVER_CRYPT);
		}
		else
		{
			_client = new L2Client();
			_client.setup(key, null);
			_server = new L2Server();
			_server.setup(key, null);
		}
	}
	
	public void decrypt(byte[] raw, final int offset, final int size)
	{
		if (_isEnabled)
			_client.crypt(raw, offset, size);
	}
	
	public void encrypt(byte[] raw, final int offset, final int size)
	{
		if (_isEnabled)
			_server.crypt(raw, offset, size);
		else
			_isEnabled = true;
	}
}