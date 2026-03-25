package net.jini.core.lookup;

import java.io.Serializable;
import java.util.Objects;

/**
 * A ServiceID is a 128-bit value that is used to uniquely identify a service.
 */
public final class ServiceID implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long mostSigBits;
    private final long leastSigBits;

    public ServiceID(long mostSigBits, long leastSigBits) {
        this.mostSigBits = mostSigBits;
        this.leastSigBits = leastSigBits;
    }

    public long getMostSignificantBits() {
        return mostSigBits;
    }

    public long getLeastSignificantBits() {
        return leastSigBits;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceID serviceID = (ServiceID) o;
        return mostSigBits == serviceID.mostSigBits && leastSigBits == serviceID.leastSigBits;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mostSigBits, leastSigBits);
    }

    @Override
    public String toString() {
        return String.format("%016x%016x", mostSigBits, leastSigBits);
    }
}
