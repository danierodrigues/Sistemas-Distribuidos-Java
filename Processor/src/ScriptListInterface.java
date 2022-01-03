import java.io.IOException;
import java.rmi.Remote;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import models.Model;
import models.ProcessorHeartbeat;
import models.Script;

public interface ScriptListInterface extends Remote {
    UUID submitScript(Script script) throws IOException;

    void resumeScripts(String processorAddress) throws IOException;

    ConcurrentHashMap<String, ProcessorHeartbeat> getProcessorsAvailableList() throws IOException;

    Model getModelToSave(UUID modelID) throws IOException;
}
