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
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.core.lookup.ServiceRegistration;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lease.Lease;
import java.util.concurrent.CopyOnWriteArrayList;
import static org.junit.jupiter.api.Assertions.*;

public class BasicJavaSpaceTest {

    private static class MockRegistrar implements ServiceRegistrar {
        private final List<ServiceItem> items = new CopyOnWriteArrayList<>();
        @Override
        public ServiceRegistration register(ServiceItem item, long leaseDuration) throws RemoteException {
            items.add(item);
            return new ServiceRegistration() {
                @Override public ServiceID getServiceID() { return item.serviceID; }
                @Override public Lease getLease() { return null; }
                @Override public void setAttributes(Entry[] attrSets) throws RemoteException {}
                @Override public void addAttributes(Entry[] attrSets) throws RemoteException {}
                @Override public void modifyAttributes(Entry[] attrSetTemplates, Entry[] attrSets) throws RemoteException {}
            };
        }
        @Override public Object lookup(ServiceTemplate tmpl) throws RemoteException { return null; }
        @Override public ServiceMatches lookup(ServiceTemplate tmpl, int maxMatches) throws RemoteException { return null; }
        @Override public EventRegistration notify(ServiceTemplate tmpl, int transitions, RemoteEventListener listener, java.rmi.MarshalledObject handback, long leaseDuration) throws RemoteException { return null; }
        @Override public Class[] getEntryClasses(ServiceTemplate tmpl) throws RemoteException { return null; }
        @Override public Object[] getFieldValues(ServiceTemplate tmpl, int setIndex, String field) throws NoSuchFieldException, RemoteException { return null; }
        @Override public Class[] getServiceTypes(ServiceTemplate tmpl, String prefix) throws RemoteException { return null; }
        @Override public ServiceID getServiceID() { return null; }
        @Override public net.jini.core.discovery.LookupLocator getLocator() throws RemoteException { return null; }
        @Override public String[] getGroups() throws RemoteException { return null; }
    }

    @Test
    public void testAutoDiscoveryRegistration() throws Exception {
        JavaSpace space = new BasicJavaSpace();
        DiscoveryHelper.AutoRegistration autoReg = DiscoveryHelper.beginAutoRegistration(space, null, null, 1000);
        
        try {
            assertEquals(0, autoReg.getRegistrations().size());
            
            // Simulate discovery of a new registrar
            MockRegistrar registrar = new MockRegistrar();
            // We need to get access to the discovery object or simulate it via the helper
            // Since AutoRegistration is a DiscoveryListener, we can call it directly for the test
            autoReg.discovered(new net.jini.discovery.DiscoveryEvent(this, new ServiceRegistrar[]{registrar}));
            
            assertEquals(1, autoReg.getRegistrations().size());
            assertEquals(1, registrar.items.size());
            assertSame(space, registrar.items.get(0).service);
            
            // Simulate discovery of another registrar
            MockRegistrar registrar2 = new MockRegistrar();
            autoReg.discovered(new net.jini.discovery.DiscoveryEvent(this, new ServiceRegistrar[]{registrar2}));
            
            assertEquals(2, autoReg.getRegistrations().size());
            assertEquals(1, registrar2.items.size());
        } finally {
            autoReg.terminate();
        }
    }

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
        
        assertNotNull(ms.getLease());
        assertTrue(ms.getLease().getExpiration() > System.currentTimeMillis());

        int count = 0;
        Entry e;
        while ((e = ms.next()) != null) {
            count++;
            assertTrue(((TestEntry)e).name.startsWith("match"));
            assertEquals(e, ms.getSnapshot());
        }
        assertEquals(2, count);
        
        // Test lease cancellation
        ms.getLease().cancel();
        assertNull(ms.next());
    }

    @Test
    public void testMatchSetLiveUpdate() throws Exception {
        JavaSpace05 space = new BasicJavaSpace();
        
        space.write(new TestEntry("live1", 1), null, 1000);
        
        Collection templates = Arrays.asList(new TestEntry("live1", null), new TestEntry("live2", null));
        MatchSet ms = space.contents(templates, null, 1000, 10);
        
        // Read first entry
        TestEntry e1 = (TestEntry) ms.next();
        assertNotNull(e1);
        assertEquals("live1", e1.name);
        
        // Currently no more entries
        assertNull(ms.next());
        
        // Write new matching entry
        space.write(new TestEntry("live2", 2), null, 1000);
        
        // MatchSet should see it now (live update)
        TestEntry e2 = (TestEntry) ms.next();
        assertNotNull(e2);
        assertEquals("live2", e2.name);
        
        assertNull(ms.next());
    }
}
