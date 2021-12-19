package models;

import java.io.Serializable;
import java.util.UUID;

public class Model implements Serializable {
    private UUID processID;
    private String processorAddress;
    private String model;

    public Model(UUID processID, String processorAddress, String model){
        this.processID = processID;
        this.processorAddress = processorAddress;
        this.model = model;
    }


    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getProcessorID() {
        return processorAddress;
    }

    public void setProcessorID(String processorID) {
        this.processorAddress = processorID;
    }

    public UUID getProcessID() {
        return processID;
    }

    public void setProcessID(UUID processmentID) {
        this.processID = processmentID;
    }
}
