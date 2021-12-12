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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import static java.lang.Double.NaN;

public class ScriptManager extends UnicastRemoteObject implements ScriptListInterface {
    private Queue<Script> queue = new LinkedList<>();
    private HashMap<UUID,Heartbeat> processorsAvailable = new HashMap<>();
    private Thread cpuCalculateThread = null;
    private Thread executeRequestThread;
    private Thread multicastReceiver;
    private double cpuUsagePercentage = 100;
    private double cpuMaxUsage = 95;
    private FtpClient ftpClient;
    private Process process;
    private MulticastPublisher multicastPublisher;
    private String heartbeatType = "setup";
    private final UUID processorID = Processor.processorID;
    protected MulticastSocket socket = null;
    protected byte[] buf = new byte[256];

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
                    Heartbeat messageClass = (Heartbeat) is.readObject();

                    manageHeartbeatsProcessors(messageClass);

                    if ("end".equals(buftest.toString())) {
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

    private void calculateCPU() throws Exception {
        this.cpuUsagePercentage = getProcessCpuLoad();

        Heartbeat heartbeat = new Heartbeat(processorID,this.cpuUsagePercentage,queue.size(), this.heartbeatType);
        this.heartbeatType = "heartbeat";

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(outputStream);
        os.writeObject(heartbeat);
        byte[] data = outputStream.toByteArray();

        multicastPublisher.multicast(data);
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

        Model model = new Model( script.getUuid(), processorID, output.toString());
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

    protected void manageHeartbeatsProcessors(Heartbeat heartbeat){
        processorsAvailable.put(heartbeat.getId(), heartbeat);
    }
}
