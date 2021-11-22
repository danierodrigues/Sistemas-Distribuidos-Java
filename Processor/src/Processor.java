import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class Processor {

    public static void main(String[] args) {
        DatagramSocket aSocket = null;
        Queue<String> queue = new LinkedList<>();

        try{
            aSocket = new DatagramSocket(6789);
            byte[] buffer = new byte[1000];

            while(true){
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(request);

                UUID uuid = UUID.randomUUID();
                System.out.println(uuid);
                byte[] uuidBytes = asBytes(uuid);
                String responseData = new String(request.getData(), 0, request.getLength());

                DatagramPacket reply = new DatagramPacket(uuidBytes,
                        uuidBytes.length, request.getAddress(), request.getPort());

                System.out.println(reply);

                aSocket.send(reply);

                int cores = Runtime.getRuntime().availableProcessors();
                System.out.println(cores);


                if(Runtime.getRuntime().availableProcessors() <= 0){
                    queue.add(responseData);
                }else{
                    Runtime.getRuntime().exec(responseData);
                }

                if(queue.size() > 0){
                    while(Runtime.getRuntime().availableProcessors() > 0){
                        Runtime.getRuntime().exec(queue.peek());
                    }
                }

            }
        }catch (SocketException e){System.out.println("Socket: " + e.getMessage());
        }catch (IOException e) {System.out.println("IO: " + e.getMessage());
        }finally {if(aSocket != null) aSocket.close();}
    }

    public static byte[] asBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

}
