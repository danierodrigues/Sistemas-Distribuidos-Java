import models.*;
import utils.HeartbeatsUtils;
import utils.MulticastPublisher;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Double.NaN;

public class ScriptManager extends UnicastRemoteObject implements ScriptListInterface {
    private ConcurrentLinkedQueue<Script> queue = new ConcurrentLinkedQueue<>();
    private ConcurrentHashMap<String, ProcessorHeartbeat> processorsAvailable = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, RestoreTasks> processorsToRestore = new ConcurrentHashMap<>();
    private Thread cpuCalculateThread = null;
    private Thread executeRequestThread;
    private Thread multicastReceiver;
    private AtomicLong cpuUsagePercentage = new AtomicLong(100);
    private final long cpuMaxUsage = 95;
    private final FtpClient ftpClient = new FtpClient(Processor.server, Processor.port, Processor.user, Processor.password);;
    private Process process;
    private MulticastPublisher multicastPublisher = new MulticastPublisher("230.0.0.0", 4446);;
    private String heartbeatType = "setup";
    private final int processorPort = Processor.processorPort;
    private final String processorAddress = "rmi://localhost:" + this.processorPort + "/scripts";
    private final long timeDifferRequests = 60;
    private ArrayList<UUID> doneTasks = new ArrayList<>();
    private ConcurrentHashMap<UUID, Script> newsTasks = new ConcurrentHashMap<>();
    private ConcurrentHashMap<UUID, Model> modelsToAdd = new ConcurrentHashMap<>();

