package uk.gav.event.utilities;

import static org.junit.Assert.*;

import org.junit.Test;

import uk.gav.event.utilities.MessageState;

public class MessageStateTest {

	@Test
	public void testIdentifyState() {
		Integer[] sIDs = {null,1,2};
		MessageState[] mss = {MessageState.NEW, MessageState.QUEUED, MessageState.ISSUED};
		
		for (int i = 0; i < sIDs.length; i++) {
			MessageState ms = MessageState.identifyState(sIDs[i]);
			assertTrue("Internal state:" + ms.getStateID()
					+ " is not correct for: " + mss[i].getStateID(), 
					(ms != null) && (ms.getStateID() == mss[i].getStateID()) );
		}
		
		assertNull("State should be null, for id=3", MessageState.identifyState(3));

	}

	@Test
	public void testProgressState() {
		MessageState[] startStates = {MessageState.NEW, MessageState.QUEUED, MessageState.ISSUED};
		MessageState[] endStates = {MessageState.QUEUED, MessageState.ISSUED, MessageState.ISSUED};
		
		for (int i = 0; i < startStates.length; i++) {
			MessageState ms = startStates[i].progressState();
			assertTrue("Progressed state:" + ms.getStateID()
					+ " is not correct, it should be: " + endStates[i].getStateID(), 
					(ms != null) && (ms.getStateID() == endStates[i].getStateID()) );
		}

	}
	
}
