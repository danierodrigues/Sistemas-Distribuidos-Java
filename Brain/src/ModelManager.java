import models.BrainHeatbeat;
import models.Model;
import utils.MulticastPublisher;
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
import java.util.ArrayList;
import java.util.UUID;

public class ModelManager extends UnicastRemoteObject implements ModelManagerInterface  {
    ArrayList<Model> modelList = new ArrayList<>();
    private MulticastPublisher multicastPublisher = new MulticastPublisher("230.0.0.0", 4446);
    private final int brainPort = Brain.brainPort;
    private String thisAddress = "rmi://localhost:" + this.brainPort + "/ModelManager";
    private final long timeDiffer = 30;
    private Model modelFound = null;

    protected ModelManager() throws RemoteException {

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

                    if(messageClass instanceof BrainHeatbeat){
                        manageHeartbeatsProcessors((BrainHeatbeat) messageClass);
                    }

                }
            } catch (IOException | ClassNotFoundException | NotBoundException e) {
                e.printStackTrace();
            }

        }));
        multicastReceiver.start();


    }

    public synchronized void addModel(Model model) throws RemoteException{
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
        BrainHeatbeat heatbeat = new BrainHeatbeat( this.thisAddress, "get_model", uuidList, null);
        multicastPublisher.publishBrainMessage(heatbeat);
        LocalDateTime time = LocalDateTime.now();
        while(this.modelFound == null || time.plusSeconds(this.timeDiffer).isBefore(LocalDateTime.now())){

        }

        if(this.modelFound != null){
            Model model = this.modelFound;
            synchronized(this) {
                this.modelFound = null;
            }
            return model;
        }

        return null;
    }

    private synchronized void manageHeartbeatsProcessors(BrainHeatbeat heartbeat) throws IOException, NotBoundException {
        if(heartbeat.getType().equals("verify_tasks")){
            ArrayList<UUID> scriptsDone = new ArrayList<>();
            heartbeat.getScriptsID().forEach(uuid ->{
                try {
                    if(getModel(uuid) != null) scriptsDone.add(uuid);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            BrainHeatbeat heartbeatVerified = new BrainHeatbeat(heartbeat.getAddress(), "verified_tasks", scriptsDone);
            multicastPublisher.publishBrainMessage(heartbeatVerified);

        } else if(heartbeat.getType().equals("get_model") && !heartbeat.getAddress().equals(this.thisAddress)){
            Model modelToPublish = null;
            for (Model model : modelList) {
                if (model.getProcessID().equals(heartbeat.getScriptsID().get(0))) {
                    synchronized(this) {
                        modelFound = model;
                    }
                }
            }
            BrainHeatbeat heatbeat = new BrainHeatbeat( heartbeat.getAddress(), "retrieve_model", null, modelToPublish);
            multicastPublisher.publishBrainMessage(heatbeat);
        } else if (heartbeat.getType().equals("retrieve_model") && heartbeat.getAddress().equals(this.thisAddress)){
            synchronized(this) {
                this.modelFound = heartbeat.getRetrievedModel();
            }
        } else if(heartbeat.getType().equals("can_add_model")){
            try{
                ScriptListInterface processor = (ScriptListInterface) Naming.lookup(heartbeat.getAddress());
                addModel(processor.getModelToSave(heartbeat.getModelId()));
            }catch (Exception e){
                System.out.println("No model added");
            }
        }
    }

}