    protected ScriptManager() throws RemoteException {
        multicastReceiver = (new Thread(() -> {
            try {
                byte[] buf = new byte[1024];
                MulticastSocket socket = new MulticastSocket(4446);
                InetAddress group = InetAddress.getByName("230.0.0.0");
                socket.joinGroup(group);
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);


                    byte[] buftest = packet.getData();
                    ByteArrayInputStream in = new ByteArrayInputStream(buftest);
                    ObjectInputStream is = new ObjectInputStream(in);
                    Object messageClass =  is.readObject();


                    if(messageClass instanceof ProcessorHeartbeat){
                        manageHeartbeatsProcessors((ProcessorHeartbeat) messageClass);
                    }
                    if(messageClass instanceof BrainHeatbeat){
                        manageHeartbeatsBrains((BrainHeatbeat) messageClass);
                    }
                    if(messageClass instanceof StabilizerHeartbeat){
                        manageHeartbeatsStabilizer((StabilizerHeartbeat) messageClass);
                    }

                    executeOutdatedProcessors();

                }
            } catch (IOException | ClassNotFoundException | NotBoundException e) {
                e.printStackTrace();
            }

        }));
        multicastReceiver.start();


        cpuCalculateThread = (new Thread(() -> {

            try {
                calculateCPU();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cpuCalculateThread.run();

        }));

        cpuCalculateThread.start();

        executeRequestThread = (new Thread(() -> {
            while( this.cpuUsagePercentage .longValue()<= cpuMaxUsage && queue.size() > 0 ){
                try {
                    executeRequest(queue.peek());
                    queue.remove();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException | NotBoundException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            executeRequestThread.run();
        }));

        executeRequestThread.start();
    }

    public UUID submitScript(Script script) throws IOException {


        try {
            UUID uuid = UUID.randomUUID();
            System.out.println(uuid);
            script.setUuid(uuid);

            if (this.cpuUsagePercentage.longValue() > cpuMaxUsage || this.cpuUsagePercentage.longValue() == NaN) {
                queue.add(script);
                newsTasks.put(script.getUuid(), script);
            } else {
                executeRequest(script);
            }
            return script.getUuid();
        }catch(Exception e) {e.printStackTrace();}

        return null;

    };

    public void resumeScripts(String processorAddress) throws IOException {
        ArrayList<Script> tasksToProcess = processorsAvailable.get(processorAddress).getNewsTasks();
        ArrayList<UUID> scriptsID = new ArrayList<UUID>();
        tasksToProcess.forEach(script -> {
            scriptsID.add(script.getUuid());
        });
        BrainHeatbeat heartbeat = new BrainHeatbeat(processorAddress, "verify_tasks", scriptsID);

        RestoreTasks tasksList = new RestoreTasks(tasksToProcess, scriptsID);
        processorsToRestore.put(processorAddress, tasksList);

        multicastPublisher.publishBrainMessage(heartbeat);

    }

    public ConcurrentHashMap<String, ProcessorHeartbeat> getProcessorsAvailableList() throws IOException{
        return processorsAvailable;
    }

    public Model getModelToSave(UUID modelID) throws IOException{
        return modelsToAdd.get(modelID);
    }

    private void calculateCPU() throws Exception {
        this.cpuUsagePercentage = new AtomicLong((long) getProcessCpuLoad());

        ProcessorHeartbeat heartbeat = new ProcessorHeartbeat(this.processorAddress, this.heartbeatType, this.cpuUsagePercentage.longValue(), doneTasks, new ArrayList<>(newsTasks.values()));
        this.heartbeatType = "heartbeat";

        multicastPublisher.publishProcessorMessage(heartbeat);
    }

    private void executeRequest(Script script) throws IOException, InterruptedException, NotBoundException {

        this.ftpClient.open();
        this.ftpClient.downloadFile(script.getFileLocation(), script.getFileLocation());
        process = Runtime.getRuntime().exec(script.getScript());

        StringBuilder output = new StringBuilder();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line + "\n");
        }


        int exitCode = process.waitFor();
        if(exitCode == 0){
            System.out.println("Script concluido com sucesso");
            System.out.println("Output: " + output);
        }else{
            System.out.println("Script não executado");
        }

        ftpClient.close();
        synchronized(this) {
            doneTasks.add(script.getUuid());
        }
        Model model = new Model( script.getUuid(), this.processorAddress, output.toString());
        modelsToAdd.put(model.getProcessID(), model);
        BrainHeatbeat heartbeat = new BrainHeatbeat(this.processorAddress, "can_add_model", model.getProcessID());
        multicastPublisher.publishBrainMessage(heartbeat);
    }

    private static double getProcessCpuLoad() throws Exception {

        MBeanServer mbs    = ManagementFactory.getPlatformMBeanServer();
        ObjectName name    = ObjectName.getInstance("java.lang:type=OperatingSystem");
        AttributeList list = mbs.getAttributes(name, new String[]{ "ProcessCpuLoad" });

        if (list.isEmpty())     return NaN;

        Attribute att = (Attribute)list.get(0);
        Double value  = (Double)att.getValue();

        if (value == -1.0)      return NaN;
        return ((int)(value * 1000) / 10.0);
    }

    private void manageHeartbeatsStabilizer(StabilizerHeartbeat heartbeat){
        if (heartbeat.getType().equals("confirmation")) {
            heartbeat.getConfirmationDoneList().forEach(uuid -> {
                synchronized(this) {
                    doneTasks.remove(uuid);
                }
            });

            heartbeat.getConfirmationNewsList().forEach(uuid -> {
                newsTasks.remove(uuid);
            });
        }
    }

    private void manageHeartbeatsBrains(BrainHeatbeat heartbeat){
        if(heartbeat.getType().equals("verified_tasks")){
            if(processorsToRestore.get(heartbeat.getAddress()) == null){
                return;
            }
            ArrayList<UUID> scriptsID = processorsToRestore.get(heartbeat.getAddress()).getScriptID();
            ArrayList<Script> tasks = processorsToRestore.get(heartbeat.getAddress()).getTasks();
            heartbeat.getScriptsID().forEach(uuid -> {
                if(scriptsID.contains(uuid)){
                    scriptsID.remove(uuid);
                }
            });
            processorsToRestore.get(heartbeat.getAddress()).setScriptID(scriptsID);
        }
    }

    private void manageHeartbeatsProcessors(ProcessorHeartbeat heartbeat){
        if (heartbeat.getType().equals("setup")) {
            processorsAvailable.put(heartbeat.getAddress(), heartbeat);
        } else if(heartbeat.getType().equals("heartbeat")){
            processorsAvailable.put(heartbeat.getAddress(), HeartbeatsUtils.setProcessorAvailable(heartbeat, processorsAvailable.get(heartbeat.getAddress())));
        }
    }

    protected void executeOutdatedProcessors() throws IOException, NotBoundException {
        for (String address : processorsToRestore.keySet()) {
            if (processorsToRestore.get(address).getDateTime().plusSeconds(this.timeDifferRequests).isBefore(LocalDateTime.now())) {
                addToQueueRequests(processorsToRestore.get(address).getScriptID(), processorsToRestore.get(address).getTasks());
                processorsToRestore.remove(address);
                break;
            }
        }
    }

    private void addToQueueRequests(ArrayList<UUID> scriptsID, ArrayList<Script> tasks){
        scriptsID.forEach(uuid -> {
            for (Script script: tasks){
                if(script.getUuid() == uuid){
                    queue.add(script);
                    continue;
                }
            }
        });
    }

}
