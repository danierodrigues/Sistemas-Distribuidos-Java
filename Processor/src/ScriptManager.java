import models.BrainHeatbeat;
import models.Model;
import models.ProcessorHeartbeat;
import models.RestoreTasks;
import models.Script;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.*;

import static java.lang.Double.NaN;

public class ScriptManager extends UnicastRemoteObject implements ScriptListInterface {
    private Queue<Script> queue = new LinkedList<>();
    private HashMap<String, ProcessorHeartbeat> processorsAvailable = new HashMap<>();
    private HashMap<String, RestoreTasks> processorsToRestore = new HashMap<>();
    private Thread cpuCalculateThread = null;
    private Thread executeRequestThread;
    private Thread multicastReceiver;
    private double cpuUsagePercentage = 100;
    private double cpuMaxUsage = 95;
    private FtpClient ftpClient;
    private Process process;
    private MulticastPublisher multicastPublisher;
    private String heartbeatType = "setup";
    private final int processorPort = Processor.processorPort;
    protected MulticastSocket socket = null;
    protected byte[] buf = new byte[1024];
    private long timeDifferRequests = 60;

    protected ScriptManager() throws RemoteException {
        multicastReceiver = (new Thread(() -> {
            try {
                socket = new MulticastSocket(4446);
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

                    executeOutdatedProcessors();

                }
            } catch (IOException | ClassNotFoundException | NotBoundException e) {
                e.printStackTrace();
            }

        }));
        multicastReceiver.start();

        multicastPublisher = new MulticastPublisher();
        ftpClient = new FtpClient(Processor.server, Processor.port, Processor.user, Processor.password);
        cpuCalculateThread = (new Thread(() -> {
            while(cpuCalculateThread.isAlive()){
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
            }

        }));

        cpuCalculateThread.start();

        executeRequestThread = (new Thread(() -> {
            while(this.cpuUsagePercentage <= cpuMaxUsage && queue.size() > 0 ){
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

            if (this.cpuUsagePercentage > cpuMaxUsage || this.cpuUsagePercentage == NaN) {
                queue.add(script);
            } else {
                executeRequest(script);
            }
            return script.getUuid();
        }catch(Exception e) {e.printStackTrace();}

        return null;

    };

    public void resumeScripts(String processorAddress) throws IOException {
        ArrayList<Script> tasksToProcess = processorsAvailable.get(processorAddress).getTasks();
        ArrayList<UUID> scriptsID = new ArrayList<UUID>();
        tasksToProcess.forEach(script -> {
            scriptsID.add(script.getUuid());
        });
        BrainHeatbeat heartbeat = new BrainHeatbeat(processorAddress, "verify_tasks", scriptsID);

        RestoreTasks tasksList = new RestoreTasks(tasksToProcess, scriptsID);
        processorsToRestore.put(processorAddress, tasksList);

        multicastPublisher.publishBrainMessage(heartbeat);

    }

    private void calculateCPU() throws Exception {
        this.cpuUsagePercentage = getProcessCpuLoad();

        ProcessorHeartbeat heartbeat = new ProcessorHeartbeat("rmi://localhost:" + this.processorPort + "/scripts", this.heartbeatType, this.cpuUsagePercentage,new ArrayList(queue));
        this.heartbeatType = "heartbeat";

        multicastPublisher.publishProcessorMessage(heartbeat);
    }

    private void executeRequest(Script script) throws IOException, InterruptedException, NotBoundException {
        ModelManagerInterface brain = null;

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

        Model model = new Model( script.getUuid(), "rmi://localhost:" + this.processorPort + "/scripts", output.toString());
        brain = (ModelManagerInterface) Naming.lookup("rmi://localhost:2023/ModelManager");
        brain.addModel(model);
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

    private void manageHeartbeatsProcessors(ProcessorHeartbeat heartbeat){
        if (heartbeat.getType().equals("setup") || heartbeat.getType().equals("heartbeat")) {
            processorsAvailable.put(heartbeat.getAddress(), heartbeat);
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
            if(heartbeat.isLast()){
                addToQueueRequests( scriptsID, tasks);
                processorsToRestore.remove(heartbeat.getAddress());
            }else{
                processorsToRestore.get(heartbeat.getAddress()).setScriptID(scriptsID);
            }
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
