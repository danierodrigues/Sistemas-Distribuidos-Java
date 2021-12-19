import models.Model;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

public interface ModelManagerInterface extends Remote {
    void addModel(Model model) throws RemoteException;

    Model getModel(UUID processID) throws RemoteException;
}
