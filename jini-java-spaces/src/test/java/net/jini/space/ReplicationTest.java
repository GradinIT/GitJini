package net.jini.space;

import net.jini.core.entry.Entry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ReplicationTest {

    public static class TestEntry implements Entry {
        public String value;
        public TestEntry() {}
        public TestEntry(String v) { this.value = v; }
    }

    @Test
    public void testReplication() throws Exception {
        BasicJavaSpace primary = new BasicJavaSpace();
        BasicJavaSpace backup = new BasicJavaSpace();
        ReplicatingJavaSpace replicatingSpace = new ReplicatingJavaSpace(primary, backup);

        // Write to primary (replicated to backup)
        replicatingSpace.write(new TestEntry("hello"), null, 10000);

        // Check both have it
        assertNotNull(primary.read(new TestEntry("hello"), null, 0), "Primary should have the entry");
        assertNotNull(backup.read(new TestEntry("hello"), null, 0), "Backup should have the entry");

        // Take from primary (replicated to backup)
        Entry taken = replicatingSpace.take(new TestEntry("hello"), null, 0);
        assertNotNull(taken, "Should have taken from primary");

        // Check both are empty
        assertNull(primary.read(new TestEntry("hello"), null, 0), "Primary should be empty");
        assertNull(backup.read(new TestEntry("hello"), null, 0), "Backup should be empty");
    }

    @Test
    public void testFailover() throws Exception {
        BasicJavaSpace primary = new BasicJavaSpace();
        BasicJavaSpace backup = new BasicJavaSpace();
        
        // Mock a failure by having primary throw an exception on read
        JavaSpace05 primaryWithFailure = new BasicJavaSpace() {
            @Override
            public Entry read(Entry tmpl, net.jini.core.transaction.Transaction txn, long timeout) 
                throws java.rmi.RemoteException {
                throw new java.rmi.RemoteException("Primary failed");
            }
        };

        ReplicatingJavaSpace replicatingSpace = new ReplicatingJavaSpace(primaryWithFailure, backup);

        // Put something in backup manually
        backup.write(new TestEntry("backup-only"), null, 10000);

        // Read from replicating space should failover to backup
        Entry found = replicatingSpace.read(new TestEntry("backup-only"), null, 0);
        assertNotNull(found, "Should have failed over to backup");
        assertEquals("backup-only", ((TestEntry)found).value);
    }
}
