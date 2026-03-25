package net.jini.space;

import net.jini.core.discovery.LookupLocator;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for finding and creating JavaSpaces using URL notation.
 */
public class SpaceFinder {

    private static final int DEFAULT_PORT = 10098;

    public static Object find(String url) throws Exception {
        if (url.startsWith("/./")) {
            return findEmbedded(url);
        } else if (url.startsWith("jini://")) {
            return findRemote(url);
        } else if (url.startsWith("java://")) {
            return findJava(url);
        } else {
            // Default to embedded if no protocol
            return findEmbedded("/./" + url);
        }
    }

    private static Object findEmbedded(String url) throws Exception {
        // format: /./spaceName?params
        String path = url.substring(3);
        int queryIdx = path.indexOf('?');
        String spaceName = queryIdx == -1 ? path : path.substring(0, queryIdx);
        Map<String, String> params = parseQueryParams(queryIdx == -1 ? "" : path.substring(queryIdx + 1));

        // For this simulation, we'll just return a BasicJavaSpace
        // In a real system, we might look up in a local registry or create it
        return new BasicJavaSpace();
    }

    private static Object findRemote(String url) throws Exception {
        // format: jini://host:port/container/spaceName?params
        URI uri = new URI(url);
        String host = uri.getHost();
        int port = uri.getPort() == -1 ? DEFAULT_PORT : uri.getPort();
        
        if ("*".equals(host)) {
            // Multicast discovery (simplified for simulation)
            // In a real Jini system, this would use LookupDiscovery
            host = "localhost"; 
        }

        LookupLocator locator = new LookupLocator(host, port);
        ServiceRegistrar registrar = locator.getRegistrar();
        
        if (registrar == null) {
            throw new Exception("Could not find Lookup Service at " + host + ":" + port);
        }

        String path = uri.getPath();
        if (path.startsWith("/")) path = path.substring(1);
        String[] parts = path.split("/");
        String spaceName = parts.length > 1 ? parts[1] : parts[0];

        ServiceTemplate tmpl = new ServiceTemplate(null, new Class[]{JavaSpace.class}, null);
        ServiceMatches matches = registrar.lookup(tmpl, 10);
        
        for (ServiceItem item : matches.items) {
            // Simplified name check
            if (item.service != null) {
                return item.service;
            }
        }

        throw new Exception("Space " + spaceName + " not found via " + url);
    }

    private static Object findJava(String url) throws Exception {
        // java://localhost:10098/containerName/spaceName
        // Similar to jini but implies a specific protocol/implementation
        return findRemote(url.replace("java://", "jini://"));
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length > 1) {
                params.put(kv[0], kv[1]);
            } else {
                params.put(kv[0], "true");
            }
        }
        return params;
    }
}
