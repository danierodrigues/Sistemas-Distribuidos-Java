import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class ScriptManager extends UnicastRemoteObject implements ScriptListInterface {
    public Queue<Script> queue = new LinkedList<>();

    protected ScriptManager() throws RemoteException {
    }

    public UUID submitScript(Script script) throws IOException, RemoteException {

        try {
            UUID uuid = UUID.randomUUID();
            System.out.println(uuid);
            script.setUuid(uuid);

            if (Runtime.getRuntime().availableProcessors() <= 0) {
                queue.add(script);
            } else {
                Runtime.getRuntime().exec(script.getScript());
            }
            return script.getUuid();
        }catch(Exception e) {e.printStackTrace();}

        return null;

    };
}
