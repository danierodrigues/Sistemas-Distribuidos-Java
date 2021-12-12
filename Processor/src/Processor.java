import java.io.BufferedReader;
import java.io.FileReader;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Processor {
    public static String server = "127.0.0.1";
    public static int port = 21;
    public static String user;
    public static String password;
    public static int processorPort;
    public static UUID processorID;

    public static void main(String[] args) {
        processorID = UUID.randomUUID();
        Registry r = null;
        List<String> credentialsList;
        processorPort = Integer.parseInt(args[0]);
        AddressRegistryInterface registry = null;
        try{
            r = LocateRegistry.createRegistry(processorPort);
        }catch(RemoteException a){
            a.printStackTrace();
        }

        try{
            // Read credentials to use the FTP Server
            try (BufferedReader br = new BufferedReader(new FileReader("../../credentials.txt"))) {
                credentialsList = br.lines().collect(Collectors.toList());
            }

            user = credentialsList.get(0);
            password = credentialsList.get(1);
            FtpClient ftpClient = new FtpClient( server, port, user,password);
            r.rebind("ftpClient", ftpClient);

            ScriptManager scriptList = new ScriptManager();
            r.rebind("scripts", scriptList );

            System.out.println("Processor server ready with the port: " + processorPort + ", with ProcessorID: " + processorID);

            registry  = (AddressRegistryInterface) Naming.lookup("rmi://localhost:2029/registry");
            registry.addAddress(processorID, "rmi://localhost:" + processorPort + "/scripts");
        }catch(Exception e) {
            System.out.println("Processor server main " + e.getMessage());
        }


    }
}
