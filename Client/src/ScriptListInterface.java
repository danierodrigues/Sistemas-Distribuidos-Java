import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

public interface ScriptListInterface extends Remote {
    UUID submitScript(Script P) throws IOException, RemoteException;
}
