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
                    try (Socket socket = ss.accept();
                         ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                         ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {
                        
                        String command = (String) ois.readObject();
                        
                        // Handle the fact that registry might contain 0.0.0.0 or actual host
                        ServiceRegistrar registrar = registry.get("0.0.0.0:" + port);
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
                            oos.writeObject(null);
                        } else if ("GET_REGISTRAR".equals(command)) {
                            // Return a remote proxy if we are being called remotely
                            String clientAddress = socket.getInetAddress().getHostAddress();
                            if (clientAddress.equals("127.0.0.1") || clientAddress.equals("0.0.0.0")) {
                                oos.writeObject(registrar);
                            } else {
                                // For remote clients, return a proxy that points back to us
                                // We use the host as defined in system properties
                                String host = System.getProperty("lus.host", "localhost");
                                // If lus.host is 0.0.0.0, we should probably use the hostname "lus" or similar
                                // But in docker-compose it is set to 0.0.0.0 for LUS, and "lus" for others.
                                if ("0.0.0.0".equals(host)) {
                                    host = "lus"; 
                                }
                                oos.writeObject(new RemoteServiceRegistrarProxy(host, port));
                            }
                        } else if ("REGISTER".equals(command)) {
                            ServiceItem item = (ServiceItem) ois.readObject();
                            long duration = ois.readLong();
                            oos.writeObject(registrar.register(item, duration));
                        } else if ("LOOKUP".equals(command)) {
                            ServiceTemplate tmpl = (ServiceTemplate) ois.readObject();
                            oos.writeObject(registrar.lookup(tmpl));
                        } else if ("SERVICE_LOOKUP".equals(command)) {
                            ServiceTemplate tmpl = (ServiceTemplate) ois.readObject();
                            oos.writeObject(registrar.serviceLookup(tmpl));
                        } else if ("LOOKUP_MULTI".equals(command)) {
                            ServiceTemplate tmpl = (ServiceTemplate) ois.readObject();
                            Object next = ois.readObject();
                            int maxMatches;
                            if (next instanceof Integer) {
                                maxMatches = (Integer) next;
                            } else {
                                // Fallback if it's not an Integer (though we expect it to be based on sendRequest)
                                maxMatches = 1; 
                            }
                            oos.writeObject(registrar.lookup(tmpl, maxMatches));
                        }
                        oos.flush();
                    } catch (Exception e) {
                        if (!serverSocket.isClosed()) {
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
        // First check local registry
        ServiceRegistrar local = registry.get(host + ":" + port);
        if (local != null) return local;

        // Then try remote lookup via socket
        try (Socket socket = new Socket(host, port);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
            
            socket.setSoTimeout(5000);
            oos.writeObject("GET_REGISTRAR");
            oos.flush();
            
            return (ServiceRegistrar) ois.readObject();
        } catch (Exception e) {
            System.err.println("[DISCOVERY] Failed to connect to " + host + ":" + port + ": " + e.getMessage());
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
                 ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
                
                oos.writeObject(command);
                for (Object arg : args) {
                    if (arg instanceof Integer) oos.writeInt((Integer) arg);
                    else if (arg instanceof Long) oos.writeLong((Long) arg);
                    else oos.writeObject(arg);
                }
                oos.flush();
                return ois.readObject();
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
