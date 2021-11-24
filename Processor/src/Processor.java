import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class Processor {

    public static void main(String[] args) {
        Registry r = null;

        try{
            r = LocateRegistry.createRegistry(2022);
        }catch(RemoteException a){
            a.printStackTrace();
        }

        try{
            ScriptManager scriptList = new ScriptManager();
            r.rebind("scripts", scriptList );

            System.out.println("Place server ready");
        }catch(Exception e) {
            System.out.println("Place server main " + e.getMessage());
        }


    }
}
