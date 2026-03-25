package net.jini.space;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * The exception InternalSpaceException may be thrown by a JavaSpaces service
 * that encounters an inconsistency in its own internal state or is unable
 * to process a request because of internal limitations.
 */
public class InternalSpaceException extends RuntimeException {
    private static final long serialVersionUID = -4167507833172939849L;

    /**
     * The nested exception, if any.
     */
    public final Throwable nestedException;

    /**
     * Create an exception with the specified message.
     *
     * @param msg the message
     */
    public InternalSpaceException(String msg) {
        super(msg);
        this.nestedException = null;
    }

    /**
     * Create an exception with the specified message and nested exception.
     *
     * @param msg the message
     * @param e the nested exception
     */
    public InternalSpaceException(String msg, Throwable e) {
        super(msg);
        this.nestedException = e;
    }

    @Override
    public void printStackTrace() {
        super.printStackTrace();
        if (nestedException != null) {
            System.err.print("Nested exception: ");
            nestedException.printStackTrace();
        }
    }

    @Override
    public void printStackTrace(PrintStream out) {
        super.printStackTrace(out);
        if (nestedException != null) {
            out.print("Nested exception: ");
            nestedException.printStackTrace(out);
        }
    }

    @Override
    public void printStackTrace(PrintWriter out) {
        super.printStackTrace(out);
        if (nestedException != null) {
            out.print("Nested exception: ");
            nestedException.printStackTrace(out);
        }
    }
}
