package net.sf.l2j.gameguard;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameguard.hwid.HWIDManager;
import net.sf.l2j.gameserver.network.GameClient;
import net.sf.l2j.gameserver.network.serverpackets.ServerClose;
import net.sf.l2j.loginserver.crypt.BlowfishEngine;

/**
 * @author Williams
 *
 */
public class GameGuard
{
	private static final CLogger LOGGER = new CLogger(GameGuard.class.getName());
	
	private static byte[] _key = new byte[16];
	
	public GameGuard()
	{
		HWIDManager.getInstance();
	}
	
	public byte[] getKey(byte[] key)
	{
		byte[] bfkey = {110, 36, 2, 15, -5, 17, 24, 23, 18, 45, 1, 21, 122, 16, -5, 12};
		try
		{
			BlowfishEngine bf = new BlowfishEngine();
			bf.init(true, bfkey);
			bf.processBlock(key, 0, _key, 0);
			bf.processBlock(key, 8, _key, 8);
		}
		catch(IOException e)
		{
			LOGGER.info("Bad key!!!");
		}
		return _key;
	}
	
	public boolean doAuthLogin(GameClient client, byte[] data, String loginName)
	{
		if (!Config.ALLOW_GUARD_SYSTEM)		
			return true;
		
		client.setLogin(loginName);
		String fullHWID = ExtractHWID(data);
		
		if (fullHWID == null)
		{
			LOGGER.info("AuthLogin CRC Check Fail! May be BOT or unprotected client! Client IP: " + client.toString());
			client.close(ServerClose.STATIC_PACKET);
			return false;
		}
		
		int LastError1 = ByteBuffer.wrap(data, 16, 4).getInt();
		if (CheckHWIDs(client, LastError1, 0))
		{
			LOGGER.info("HWID error, look protection_logs.txt file, from IP: " + client.toString());
			client.close(ServerClose.STATIC_PACKET);
			return false;
		}
		
		int VerfiFlag = ByteBuffer.wrap(data, 40, 4).getInt();
		if (!checkVerfiFlag(client, VerfiFlag))
			return false; 
		
		return true;
	}
	
	public static boolean checkVerfiFlag(GameClient client, int flag)
	{
		boolean result = true;
		int fl = Integer.reverseBytes(flag);
		if (fl == -1)
			return false; 
		if (fl == 1342177280)
			return false; 
		if ((fl & 0x1) != 0)
			result = false; 
		if ((fl & 0x10) != 0)
			result = false; 
		if ((fl & 0x10000000) != 0)
			result = false; 
		return result;
	}
	
	public String ExtractHWID(byte[] _data)
	{
		if (!ArraysUtil.verifyChecksum(_data, 0, _data.length))		
			return null;
		
		StringBuilder resultHWID = new StringBuilder();
		
		for (int i = 0; i < (_data.length - 8); ++i)
		{
			resultHWID.append(fillHex(_data[i] & 255, 2));
		}
		
		return resultHWID.toString();
	}
	
	public String fillHex(int data, int digits)
	{
		String number = Integer.toHexString(data);
		
		for (int i = number.length(); i < digits; ++i)
			number = "0" + number;
		
		return number;
	}
	
	public boolean CheckHWIDs(GameClient client, int LastError1, int LastError2)
	{
		boolean resultHWID = false;
		boolean resultLastError = false;
		String HWID = client.getHwid();
		
		if (HWID.equalsIgnoreCase("fab800b1cc9de379c8046519fa841e6"))
		{
			if (Config.PROTECT_KICK_WITH_EMPTY_HWID)
				resultHWID = true;
		}
		
		if (LastError1 != 0)
		{
			if (Config.PROTECT_KICK_WITH_LASTERROR_HWID)
				resultLastError = true;
		}
		
		return resultHWID || resultLastError;
	}
	
	public boolean checkPlayerWithHWID(GameClient client, int playerID, String playerName)
	{
		if (!Config.ALLOW_GUARD_SYSTEM)
			return true;
		
		playerName = client.getPlayer().getName();
		playerID = client.getPlayer().getObjectId();
		
		if (Config.PROTECT_WINDOWS_COUNT > 0)
		{
			final int count = HWIDManager.getInstance().getCountByHWID(client.getHwid());
			if (count > Config.PROTECT_WINDOWS_COUNT && count > HWIDManager.getInstance().getAllowedWindowsCount(client))
			{
				LOGGER.info("Multi windows: " + client.toString());
				client.close(ServerClose.STATIC_PACKET);
				return false;
			}
		}
		addPlayer(client);
		return true;
	}
	
	public void addPlayer(GameClient client)
	{
		if (Config.ALLOW_GUARD_SYSTEM && (client != null))
			HWIDManager.getInstance().addPlayer(client);	
	}
	
	public void removePlayer(GameClient client)
	{
		if (Config.ALLOW_GUARD_SYSTEM && (client != null))		
			HWIDManager.getInstance().removePlayer(client.getPlayer().getName());		
	}
	
	public void doDisconection(GameClient client)
	{
		removePlayer(client);
	}
	
	public static GameGuard getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final GameGuard INSTANCE = new GameGuard();
	}
}