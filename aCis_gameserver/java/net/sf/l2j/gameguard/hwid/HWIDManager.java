package net.sf.l2j.gameguard.hwid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameguard.hwid.HWIDInfoList.LockType;
import net.sf.l2j.gameserver.network.GameClient;


public class HWIDManager
{
	private static final CLogger LOGGER = new CLogger(HWIDManager.class.getName());
	
	private static final Map<Integer, HWIDInfoList> _listHWID = new HashMap<>();
	private static final Map<String, InfoSet> _objects = new HashMap<>();
	
	public HWIDManager()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM hwid_info"))
		{
			try (ResultSet rset = statement.executeQuery())
			{
				int counterHWIDInfo = 0;
				
				while (rset.next())
				{
					HWIDInfoList hInfo = new HWIDInfoList(counterHWIDInfo);
					hInfo.setHwids(rset.getString("HWID"));
					hInfo.setCount(rset.getInt("WindowsCount"));
					hInfo.setLogin(rset.getString("Account"));
					hInfo.setPlayerID(rset.getInt("PlayerID"));
					hInfo.setLockType(LockType.valueOf(rset.getString("LockType")));
					_listHWID.put(Integer.valueOf(counterHWIDInfo), hInfo);
					counterHWIDInfo++;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		LOGGER.info("Loaded " + _listHWID.size() + " HWIDs");
	}
	
	public void updateHWIDInfo(GameClient client, int windowscount)
	{
		updateHWIDInfo(client, windowscount, LockType.NONE);
	}
	
	public void updateHWIDInfo(GameClient client, int windowsCount, LockType lockType)
	{
		int counterHwidInfo = _listHWID.size();
		boolean isFound = false;
		
		for (int i = 0; i < _listHWID.size(); i++)
		{
			if (!_listHWID.get(Integer.valueOf(i)).getHWID().equals(client.getHwid()))
				continue;
			
			isFound = true;
			counterHwidInfo = i;
			break;
		}
		
		HWIDInfoList hInfo = new HWIDInfoList(counterHwidInfo);
		hInfo.setHwids(client.getHwid());
		hInfo.setCount(windowsCount);
		hInfo.setLogin(client.getAccountName());
		hInfo.setPlayerID(client.getPlayer().getObjectId());
		hInfo.setLockType(lockType);
		_listHWID.put(Integer.valueOf(counterHwidInfo), hInfo);
		
		if (isFound)
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("UPDATE hwid_info SET WindowsCount=?,Account=?,PlayerID=?,LockType=? WHERE HWID=?"))
			{
				
				statement.setInt(1, windowsCount);
				statement.setString(2, client.getAccountName());
				statement.setInt(3, client.getPlayer().getObjectId());
				statement.setString(4, lockType.toString());
				statement.setString(5, client.getHwid());
				statement.execute();
			}
			catch (Exception e){}
		}
		else
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("INSERT INTO hwid_info (HWID, WindowsCount, Account, PlayerID, LockType) values (?,?,?,?,?)");)
			{
				statement.setString(1, client.getHwid());
				statement.setInt(2, windowsCount);
				statement.setString(3, client.getAccountName());
				statement.setInt(4, client.getPlayer().getObjectId());
				statement.setString(5, lockType.toString());
				statement.execute();
			}
			catch (Exception e){}
		}
	}
	
	public void updateHWIDInfo(GameClient client, LockType lockType)
	{
		updateHWIDInfo(client, 1, lockType);
	}
	
	public boolean checkLockedHWID(GameClient client)
	{
		if (_listHWID.size() == 0)
			return false;
		
		boolean result = false;
		
		for (int i = 0; i < _listHWID.size(); i++)
		{
			switch (_listHWID.get(Integer.valueOf(i)).getLockType().ordinal())
			{
				case 1:
					break;
				case 2:
					if ((client.getPlayer().getObjectId() == 0) || (_listHWID.get(Integer.valueOf(i)).getPlayerID() != client.getPlayer().getObjectId()))
						continue;
					
					if (_listHWID.get(Integer.valueOf(i)).getHWID().equals(client.getHwid()))
						return false;
					
					result = true;
					break;
				case 3:
					if (!_listHWID.get(Integer.valueOf(i)).getLogin().equals(client.getLogin()))
						continue;
					
					if (_listHWID.get(Integer.valueOf(i)).getHWID().equals(client.getHwid()))
						return false;
					
					result = true;
			}
			
		}
		
		return result;
	}
	
	public int getAllowedWindowsCount(GameClient client)
	{
		if (_listHWID.size() == 0)
			return -1;
		
		for (int i = 0; i < _listHWID.size(); i++)
		{
			if (!_listHWID.get(Integer.valueOf(i)).getHWID().equals(client.getHwid()))
				continue;
			
			if (_listHWID.get(Integer.valueOf(i)).getHWID().equals(""))
				return -1;
			
			return _listHWID.get(Integer.valueOf(i)).getCount();
		}
		
		return -1;
	}
	
	public int getCountHwidInfo()
	{
		return _listHWID.size();
	}
	
	public static HWIDManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final HWIDManager INSTANCE = new HWIDManager();
	}
	
	public class InfoSet
	{
		public String _playerName = "";
		public long _lastGGSendTime;
		public long _lastGGRecvTime;
		public int _attempts;
		public String _HWID = "";
		
		public InfoSet(String name, String HWID)
		{
			_playerName = name;
			_lastGGSendTime = System.currentTimeMillis();
			_lastGGRecvTime = _lastGGSendTime;
			_attempts = 0;
			_HWID = HWID;
		}
	}
	
	public void addPlayer(GameClient client)
	{
		updateHWIDInfo(client, 1);
		_objects.put(client.getPlayer().getName(), new InfoSet(client.getPlayer().getName(), client.getHwid()));
	}
	
	public void removePlayer(String name)
	{
		if (!_objects.containsKey(name))
			_objects.remove(name);
	}

	public int getCountByHWID(String HWID)
	{
		int result = 0;
		for (InfoSet object : _objects.values())
		{
			if (object._HWID.equals(HWID))
				result++; 
		} 
		return result;
	}	
}