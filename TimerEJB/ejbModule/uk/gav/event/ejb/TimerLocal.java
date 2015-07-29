package uk.gav.event.ejb;

import java.util.Set;

import javax.ejb.Local;

/**
 * 
 * @author gavin
 *
 *         Local interface for the timer EJB. Later J2EE/Weblogic versions will
 *         not require this interface.
 */
@Local
public interface TimerLocal {
	public void createTimer(String id);

	public void cancelTimer(String id);

	public void cancelAllTimers();

	public Set<String> getTimerIDs();
}
