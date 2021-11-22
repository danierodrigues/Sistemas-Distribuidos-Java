import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.UUID;

public class Client {

    public static void main(String[] args) {
        DatagramSocket aSocket = null;

        try {
            aSocket = new DatagramSocket();

            byte [] m = "cmd /c dir".getBytes();
            InetAddress aHost = InetAddress.getByName("localhost");
            int serverPort = 6789;

            DatagramPacket request = new DatagramPacket(m,  m.length, aHost, serverPort);

            aSocket.send(request);

            byte[] buffer = new byte[1000];

            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);

            aSocket.receive(reply);

            UUID uuid = asUuid(reply.getData());
            System.out.println(uuid);


        }catch (SocketException e){System.out.println("Socket: " + e.getMessage());
        }catch (IOException e){System.out.println("IO: " + e.getMessage());
        }finally {if(aSocket != null) aSocket.close();}
    }

    public static UUID asUuid(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong);
    }
}
