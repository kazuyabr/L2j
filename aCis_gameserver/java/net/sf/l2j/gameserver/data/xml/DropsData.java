package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.item.DropData;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

/**
 * @author Williams
 *
 */
public class DropsData implements IXmlReader
{
	private final Map<Integer, List<DropCategory>> _drops = new HashMap<>();

	@Override
	public void load()
	{
		parseFile("./data/xml/drops.xml");
		LOGGER.info("Loaded {} Drops.", _drops.size());
	}

	public DropsData()
	{
		load();
	}
	
	public void reload()
	{
		_drops.clear();
		load();
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{	
		forEach(doc, "list", listNode -> forEach(listNode, "drops", Node ->
		{
			final int drop = parseInteger(Node.getAttributes(), "id");
			final List<DropCategory> category = _drops.computeIfAbsent(drop, k -> new ArrayList<>());
			forEach(Node, "item", itemNode ->
			{
				final NamedNodeMap attrs = itemNode.getAttributes();
			
				final int categoryType = parseInteger(attrs, "category");
				
				final DropData dropDat = new DropData();
				dropDat.setItemId(parseInteger(attrs, "id"));
				dropDat.setMinDrop(parseInteger(attrs, "min"));
				dropDat.setMaxDrop(parseInteger(attrs, "max"));
				dropDat.setChance(parseInteger(attrs, "chance"));
				
				boolean catExists = false;
				for (final DropCategory cat : category)
				{
					if (cat.getCategoryType() == categoryType)
					{
						cat.addDropData(dropDat, true);
						catExists = true;
						break;
					}
				}
				if (!catExists)
				{
					final DropCategory cat = new DropCategory(categoryType);
					cat.addDropData(dropDat, true);
					category.add(cat);
				}
			});
		}));
	}

	public List<DropCategory> getDroplist(int dropId)
	{
		return _drops.get(dropId);
	}
	
	public static DropsData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DropsData _instance = new DropsData();
	}
}