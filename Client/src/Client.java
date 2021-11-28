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
        try{
            ftpClient = (FtpClientInterface) Naming.lookup("rmi://localhost:2022/ftpClient");
            l  = (ScriptListInterface) Naming.lookup("rmi://localhost:2022/scripts");

            ftpClient.open();
            File file = new File("C:\\Users\\danie\\Desktop\\Cadeiras de licenciatura\\Sistemas Distribuidos\\repositorio projeto final\\SistemasDistribuidos\\Client\\hello_world.bat");
            ftpClient.putFileToPath(file,"./hello_world.bat");
            ftpClient.close();

            Script script = new Script("cmd /c hello_world.bat", "./hello_world.bat");
            UUID uuidOfScript = l.submitScript(script);


            System.out.println(uuidOfScript);
        } catch(RemoteException e) {
            System.out.println(e.getMessage());
        }catch(Exception e) {e.printStackTrace();}

    }
}
