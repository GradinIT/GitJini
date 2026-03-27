package net.jini.lookup;

import net.jini.core.lookup.*;
import net.jini.core.entry.Entry;

import java.rmi.RemoteException;

public class ManualTest {

    public static void main(String[] args) throws RemoteException {
        BasicLookupService lus = new BasicLookupService();
        
        String myService = "Hello Jini";
        ServiceItem item = new ServiceItem(null, myService, new Entry[0]);
        
        ServiceRegistration reg = lus.register(item, 1000);
        System.out.println("[DEBUG_LOG] ServiceID: " + reg.getServiceID());
        
        ServiceTemplate tmpl = new ServiceTemplate(null, new Class[]{String.class}, null);
        Object found = lus.lookup(tmpl);
        
        System.out.println("[DEBUG_LOG] Found: " + found);
        
        if ("Hello Jini".equals(found)) {
            System.out.println("[DEBUG_LOG] TEST PASSED");
        } else {
            System.out.println("[DEBUG_LOG] TEST FAILED");
            System.exit(1);
        }
    }
}
