package net.jini.core.transaction;

/**
 * Base class for transaction-related exceptions.
 */
public class TransactionException extends Exception {
    private static final long serialVersionUID = -1L;

    public TransactionException() {
        super();
    }

    public TransactionException(String s) {
        super(s);
    }
}
