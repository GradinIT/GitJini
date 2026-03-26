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

    public static Object createPartitionedProxy(JavaSpace05[] partitions) {
        return new PartitionedJavaSpace(partitions);
    }

    private static Object findEmbedded(String url) throws Exception {
        // format: /./spaceName?params
        String path = url.substring(3);
        int queryIdx = path.indexOf('?');
        String spaceName = queryIdx == -1 ? path : path.substring(0, queryIdx);
        Map<String, String> params = parseQueryParams(queryIdx == -1 ? "" : path.substring(queryIdx + 1));

        BasicJavaSpace localSpace = new BasicJavaSpace();
        
        // Check for clustering parameters
        if (params.containsKey("total_members")) {
            return createClusteredProxy(localSpace, spaceName, params);
        }

        return localSpace;
    }

    private static Object createClusteredProxy(JavaSpace05 localSpace, String spaceName, Map<String, String> params) throws Exception {
        String totalMembersStr = params.get("total_members");
        String[] parts = totalMembersStr.split(",");
        int primaries = Integer.parseInt(parts[0]);
        int backups = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        
        int id = Integer.parseInt(params.getOrDefault("id", "1"));
        int backupId = Integer.parseInt(params.getOrDefault("backup_id", "0"));

        if (backups > 0) {
            // Find our partner (if we are primary, find backup; if we are backup, find primary)
            // For simulation, we'll assume we can find it via LUS
            JavaSpace05 partner = null;
            try {
                String partnerUrl;
                if (backupId == 0) {
                    // We are primary, find backup 1
                    partnerUrl = "jini://localhost:1099/*/space?total_members=" + totalMembersStr + "&id=" + id + "&backup_id=1";
                } else {
                    // We are backup, find primary
                    partnerUrl = "jini://localhost:1099/*/space?total_members=" + totalMembersStr + "&id=" + id + "&backup_id=0";
                }
                // partner = (JavaSpace05) find(partnerUrl); // This would cause infinite recursion or long wait
            } catch (Exception ignored) {}

            return new ReplicatingJavaSpace(localSpace, partner, backupId > 0);
        }
        
        return localSpace;
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
