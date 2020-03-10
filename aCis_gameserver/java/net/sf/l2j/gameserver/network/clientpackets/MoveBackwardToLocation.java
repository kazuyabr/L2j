package net.sf.l2j.gameserver.network.clientpackets;

import java.nio.BufferUnderflowException;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.EnchantResult;
import net.sf.l2j.gameserver.network.serverpackets.StopMove;

public class MoveBackwardToLocation extends L2GameClientPacket
{
	private int _targetX;
	private int _targetY;
	private int _targetZ;
	private int _originX;
	private int _originY;
	private int _originZ;
	
	@SuppressWarnings("unused")
	private int _moveMovement;
	
	@Override
	protected void readImpl()
	{
		_targetX = readD();
		_targetY = readD();
		_targetZ = readD();
		_originX = readD();
		_originY = readD();
		_originZ = readD();
		
		try
		{
			_moveMovement = readD(); // is 0 if cursor keys are used 1 if mouse is used
		}
		catch (BufferUnderflowException e)
		{
			if (Config.L2WALKER_PROTECTION)
			{
				final Player player = getClient().getPlayer();
				if (player != null)
					player.logout(false);
			}
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;

		player.updateLastAction();
		
		if (player.isOutOfControl())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.getActiveEnchantItem() != null)
		{
			player.setActiveEnchantItem(null);
			player.sendPacket(EnchantResult.CANCELLED);
			player.sendPacket(SystemMessageId.ENCHANT_SCROLL_CANCELLED);
		}
		
		if (_targetX == _originX && _targetY == _originY && _targetZ == _originZ)
		{
			player.sendPacket(new StopMove(player));
			return;
		}
		
		// Correcting targetZ from floor level to head level
		_targetZ += player.getCollisionHeight();
		
		if (player.getTeleMode() > 0)
		{
			if (player.getTeleMode() == 1)
				player.setTeleMode(0);
			
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.teleportTo(_targetX, _targetY, _targetZ, 0);
			return;
		}
		
		double dx = _targetX - _originX;
		double dy = _targetY - _originY;
		
		if ((dx * dx + dy * dy) > 98010000) // 9900*9900
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		player.getAI().setIntention(IntentionType.MOVE_TO, new Location(_targetX, _targetY, _targetZ));
	}
}