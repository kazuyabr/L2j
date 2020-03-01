package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.taskmanager.PremiumTaskManager;

/**
 * @author Williams
 *
 */
public class AdminPremium implements IAdminCommandHandler
{
	private static String[] _adminCommands =
	{
		"admin_setaio",
		"admin_sethero",
		"admin_setvip",
		
		"admin_removeaio",
		"admin_removehero",
		"admin_removevip"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		String player = "";
		int value = 1;
		Player target = null;
		
		// One parameter, player name
		if (st.hasMoreTokens())
		{
			player = st.nextToken();
			target = World.getInstance().getPlayer(player);
			
			// Second parameter, duration
			if (st.hasMoreTokens())
			{
				try
				{
					value = Integer.parseInt(st.nextToken());
				}
				catch (NumberFormatException nfe)
				{
					activeChar.sendMessage("Invalid number format used: " + nfe);
					return false;
				}
			}
		}
		else
		{
			// If there is no name, select target
			if (activeChar.getTarget() != null && activeChar.getTarget() instanceof Player)
				target = (Player) activeChar.getTarget();
		}
		
		if (command.startsWith("admin_setaio"))
		{
			if (target == null && player.equals(""))
			{
				activeChar.sendMessage("Usage: //setaio <char_name> [duration_days]");
				return false;
			}
			
			if (target != null)
				doAio(target, value);
		}
		else if (command.startsWith("admin_removeaio"))
		{
			if (target == null && player.equals(""))
			{
				activeChar.sendMessage("Usage: //removeaio <char_name>");
				return false;
			}
			
			if (target != null)
			{
				if (target.isAio())
			{
					target.setAio(false);
					activeChar.sendMessage(target.getName() + " your Aio has been removed.");
				}
				else
					activeChar.sendMessage(target.getName() + " it's not Aio.");
			}
		}
		else if (command.startsWith("admin_sethero"))
		{
			if (target == null && player.equals(""))
			{
				activeChar.sendMessage("Usage: //sethero <char_name> [duration_days]");
				return false;
			}
			
			if (target != null)
				doHero(target, value);
		}
		else if (command.startsWith("admin_removehero"))
		{
			if (target == null && player.equals(""))
			{
				activeChar.sendMessage("Usage: //removehero <char_name>");
				return false;
			}
			
			if (target != null)
			{
				if (target.isHero())
				{
					target.setHero(false);
					activeChar.sendMessage(target.getName() + " your Hero has been removed.");
				}
				else
					activeChar.sendMessage(target.getName() + " it's not Hero.");
			}
		}
		else if (command.startsWith("admin_setvip"))
		{
			if (target == null && player.equals(""))
			{
				activeChar.sendMessage("Usage: //setvip <char_name> [duration_days]");
				return false;
			}
			
			if (target != null)
				doVip(target, value);
		}
		else if (command.startsWith("admin_removevip"))
		{
			if (target == null && player.equals(""))
			{
				activeChar.sendMessage("Usage: //removevip <char_name>");
				return false;
			}
			
			if (target != null)
			{
				if (target.isVip())
				{
					target.setVip(false);
					activeChar.sendMessage(target.getName() + " your VIP has been removed.");
				}
				else
					activeChar.sendMessage(target.getName() + " it's not Vip.");
			}
		}
		return true;
	}

	public static void doAio(Player target, int time)
	{
		target.setAio(true);
		PremiumTaskManager.getInstance().add(target);
		
		long remainingTime = target.getMemos().getLong("aioTime", 0);
		if (remainingTime > 0)
		{
			target.getMemos().set("aioTime", remainingTime + TimeUnit.DAYS.toMillis(time));
			target.sendMessage(target.getName() + " his Aio was extended by " + time + " day(s).");
		}
		else
		{
			target.getMemos().set("aioTime", System.currentTimeMillis() + TimeUnit.DAYS.toMillis(time));
			target.sendMessage(target.getName() + " now you are Aio, your duration is " + time + " day(s).");
			
			for (IntIntHolder item : Config.LIST_AIO_ITEMS)
			{
				if (item.getId() > 0)
				{
					target.addItem("Add", item.getId(), item.getValue(), target, true);
					target.getInventory().equipItemAndRecord(target.getInventory().getItemByItemId(item.getId()));
				}
			}
		}
	}

	public static void doHero(Player target, int time)
	{
		target.setHero(true);
		PremiumTaskManager.getInstance().add(target);

		long remainingTime = target.getMemos().getLong("heroTime", 0);
		if (remainingTime > 0)
		{
			target.getMemos().set("heroTime", remainingTime + TimeUnit.DAYS.toMillis(time));
			target.sendMessage(target.getName() + " your Hero was extended by " + time + " dias(s).");
		}
		else
		{
			target.getMemos().set("heroTime", System.currentTimeMillis() + TimeUnit.DAYS.toMillis(time));
			target.sendMessage(target.getName() + " now you are a Hero, your duration is " + time + " dia(s).");
		}
	}
	
	public static void doVip(Player target, int time)
	{
		target.setVip(true);
		PremiumTaskManager.getInstance().add(target);
		
		long remainingTime = target.getMemos().getLong("vipTime", 0);
		if (remainingTime > 0)
		{
			target.getMemos().set("vipTime", remainingTime + TimeUnit.DAYS.toMillis(time));
			target.sendMessage(target.getName() + " your VIP was extended by " + time + " day(s).");
		}
		else
		{
			target.getMemos().set("vipTime", System.currentTimeMillis() + TimeUnit.DAYS.toMillis(time));
			target.sendMessage(target.getName() + " now you are a VIP, your duration is " + time + " day(s).");
			
			for (IntIntHolder item : Config.LIST_VIP_ITEMS)
			{
				if (item.getId() > 0)
					target.addItem("Add", item.getId(), item.getValue(), target, true);
			}
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return _adminCommands;
	}
}