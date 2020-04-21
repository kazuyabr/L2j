package net.sf.l2j.gameserver.model.actor.instance;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.manager.RaidBossInfoManager;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.item.DropData;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Williams
 *
 */
public class RaidBosInfo extends Folk
{
	public RaidBosInfo(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String currentCommand = st.nextToken();
		
		if (currentCommand.startsWith("RaidBossInfo"))
		{
			int pageId = Integer.parseInt(st.nextToken());
			LAST_PAGE.put(player.getObjectId(), pageId);
			showRaidBossInfo(player, pageId);
		}
		else if (currentCommand.startsWith("RaidBossDrop"))
		{
			int bossId = Integer.parseInt(st.nextToken());
			int pageId = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
			sendRaidDrop(player, bossId, pageId);
		}
		
		super.onBypassFeedback(player, command);
	}
	
	private void showRaidBossInfo(Player player, int pageId)
	{
		List<Integer> infos = new ArrayList<>();
		infos.addAll(Config.LIST_RAID_BOSS_IDS);
		
		final int limit = Config.RAID_BOSS_INFO_PAGE_LIMIT;
		final int max = infos.size() / limit + (infos.size() % limit == 0 ? 0 : 1);
		infos = infos.subList((pageId - 1) * limit, Math.min(pageId * limit, infos.size()));
		
		final StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<center>");
		sb.append("<body>");
		sb.append("<table width=\"256\">");
		sb.append("<tr><td width=\"256\" align=\"center\">%name%</td></tr>");
		sb.append("</table>");
		sb.append("<br>");
		sb.append("<table width=\"256\">");
		sb.append("<tr><td width=\"256\" align=\"left\">" + MESSAGE[0][Rnd.get(MESSAGE.length)].replace("%player%", player.getName()) + "</td></tr>");
		sb.append("</table>");
		sb.append("<br>");
		sb.append("<table width=\"224\" bgcolor=\"000000\">");
		sb.append("<tr><td width=\"224\" align=\"center\">Raid Boss Infos</td></tr>");
		sb.append("</table>");
		sb.append("<br>");
		sb.append("<table width=\"256\">");
		
		for (int bossId : infos)
		{
			final NpcTemplate template = NpcData.getInstance().getTemplate(bossId);
			if (template == null)
				continue;
			
			String bossName = template.getName();
			if (bossName.length() > 23)
				bossName = bossName.substring(0, 23) + "...";
			
			final long respawnTime = RaidBossInfoManager.getInstance().getRaidBossRespawnTime(bossId);
			if (respawnTime <= System.currentTimeMillis())
			{
				sb.append("<tr>");
				sb.append("<td width=\"146\" align=\"left\"><a action=\"bypass -h npc_%objectId%_RaidBossDrop " + bossId + "\">" + bossName + "</a></td>");
				sb.append("<td width=\"110\" align=\"right\"><font color=\"9CC300\">Alive</font></td>");
				sb.append("</tr>");
			}
			else
			{
				sb.append("<tr>");
				sb.append("<td width=\"146\" align=\"left\"><a action=\"bypass -h npc_%objectId%_RaidBossDrop " + bossId + "\">" + bossName + "</a></td>");
				sb.append("<td width=\"110\" align=\"right\"><font color=\"FB5858\">Dead</font> " + new SimpleDateFormat(Config.RAID_BOSS_DATE_FORMAT).format(new Date(respawnTime)) + "</td>");
				sb.append("</tr>");
			}
		}
		
		sb.append("</table>");
		sb.append("<br>");
		sb.append("<table width=\"224\" cellspacing=\"2\">");
		sb.append("<tr>");
		
		for (int x = 0; x < max; x++)
		{
			final int pageNr = x + 1;
			if (pageId == pageNr)
				sb.append("<td align=\"center\">" + pageNr + "</td>");
			else
				sb.append("<td align=\"center\"><a action=\"bypass -h npc_%objectId%_RaidBossInfo " + pageNr + "\">" + pageNr + "</a></td>");
		}
		
		sb.append("</tr>");
		sb.append("</table>");
		sb.append("<br>");
		sb.append("<table width=\"160\" cellspacing=\"2\">");
		sb.append("<tr>");
		sb.append("<td width=\"160\" align=\"center\"><a action=\"bypass -h npc_%objectId%_Chat 0\">Return</a></td>");
		sb.append("</tr>");
		sb.append("</table>");
		sb.append("</center>");
		sb.append("</body>");
		sb.append("</html>");
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setHtml(sb.toString());
		html.replace("%name%", getName());
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}

