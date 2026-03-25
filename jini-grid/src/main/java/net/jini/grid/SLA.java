package net.jini.grid;

import java.io.Serializable;

public class SLA implements Serializable {
    private String clusterSchema = "default"; // default, sync-replicated, async-replicated, partitioned
    private int numberOfInstances = 1;
    private int numberOfBackups = 0;
    private int maxInstancesPerVM = 1;

    public String getClusterSchema() {
        return clusterSchema;
    }

    public void setClusterSchema(String clusterSchema) {
        this.clusterSchema = clusterSchema;
    }

    public int getNumberOfInstances() {
        return numberOfInstances;
    }

    public void setNumberOfInstances(int numberOfInstances) {
        this.numberOfInstances = numberOfInstances;
    }

    public int getNumberOfBackups() {
        return numberOfBackups;
    }

    public void setNumberOfBackups(int numberOfBackups) {
        this.numberOfBackups = numberOfBackups;
    }

    public int getMaxInstancesPerVM() {
        return maxInstancesPerVM;
    }

    public void setMaxInstancesPerVM(int maxInstancesPerVM) {
        this.maxInstancesPerVM = maxInstancesPerVM;
    }

    @Override
    public String toString() {
        return "SLA{" +
                "clusterSchema='" + clusterSchema + '\'' +
                ", numberOfInstances=" + numberOfInstances +
                ", numberOfBackups=" + numberOfBackups +
                ", maxInstancesPerVM=" + maxInstancesPerVM +
                '}';
    }
}
