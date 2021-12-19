import models.BrainHeatbeat;
import models.Model;
import models.ProcessorHeartbeat;
import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class ModelManager extends UnicastRemoteObject implements ModelManagerInterface  {
    ArrayList<Model> modelList = new ArrayList<>();
    private HashMap<String, BrainHeatbeat> brainsAvailable = new HashMap<>();
    private HashMap<String, int[]> brainsChecking = new HashMap<>();
    private Thread multicastReceiver;
    private Thread brainPublishThread;
    protected MulticastSocket socket = null;
    protected byte[] buf = new byte[1024];
    private MulticastPublisher multicastPublisher;
    private String heartbeatType = "setup";
    private int brainPort = Brain.brainPort;
    long timeDiffer = 30;
    private Model modelFound = null;

    protected ModelManager() throws RemoteException {

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
                    Object messageClass = is.readObject();

                    if(messageClass instanceof ProcessorHeartbeat){
                    }
                    if(messageClass instanceof BrainHeatbeat){
                        manageHeartbeatsProcessors((BrainHeatbeat) messageClass);
                    }

                    removeOutdatedBrains();

                }
            } catch (IOException | ClassNotFoundException | NotBoundException e) {
                e.printStackTrace();
            }

        }));
        multicastReceiver.start();

        multicastPublisher = new MulticastPublisher();
        brainPublishThread = (new Thread(() -> {
            while(brainPublishThread.isAlive()){
                try {
                    publishBrain();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }));

        brainPublishThread.start();

    }

    public void addModel(Model model) throws RemoteException{
        modelList.add(model);
    }

    public Model getModel(UUID processID) throws IOException {
        for (Model model : modelList) {
            if (model.getProcessID().equals(processID)) {
                return model;
            }
        }
        ArrayList<UUID> uuidList = new ArrayList<>();
        uuidList.add(processID);
        BrainHeatbeat heatbeat = new BrainHeatbeat( "rmi://localhost:" + this.brainPort + "/ModelManager", "get_model", uuidList, null);
        publishMessage(heatbeat);
        LocalDateTime time = LocalDateTime.now();
        while(this.modelFound == null || time.plusSeconds(this.timeDiffer).isBefore(LocalDateTime.now())){

        }

        if(this.modelFound != null){
            Model model = this.modelFound;
            this.modelFound = null;
            return model;
        }

        return null;
    }

    protected void manageHeartbeatsProcessors(BrainHeatbeat heartbeat) throws IOException {
        if (heartbeat.getType().equals("setup") || heartbeat.getType().equals("heartbeat")) {
            brainsAvailable.put(heartbeat.getAddress(), heartbeat);
        } else if(heartbeat.getType().equals("verify_tasks")){
            boolean isLast = false;
            ArrayList<UUID> scriptsDone = new ArrayList<>();
            heartbeat.getScriptsID().forEach(uuid ->{
                try {
                    if(getModel(uuid) != null) scriptsDone.add(uuid);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });


            if(brainsChecking.get(heartbeat.getAddress()) != null){
                int [] array = brainsChecking.get(heartbeat.getAddress());
                array[1] += 1;
                brainsChecking.put(heartbeat.getAddress(), array);
                if(array[0] == array[1]) isLast = true;
            }else{
                int[] array = { brainsAvailable.size(), 0 };
                brainsChecking.put(heartbeat.getAddress(), array);
            }

            BrainHeatbeat heartbeatVerified = new BrainHeatbeat(heartbeat.getAddress(), "verified_tasks", scriptsDone, isLast);
            publishMessage(heartbeatVerified);

        } else if(heartbeat.getType().equals("verified_tasks")){
            if(brainsAvailable.get(heartbeat.getAddress()) != null){
                int [] array = brainsChecking.get(heartbeat.getAddress());
                array[1] += 1;
                brainsChecking.put(heartbeat.getAddress(), array);
                if(array[0] == array[1]){
                    brainsChecking.remove(heartbeat.getAddress());
                }
            }else{
                int[] array = { brainsAvailable.size(), 0 };
                brainsChecking.put(heartbeat.getAddress(), array);
            }
        } else if(heartbeat.getType().equals("get_model") && !heartbeat.getAddress().equals("rmi://localhost:" + this.brainPort + "/ModelManager")){
            Model modelToPublish = null;
            for (Model model : modelList) {
                if (model.getProcessID().equals(heartbeat.getScriptsID().get(0))) {
                    modelFound = model;
                }
            }
            BrainHeatbeat heatbeat = new BrainHeatbeat( heartbeat.getAddress(), "retrieve_model", null, modelToPublish);
            publishMessage(heatbeat);
        } else if (heartbeat.getType().equals("retrieve_model") && heartbeat.getAddress().equals("rmi://localhost:" + this.brainPort + "/ModelManager")){
            this.modelFound = heartbeat.getRetrievedModel();
        }
    }

    private void publishBrain() throws Exception {
        BrainHeatbeat heartbeat = new BrainHeatbeat("rmi://localhost:" + this.brainPort + "/ModelManager", this.heartbeatType, null);
        this.heartbeatType = "heartbeat";

        publishMessage(heartbeat);
    }

    private void publishMessage(BrainHeatbeat heartbeat) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(outputStream);
        os.writeObject(heartbeat);
        byte[] data = outputStream.toByteArray();

        multicastPublisher.multicast(data);
    }

    private void removeOutdatedBrains() throws IOException, NotBoundException {
        for (String address : brainsAvailable.keySet()) {
            if (brainsAvailable.get(address).getTime().plusSeconds(this.timeDiffer).isBefore(LocalDateTime.now())) { //If heartbeat time more timeDiffer.value is before than now
                brainsAvailable.remove(address);
                break;
            }
        }
    }

}
