import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.UUID;

public class AddressRegistry extends UnicastRemoteObject implements AddressRegistryInterface {
    private HashMap<UUID, String> addressMap = new HashMap<>();
    protected AddressRegistry() throws RemoteException {
    }


    public void addAddress(UUID addressID, String serverAddress) throws RemoteException {
        addressMap.put(addressID,serverAddress);
    }

    public String resolve(UUID addressID) throws RemoteException {
        return addressMap.get(addressID);
    }
}
