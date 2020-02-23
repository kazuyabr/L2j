package net.sf.l2j.gameguard.hwid;

public class HWIDInfoList
{
	private final int _id;
	private String _hwid;
	private int _count;
	private int _playerID;
	private String _login;
	private LockType _lockType;
	
	public HWIDInfoList(int id)
	{
		_id = id;
	}

	public int getId()
	{
		return _id;
	}
	
	public int getCount()
	{
		return _count;
	}
	
	public void setCount(int count)
	{
		_count = count;
	}
	
	public int getPlayerID()
	{
		return _playerID;
	}
	
	public void setPlayerID(int playerID)
	{
		_playerID = playerID;
	}
	
	public void setLogin(String login)
	{
		_login = login;
	}

	public String getHWID()
	{
		return _hwid;
	}
	
	public void setHWID(String HWID)
	{
		_hwid = HWID;
	}
	
	public LockType getLockType()
	{
		return _lockType;
	}
	
	public String getLogin()
	{
		return _login;
	}
	
	public void setLockType(LockType lockType)
	{
		_lockType = lockType;
	}
	
	public void setHwids(String hwid)
	{
		_hwid = hwid;
		_count = 1;
	}
	
	public static enum LockType
	{
		PLAYER_LOCK,
		ACCOUNT_LOCK,
		NONE;
	}
}