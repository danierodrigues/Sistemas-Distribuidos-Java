package models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

public class BrainHeatbeat extends Heartbeat implements Serializable {
    private ArrayList<UUID> scriptsID;
    private Model retrievedModel;
    private UUID modelId;

    public BrainHeatbeat(String address, String type, ArrayList<UUID> scriptsID) {
        super(address, type);
        this.scriptsID = scriptsID;
    }

    public BrainHeatbeat(String address, String type, ArrayList<UUID> scriptsID, Model retrievedModel) {
        super(address, type);
        this.scriptsID = scriptsID;
        this.retrievedModel = retrievedModel;
    }

    public BrainHeatbeat(String address, String type, UUID modelId){
        super(address, type);
        this.modelId = modelId;
    }

    public BrainHeatbeat(String address, String type, ArrayList<UUID> scriptsID, boolean isLast) {
        super(address, type);
        this.scriptsID = scriptsID;
    }

    public ArrayList<UUID> getScriptsID() {
        return scriptsID;
    }

    public void setScriptsID(ArrayList<UUID> scriptsID) {
        this.scriptsID = scriptsID;
    }

    public Model getRetrievedModel() {
        return retrievedModel;
    }

    public void setRetrievedModel(Model retrievedModel) {
        this.retrievedModel = retrievedModel;
    }

    public UUID getModelId() {return modelId;}

    public void setModelId(UUID modelId) {this.modelId = modelId;}
}
