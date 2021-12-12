import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RegistryMain {
    public static void main(String[] args) {
        Registry r = null;
        try{
            r = LocateRegistry.createRegistry(2029);
        }catch(RemoteException a){
            a.printStackTrace();
        }

        try{
            AddressRegistry registry = new AddressRegistry();
            r.rebind("registry", registry );

            System.out.println("Registry server ready in port: " + 2029);
        }catch(Exception e) {
            System.out.println("Registry server main " + e.getMessage());
        }
    }
}
