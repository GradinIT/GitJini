package net.jini.space;

import java.util.Properties;

/**
 * FactoryBean for creating an embedded JavaSpace.
 */
public class EmbeddedSpaceFactoryBean {

    private String name;
    private String lookupGroups;
    private int lookupTimeout = 5000;
    private boolean versioned = false;
    private Properties properties = new Properties();

    public void setName(String name) {
        this.name = name;
    }

    public void setLookupGroups(String lookupGroups) {
        this.lookupGroups = lookupGroups;
    }

    public void setLookupTimeout(int lookupTimeout) {
        this.lookupTimeout = lookupTimeout;
    }

    public void setVersioned(boolean versioned) {
        this.versioned = versioned;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public Object getObject() throws Exception {
        // Construct a URL for SpaceFinder based on the properties
        StringBuilder url = new StringBuilder("/./").append(name);
        url.append("?versioned=").append(versioned);
        if (lookupGroups != null) {
            url.append("&groups=").append(lookupGroups);
        }
        url.append("&timeout=").append(lookupTimeout);
        
        // Add other properties if any (simplified)
        for (String key : properties.stringPropertyNames()) {
            url.append("&").append(key).append("=").append(properties.getProperty(key));
        }

        return SpaceFinder.find(url.toString());
    }
}
