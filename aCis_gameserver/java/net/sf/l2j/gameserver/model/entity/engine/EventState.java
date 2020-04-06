package net.sf.l2j.gameserver.model.entity.engine;

/**
 * @author Anarchy
 *
 * This enum contains all the necessary states used
 * by events and the event manager.
 */
public enum EventState
{
	INACTIVE,
	REGISTERING,
	TELEPORTING,
	RUNNING
}