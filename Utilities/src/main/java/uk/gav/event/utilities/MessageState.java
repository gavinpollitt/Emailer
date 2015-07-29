package uk.gav.event.utilities;

/**
 * 
 * @author gavin
 * 
 * Enumeration to centralise the maintenance of the email state used in both the Java
 * code and on the database/
 *
 */
public enum MessageState {
	NEW(null), QUEUED(1), ISSUED(2);

	private Integer stateID;

	MessageState(Integer stateID) {
		this.stateID = stateID;
	}

	public Integer getStateID() {
		return stateID;
	}

	public void setStateID(Integer stateID) {
		this.stateID = stateID;
	}

	/**
	 * 
	 * @param stateID The integer value stored on the database corresponding to this enumeration constant.
	 * @return The enumeration constant corresponding to the internal value.
	 */
	public static MessageState identifyState(Integer stateID) {
		MessageState fState = null;
		for (MessageState ms : MessageState.values()) {
			if (ms.getStateID() == stateID) {
				fState = ms;
			}
		}

		return fState;
	}

	/**
	 * 
	 * @return The next legal state for the current enumeration type.
	 */
	public MessageState progressState() {
		MessageState nms = null;
		switch (this) {
		case NEW:
			nms = QUEUED;
			break;
		case QUEUED:
			nms = ISSUED;
			break;
		case ISSUED:
			nms = ISSUED;
			break;
		default:
			nms = null;
		}

		return nms;
	}

}
