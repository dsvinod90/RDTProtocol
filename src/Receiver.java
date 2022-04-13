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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Receiver extends Thread {
    // Class fields
    private MulticastSocket socket; // Socket on which the RDT will be operating
    private InetAddress group; // group subscribed to the multicast IP
    private DatagramPacket packet; // Packet that will be received
    private Rover rover; // Rover that belongs to this receiver module
    private byte[] buf; // input buffer
    private LinkedHashMap<Integer, byte[]> packetArray; // receiver buffer for tracking packets
    private List<Integer> missingSequences; // Set of missing sequence numbers

    public Receiver() {}
    /**
     * Constructor for this module
     * @param rover Rover
     */
    public Receiver(Rover rover) {
        this.rover = rover;
        this.packetArray = new LinkedHashMap<>();
        this.missingSequences = new ArrayList<>();
        try {
            System.out.println("Listening on port: " + rover.getPort());
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
                        byte[] relevantBytes = RdtProtocol.extractData(incomingBytes);
                        packetArray.put(seq, relevantBytes);
                        this.rover.getSenderModule().sendAcknowledgement(seq+1, sendingRoverId);
                        if (isLastPacket(relevantBytes)) {
                            if (!isPacketMissing()) {
                                // requesetMissingPackets();
                                if (isPacketInSequence()) {
                                    System.out.println("Writing to file");
                                    for (Map.Entry<Integer, byte[]> entry : packetArray.entrySet()) {
                                        fos.write(entry.getValue());
                                    }
                                } else {
                                    TreeMap<Integer, byte[]> sorted = arrangePacketsInSequence();
                                    System.out.println("Sorting packets and writing to file");
                                    for (Map.Entry<Integer, byte[]> entry : sorted.entrySet()) {
                                        fos.write(entry.getValue());
                                    }
                                }
                            }
                        }
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

    private boolean isLastPacket(byte[] packet) {
        int index = packet.length - 1;
        while (index >= 2) {
            if (packet[index] == 102 && packet[index - 1] == 111 && packet[index - 2] == 101) {
                return true;
            }
            index --;
        }
        return false;
    }

    private boolean isPacketMissing() {
        boolean isMissing = false;
        Set<Integer> sequences = packetArray.keySet();
        for (int index = 1; index <= sequences.size(); index++) {
            if (!sequences.contains(index)) {
                System.out.println("Packet missing: " + index);
                missingSequences.add(index);
                isMissing = true;
            }
        }
        return isMissing;
    }

    // private void requesetMissingPackets() {
    //     this.rover.getSenderModule().requestMissingPackets(seq+1, sendingRoverId);
    // }

    private boolean isPacketInSequence() {
        List<Integer> intList = new ArrayList<>();
        for (Map.Entry<Integer, byte[]> entry : packetArray.entrySet()) {
            intList.add(entry.getKey());
        }
        for (int index = 1; index <= packetArray.size(); index++) {
            if (index != intList.get(index - 1)) {
                System.out.println("Packet out of sequence: " + intList.get(index));
                return false;
            }
        }
        return true;
    }

    private TreeMap<Integer, byte[]> arrangePacketsInSequence() {
        TreeMap<Integer, byte[]> sortedPackets = new TreeMap<>();
        sortedPackets.putAll(packetArray);
        return sortedPackets;
    }
}
