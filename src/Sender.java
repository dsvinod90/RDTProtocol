/**
 * Sender module that sends all the RDT Protocol data
 * @author Vinod Dalavai - vd1605
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Sender extends Thread {
    // Class fields
    private DatagramSocket socket; // Socket on which the data will be sent
    private InetAddress address; // IP address of the rover that sends the data
    private DatagramPacket packet;  // Packet of data that will be sent over the socket
    private File file; // File to be sent
    private Rover rover; // Rover related to the sender
    private List<RdtProtocol> segments; // list of segments to be sent over the socket
    private byte destinationRoverId = 0;

    /**
     * Constructor for the Sender
     * @param rover Rover
     */
    public Sender(Rover rover, byte destinationRoverId) {
        this.rover = rover;
        try {
            this.socket = new DatagramSocket();
            this.segments = new ArrayList<>();
            this.address = InetAddress.getByName(rover.getMulticastIP());
            this.destinationRoverId = destinationRoverId;
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * Setter for file
     * @param file  File
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Method to send data on the socket
     */
    public void run() {
        try {
            FileInputStream fis = new FileInputStream(this.file);
            int sequence = 0;
            int offset = 0;
            byte[] buf = fis.readAllBytes();
            while(offset < buf.length) {
                byte[] temp = this.sliceInEqualParts(buf, offset);
                RdtProtocol protocol = new RdtProtocol(temp, rover.getRoverId(), destinationRoverId);
                protocol.setSeq(++sequence);
                protocol.prepareSegment();
                this.segments.add(protocol);
                packet = new DatagramPacket(protocol.getByteStream(), protocol.getByteStream().length, this.address, this.rover.getPort());
                this.socket.send(packet);
                offset += temp.length;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendAcknowledgement(int ack, byte destinationRoverId) {
        RdtProtocol protocol = new RdtProtocol(null, this.rover.getRoverId(), destinationRoverId);
        protocol.setSeq(1);
        protocol.setAck(true);
        protocol.setAcknowledgementNumber(ack);;
        protocol.prepareSegment();
        this.segments.add(protocol);
        packet = new DatagramPacket(protocol.getByteStream(), protocol.getByteStream().length, this.address, this.rover.getPort());
        try {
            this.socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] sliceInEqualParts(byte[] byteArray, int start) {
        byte[] temp = new byte[RdtProtocol.DATAGRAM_LENGTH];
        try{
            for (int index = 0; index < temp.length; index++) {
                temp[index] = byteArray[index + start];
            }
            return temp;
        } catch (ArrayIndexOutOfBoundsException e) {
            return temp;
        }
    }

    public static void main(String[] args) {
        Sender sender = new Sender(new Rover((byte)(1), (byte)(2), 3000, "240.0.0.1"), (byte)2);
        sender.setFile(new File("src/superman.txt"));
        sender.run();
    }
}
