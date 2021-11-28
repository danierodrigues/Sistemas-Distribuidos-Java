import java.io.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ScriptManager extends UnicastRemoteObject implements ScriptListInterface {
    public Queue<Script> queue = new LinkedList<>();

    protected ScriptManager() throws RemoteException {
    }

    public UUID submitScript(Script script) throws IOException {
        FtpClient ftpClient = new FtpClient(Processor.server, Processor.port, Processor.user, Processor.password);
        Process process;
        try {
            UUID uuid = UUID.randomUUID();
            System.out.println(uuid);
            script.setUuid(uuid);

            if (Runtime.getRuntime().availableProcessors() <= 0) {
                queue.add(script);
            } else {
                ftpClient.open();
                ftpClient.downloadFile(script.getFileLocation(), script.getFileLocation());
                process = Runtime.getRuntime().exec(script.getScript());

                StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
                Executors.newSingleThreadExecutor().submit(streamGobbler);
                int exitCode = process.waitFor();
                if(exitCode == 0){
                    System.out.println("Concluido com sucesso");
                }

                ftpClient.close();
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

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
    }
}
