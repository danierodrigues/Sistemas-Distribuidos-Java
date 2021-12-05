import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.Double.NaN;

public class ScriptManager extends UnicastRemoteObject implements ScriptListInterface {
    private final UUID processorID = UUID.randomUUID();
    private Queue<Script> queue = new LinkedList<>();
    private Thread cpuCalculateThread = null;
    private Thread executeRequestThread;
    private double cpuUsagePercentage = 100;
    private double cpuMaxUsage = 95;
    private FtpClient ftpClient;
    private Process process;

    protected ScriptManager() throws RemoteException {
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

    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        //@Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
    }

    private void calculateCPU() throws Exception {
        this.cpuUsagePercentage = getProcessCpuLoad();
    }

    private void executeRequest(Script script) throws IOException, InterruptedException, NotBoundException {
        ModelManagerInterface brain = null;

        this.ftpClient.open();
        this.ftpClient.downloadFile(script.getFileLocation(), script.getFileLocation());
        process = Runtime.getRuntime().exec(script.getScript());

        String result = new BufferedReader(new InputStreamReader(process.getInputStream()))
                .lines().collect(Collectors.joining("\n"));
        StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
        Executors.newSingleThreadExecutor().submit(streamGobbler);
        int exitCode = process.waitFor();
        if(exitCode == 0){
            System.out.println("Concluido com sucesso");
        }

        ftpClient.close();

        Model model = new Model( script.getUuid(), processorID, result);
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
}
