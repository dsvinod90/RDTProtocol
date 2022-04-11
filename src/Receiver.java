/**
 * Receiver module that receives all the RdtProtocol data
 * @author Vinod Dalavai - vd1605
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Receiver extends Thread {
    // Class fields
    private MulticastSocket socket; // Socket on which the RDT will be operating
    private InetAddress group; // group subscribed to the multicast IP
    private DatagramPacket packet; // Packet that will be received
    private Rover rover; // Rover that belongs to this receiver module
    private byte[] buf; // input buffer
    private List<Integer> receiverBuffer;

    /**
     * Constructor for this module
     * @param rover Rover
     */
    public Receiver(Rover rover) {
        this.rover = rover;
        receiverBuffer = new ArrayList<>();
        try {
            this.socket = new MulticastSocket(rover.getPort());
            this.buf = new byte[RdtProtocol.DATAGRAM_LENGTH];
            this.packet = new DatagramPacket(buf, buf.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to receive data on the socket
     */
    @Override
    public void run() {
        try {
            FileOutputStream fos = new FileOutputStream(new File("received.JPG"));
            this.group = InetAddress.getByName(rover.getMulticastIP());
            this.socket.joinGroup(this.group);
            while(true) {
                this.socket.receive(packet);
                byte[] incomingBytes = packet.getData();
                int seq = convertByteToInt(slice(incomingBytes, 0, 3));
                byte sendingRoverId = incomingBytes[4];
                byte receivingRoverId = incomingBytes[5];
                if (receivingRoverId == this.rover.getRoverId() || receivingRoverId == 0) {
                    if (incomingBytes[6] == (byte)(1)) { // if received acknowledgement
                        System.out.println("ACK Received: " + convertByteToInt(slice(incomingBytes, 8, 11)));
                    } else if (incomingBytes[6] == (byte)0) { // if not an ACK
                        System.out.println("Receiving SEQ=" + seq + ", SENDER=" + sendingRoverId + ", RECEIVER=" + receivingRoverId);
                        this.rover.getSenderModule().sendAcknowledgement(seq+1, sendingRoverId);
                        fos.write(RdtProtocol.extractData(incomingBytes));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] slice(byte[] arr, int start, int end) {
        byte[] temp = new byte[end - start + 1];
        for (int index = 0; index < temp.length; index ++) {
            temp[index] = arr[start];
            start++;
        }
        return temp;
    }
    private int convertByteToInt(byte[] byteArr) {
        return (byteArr[0] << 24 | byteArr[1] << 16 | byteArr[2] << 8 | byteArr[3]);
    }
}
