import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.UUID;

public class Client {

    public static void main(String[] args) {
        ScriptListInterface l = null;
        try{
            l  = (ScriptListInterface) Naming.lookup("rmi://localhost:2022/scripts");
            Script script = new Script("cmd /c dir");
            UUID uuidOfScript = l.submitScript(script);

            System.out.println(uuidOfScript);
        } catch(RemoteException e) {
            System.out.println(e.getMessage());
        }catch(Exception e) {e.printStackTrace();}

    }

}
