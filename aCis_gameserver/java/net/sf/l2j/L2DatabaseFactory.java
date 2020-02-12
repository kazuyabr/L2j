package net.sf.l2j;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class L2DatabaseFactory
{
	protected static Logger _log = Logger.getLogger(L2DatabaseFactory.class.getName());
	
	private HikariDataSource _source;
	
	public static L2DatabaseFactory getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public L2DatabaseFactory() throws SQLException
	{
		try
		{
			_source = new HikariDataSource();
			_source.setDriverClassName("org.mariadb.jdbc.Driver");
			_source.setJdbcUrl(Config.DATABASE_URL);
			_source.setUsername(Config.DATABASE_LOGIN);
			_source.setPassword(Config.DATABASE_PASSWORD);
			_source.setMaximumPoolSize(Math.max(10, Config.DATABASE_MAX_CONNECTIONS));
			_source.setIdleTimeout(0);
			
			// A maximum life time of 15 minutes
			_source.setMaxLifetime(900000);
			
			/* Test the connection */
			_source.getConnection().close();
		}
		catch (SQLException x)
		{
			throw x;
		}
		catch (Exception e)
		{
			throw new SQLException("could not init DB connection:" + e);
		}
	}
	
	public void shutdown()
	{
		try
		{
			_source.close();
		}
		catch (Exception e)
		{
			_log.log(Level.INFO, "", e);
		}
		
		try
		{
			_source = null;
		}
		catch (Exception e)
		{
			_log.log(Level.INFO, "", e);
		}
	}
	
	/**
	 * Use brace as a safty precaution in case name is a reserved word.
	 * @param whatToCheck the list of arguments.
	 * @return the list of arguments between brackets.
	 */
	public static final String safetyString(String... whatToCheck)
	{
		final StringBuilder sb = new StringBuilder();
		for (String word : whatToCheck)
		{
			if (sb.length() > 0)
				sb.append(", ");
			
			sb.append('`');
			sb.append(word);
			sb.append('`');
		}
		return sb.toString();
	}
	
	public Connection getConnection()
	{
		Connection con = null;
		
		while (con == null)
		{
			try
			{
				con = _source.getConnection();
			}
			catch (SQLException e)
			{
				_log.warning("L2DatabaseFactory: getConnection() failed, trying again " + e);
			}
		}
		return con;
	}
	
	private static class SingletonHolder
	{
		protected static final L2DatabaseFactory _instance;
		
		static
		{
			try
			{
				_instance = new L2DatabaseFactory();
			}
			catch (Exception e)
			{
				throw new ExceptionInInitializerError(e);
			}
		}
	}
}