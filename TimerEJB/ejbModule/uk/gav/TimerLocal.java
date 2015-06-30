package uk.gav;

import javax.ejb.Local;

@Local
public interface TimerLocal {
    public void createTimer();
    
    public void cancelTimer();

}
