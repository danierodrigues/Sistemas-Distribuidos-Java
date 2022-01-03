import models.ProcessorHeartbeat;
import utils.HeartbeatsUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

public class StabilizerManager extends UnicastRemoteObject implements StabilizerManagerInterface{
    private ConcurrentHashMap<String, ProcessorHeartbeat> processorsAvailable = new ConcurrentHashMap<>();
    private final long timeDiffer = 30;

    protected StabilizerManager() throws RemoteException {

        Thread multicastReceiver = (new Thread(() -> {
            try {
                byte[] buf = new byte[1024];
                MulticastSocket socket = new MulticastSocket(4446);
                InetAddress group = InetAddress.getByName("230.0.0.0");
                socket.joinGroup(group);
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    byte[] bufPacket = packet.getData();
                    ByteArrayInputStream in = new ByteArrayInputStream(bufPacket);
                    ObjectInputStream is = new ObjectInputStream(in);
                    Object messageClass = is.readObject();

                    if(messageClass instanceof ProcessorHeartbeat){
                        manageHeartbeatsProcessors((ProcessorHeartbeat) messageClass);
                    }

                    removeOutdatedProcessors();

                }
            } catch (IOException | ClassNotFoundException | NotBoundException e) {
                e.printStackTrace();
            }

        }));
        multicastReceiver.start();

    }

    public String bestProcessor(){
        double bestPerformanceValue = 100;
        String addressProcessor = null;
        for (String key : processorsAvailable.keySet()) {
            if(bestPerformanceValue > processorsAvailable.get(key).getCpuUsage() ){
                addressProcessor = key;
                bestPerformanceValue = processorsAvailable.get(key).getCpuUsage();
            }
        }
        if(addressProcessor == null) return null;
        return addressProcessor;
    };

    private void manageHeartbeatsProcessors(ProcessorHeartbeat heartbeat) throws IOException, NotBoundException {
        if (heartbeat.getType().equals("setup")) processorsAvailable.put(heartbeat.getAddress(), heartbeat);
        else if(heartbeat.getType().equals("heartbeat")){
            if(processorsAvailable.get(heartbeat.getAddress()) != null){
                processorsAvailable.put(heartbeat.getAddress(), heartbeat);
                processorsAvailable.put(heartbeat.getAddress(), HeartbeatsUtils.setProcessorAvailable(heartbeat, processorsAvailable.get(heartbeat.getAddress())));
            }
            else{
                ScriptListInterface bestProcessor  = (ScriptListInterface) Naming.lookup(bestProcessor());
                this.processorsAvailable = bestProcessor.getProcessorsAvailableList();
            }
        }

    }

    private void removeOutdatedProcessors() throws IOException, NotBoundException {
        for (String address : processorsAvailable.keySet()) {
            if (processorsAvailable.get(address).getTime().plusSeconds(this.timeDiffer).isBefore(LocalDateTime.now())) { //If heartbeat time more timeDiffer.value is before than now
                sendRequestsToBestProcessor(address);
                processorsAvailable.remove(address);
                break;
            }
        }
    }

    private void sendRequestsToBestProcessor(String processorAddress) throws IOException, NotBoundException {
        ScriptListInterface bestProcessor  = (ScriptListInterface) Naming.lookup(bestProcessor());
        bestProcessor.resumeScripts(processorAddress);
    }

}
