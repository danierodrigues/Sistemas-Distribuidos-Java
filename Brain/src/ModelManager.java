import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.UUID;

public class ModelManager extends UnicastRemoteObject implements ModelManagerInterface  {
    ArrayList<Model> modelList = new ArrayList<>();

    protected ModelManager() throws RemoteException {
    }

    public void addModel(Model model) throws RemoteException{
        modelList.add(model);
    }

    public Model getModel(UUID processID) throws RemoteException{
        for (Model model : modelList) {
            if (model.getProcessID().equals(processID)) {
                return model;
            }
        }
        System.out.println("Modelo não encontrado.");
        return null;
    }
}
