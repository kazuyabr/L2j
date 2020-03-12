package net.sf.l2j.gameserver.skills.conditions;

import java.util.List;

import net.sf.l2j.gameserver.skills.Env;

/**
 * @author Williams
 *
 */
public class ConditionPlayerClassId extends Condition
{
	private final List<Integer> _class;
	
	public ConditionPlayerClassId(List<Integer> race)
	{
		_class = race;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		if (env.getPlayer() == null)
			return false;
		
		return _class.contains(env.getPlayer().getClassId().getId());
	}
}