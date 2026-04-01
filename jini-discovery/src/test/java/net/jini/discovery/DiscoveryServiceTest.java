package net.jini.discovery;

import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.lookup.BasicLookupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DiscoveryServiceTest {

    private static final int PORT = 11099;

    @BeforeEach
    public void setup() {
        DiscoveryService.stopServer();
    }

    @AfterEach
    public void tearDown() {
        DiscoveryService.stopServer();
    }

    @Test
    public void testRemoteGetRegistrar() throws Exception {
        ServiceRegistrar registrar = new BasicLookupService();
        DiscoveryService.register("0.0.0.0", PORT, registrar);

        // Wait a bit for server to start
        Thread.sleep(500);

        ServiceRegistrar found = DiscoveryService.getRegistrar("localhost", PORT);
        assertNotNull(found, "Should find the registrar remotely");
        // Verify we got the ACTUAL registrar (singleton) since it's local
        assertTrue(found instanceof BasicLookupService, "Should be local instance due to optimization");
        
        try {
            found.getEntryClasses(new ServiceTemplate(null, null, null));
        } catch (Exception e) {
            fail("Should not throw exception");
        }
    }

    @Test
    public void testProxySerialization() throws Exception {
        ServiceRegistrar registrar = new BasicLookupService();
        DiscoveryService.register("0.0.0.0", PORT, registrar);
        Thread.sleep(500);

        // Manually create a proxy to force remote call
        java.lang.reflect.Constructor<?> cons = Class.forName("net.jini.discovery.DiscoveryService$RemoteServiceRegistrarProxy")
                .getDeclaredConstructor(String.class, int.class);
        cons.setAccessible(true);
        ServiceRegistrar proxy = (ServiceRegistrar) cons.newInstance("localhost", PORT);

        // Let's verify LOOKUP_MULTI works with the new serialization via the proxy
        net.jini.core.lookup.ServiceTemplate tmpl = new net.jini.core.lookup.ServiceTemplate(null, null, null);
        net.jini.core.lookup.ServiceMatches matches = proxy.lookup(tmpl, 10);
        assertNotNull(matches);
    }
}
