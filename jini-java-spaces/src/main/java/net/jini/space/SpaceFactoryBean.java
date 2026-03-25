package net.jini.space;

import net.jini.space.config.modifiers.WriteModifier;

/**
 * FactoryBean for creating a GigaSpace proxy.
 */
public class SpaceFactoryBean {

    private Object space;
    private boolean clustered = true;
    private long defaultReadTimeout = 0;
    private long defaultTakeTimeout = 0;
    private long defaultWriteLease = Long.MAX_VALUE;
    private WriteModifier[] defaultWriteModifiers;

    public void setSpace(Object space) {
        this.space = space;
    }

    public void setClustered(boolean clustered) {
        this.clustered = clustered;
    }

    public void setDefaultReadTimeout(long defaultReadTimeout) {
        this.defaultReadTimeout = defaultReadTimeout;
    }

    public void setDefaultTakeTimeout(long defaultTakeTimeout) {
        this.defaultTakeTimeout = defaultTakeTimeout;
    }

    public void setDefaultWriteLease(long defaultWriteLease) {
        this.defaultWriteLease = defaultWriteLease;
    }

    public void setDefaultWriteModifiers(WriteModifier[] defaultWriteModifiers) {
        this.defaultWriteModifiers = defaultWriteModifiers;
    }

    public Object getObject() {
        // In a real system, this would return a GigaSpace proxy wrapper.
        // For simulation, we return the space object itself or a proxy.
        return space;
    }
}
