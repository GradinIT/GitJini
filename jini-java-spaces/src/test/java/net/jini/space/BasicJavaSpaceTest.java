package net.jini.space;

import net.jini.core.entry.Entry;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import org.junit.jupiter.api.Test;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.jini.core.lease.Lease;
import static org.junit.jupiter.api.Assertions.*;

public class BasicJavaSpaceTest {

    private static class MockTransaction implements Transaction {
        @Override public void commit() throws TransactionException, RemoteException {}
        @Override public void abort() throws TransactionException, RemoteException {}
    }

    public static class TestEntry implements Entry {
        public String name;
        public Integer value;

        public TestEntry() {}
        public TestEntry(String name, Integer value) {
            this.name = name;
            this.value = value;
        }
    }

    @Test
    public void testWriteReadTake() throws Exception {
        JavaSpace space = new BasicJavaSpace();
        TestEntry entry1 = new TestEntry("test1", 1);
        
        space.write(entry1, null, 1000);
        
        // Read with exact match
        TestEntry template = new TestEntry("test1", null);
        TestEntry result = (TestEntry) space.read(template, null, 0);
        assertNotNull(result);
        assertEquals("test1", result.name);
        assertEquals(1, result.value);
        
        // Take entry
        TestEntry taken = (TestEntry) space.take(template, null, 0);
        assertNotNull(taken);
        assertEquals("test1", taken.name);
        
        // Should be gone now
        TestEntry result2 = (TestEntry) space.read(template, null, 0);
        assertNull(result2);
    }
    
    @Test
    public void testMultipleEntries() throws Exception {
        JavaSpace space = new BasicJavaSpace();
        space.write(new TestEntry("a", 1), null, 1000);
        space.write(new TestEntry("b", 2), null, 1000);
        
        TestEntry template = new TestEntry("b", null);
        TestEntry result = (TestEntry) space.read(template, null, 0);
        assertNotNull(result);
        assertEquals("b", result.name);
        assertEquals(2, result.value);
    }

    @Test
    public void testWildcardMatch() throws Exception {
        JavaSpace space = new BasicJavaSpace();
        space.write(new TestEntry("a", 1), null, 1000);
        
        TestEntry result = (TestEntry) space.read(null, null, 0);
        assertNotNull(result);
        assertEquals("a", result.name);
    }

    @Test
    public void testTransactions() throws Exception {
        BasicJavaSpace space = new BasicJavaSpace();
        Transaction txn = new MockTransaction();
        
        space.write(new TestEntry("txn1", 100), txn, 1000);
        
        // Should not be visible to null transaction
        assertNull(space.read(new TestEntry("txn1", null), null, 0));
        
        // Should be visible to same transaction
        assertNotNull(space.read(new TestEntry("txn1", null), txn, 0));
        
        space.commit(txn);
        
        // Should be visible now
        assertNotNull(space.read(new TestEntry("txn1", null), null, 0));
    }

    @Test
    public void testNotifications() throws Exception {
        JavaSpace space = new BasicJavaSpace();
        AtomicInteger eventCount = new AtomicInteger(0);
        
        RemoteEventListener listener = (event) -> {
            eventCount.incrementAndGet();
        };
        
        space.notify(new TestEntry("notify", null), null, listener, 1000, null);
        
        space.write(new TestEntry("notify", 1), null, 1000);
        space.write(new TestEntry("other", 2), null, 1000);
        space.write(new TestEntry("notify", 3), null, 1000);
        
        assertEquals(2, eventCount.get());
    }

    @Test
    public void testJavaSpace05WriteTake() throws Exception {
        JavaSpace05 space = new BasicJavaSpace();
        
        List entries = Arrays.asList(new TestEntry("batch1", 1), new TestEntry("batch2", 2));
        List durations = Arrays.asList(1000L, 2000L);
        
        List leases = space.write(entries, null, durations);
        assertEquals(2, leases.size());
        assertTrue(leases.get(0) instanceof Lease);
        
        Collection templates = Arrays.asList(new TestEntry("batch1", null), new TestEntry("batch2", null));
        Collection results = space.take(templates, null, 0, 10);
        assertEquals(2, results.size());
    }

    @Test
    public void testJavaSpace05Contents() throws Exception {
        JavaSpace05 space = new BasicJavaSpace();
        space.write(new TestEntry("match1", 1), null, 1000);
        space.write(new TestEntry("match2", 2), null, 1000);
        space.write(new TestEntry("other", 3), null, 1000);
        
        Collection templates = Arrays.asList(new TestEntry("match1", null), new TestEntry("match2", null));
        MatchSet ms = space.contents(templates, null, 1000, 10);
        
        int count = 0;
        Entry e;
        while ((e = ms.next()) != null) {
            count++;
            assertTrue(((TestEntry)e).name.startsWith("match"));
        }
        assertEquals(2, count);
    }
}
