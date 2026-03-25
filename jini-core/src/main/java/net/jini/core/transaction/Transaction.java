package net.jini.core.transaction;

import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * The Transaction interface defines the operations that can be performed on a transaction.
 */
public interface Transaction extends Serializable {
    /**
     * Commit the transaction.
     *
     * @throws TransactionException if the transaction cannot be committed
     * @throws RemoteException if a communication error occurs
     */
    void commit() throws TransactionException, RemoteException;

    /**
     * Abort the transaction.
     *
     * @throws TransactionException if the transaction cannot be aborted
     * @throws RemoteException if a communication error occurs
     */
    void abort() throws TransactionException, RemoteException;
}
