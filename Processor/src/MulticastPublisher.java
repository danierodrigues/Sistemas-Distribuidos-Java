import models.BrainHeatbeat;
import models.ProcessorHeartbeat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class MulticastPublisher {
    public void multicast(byte[] buf) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        InetAddress group = InetAddress.getByName("230.0.0.0");

        DatagramPacket packet
                = new DatagramPacket(buf, buf.length, new InetSocketAddress(group, 4446));
        socket.send(packet);

        socket.close();
    }

    public void publishProcessorMessage(ProcessorHeartbeat heartbeat) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(outputStream);
        os.writeObject(heartbeat);
        byte[] data = outputStream.toByteArray();

        multicast(data);
    }

    public void publishBrainMessage(BrainHeatbeat heartbeat) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(outputStream);
        os.writeObject(heartbeat);
        byte[] data = outputStream.toByteArray();

        multicast(data);
    }
}