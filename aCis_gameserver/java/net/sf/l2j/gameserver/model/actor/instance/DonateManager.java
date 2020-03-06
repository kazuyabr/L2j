package net.sf.l2j.gameserver.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.enums.actors.Sex;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminPremium;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.data.sql.PlayerInfoTable;
import net.sf.l2j.gameserver.data.xml.DonateData;
import net.sf.l2j.gameserver.data.xml.DonateData.Donate;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Williams
 *
 */
public class DonateManager extends Folk
{
	private static final Logger GMAUDIT_LOG = Logger.getLogger("gmaudit");
	
	public DonateManager(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String currentCommand = st.nextToken();
		int serviceId = Integer.parseInt(st.nextToken());
		
		if (OlympiadManager.getInstance().isRegistered(player))
		{
			player.sendMessage("Sorry "+ player.getName() + " you cannot use my services registered with Olympiad.");
			return;
		}
		else if (player.getEvent() != null && player.getEvent().isStarted())
		{
			player.sendMessage("Sorry "+ player.getName() + " you cannot use my services registered in Event.");
			return;	
		}
		
		for (Donate service : DonateData.getInstance().getDonate())
		{
			if (service.getService() != serviceId)
				continue;
			
			for (IntIntHolder price : service.getPrice())
			{
				if (player.getInventory().getInventoryItemCount(price.getId(), -1) < price.getValue())
				{
					player.sendMessage("You do not have "+ ItemTable.getInstance().getTemplate(price.getId()).getName() + " sufficient.");
					return;
				}
				
				if (currentCommand.startsWith("aio"))
				{
					if (player.isVip())
					{
						player.sendMessage("Sorry Vip can't become Aio.");
						return;
					}
					
					player.destroyItemByItemId("", price.getId(), price.getValue(), player, true);
					AdminPremium.doAio(player, service.getDuration());
					
					GMAUDIT_LOG.info(player.getName() + " buy "+ service.getDuration() +" AIO days. Your ID [" + player.getObjectId() + "]");
				}
				else if (currentCommand.startsWith("clanLevel"))		
				{		
					if (player.getClan() == null)
					{
						player.sendMessage("Sorry "+ player.getName() +" the more you are without a clan.");
						return;
					}
					
					if (player.isClanLeader())
					{
						if (player.getClan().getLevel() == 8)
						{
							player.sendMessage("Sorry, but your clan is already level 8!");
							return;
						}
						
						player.destroyItemByItemId("", price.getId(), price.getValue(), player, true);
						
						player.getClan().changeLevel(8);
						player.sendMessage("Congratulations "+ player.getName() +" you just bought Level 8 for your clan.");
						
						GMAUDIT_LOG.info(player.getName() + " bought Level 8 for the clan "+ player.getClan().getName() +". Your ID [" + player.getObjectId() + "]");
					}
					else        
						player.sendMessage("Sorry but only the clan leader "+ player.getClan().getName() +" can use this service.");  
				}
				else if (currentCommand.startsWith("clanSkill"))		
				{		
					if (player.getClan() == null)
					{
						player.sendMessage("Sorry "+ player.getName() +" the more you are without a clan.");
						return;
					}
					
					if (player.isClanLeader())
					{
						player.destroyItemByItemId("", price.getId(), price.getValue(), player, true);
						
						for (int i = 370; i <= 391; i++)
							player.getClan().addNewSkill(SkillTable.getInstance().getInfo(i, SkillTable.getInstance().getMaxLevel(i)));            
						
						player.sendMessage("Congratulations "+ player.getName() +" you just bought all the skills for your clan.");
						
						GMAUDIT_LOG.info(player.getName() + " bought all clan skills, for the clan "+ player.getClan().getName() +". Your ID [" + player.getObjectId() + "]");
					}
					else        
						player.sendMessage("Sorry but only the clan leader "+ player.getClan().getName() +" can use this service.");  
				}
				else if (currentCommand.startsWith("clanRep"))		
				{		
					if (player.getClan() == null)
					{
						player.sendMessage("Sorry "+ player.getName() +" the more you are without a clan.");
						return;
					}
					
					if (player.isClanLeader())
					{
						player.destroyItemByItemId("", price.getId(), price.getValue(), player, true);
						player.getClan().addReputationScore(100000);    
						player.getClan().updateClanInDB();
						player.sendMessage("Congratulations "+ player.getName() +" you just bought 100000 reputation for your clan.");
						
						GMAUDIT_LOG.info(player.getName() + " bought 100000 reputation, for the clan "+ player.getClan().getName() +". Your ID [" + player.getObjectId() + "]");
					}
					else        
						player.sendMessage("Sorry but only the clan leader "+ player.getClan().getName() +" pode usar esse serviço."); 
				}
				else if (currentCommand.startsWith("clanName"))		
				{	
					String newClanName = st.nextToken();
					
					if (player.getClan() == null)
					{
						player.sendMessage("Sorry "+ player.getName() +" the more you are without a clan.");
						return;
					}
					
					if (!player.isClanLeader())
					{
						player.sendMessage("Sorry but only the clan leader "+ player.getClan().getName() +" can use this service."); 
						return;
					}
					else if (player.getClan().getLevel() < 5)
					{
						player.sendMessage("Your clan must be at least level 5 to change the name.");
						return;
					}
					else if (!StringUtil.isValidString(newClanName, "^[A-Za-z0-9]{3,16}$"))
					{
						player.sendMessage("Incorrect name. Please try again.");
						return;
					}
					else if (newClanName.equals(player.getClan().getName()))
					{
						player.sendMessage("Please choose a different name.");
						return;
					}
					else if (ClanTable.getInstance().getClanByName(newClanName) != null)
					{
						player.sendMessage("The name " + newClanName + " already exists.");
						return;
					}
					
					player.destroyItemByItemId("", price.getId(), price.getValue(), player, true);
					player.getClan().setName(newClanName);
					player.sendMessage("Your new clan name is " + newClanName);
					
					ThreadPool.schedule(() -> player.logout(false), 1000);
					GMAUDIT_LOG.info(player.getName() + " changed the name of the clan "+ player.getClan().getName() +" for "+ newClanName +". Your ID [" + player.getObjectId() + "]");
				}
				else if (currentCommand.startsWith("hero"))		
				{		
					if (player.isHero())
					{
						player.sendMessage("Sorry but you are already a class hero." + player.setClassName(player.getBaseClass()));
						return;
					}
					else if (player.getBaseClass() != player.getClassId().getId())
					{
						player.sendMessage("You need to be with your Class "+ player.setClassName(player.getBaseClass()) + " to be able to change.");
						return;
					}
					
					player.destroyItemByItemId("", price.getId(), price.getValue(), player, true);
					AdminPremium.doHero(player, service.getDuration());
					
					GMAUDIT_LOG.info(player.getName() + " bought "+ service.getDuration() +" Hero days. Your ID [" + player.getObjectId() + "]");
				}
				else if (currentCommand.startsWith("classe"))		
				{	
					if (player.getBaseClass() != player.getClassId().getId())
					{
						player.sendMessage("You need to be with your Class "+ player.setClassName(player.getBaseClass()) + " to be able to change.");
						return;
					}
					
					String classes = st.nextToken();
					switch (classes)
					{
						case "Duelist":
							if (player.getClassId().getId() == 88)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 88);
							break;
						case "Dreadnought":
							if (player.getClassId().getId() == 89)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 89);
							break;
						case "Phoenix_Knight":
							if (player.getClassId().getId() == 90)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 90);
							break;
						case "Hell_Knight":
							if (player.getClassId().getId() == 91)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 91);
							break;
						case "Saggitarius":
							if (player.getClassId().getId() == 92)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 92);
							break;
						case "Adventure":
							if (player.getClassId().getId() == 93)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 93);
							break;
						case "Archmage":
							if (player.getClassId().getId() == 94)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 94);
							break;
						case "Soultaker":
							if (player.getClassId().getId() == 95)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 95);
							break;
						case "Arcana_Lord":
							if (player.getClassId().getId() == 96)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 96);
							break;
						case "Cardial":
							if (player.getClassId().getId() == 97)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 97);
							break;
						case "Hierophant":
							if (player.getClassId().getId() == 98)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 98);
							break;
						case "Evas_Templar":
							if (player.getClassId().getId() == 99)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 99);
							break;
						case "Sword_Muse":
							if (player.getClassId().getId() == 100)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 100);
							break;
						case "Wind_Rider":
							if (player.getClassId().getId() == 101)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 101);
							break;
						case "Moonlight_Sentinel":
							if (player.getClassId().getId() == 102)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 102);
							break;
						case "Mystic_Muse":
							if (player.getClassId().getId() == 103)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 103);
							break;
						case "Elemental_Master":
							if (player.getClassId().getId() == 104)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 104);
							break;
						case "Evas_Saint":
							if (player.getClassId().getId() == 105)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 105);
							break;
						case "Shillie_Templar":
							if (player.getClassId().getId() == 106)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 106);
							break;
						case "Spectral_Dancer":
							if (player.getClassId().getId() == 107)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 107);
							break;
						case "Ghost_Hunter":
							if (player.getClassId().getId() == 108)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 108);
							break;
						case "Ghost_Sentinel":
							if (player.getClassId().getId() == 109)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 109);
							break;
						case "Storm_Screamer":
							if (player.getClassId().getId() == 110)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 110);
							break;
						case "Spectral_Master":
							if (player.getClassId().getId() == 111)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 111);
							break;
						case "Shillien_Saint":
							if (player.getClassId().getId() == 112)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 112);
							break;
						case "Titan":
							if (player.getClassId().getId() == 113)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 113);
							break;
						case "Grand_Khavatari":
							if (player.getClassId().getId() == 114)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 114);
							break;
						case "Dominator":
							if (player.getClassId().getId() == 115)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 115);
							break;
						case "Doomcryer":
							if (player.getClassId().getId() == 116)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 116);
							break;
						case "Fortune_Seeker":
							if (player.getClassId().getId() == 117)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 117);
							break;
						case "Maestro":
							if (player.getClassId().getId() == 118)
							{
								player.sendMessage(player.getName() + " your class is already " + classes + " choose another.");
								return;
							}
							
							getClassId(player, 118);
							break;
					}
					
					player.destroyItemByItemId("", price.getId(), price.getValue(), player, true);
					
					GMAUDIT_LOG.info(player.getName() + " mudou sua classe para "+ classes +". Seu ID [" + player.getObjectId() + "]");
				}
				else if (currentCommand.startsWith("name"))		
				{
					final String newName = st.nextToken();
					
					if (!StringUtil.isValidString(newName, "^[A-Za-z0-9]{3,16}$"))
					{
						player.sendMessage("Incorrect name. Please try again.");
						return;
					}
					
					// Name is a npc name.
					if (NpcData.getInstance().getTemplateByName(newName) != null)
					{
						player.sendMessage("Incorrect name. Please try again.");
						return;
					}
					
					// Name already exists.
					if (PlayerInfoTable.getInstance().getPlayerObjectId(newName) > 0)
					{
						player.sendMessage("Incorrect name. Please try again.");
						return;
					}
					
					player.destroyItemByItemId("", price.getId(), price.getValue(), player, true);
					
					player.setName(newName);
					PlayerInfoTable.getInstance().updatePlayerData(player, false);
					player.sendMessage("You just changed nick. Remembering that your old name was saved.");
					player.broadcastUserInfo();
					player.store();
					
					GMAUDIT_LOG.info(player.getName() + " used the name change service your ID is  [" + player.getObjectId() + "]");
				}
				else if (currentCommand.startsWith("nobles"))		
				{				
					if (player.isNoble())
					{
						player.sendMessage("Sorry "+ player.getName() +" but you are already Nobles.");
						return;
					}
					
					player.destroyItemByItemId("", price.getId(), price.getValue(), player, true);
					player.setNoble(true, true);
					player.sendMessage("Congratulations "+ player.getName() +" you just bought Nobles.");
					player.broadcastUserInfo();
					
					GMAUDIT_LOG.info(player.getName() + " bought nobles. Your ID [" + player.getObjectId() + "]");
				}
				else if (currentCommand.startsWith("level"))		
				{			
					if (player.getLevel() > 81)
					{
						player.sendMessage("You are already level 81.");
						return;
					}
					
					player.destroyItemByItemId("", price.getId(), price.getValue(), player, true);
					player.getStat().addLevel((byte) 81);
					player.sendMessage("Congratulations "+ player.getName() +" you just bought level 81.");
					
					GMAUDIT_LOG.info(player.getName() + " purchased level 81. Your ID [" + player.getObjectId() + "]");
				}
				else if (currentCommand.startsWith("vip"))		
				{		
					if (player.isAio())
					{
						player.sendMessage("Sorry Aio can't become a VIP.");
						return;
					}
					
					player.destroyItemByItemId("", price.getId(), price.getValue(), null, true);
					AdminPremium.doVip(player, service.getDuration());
					
					GMAUDIT_LOG.info(player.getName() + " bought "+ service.getDuration() +" VIP days. Your ID [" + player.getObjectId() + "]");
				}
				else if (currentCommand.startsWith("gender"))		
				{			
					
					player.destroyItemByItemId("", price.getId(), price.getValue(), player, true);
					player.getAppearance().setSex(player.getAppearance().getSex() == Sex.MALE ? Sex.FEMALE : Sex.MALE);
					player.sendMessage("Congratulations "+ player.getName() +" you just changed genders. You will be disconnected in 3 seconds.");
					player.broadcastUserInfo();
					
					player.decayMe();
					player.spawnMe();
					ThreadPool.schedule(() -> player.logout(false), 3000);
					GMAUDIT_LOG.info(player.getName() + " bought nobles. Your ID [" + player.getObjectId() + "]");
				}
			}
		}
		
		super.onBypassFeedback(player, command);
	}
	
	public void getClassId(Player player, int classId)
	{
		if (!player.isSubClassActive()) 
			player.setBaseClass(classId);
		
		player.setClassId(classId);
		player.store();
		player.broadcastUserInfo();
		player.sendSkillList();
		player.getAvailableSkills();
		player.disarmWeapons();
		player.stopAllEffectsExceptThoseThatLastThroughDeath();
		player.sendMessage(player.getName() + " your new class is " + player.getTemplate().getClassName() + ".");
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			// Remove all henna info stored for this sub-class.
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_hennas WHERE char_obj_id=? AND class_index=?"))
			{
				ps.setInt(1, player.getObjectId());
				ps.setInt(2, 0);
				ps.execute();
			}

			// Remove all shortcuts info stored for this sub-class.
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=? AND class_index=?"))
			{
				ps.setInt(1, player.getObjectId());
				ps.setInt(2, 0);
				ps.execute();
			}
			
			// Remove all effects info stored for this sub-class.
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_skills_save WHERE char_obj_id=? AND class_index=?"))
			{
				ps.setInt(1, player.getObjectId());
				ps.setInt(2, 0);
				ps.execute();
			}
			
			// Remove all skill info stored for this sub-class.
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_skills WHERE char_obj_id=? AND class_index=?"))
			{
				ps.setInt(1, player.getObjectId());
				ps.setInt(2, 0);
				ps.execute();
			}
			
			// remove hero
			try (PreparedStatement statement = con.prepareStatement("DELETE FROM heroes WHERE char_id=?"))
			{
				statement.setInt(1, player.getObjectId());
				statement.execute();
			}
		}
		catch (Exception e)
		{
			
		}
		
		ThreadPool.schedule(() -> player.logout(false), 1000);
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		String name = "data/html/donate/" + getNpcId() + ".htm";
		if (val != 0)
			name = "data/html/donate/" + getNpcId() + "-" + val + ".htm";
		
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(name);
		html.replace("%objectId%", getObjectId());
		html.replace("%player%", player.getName());
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}