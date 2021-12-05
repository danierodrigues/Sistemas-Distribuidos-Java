import com.jcraft.jsch.*;
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.UUID;

public class Client {

    public static void main(String[] args) {
        ScriptListInterface l = null;
        FtpClientInterface ftpClient = null;
        ModelManagerInterface brainServer = null;
        try{
            ftpClient = (FtpClientInterface) Naming.lookup("rmi://localhost:2022/ftpClient");
            l  = (ScriptListInterface) Naming.lookup("rmi://localhost:2022/scripts");
            brainServer = (ModelManagerInterface) Naming.lookup("rmi://localhost:2023/ModelManager");

            ftpClient.open();
            File file = new File("C:\\Users\\danie\\Desktop\\Cadeiras de licenciatura\\Sistemas Distribuidos\\repositorio projeto final\\SistemasDistribuidos\\Client\\hello_world.bat");
            ftpClient.putFileToPath(file,"./hello_world.bat");
            ftpClient.close();

            Script script = new Script("cmd /c hello_world.bat", "./hello_world.bat");
            UUID uuidOfScript = l.submitScript(script);
            Script script2 = new Script("cmd /c hello_world.bat", "./hello_world.bat");
            l.submitScript(script2);
            Script script3 = new Script("cmd /c hello_world.bat", "./hello_world.bat");
            l.submitScript(script3);
            Script script5 = new Script("cmd /c hello_world.bat", "./hello_world.bat");
            l.submitScript(script5);
            Script script6 = new Script("cmd /c hello_world.bat", "./hello_world.bat");
            l.submitScript(script6);

            System.out.println("Model: " + brainServer.getModel(uuidOfScript).getModel());

            System.out.println(uuidOfScript);
        } catch(RemoteException e) {
            System.out.println(e.getMessage());
        }catch(Exception e) {e.printStackTrace();}

    }
}
