package net.sf.l2j.gameserver.model.zone.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.zone.SpawnZoneType;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.taskmanager.ZoneTaskManager;

/**
 * A zone extending {@link SpawnZoneType}, where summoning is forbidden. The place is considered a pvp zone (no flag, no karma). It is used for arenas.
 */
public class MultiZone extends SpawnZoneType
{
	private String _name;
	
	private boolean _isNoStore;
	private boolean _isNoSummonFriend;
	private boolean _isFlag;
	private static boolean _isRevive;
	private boolean _isHeal;
	
	private int _time;
	private int _reviveDelay;
	
	private List<Integer> _restrictedItems = new ArrayList<>();
	private List<Location> _reviveLocations = new ArrayList<>();
	
	public MultiZone(int id)
	{
		super(id);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("name"))
			_name = value;
		else if (name.equals("time"))
			_time = Integer.parseInt(value);
		else if (name.equals("isNoStore"))
			_isNoStore = Boolean.parseBoolean(value);
		else if (name.equals("isFlag"))
			_isFlag = Boolean.parseBoolean(value);
		else if (name.equals("isNoSummonFriend"))
			_isNoSummonFriend = Boolean.parseBoolean(value);
		else if (name.equals("isHeal"))
			_isHeal = Boolean.parseBoolean(value);
		else if (name.equals("isRevive"))
			_isRevive = Boolean.parseBoolean(value);
		else if (name.equals("reviveDelay"))
			_reviveDelay = Integer.parseInt(value);
		else if (name.equals("restrictedItems"))
			_restrictedItems= parseIntegers(value);
		else if (name.equals("reviveLocations"))
		{
			for (String locs : value.split(";"))
				_reviveLocations.add(new Location(Integer.valueOf(locs.split(",")[0]), Integer.valueOf(locs.split(",")[1]), Integer.valueOf(locs.split(",")[2])));
		}
		else
			super.setParameter(name, value);
	}
	
	@Override
	protected void onEnter(Creature character)
	{
		if (!isActive())
			return;
		
		if (_isNoStore)
			character.setInsideZone(ZoneId.NO_STORE, true);
		
		if (_isNoSummonFriend)
			character.setInsideZone(ZoneId.NO_SUMMON_FRIEND, true);
		
		if (character instanceof Player || character instanceof Pet)
		{
			final Player player = (Player) character;
			player.sendPacket(new ExShowScreenMessage("You have entered a multi zone.", 5000));
			
			if (_isFlag)
				player.updatePvPFlag(1);
			
			checkItemRestriction(player);
		}
		
		character.setInsideZone(ZoneId.MULTI, true);
	}
	
	@Override
	protected void onExit(Creature character)
	{
		if (!isActive())
			return;

		if (_isNoStore)
			character.setInsideZone(ZoneId.NO_STORE, false);
		
		if (_isNoSummonFriend)
			character.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false);
		
		if (character instanceof Player)
		{
			final Player player = (Player) character;
			player.sendPacket(new ExShowScreenMessage("You have left a multi zone.", 5000));
			
			if (_isFlag)
				player.updatePvPFlag(0);
		}
		
		character.setInsideZone(ZoneId.MULTI, false);
	}
	
	public void onDie(Creature character)
	{
		if (!isActive())
			return;

		if (character instanceof Player && _isRevive)
		{
			ThreadPool.schedule(() -> respawnCharacter(character), _reviveDelay * 1000);
			character.sendPacket(new ExShowScreenMessage("You will be revived in " + _reviveDelay + " second(s).", 5000));
		}
	}
	
	public void onRevive(Creature character)
	{
		if (!isActive())
			return;

		if (character instanceof Player && _isHeal)
		{
			final Summon pet = character.getSummon();
			if (pet != null)
				pet.setCurrentHpMp(pet.getMaxHp(), pet.getMaxMp());
			
			character.setCurrentCp(character.getMaxCp());
			character.setCurrentHpMp(character.getMaxHp(), character.getMaxMp());
		}
	}
	
	private void respawnCharacter(Creature character)
	{
		if (character.isDead())
		{
			character.doRevive();
			character.teleportTo(Rnd.get(_reviveLocations), 20);
		}
	}
	
	public void checkItemRestriction(Player player)
	{
		for (ItemInstance item : player.getInventory().getPaperdollItems())
		{
			if (item == null || !isRestrictedItem(item.getItemId()))
				continue;
			
			player.getInventory().unEquipItemInSlot(item.getLocationSlot());
			
			InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(item);
			player.sendPacket(iu);
		}
	}
	
	public List<Integer> parseIntegers(String line)
	{
		final List<Integer> value = new ArrayList<>();
		Arrays.asList(line.split(";")).forEach(id -> value.add(Integer.parseInt(id)));
		
		return value;
	}

	public boolean isActive()
	{
		return ZoneTaskManager.getInstance().getCurrentZoneId() == getId();
	}

	public Location getLoc()
	{
		return _reviveLocations.get(Rnd.get(0, _reviveLocations.size() - 1));
	}

	public boolean isRestrictedItem(int itemId)
	{
		return _restrictedItems.contains(itemId);
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int getTime()
	{
		return _time;
	}
	
	public int getRevive()
	{
		return _reviveDelay;	
	}

	public static boolean isRevive()
	{
		return _isRevive;
	}

	public boolean isFlag()
	{
		return _isFlag;
	}
}