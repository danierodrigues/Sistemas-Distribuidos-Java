import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

public interface StabilizerManagerInterface extends Remote {
    UUID bestProcessor() throws RemoteException;
}
