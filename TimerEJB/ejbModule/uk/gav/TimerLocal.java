package uk.gav;

import java.util.Set;

import javax.ejb.Local;

@Local
public interface TimerLocal {
    public void createTimer(String id);
    
    public void cancelTimer(String id);
    
	public void cancelAllTimers();
    
	public Set<String> getTimerIDs();
}
