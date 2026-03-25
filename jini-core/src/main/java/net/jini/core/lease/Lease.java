package net.jini.core.lease;

import java.io.Serializable;

/**
 * A Lease interface for resources that are granted for a specific period.
 */
public interface Lease extends Serializable {
    long FOREVER = Long.MAX_VALUE;
    long ANY = -1;

    long getExpiration();

    void renew(long duration) throws LeaseException, UnknownLeaseException;

    void cancel() throws LeaseException, UnknownLeaseException;
}
