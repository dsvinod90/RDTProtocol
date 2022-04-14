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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

public class Receiver extends Thread {
    private static final byte TRUE      = (byte) 1;
    private static final byte FALSE     = (byte) 0;
    public static final int BUFFER_SIZE = 20;
    // Class fields
    private MulticastSocket socket; // Socket on which the RDT will be operating
    private InetAddress group; // group subscribed to the multicast IP
    private DatagramPacket packet; // Packet that will be received
    private Rover rover; // Rover that belongs to this receiver module
    private byte[] buf; // input buffer
    private LinkedHashMap<Integer, byte[]> packetArray; // receiver buffer for tracking packets
    private List<Integer> missingSequences; // Set of missing sequence numbers
    private FileOutputStream fos = null; // Output stream that writes to file
    private String fileExtension = "JPG"; // Extension of the output file
    public static Receiver receiver = null; // static object of the same class
    private static int previousAck = 1;
    private static int currentAck = 1;

    /**
     * Empty constructor for this class
     */
    public Receiver() {}

    /**
     * Constructor for this class
     * @param rover Rover
     */
    public Receiver(Rover rover) {
        this.rover = rover;
        this.packetArray = new LinkedHashMap<>(BUFFER_SIZE);
        this.missingSequences = new ArrayList<>();
        this.buf = new byte[RdtProtocol.DATAGRAM_LENGTH];
        this.packet = new DatagramPacket(buf, buf.length);
        try {
            this.group = InetAddress.getByName(rover.getMulticastIP());
            this.fos = new FileOutputStream(new File(this.receivedFileName()));
            this.socket = new MulticastSocket(rover.getPort());
            this.socket.joinGroup(this.group);
            System.out.println("Listening on port: " + rover.getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to create an instance of the class and assign it to a static variable
     * @param rover Rover
     */
    public static void createInstance(Rover rover) {
        if (receiver == null) {
            receiver = new Receiver(rover);
        }
    }

    /**
     * Method to return the instance of this class
     * @return  Receiver
     */
    public static Receiver fetchInstance() {
        if (receiver != null) return receiver;
        return null;
    }

    /**
     * Method to receive data on the socket
     */
    @Override
    public void run() {
        try {
            while(true) {
                this.socket.receive(packet);
                byte[] incomingBytes = packet.getData();
                int seq = this.getSequenceNumber(incomingBytes);
                byte sendingRoverId = incomingBytes[RdtProtocol.SOURCE_ID_POSITION];
                byte receivingRoverId = incomingBytes[RdtProtocol.DESTINATION_ID_POSITION];
                // if receiveing rover id is this rover or is all the rovers
                if (receivingRoverId == this.rover.getRoverId() || receivingRoverId == 0) {
                    // if incoming packet is an acknowledgement
                    if (this.incomingPacketIsAcknowledgement(incomingBytes)) {
                        // this.rover.getSenderModule().wait();
                        previousAck = currentAck;
                        currentAck = this.getAcknowledgementNumber(incomingBytes);
                        System.out.println("ACK Received: " + currentAck);
                        if (this.rover.getSenderModule().verifyAcknowledgement(currentAck)) {
                            System.out.println("ALL OKAY!");
                            this.notifySenderToResumeSending();
                        }
                    } else { // if incoming packet is not an acknowledgement
                        this.processIncomingBytes(incomingBytes, seq, sendingRoverId);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void notifySenderToResumeSending() {
        synchronized(this.rover) {
            this.rover.notifyAll();
        }
    }
    /**
     * Method to set the file extension of the output file
     */
    public void setFileExtension(String extension) {
        this.fileExtension = extension;
    }

    /**
     * Method to set the received file name
     * @return
     */
    private String receivedFileName() {
        return("received" + "_" + this.generateRandomNumber() + "." + this.fileExtension);
    }

    /**
     * Method to fetch the sequence number from the incoming bytes
     * @param   incomingBytes   byte[]
     * @return  int
     */
    private int getSequenceNumber(byte[] incomingBytes) {
        return (
            convertByteToInt(
                slice(
                    incomingBytes,
                    RdtProtocol.SEQ_START_POSITION,
                    RdtProtocol.SEQ_END_POSITION
                    )
                )
            );
    }

    /**
     * Method to fetch the ackowledgement number from the incoming bytes
     * @param   incomingBytes   byte[]
     * @return  int
     */
    private int getAcknowledgementNumber(byte[] incomingBytes) {
        return (
            convertByteToInt(
                slice(
                    incomingBytes,
                    RdtProtocol.ACK_NUMBER_START_POSITION,
                    RdtProtocol.ACK_NUMBER_END_POSITION
                    )
                )
            );
    }

    /**
     * Method that generates a random number between 0 and 1000
     * @return  int
     */
    private int generateRandomNumber() {
        Random rand = new Random();
        return rand.nextInt(1000);
    }

    /**
     * Method to slice a portion of the array and return the sliced portion
     * @param arr       byte[]
     * @param start     int
     * @param end       int
     * @return          byte[]
     */
    private byte[] slice(byte[] arr, int start, int end) {
        byte[] temp = new byte[end - start + 1];
        for (int index = 0; index < temp.length; index ++) {
            temp[index] = arr[start];
            start++;
        }
        return temp;
    }

    /**
     * Method to convert bytes to integer
     * @param byteArr   byte[]
     * @return          int
     */
    private int convertByteToInt(byte[] byteArr) {
        return (byteArr[0] << 24 | byteArr[1] << 16 | byteArr[2] << 8 | byteArr[3]);
    }

    /**
     * Method to check if the packet received is the last packet to be received from the sender
     * @param packet    byte[]
     * @return          boolean
     */
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

    /**
     * Method to check if any packet is missing based on the sequence numbers received
     * @return boolean
     */
    private boolean isPacketMissing() {
        boolean isMissing = false;
        Set<Integer> sequences = packetArray.keySet();
        System.out.println("Set of all SEQ: " + Arrays.toString(sequences.toArray()));
        int minSequence = previousAck;
        System.out.println("Min Sequence: " + minSequence);
        int maxSequence = minSequence + (BUFFER_SIZE - 1);
        System.out.println("Max Sequence: " + maxSequence);
        for (int index = minSequence; index <= maxSequence; index++) {
            if (!sequences.contains(index)) {
                isMissing = true;
                missingSequences.add(index);
            }
        }
        System.out.println("No missing packets....");
        return isMissing;
    }

    /**
     * Method to check if the arriving packets at the buffer are in sequence
     * @return boolean
     */
    private boolean isPacketInSequence() {
        List<Integer> sequenceList = new ArrayList<>();
        for (Map.Entry<Integer, byte[]> entry : packetArray.entrySet()) {
            sequenceList.add(entry.getKey());
        }
        int minSequence = Collections.min(sequenceList);
        int maxSequence = Collections.max(sequenceList);
        for (int index = minSequence; index <= maxSequence; index++) {
            if (index != sequenceList.get(index - minSequence)) {
                System.out.println("Packet arrived out of sequence: " + sequenceList.get(index));
                return false;
            }
        }
        System.out.println("Packets arrived in sequence....");
        return true;
    }

    /**
     * Method to sort the packets based on the sequence numbers
     * @return TreeMap<Integer, byte[]>
     */
    private TreeMap<Integer, byte[]> arrangePacketsInSequence() {
        TreeMap<Integer, byte[]> sortedPackets = new TreeMap<>();
        System.out.println("Sorting packets in sequence....");
        sortedPackets.putAll(packetArray);
        return sortedPackets;
    }

    /**
     * Method to check if the incoming packet is an acknowledgement
     * @return  boolean
     */
    private boolean incomingPacketIsAcknowledgement(byte[] incomingBytes) {
        return (incomingBytes[RdtProtocol.ACK_FLAG_POSITION] == TRUE);
    }

    /**
     * Method to process the incoming bytes and deliver to the application layer at the receiving end
     * @param incomingBytes     byte[]
     * @param seq               int
     * @param sendingRoverId    byte
     * @throws IOException
     */
    private void processIncomingBytes(byte[] incomingBytes, int seq, byte sendingRoverId) throws IOException {
        byte[] relevantBytes = RdtProtocol.extractData(incomingBytes);
        if (packetArray.size() < BUFFER_SIZE - 1) {
            System.out.println("Receiving SEQ: " + seq);
            System.out.println("Buffer SIZE: " + (BUFFER_SIZE - packetArray.size()) + " | Packet Size: " + packetArray.size());
            packetArray.put(seq, relevantBytes);
        } else if (packetArray.size() == BUFFER_SIZE - 1) {
            System.out.println("Receiving SEQ: " + seq);
            System.out.println("Buffer SIZE: " + (BUFFER_SIZE - packetArray.size()) + " | Packet Size: " + packetArray.size());
            packetArray.put(seq, relevantBytes);
            this.rover.getSenderModule().sendAcknowledgement(seq+1, sendingRoverId);
            System.out.println("Next exptected SEQ: " + currentAck);
        } else {
            if (!isPacketMissing()) {
                if (isPacketInSequence()) {
                    this.writeToFile(packetArray);
                } else {
                    TreeMap<Integer, byte[]> sorted = arrangePacketsInSequence();
                    this.writeToFile(sorted);
                }
                this.runGarbageCollector();
            } else {
                System.out.println("Missing packets with SEQ: " + Arrays.toString(missingSequences.toArray()));
            }
        }
    }

    /**
     * Method to write bytes to output file
     * @throws IOException
     */
    private void writeToFile(Map<Integer, byte[]> sortedPackets) throws IOException {
        System.out.println("Writing to file....");
        for (Map.Entry<Integer, byte[]> entry : sortedPackets.entrySet()) {
            fos.write(entry.getValue());
        }
    }

    /**
     * Method to empty the receiving buffer
     */
    private void runGarbageCollector() {
        System.out.println("Cleaning buffer to accept new packets....");
        this.packetArray = new LinkedHashMap<>();
    }

    /**
     * Only for testing purposes
     */
    public static void main(String[] args) {
       Receiver r = new Receiver(new Rover((byte) 1, (byte)2, 3000, "224.0.0.1"));
       byte[] arr = {(byte)1, (byte)2};
       r.packetArray.put(1, arr);
       int size = r.packetArray.size();
       System.out.println(size);
    }
}
