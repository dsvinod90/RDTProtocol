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
    private int receiverPort; // port of the receiver
    private static int nextSeq = 0;
    private static boolean okay = true;

    /**
     * Constructor for the Sender
     * @param rover Rover
     */
    public Sender(Rover rover, byte destinationRoverId, int receiverPort) {
        this.rover = rover;
        this.receiverPort = receiverPort;
        try {
            this.socket = new DatagramSocket();
            System.out.println("Sending from: " + this.socket.getLocalPort());
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
                for(int count = 0; count < Receiver.BUFFER_SIZE; count++) {
                    byte[] temp = this.sliceInEqualParts(buf, offset);
                    RdtProtocol protocol = new RdtProtocol(temp, rover.getRoverId(), destinationRoverId);
                    protocol.setSeq(++sequence);
                    nextSeq = sequence+1;
                    protocol.prepareSegment();
                    this.segments.add(protocol);
                    packet = new DatagramPacket(
                        protocol.getByteStream(),
                        protocol.getByteStream().length,
                        this.address,
                        this.rover.getPort()
                    );
                    System.out.println("Sending SEQ: " + protocol.getSeq());
                    this.socket.send(packet);
                    offset += temp.length;
                }
                this.waitForAcknowledgement();
                sleep(20);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void waitForAcknowledgement() {
        synchronized(this.rover) {
            try {
                this.rover.wait();
                System.out.println("Resume sending");    
            } catch (InterruptedException e) {
                if (!okay) {
                    System.out.println("Handle this scenario");
                }
            }
        }
    }

    public void sendAcknowledgement(int ack, byte destinationRoverId) {
        RdtProtocol protocol = new RdtProtocol(null, this.rover.getRoverId(), destinationRoverId);
        protocol.setSeq(1);
        protocol.setAck(true);
        protocol.setAcknowledgementNumber(ack);;
        protocol.prepareSegment();
        this.segments.add(protocol);
        System.out.println("Sending ACK to: " + destinationRoverId);
        packet = new DatagramPacket(protocol.getByteStream(), protocol.getByteStream().length, this.address, this.receiverPort);
        try {
            sleep(300);
            this.socket.send(packet);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean verifyAcknowledgement(int ack) {
        return ack == nextSeq;
    }

    private byte[] sliceInEqualParts(byte[] byteArray, int start) {
        byte[] temp = new byte[RdtProtocol.DATAGRAM_LENGTH];
        for (int index = 0; index < temp.length; index++) {
            try{
                temp[index] = byteArray[index + start];
            } catch (ArrayIndexOutOfBoundsException ex) {
                temp[index] = (byte) 101;
                temp[index+1] = (byte) 111;
                temp[index+2] = (byte) 102;
                break;
            }
        }
        return temp;
    }
}
