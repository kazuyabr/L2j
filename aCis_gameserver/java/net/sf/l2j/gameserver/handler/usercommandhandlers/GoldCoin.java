package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;

/**
 * @author Williams
 *
 */
public class GoldCoin implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		114, 115
	};
	
	@Override
	public boolean useUserCommand(int id, Player activeChar)
	{
		if (id == 114)
		{
			for (IntIntHolder gold : Config.BANKING_SYSTEM_GOLDCOIN)
			{
				if (activeChar.getInventory().getInventoryItemCount(57, 0) >= Config.BANKING_SYSTEM_ADENA)
				{
					activeChar.reduceAdena("Gold Coin", Config.BANKING_SYSTEM_ADENA, activeChar, true);
					activeChar.addItem("Gold Coin", gold.getId(), gold.getValue(), activeChar, true);
				}
				else
					activeChar.sendMessage("You don't have enough Adena to convert to "+  ItemTable.getInstance().getTemplate(gold.getId()).getName() +"(s), You need to " + Config.BANKING_SYSTEM_ADENA + " Adena.");
			}
		}
		else if (id == 115)
		{
			// If player hasn't enough space for adena
			final long a = activeChar.getInventory().getInventoryItemCount(57, 0);
			final long b = Config.BANKING_SYSTEM_ADENA;
			if (a + b > Integer.MAX_VALUE)
			{
				activeChar.sendMessage("You do not have enough space for the entire adena in the inventory!");
				return false;
			}
						
			for (IntIntHolder gold : Config.BANKING_SYSTEM_GOLDCOIN)
			{
				if (activeChar.getInventory().getInventoryItemCount(gold.getId(), 0) >= gold.getValue())
				{
					activeChar.destroyItemByItemId("Adena", gold.getId(), gold.getValue(), activeChar, true);
					activeChar.addAdena("Adena", Config.BANKING_SYSTEM_ADENA, activeChar, true);
				}
				else
					activeChar.sendMessage("You have none "+ ItemTable.getInstance().getTemplate(gold.getId()).getName() +" to exchange for " + Config.BANKING_SYSTEM_ADENA + " Adena.");
			}
		}
		
		return true;
	}
	
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}