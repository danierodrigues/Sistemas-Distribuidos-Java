import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

public interface AddressRegistryInterface extends Remote {
    void addAddress(UUID addressID, String serverAddress) throws RemoteException;

    String resolve(UUID addressID) throws RemoteException;
}
