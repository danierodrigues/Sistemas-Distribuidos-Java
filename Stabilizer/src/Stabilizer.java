import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Stabilizer {
    public static void main(String[] args) {
        Registry r = null;
        try{
            r = LocateRegistry.createRegistry(2024);
        }catch(RemoteException a){
            a.printStackTrace();
        }

        try{
            StabilizerManager stabilizerManager = new StabilizerManager();
            r.rebind("stabilizerManager", stabilizerManager);

            System.out.println("Stabilizer server ready in port: " + 2024);
        }catch(Exception e) {
            System.out.println("Brain server main " + e.getMessage());
        }
    }
}
