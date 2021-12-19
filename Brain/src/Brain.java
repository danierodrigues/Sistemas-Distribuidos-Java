import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Brain {
    public static int brainPort;

    public static void main(String[] args) {
        Registry r = null;
        brainPort = Integer.parseInt(args[0]);

        try{
            r = LocateRegistry.createRegistry(brainPort);
        }catch(RemoteException a){
            a.printStackTrace();
        }

        try{
            ModelManager ModelManager = new ModelManager();
            r.rebind("ModelManager", ModelManager );

            System.out.println("Brain server ready in port: " + 2023);
        }catch(Exception e) {
            System.out.println("Brain server main " + e.getMessage());
        }
    }
}
