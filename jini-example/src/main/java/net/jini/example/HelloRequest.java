package net.jini.example;

import net.jini.core.export.ServiceRouting;
import java.io.Serializable;

public class HelloRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @ServiceRouting
    public String routingKey;
    
    public String name;

    public HelloRequest(String routingKey, String name) {
        this.routingKey = routingKey;
        this.name = name;
    }
}
