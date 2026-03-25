package net.jini.core.lease;

public class UnknownLeaseException extends LeaseException {
    public UnknownLeaseException() { super(); }
    public UnknownLeaseException(String reason) { super(reason); }
}
