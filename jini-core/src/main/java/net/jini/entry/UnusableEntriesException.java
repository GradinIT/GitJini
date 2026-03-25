package net.jini.entry;

import java.util.Collection;
import net.jini.core.entry.UnusableEntryException;

/**
 * Thrown from methods that normally return a collection of Entry instances when one or more of the entries can't be
 * unmarshalled.
 */
public class UnusableEntriesException extends Exception {
    private static final long serialVersionUID = -1L;
    private final Collection entries;
    private final Collection exceptions;

    public UnusableEntriesException(String message, Collection entries, Collection exceptions) {
        super(message);
        this.entries = entries;
        this.exceptions = exceptions;
    }

    public Collection getEntries() {
        return entries;
    }

    public Collection getUnusableEntryExceptions() {
        return exceptions;
    }
}
