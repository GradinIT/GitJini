package net.jini.core.entry;

/**
 * Exception thrown when an entry cannot be unmarshalled.
 */
public class UnusableEntryException extends Exception {
    private static final long serialVersionUID = -1L;

    public UnusableEntryException() {
        super();
    }

    public UnusableEntryException(String s) {
        super(s);
    }
}
