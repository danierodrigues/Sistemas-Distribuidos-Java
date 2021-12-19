import models.Script;

import java.io.File;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.UUID;

public class Client {

    public static void main(String[] args) {
        ScriptListInterface l = null;
        FtpClientInterface ftpClient = null;
        ModelManagerInterface brainServer = null;
        StabilizerManagerInterface stabilizer = null;
        try{
            Thread t = (new Thread() {
                public void run() {
                        Stabilizer.main(new String[0]);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Brain.main(new String[]{"2023"});
                    Brain.main(new String[]{"2028"});
                    Processor.main(new String[]{"2025"});
                    Processor.main(new String[]{"2026"});
                    Processor.main(new String[]{"2027"});
                }
            });
            t.start();
            Thread.sleep(7000);


            ftpClient = (FtpClientInterface) Naming.lookup("rmi://localhost:2025/ftpClient");
            brainServer = (ModelManagerInterface) Naming.lookup("rmi://localhost:2023/ModelManager");
            stabilizer = (StabilizerManagerInterface) Naming.lookup("rmi://localhost:2024/stabilizerManager");

            ftpClient.open();
            File file = new File("C:\\Users\\danie\\Desktop\\Cadeiras de licenciatura\\Sistemas Distribuidos\\repositorio projeto final\\SistemasDistribuidos\\Client\\hello_world.bat");
            ftpClient.putFileToPath(file,"./hello_world.bat");
            ftpClient.close();



            l  = (ScriptListInterface) Naming.lookup(stabilizer.bestProcessor());
            Script script = new Script("cmd /c hello_world.bat", "./model.bat");
            UUID uuidOfScript = l.submitScript(script);

            l  = (ScriptListInterface) Naming.lookup(stabilizer.bestProcessor());
            Script script2 = new Script("cmd /c hello_world.bat", "./model.bat");
            l.submitScript(script2);

            l  = (ScriptListInterface) Naming.lookup(stabilizer.bestProcessor());
            Script script3 = new Script("cmd /c hello_world.bat", "./model.bat");
            l.submitScript(script3);

            l  = (ScriptListInterface) Naming.lookup(stabilizer.bestProcessor());
            Script script5 = new Script("cmd /c hello_world.bat", "./model.bat");
            l.submitScript(script5);

            l  = (ScriptListInterface) Naming.lookup(stabilizer.bestProcessor());
            Script script6 = new Script("cmd /c hello_world.bat", "./model.bat");
            l.submitScript(script6);

            System.out.println("Model: " + brainServer.getModel(uuidOfScript).getModel());
        } catch(RemoteException e) {
            System.out.println(e.getMessage());
        }catch(Exception e) {e.printStackTrace();}

    }

}
