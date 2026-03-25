package net.jini.lookup;

import net.jini.core.lookup.*;
import net.jini.core.entry.Entry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.rmi.RemoteException;

public class BasicLookupServiceTest {

    @Test
    public void testRegisterAndLookup() throws RemoteException {
        BasicLookupService lus = new BasicLookupService();
        
        String myService = "Hello Jini";
        ServiceItem item = new ServiceItem(null, myService, new Entry[0]);
        
        ServiceRegistration reg = lus.register(item, 1000);
        assertNotNull(reg.getServiceID());
        
        ServiceTemplate tmpl = new ServiceTemplate(null, new Class[]{String.class}, null);
        Object found = lus.lookup(tmpl);
        
        assertEquals(myService, found);
    }

    @Test
    public void testLookupNoMatch() throws RemoteException {
        BasicLookupService lus = new BasicLookupService();
        ServiceTemplate tmpl = new ServiceTemplate(null, new Class[]{Integer.class}, null);
        Object found = lus.lookup(tmpl);
        assertNull(found);
    }
}
