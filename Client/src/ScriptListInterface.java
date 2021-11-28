import java.io.IOException;
import java.rmi.Remote;
import java.util.UUID;

public interface ScriptListInterface extends Remote {
    UUID submitScript(Script P) throws IOException;
}
