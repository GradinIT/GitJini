package net.jini.core.event;

import java.io.Serializable;

/**
 * RemoteEvent is the base class for all distributed events.
 */
public class RemoteEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    protected Object source;
    protected long eventID;
    protected long seqNum;
    protected Serializable handback;

    public RemoteEvent(Object source, long eventID, long seqNum, Serializable handback) {
        this.source = source;
        this.eventID = eventID;
        this.seqNum = seqNum;
        this.handback = handback;
    }

    public Object getSource() { return source; }
    public long getID() { return eventID; }
    public long getSequenceNumber() { return seqNum; }
    public Serializable getRegistrationObject() { return handback; }
}
