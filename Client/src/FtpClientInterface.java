import java.io.File;
import java.io.IOException;
import java.rmi.Remote;

public interface FtpClientInterface  extends Remote {
    void open() throws IOException;

    void close() throws IOException;

    void downloadFile(String source, String destination) throws IOException;

    void putFileToPath(File file, String path) throws IOException;
}
