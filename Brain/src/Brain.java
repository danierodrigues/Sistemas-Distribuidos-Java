import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Brain {
    public static void main(String[] args) {
        Registry r = null;

        try{
            r = LocateRegistry.createRegistry(2023);
        }catch(RemoteException a){
            a.printStackTrace();
        }

        try{
            ModelManager ModelManager = new ModelManager();
            r.rebind("ModelManager", ModelManager );

            System.out.println("Brain server ready");
        }catch(Exception e) {
            System.out.println("Brain server main " + e.getMessage());
        }
    }
}
