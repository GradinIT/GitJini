package net.jini.discovery;

import java.util.EventListener;

/**
 * Interface that allows receiving discovery events.
 */
public interface DiscoveryListener extends EventListener {
    /**
     * Called when new lookup services are discovered.
     * @param event the discovery event containing discovered registrars
     */
    void discovered(DiscoveryEvent event);

    /**
     * Called when lookup services are discarded.
     * @param event the discovery event containing discarded registrars
     */
    void discarded(DiscoveryEvent event);
}
