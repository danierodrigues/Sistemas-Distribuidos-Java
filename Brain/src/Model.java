import java.io.Serializable;
import java.util.UUID;

public class Model implements Serializable {
    private UUID processID;
    private UUID processorID;
    private String model;

    public Model( UUID processID, UUID processorID, String model){
        this.processID = processID;
        this.processorID = processorID;
        this.model = model;
    }


    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public UUID getProcessorID() {
        return processorID;
    }

    public void setProcessorID(UUID processorID) {
        this.processorID = processorID;
    }

    public UUID getProcessID() {
        return processID;
    }

    public void setProcessID(UUID processmentID) {
        this.processID = processmentID;
    }
}
