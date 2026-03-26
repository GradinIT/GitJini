package net.jini.space.dao;
import net.jini.core.entry.Entry;

public class SpaceEntity implements Entry {
    public String id;
    public String data;

    public SpaceEntity() {}

    public SpaceEntity(String id, String data) {
        this.id = id;
        this.data = data;
    }
}
