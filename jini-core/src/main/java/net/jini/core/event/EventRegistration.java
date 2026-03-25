package net.jini.core.event;

import net.jini.core.lease.Lease;
import java.io.Serializable;

/**
 * EventRegistration is returned when a listener registers with an event generator.
 */
public class EventRegistration implements Serializable {
    private static final long serialVersionUID = 1L;

    protected long eventID;
    protected Object source;
    protected Lease lease;
    protected long seqNum;

    public EventRegistration(long eventID, Object source, Lease lease, long seqNum) {
        this.eventID = eventID;
        this.source = source;
        this.lease = lease;
        this.seqNum = seqNum;
    }

    public long getID() { return eventID; }
    public Object getSource() { return source; }
    public Lease getLease() { return lease; }
    public long getSequenceNumber() { return seqNum; }
}
