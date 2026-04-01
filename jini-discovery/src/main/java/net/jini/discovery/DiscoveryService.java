package net.jini.discovery;

import net.jini.core.lookup.*;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.EventRegistration;
import net.jini.core.discovery.LookupLocator;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A registry for simulated discovery services. 
 * Allows lookup services to register themselves for local or remote discovery.
 */
public class DiscoveryService {
    private static final Map<String, ServiceRegistrar> registry = new ConcurrentHashMap<>();
    private static ServerSocket serverSocket;
    private static Thread listenerThread;

    public static void register(String host, int port, ServiceRegistrar registrar) {
        registry.put(host + ":" + port, registrar);
        startServer(port);
    }

    private static synchronized void startServer(int port) {
        if (serverSocket != null) return;
        try {
            serverSocket = new ServerSocket(port);
            listenerThread = new Thread(() -> {
                System.out.println("[DISCOVERY] Server listening on port " + port);
                while (true) {
                    ServerSocket ss = serverSocket;
                    if (ss == null || ss.isClosed()) break;
                    try {
                        final Socket socket = ss.accept();
                        new Thread(() -> {
                            try (Socket s = socket;
                                 ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream())) {
                                
                                oos.flush(); // Send header immediately to avoid EOFException on client's new ObjectInputStream
                                
                                try (ObjectInputStream ois = new ObjectInputStream(s.getInputStream())) {
                                    Object input = ois.readObject();
                                    if (!(input instanceof String)) {
                                        return;
                                    }
                                    String command = (String) input;
                                    System.out.println("[DISCOVERY] Command '" + command + "' received from " + socket.getInetAddress());
                                    
                                    // Handle the fact that registry might contain 0.0.0.0 or actual host
                                    ServiceRegistrar registrar = registry.get("0.0.0.0:" + port);
                                    if (registrar == null) {
                                        registrar = registry.get(socket.getLocalAddress().getHostAddress() + ":" + port);
                                    }
                                    if (registrar == null) {
                                        registrar = registry.get("localhost:" + port);
                                    }
                                    if (registrar == null) {
                                        registrar = registry.get("127.0.0.1:" + port);
                                    }
                                    if (registrar == null) {
                                        // Fallback to any host with the given port
                                        for (Map.Entry<String, ServiceRegistrar> entry : registry.entrySet()) {
                                            if (entry.getKey().endsWith(":" + port)) {
                                                registrar = entry.getValue();
                                                break;
                                            }
                                        }
                                    }
                                    
                                    if (registrar == null) {
                                        System.out.println("[DISCOVERY] Command '" + command + "' received but no registrar found for port " + port + ". Registry keys: " + registry.keySet());
                                        oos.writeObject(null);
                                    } else if ("GET_REGISTRAR".equals(command)) {
                                        // Return a remote proxy if we are being called remotely
                                        String clientAddress = socket.getInetAddress().getHostAddress();
                                        if (clientAddress.equals("127.0.0.1") || clientAddress.equals("0.0.0.0") || "localhost".equals(clientAddress)) {
                                            System.out.println("[DISCOVERY] Serving GET_REGISTRAR locally to " + clientAddress);
                                            oos.writeObject(registrar);
                                        } else {
                                            // For remote clients, return a proxy that points back to us.
                                            // In Docker/cloud environments, we might need to use a public address.
                                            String proxyHost = System.getProperty("discovery.proxy.host");
                                            if (proxyHost == null) {
                                                // If we're inside Docker, we probably want to use the hostname we're listening on.
                                                // If that's 0.0.0.0, it's not helpful.
                                                proxyHost = System.getProperty("lus.host", "localhost");
                                                if ("0.0.0.0".equals(proxyHost)) {
                                                    // Fallback to what we are actually listening on if available, or localhost.
                                                    proxyHost = socket.getLocalAddress().getHostName();
                                                    if ("0.0.0.0".equals(proxyHost) || "localhost".equals(proxyHost) || "127.0.0.1".equals(proxyHost)) {
                                                        proxyHost = "localhost";
                                                    }
                                                }
                                            }
                                            System.out.println("[DISCOVERY] Serving GET_REGISTRAR remotely to " + clientAddress + " using proxyHost=" + proxyHost);
                                            oos.writeObject(new RemoteServiceRegistrarProxy(proxyHost, port));
                                        }
                                    } else if ("REGISTER".equals(command)) {
                                        try {
                                            ServiceItem item = (ServiceItem) ois.readObject();
                                            Object next = ois.readObject();
                                            long duration = 0;
                                            if (next instanceof Long) {
                                                duration = (Long) next;
                                            }
                                            ServiceRegistration reg = registrar.register(item, duration);
                                            oos.writeObject(reg);
                                        } catch (NotSerializableException nse) {
                                            System.err.println("[DISCOVERY] NotSerializableException during REGISTER: " + nse.getMessage());
                                            nse.printStackTrace();
                                            throw nse;
                                        }
                                    } else if ("LOOKUP".equals(command)) {
                                        ServiceTemplate tmpl = (ServiceTemplate) ois.readObject();
                                        Object result = registrar.lookup(tmpl);
                                        System.out.println("[DISCOVERY] LOOKUP called from " + socket.getInetAddress() + " with types " + (tmpl.serviceTypes != null && tmpl.serviceTypes.length > 0 ? tmpl.serviceTypes[0].getName() : "null") + ". Found: " + (result != null));
                                        oos.writeObject(result);
                                    } else if ("SERVICE_LOOKUP".equals(command)) {
                                        ServiceTemplate tmpl = (ServiceTemplate) ois.readObject();
                                        Object result = registrar.serviceLookup(tmpl);
                                        System.out.println("[DISCOVERY] SERVICE_LOOKUP called from " + socket.getInetAddress() + " with types " + (tmpl.serviceTypes != null && tmpl.serviceTypes.length > 0 ? tmpl.serviceTypes[0].getName() : "null") + ". Found: " + (result != null));
                                        oos.writeObject(result);
                                    } else if ("LOOKUP_MULTI".equals(command)) {
                                        ServiceTemplate tmpl = (ServiceTemplate) ois.readObject();
                                        Object next = ois.readObject();
                                        int maxMatches = 1;
                                        if (next instanceof Integer) {
                                            maxMatches = (Integer) next;
                                        }
                                        ServiceMatches matches = registrar.lookup(tmpl, maxMatches);
                                        System.out.println("[DISCOVERY] LOOKUP_MULTI called from " + socket.getInetAddress() + " with types " + (tmpl.serviceTypes != null && tmpl.serviceTypes.length > 0 ? tmpl.serviceTypes[0].getName() : "null") + ". Found " + (matches != null ? matches.totalMatches : 0) + " matches.");
                                        oos.writeObject(matches);
                                    }
                                    oos.flush();
                                } catch (IOException | ClassNotFoundException e) {
                                    // Connection might be closed or data is invalid
                                }
                            } catch (IOException e) {
                                // Connection issue
                            }
                        }).start();
                    } catch (IOException e) {
                        ServerSocket ssForCheck = serverSocket;
                        if (ssForCheck != null && !ssForCheck.isClosed()) {
                            // Silence common socket reset errors during shutdown
                        }
                    }
                }
            });
            listenerThread.setDaemon(true);
            listenerThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void stopServer() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
                if (listenerThread != null) {
                    listenerThread.interrupt();
                    listenerThread = null;
                }
                registry.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static ServiceRegistrar getRegistrar(String host, int port) {
        // First check local registry with exact key
        ServiceRegistrar local = registry.get(host + ":" + port);
        if (local != null) return local;

        // Check local registry with common localhost aliases if host is local
        if ("localhost".equals(host) || "127.0.0.1".equals(host) || "0.0.0.0".equals(host)) {
            local = registry.get("0.0.0.0:" + port);
            if (local == null) {
                local = registry.get("localhost:" + port);
            }
            if (local == null) {
                local = registry.get("127.0.0.1:" + port);
            }
            if (local == null) {
                // Fallback to any host in the registry for this port
                for (Map.Entry<String, ServiceRegistrar> entry : registry.entrySet()) {
                    if (entry.getKey().endsWith(":" + port)) {
                        return entry.getValue();
                    }
                }
            }
            if (local != null) return local;
        }

        // Then try remote lookup via socket
        try (Socket socket = new Socket(host, port);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {
            
            oos.flush(); // Send header immediately
            
            try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
                socket.setSoTimeout(5000);
                
                oos.writeObject("GET_REGISTRAR");
                oos.flush();
                
                Object result = ois.readObject();
                return (ServiceRegistrar) result;
            }
        } catch (IOException | ClassNotFoundException e) {
            // Only report failure if we are NOT trying to connect to localhost when nothing is there
            if (!"localhost".equals(host) && !"127.0.0.1".equals(host)) {
                System.err.println("[DISCOVERY] Failed to connect to " + host + ":" + port + ": " + e.getMessage());
                throw new RuntimeException("Failed to connect to " + host + ":" + port, e);
            }
            return null;
        }
    }

    /**
     * A proxy that forwards ServiceRegistrar calls over a socket to the DiscoveryService server.
     */
    private static class RemoteServiceRegistrarProxy implements ServiceRegistrar, Serializable {
        private final String host;
        private final int port;

        public RemoteServiceRegistrarProxy(String host, int port) {
            this.host = host;
            this.port = port;
        }

        private Object sendRequest(String command, Object... args) throws RemoteException {
            try (Socket socket = new Socket(host, port);
                 ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {
                
                oos.flush(); // Send header immediately
                
                try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
                    oos.writeObject(command);
                    oos.flush();
                    for (Object arg : args) {
                        oos.writeObject(arg);
                    }
                    oos.flush();
                    return ois.readObject();
                }
            } catch (Exception e) {
                throw new RemoteException("Failed to forward " + command + " to " + host + ":" + port, e);
            }
        }

        @Override
        public ServiceRegistration register(ServiceItem item, long leaseDuration) throws RemoteException {
            return (ServiceRegistration) sendRequest("REGISTER", item, leaseDuration);
        }

        @Override
        public Object lookup(ServiceTemplate tmpl) throws RemoteException {
            return sendRequest("LOOKUP", tmpl);
        }

        @Override
        public Object serviceLookup(ServiceTemplate tmpl) throws RemoteException {
            return sendRequest("SERVICE_LOOKUP", tmpl);
        }

        @Override
        public ServiceMatches lookup(ServiceTemplate tmpl, int maxMatches) throws RemoteException {
            return (ServiceMatches) sendRequest("LOOKUP_MULTI", tmpl, maxMatches);
        }

        @Override
        public EventRegistration notify(ServiceTemplate tmpl, int transitions, RemoteEventListener listener, MarshalledObject handback, long leaseDuration) throws RemoteException {
            return null; // Not implemented for simulation
        }

        @Override public Class[] getEntryClasses(ServiceTemplate tmpl) throws RemoteException { return new Class[0]; }
        @Override public Object[] getFieldValues(ServiceTemplate tmpl, int setIndex, String field) throws NoSuchFieldException, RemoteException { return new Object[0]; }
        @Override public Class[] getServiceTypes(ServiceTemplate tmpl, String prefix) throws RemoteException { return new Class[0]; }
        @Override public ServiceID getServiceID() { return new ServiceID(0, 0); }
        @Override public LookupLocator getLocator() throws RemoteException { return new LookupLocator(host, port); }
        @Override public String[] getGroups() throws RemoteException { return new String[]{""}; }
    }
}
