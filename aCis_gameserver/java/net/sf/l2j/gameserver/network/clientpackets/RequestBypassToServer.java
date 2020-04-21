package net.sf.l2j.gameserver.network.clientpackets;

import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.data.manager.HeroManager;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.OlympiadManagerNpc;
import net.sf.l2j.gameserver.model.entity.engine.vote.VoteHopzone;
import net.sf.l2j.gameserver.model.entity.engine.vote.VoteNetwork;
import net.sf.l2j.gameserver.model.entity.engine.vote.VoteTopzone;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.FloodProtectors;
import net.sf.l2j.gameserver.network.FloodProtectors.Action;
import net.sf.l2j.gameserver.network.GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.scripting.QuestState;

public final class RequestBypassToServer extends L2GameClientPacket
{
	private static final Logger GMAUDIT_LOG = Logger.getLogger("gmaudit");
	
	private String _command;
	
	@Override
	protected void readImpl()
	{
		_command = readS();
	}
	
	@Override
	protected void runImpl()
	{
		if (_command.isEmpty())
			return;
		
		if (!FloodProtectors.performAction(getClient(), Action.SERVER_BYPASS))
			return;
		
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		if (_command.startsWith("admin_"))
		{
			String command = _command.split(" ")[0];
			
			final IAdminCommandHandler ach = AdminCommandHandler.getInstance().getHandler(command);
			if (ach == null)
			{
				if (player.isGM())
					player.sendMessage("The command " + command.substring(6) + " doesn't exist.");
				
				LOGGER.warn("No handler registered for admin command '{}'.", command);
				return;
			}
			
			if (!AdminData.getInstance().hasAccess(command, player.getAccessLevel()))
			{
				player.sendMessage("You don't have the access rights to use this command.");
				LOGGER.warn("{} tried to use admin command '{}' without proper Access Level.", player.getName(), command);
				return;
			}
			
			if (Config.GMAUDIT)
				GMAUDIT_LOG.info(player.getName() + " [" + player.getObjectId() + "] used '" + _command + "' command on: " + ((player.getTarget() != null) ? player.getTarget().getName() : "none"));
			
			ach.useAdminCommand(_command, player);
		}
		else if (_command.startsWith("player_help "))
		{
			final String path = _command.substring(12);
			if (path.indexOf("..") != -1)
				return;
			
			final StringTokenizer st = new StringTokenizer(path);
			final String[] cmd = st.nextToken().split("#");
			
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/help/" + cmd[0]);
			if (cmd.length > 1)
			{
				final int itemId = Integer.parseInt(cmd[1]);
				html.setItemId(itemId);
				
				if (itemId == 7064 && cmd[0].equalsIgnoreCase("lidias_diary/7064-16.htm"))
				{
					final QuestState qs = player.getQuestState("Q023_LidiasHeart");
					if (qs != null && qs.getInt("cond") == 5 && qs.getInt("diary") == 0)
						qs.set("diary", "1");
				}
			}
			html.disableValidation();
			player.sendPacket(html);
		}
		else if (_command.startsWith("npc_"))
		{
			if (!player.validateBypass(_command))
				return;
			
			int endOfId = _command.indexOf('_', 5);
			String id;
			if (endOfId > 0)
				id = _command.substring(4, endOfId);
			else
				id = _command.substring(4);
			
			try
			{
				final WorldObject object = World.getInstance().getObject(Integer.parseInt(id));
				
				if (object != null && object instanceof Npc && endOfId > 0 && ((Npc) object).canInteract(player))
					((Npc) object).onBypassFeedback(player, _command.substring(endOfId + 1));
				
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
			catch (NumberFormatException nfe)
			{
			}
		}
		// Navigate throught Manor windows
		else if (_command.startsWith("manor_menu_select?"))
		{
			WorldObject object = player.getTarget();
			if (object instanceof Npc)
				((Npc) object).onBypassFeedback(player, _command);
		}
		else if (_command.startsWith("bbs_") || _command.startsWith("_bbs") || _command.startsWith("_friend") || _command.startsWith("_mail") || _command.startsWith("_block"))
		{
			CommunityBoard.getInstance().handleCommands(getClient(), _command);
		}
		else if (_command.startsWith("Quest "))
		{
			if (!player.validateBypass(_command))
				return;
			
			String[] str = _command.substring(6).trim().split(" ", 2);
			if (str.length == 1)
				player.processQuestEvent(str[0], "");
			else
				player.processQuestEvent(str[0], str[1]);
		}
		else if (_command.startsWith("_match"))
		{
			String params = _command.substring(_command.indexOf("?") + 1);
			StringTokenizer st = new StringTokenizer(params, "&");
			int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
			int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
			int heroid = HeroManager.getInstance().getHeroByClass(heroclass);
			if (heroid > 0)
				HeroManager.getInstance().showHeroFights(player, heroclass, heroid, heropage);
		}
		else if (_command.startsWith("_diary"))
		{
			String params = _command.substring(_command.indexOf("?") + 1);
			StringTokenizer st = new StringTokenizer(params, "&");
			int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
			int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
			int heroid = HeroManager.getInstance().getHeroByClass(heroclass);
			if (heroid > 0)
				HeroManager.getInstance().showHeroDiary(player, heroclass, heroid, heropage);
		}
		else if (_command.startsWith("arenachange")) // change
		{
			final boolean isManager = player.getCurrentFolk() instanceof OlympiadManagerNpc;
			if (!isManager)
			{
				// Without npc, command can be used only in observer mode on arena
				if (!player.isInObserverMode() || player.isInOlympiadMode() || player.getOlympiadGameId() < 0)
					return;
			}
			
			if (OlympiadManager.getInstance().isRegisteredInComp(player))
			{
				player.sendPacket(SystemMessageId.WHILE_YOU_ARE_ON_THE_WAITING_LIST_YOU_ARE_NOT_ALLOWED_TO_WATCH_THE_GAME);
				return;
			}
			
			final int arenaId = Integer.parseInt(_command.substring(12).trim());
			player.enterOlympiadObserverMode(arenaId);
		}
		else if (_command.startsWith("send_report"))
		{
			StringTokenizer st = new StringTokenizer(_command);
			st.nextToken();
			
			String msg = "";
			String type = st.nextToken();
			
			final GameClient info = player.getClient().getConnection().getClient();
			
			try
			{
				while (st.hasMoreTokens())
					msg = msg + st.nextToken() + " ";
				
				if (msg.equals(""))
				{
					player.sendMessage("The message box cannot be empty.");
					return;
				}
				
				switch (type)
				{
					case "Armaduras":
						break;
					case "Boss":
						break;
					case "Skills":
						break;
					case "Quests":
						break;
					case "Other":
						break;		
				}
				
				GMAUDIT_LOG.info("Bug Report Info: " + info + "\r\nBug Type: " + type + "\r\nMessage: " + msg);
				player.sendMessage("Report sent. Gms will check it out soon, thanks.");
				AdminData.getInstance().broadcastMessageToGMs("Report Manager: "+ player.getName() + " submitted a bug report.");
			}
			catch (Exception e)
			{
				player.sendMessage("Something went wrong, try again.");
			}
		}
		else if (_command.startsWith("farmzone1"))
		{
			player.enterObserverMode(149918, -112541, -2080);
			player.doPreview(90);
			
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile("data/html/preview/previewme.htm");
			html.replace("%name%", player.getName());
			player.sendPacket(html);
		}
		else if (_command.startsWith("farmzone2"))
		{
			player.enterObserverMode(181387, -78694, -2732);
			player.doPreview(90);
			
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile("data/html/preview/previewme.htm");
			html.replace("%name%", player.getName());
			player.sendPacket(html);
		}
		else if (_command.startsWith("pvpzone"))
		{
			player.enterObserverMode(10468, -24569, -3645);
			player.doPreview(90);
			
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile("data/html/preview/previewme.htm");
			html.replace("%name%", player.getName());
			player.sendPacket(html);
		}
		else if (_command.startsWith("teleport"))
		{
			player.leaveObserverMode();
			player.teleToLocation(player.getTemplate().getRandomSpawn());
		}
		else if (_command.startsWith("vote "))
		{
			String voteSiteName = _command.substring(5);
			switch(voteSiteName)
			{
				case "hopzone":
					new Thread(() -> {
						if (player.eligibleToVoteHop())
						{
							VoteHopzone voteHop = new VoteHopzone();
							if (voteHop.hasVoted(player))
							{
								voteHop.updateDB(player, "HopZone");
								voteHop.setVoted(player);
								voteHop.reward(player);
							}
							else
								player.sendMessage("You haven't voted yet.");
						}
						else
							GMAUDIT_LOG.info(player.getName() + " tried to send a bypass with adrenalin/phx");
					}).start();
					break;
				case "topzone":
					new Thread(() -> {
						if (player.eligibleToVoteTop())
						{
							VoteTopzone voteTop = new VoteTopzone();
							if (voteTop.hasVoted(player))
							{
								voteTop.updateDB(player, "TopZone");
								voteTop.setVoted(player);
								voteTop.reward(player);
							}
							else
								player.sendMessage("You haven't voted yet.");
						}
						else
							GMAUDIT_LOG.info(player.getName() + " tried to send a bypass with adrenalin/phx");
					}).start();
					break;
				case "network":
					new Thread(() -> {
						if (player.eligibleToVoteNet())
						{
							VoteNetwork voteNet = new VoteNetwork();
							if (voteNet.hasVoted(player))
							{
								voteNet.updateDB(player, "NetWork");
								voteNet.setVoted(player);
								voteNet.reward(player);
							}
							else
								player.sendMessage("You haven't voted yet.");
						}
						else
							GMAUDIT_LOG.info(player.getName() + " tried to send a bypass with adrenalin/phx");
					}).start();
					break;
			}
		}
		else if (_command.startsWith("droplist"))
		{
			StringTokenizer st = new StringTokenizer(_command, " ");
			st.nextToken();
			
			int npcId = Integer.parseInt(st.nextToken());
			int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;

			Npc.sendNpcDrop(player, npcId, page);
		}
	}
}