	public void sendRaidDrop(Player player, int npcId, int page)
	{
		final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
		if (template == null)
			return; 
		
		if (template.getDropData().isEmpty()) 
		{
			player.sendMessage("This target have not drop info.");
			return;
		} 
		
		final List<DropCategory> list = new ArrayList<>();
		template.getDropData().forEach(c -> list.add(c));
		Collections.reverse(list);
		
		int myPage = 1;
		int i = 0;
		int shown = 0;
		boolean hasMore = false;
		
		final StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<center>");
		sb.append("<body>");
		sb.append("<table width=\"256\">");
		sb.append("<tr><td width=\"256\" align=\"center\">%name%</td></tr>");
		sb.append("</table>");
		sb.append("<br>");
		sb.append("<table width=\"256\">");
		sb.append("<tr><td width=\"256\" align=\"left\">" + MESSAGE[1][Rnd.get(MESSAGE.length)].replace("%boss%", template.getName()) + "</td></tr>");
		sb.append("</table>");
		sb.append("<br>");
		sb.append("<table width=\"224\" bgcolor=\"000000\">");
		sb.append("<tr><td width=\"224\" align=\"center\">Raid Boss Drops</td></tr>");
		sb.append("</table>");
		sb.append("<br>");
		
		for (DropCategory cat : list) 
		{
			if (shown == PAGE_LIMIT)
			{
				hasMore = true;
				break;
			} 
			
			for (DropData drop : cat.getAllDrops())
			{
				final double chance = Math.min(100, (((drop.getItemId() == 57) ? drop.getChance() * Config.RATE_DROP_ADENA : drop.getChance() * Config.RATE_DROP_ITEMS) / 10000));
				final Item item = ItemTable.getInstance().getTemplate(drop.getItemId());

				String name = item.getName();
				if (name.startsWith("Recipe: "))
					name = "R: " + name.substring(8);
				
				if (name.length() >= 45)
					name = name.substring(0, 42) + "...";
				
				String percent = null;
				if (chance <= 0.001)
				{
					DecimalFormat df = new DecimalFormat("#.####");
					percent = df.format(chance);
				}
				else if (chance <= 0.01)
				{
					DecimalFormat df = new DecimalFormat("#.###");
					percent = df.format(chance);
				}
				else
				{
					DecimalFormat df = new DecimalFormat("##.##");
					percent = df.format(chance);
				}
				
				if (myPage != page)
				{
					i++;
					if (i == PAGE_LIMIT)
					{
						myPage++;
						i = 0;
					}
					continue;
				}
				
				if (shown == PAGE_LIMIT)
				{
					hasMore = true;
					break;
				}
				
				sb.append(((shown % 2) == 0 ? "<table width=\"280\" bgcolor=\"000000\"><tr>" : "<table width=\"280\"><tr>"));
				sb.append("<td width=44 height=41 align=center><table bgcolor=" + (cat.isSweep() ? "FF00FF" : "FFFFFF") + " cellpadding=6 cellspacing=\"-5\"><tr><td><button width=32 height=32 back=" + item.getIcon() + " fore=" + item.getIcon() + "></td></tr></table></td>");
				sb.append("<td width=240>" + (cat.isSweep() ? ("<font color=ff00ff>" + name + "</font>") : name) + "<br1><font color=B09878>" + (cat.isSweep() ? "Spoil" : "Drop") + " Chance : " + percent + "%</font></td>");
				sb.append("</tr></table><img src=L2UI.SquareGray width=280 height=1>");
				shown++;
			} 
		} 

		// Build page footer.
		sb.append("<br><img src=\"L2UI.SquareGray\" width=277 height=1><table width=\"100%\" bgcolor=000000><tr>");
		
		if (page > 1)
			StringUtil.append(sb, "<td align=left width=70><a action=\"bypass -h npc_%objectId%_RaidBossDrop "+ npcId + " ", (page - 1), "\">Previous</a></td>");
		else
			StringUtil.append(sb, "<td align=left width=70>Previous</td>");
		
		StringUtil.append(sb, "<td align=center width=100>Page ", page, "</td>");
		
		if (page < shown)
			StringUtil.append(sb, "<td align=right width=70>" + (hasMore ? "<a action=\"bypass -h npc_%objectId%_RaidBossDrop " + npcId + " " + (page + 1) + "\">Next</a>" : "") + "</td>");
		else
			StringUtil.append(sb, "<td align=right width=70>Next</td>");
		
		sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=277 height=1>");
		sb.append("<br>");
		sb.append("<center>");
		sb.append("<table width=\"160\" cellspacing=\"2\">");
		sb.append("<tr>");
		sb.append("<td width=\"160\" align=\"center\"><a action=\"bypass -h npc_%objectId%_RaidBossInfo " + LAST_PAGE.get(player.getObjectId()) + "\">Return</a></td>");
		sb.append("</tr>");
		sb.append("</table>");
		sb.append("</center>");
		sb.append("</body>");
		sb.append("</html>");

		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setHtml(sb.toString());
		html.replace("%name%", getName());
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}

	@Override
	public void showChatWindow(Player player, int val)
	{
		String name = "data/html/raidbossinfo/" + getNpcId() + ".htm";
		if (val != 0)
			name = "data/html/raidbossinfo/" + getNpcId() + "-" + val + ".htm";
		
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(name);
		html.replace("%objectId%", getObjectId());
		html.replace("%npcName%", getName());
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}