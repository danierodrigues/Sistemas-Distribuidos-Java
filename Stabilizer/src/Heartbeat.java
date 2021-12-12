import java.io.Serializable;
import java.util.UUID;

public class Heartbeat implements Serializable {
    private UUID id;
    private String type;
    private double cpuUsage;
    private double requestsInQueue;


    public Heartbeat(UUID id, double cpuUsage, double requestsInQueue, String type) {
        this.id = id;
        this.cpuUsage = cpuUsage;
        this.requestsInQueue = requestsInQueue;
        this.type = type;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public double getRequestsInQueue() {
        return requestsInQueue;
    }

    public void setRequestsInQueue(double requestsInQueue) {
        this.requestsInQueue = requestsInQueue;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UUID getId() { return id; }

    public void setId(UUID id) { this.id = id; }
}
