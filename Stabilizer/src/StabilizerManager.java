import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.UUID;

public class StabilizerManager extends UnicastRemoteObject implements StabilizerManagerInterface{
    private HashMap<UUID,Heartbeat> processorsAvailable = new HashMap<>();
    private Thread multicastReceiver;
    protected MulticastSocket socket = null;
    protected byte[] buf = new byte[256];

    protected StabilizerManager() throws RemoteException {

        multicastReceiver = (new Thread(() -> {
            try {
                socket = new MulticastSocket(4446);
                InetAddress group = InetAddress.getByName("230.0.0.0");
                socket.joinGroup(group);
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    byte[] bufPacket = packet.getData();
                    ByteArrayInputStream in = new ByteArrayInputStream(bufPacket);
                    ObjectInputStream is = new ObjectInputStream(in);
                    Heartbeat messageClass = (Heartbeat) is.readObject();

                    manageHeartbeatsProcessors(messageClass);

                    if ("end".equals(bufPacket.toString())) {
                        break;
                    }
                }
                socket.leaveGroup(group);
                socket.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

        }));
        multicastReceiver.start();

    }

    public UUID bestProcessor(){
        double bestPerformanceValue = 0;
        UUID keyProcessor = null;
        for (UUID key : processorsAvailable.keySet()) {
            double performance = (100 - processorsAvailable.get(key).getCpuUsage()) / (processorsAvailable.get(key).getRequestsInQueue() == 0 ? 1 : processorsAvailable.get(key).getRequestsInQueue());
            if(bestPerformanceValue < performance ){
                keyProcessor = key;
                bestPerformanceValue = performance;
            }
        }
        if(keyProcessor == null) return null;
        return keyProcessor;
    };

    protected void manageHeartbeatsProcessors(Heartbeat heartbeat){
        processorsAvailable.put(heartbeat.getId(), heartbeat);
    }

}
