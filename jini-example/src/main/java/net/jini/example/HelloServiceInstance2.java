package net.jini.example;

import net.jini.core.export.ExportedService;

@ExportedService(id = "hello-service", instanceId = "instance-2")
public class HelloServiceInstance2 extends HelloServiceImpl {
    public HelloServiceInstance2(String instanceName) {
        super(instanceName);
    }
}
