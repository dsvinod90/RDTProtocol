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
    private String fileExtension = "jpg"; // Extension of the output file
    public static Receiver receiver = null; // static object of the same class
    private static int previousStartSeqOfPacketArray = 1;
    private static int currentStartSeqOfPacketArray = 1;
    private static int currentAck = 1;
    private byte sendingRoverId;
    private byte receivingRoverId;

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
        this.buf = new byte[RdtProtocol.DATAGRAM_LENGTH + RdtProtocol.FIXED_HEADER_SIZE];
        this.packet = new DatagramPacket(buf, buf.length);
        try {
            this.group = InetAddress.getByName(rover.getMulticastIP());
            this.fos = new FileOutputStream(new File(this.receivedFileName()));
            this.socket = new MulticastSocket(rover.getPort());
            this.socket.joinGroup(this.group);
            if (rover.getAction().equalsIgnoreCase("receive")) {
                System.out.println("Setting SO_TIMEOUT");
                this.socket.setSoTimeout(5_000);
            }
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
        while(true) {
            try {
                this.socket.receive(packet);
                byte[] incomingBytes = packet.getData();
                int seq = this.getSequenceNumber(incomingBytes);
                sendingRoverId = incomingBytes[RdtProtocol.SOURCE_ID_POSITION];
                receivingRoverId = incomingBytes[RdtProtocol.DESTINATION_ID_POSITION];
                // if receiveing rover id is this rover or is all the rovers
                if (receivingRoverId == this.rover.getRoverId() || receivingRoverId == 0) {
                    // if incoming packet is an acknowledgement
                    if (!this.requestForFileTransfer(incomingBytes)) {
                        byte byteCommand = incomingBytes[RdtProtocol.COMMAND_FLAG_POSITION];
                        int command = (int) byteCommand;
                        System.out.println(">> Received Command: " + command);
                        if (this.incomingPacketIsAcknowledgement(incomingBytes)) {
                            System.out.println(">> Received ACK");
                            break;
                        }
                        if (command == CommandMap.ROVER_COMMANDS.get(CommandMap.Constants.FORWARD)) {
                            System.out.println("Rover has moved forward one unit of distance");
                        } else if (command == CommandMap.ROVER_COMMANDS.get(CommandMap.Constants.BACK)) {
                            System.out.println("Rover has moved back one unit of distance");
                        } else if (command == CommandMap.ROVER_COMMANDS.get(CommandMap.Constants.LEFT)) {
                            System.out.println("Rover has turned left");
                        } else if (command == CommandMap.ROVER_COMMANDS.get(CommandMap.Constants.RIGHT)) {
                            System.out.println("Rover has turned right");
                        }
                        System.out.println(">> Sending acknowledgement for movement action");
                        this.rover.getSenderModule().sendAcknowledgement(currentStartSeqOfPacketArray, sendingRoverId, byteCommand);
                    } else {
                        if (this.incomingPacketIsAcknowledgement(incomingBytes)) {
                            // this.rover.getSenderModule().wait();
                            currentAck = this.getAcknowledgementNumber(incomingBytes);
                            System.out.println(">> ACK Received: " + currentAck);
                            if (this.rover.getSenderModule().verifyAcknowledgement(currentAck)) {
                                System.out.println(">> ALL OKAY!");
                                this.notifySender();
                            }
                            // if incoming packet is a negative acknowledgement
                        } else if (this.incomingPacketIsNegativeAcknowledgement(incomingBytes)) {
                            System.out.println(">> NAK Received: ");
                            byte[] missingSequenceArray = RdtProtocol.extractData(incomingBytes);
                            this.notifySenderToResendPackets(missingSequenceArray);
                        } else if (this.incomingPacketIsFinishMessage(incomingBytes)) {
                            System.out.println(">> FIN Received: ");
                            System.out.println(">> File has been downloaded");
                            System.out.println("------------------------------------------------------------------------------");
                            break;
                        } else { // if incoming packet is a datagram with file contents
                            if (missingSequences.size() > 0 && missingSequences.contains(seq)) { // if there are missing packets check if all have arrived now
                                System.out.println(">> Received missing packets");
                                System.out.println(">> Packet Array Size: " + packetArray.size());
                                this.processIncomingBytes(incomingBytes, seq, sendingRoverId);
                                missingSequences.remove((Integer)seq);
                                if (missingSequences.size() == 0) {
                                    System.out.println(">> Sending ACK for missing packets: ");
                                    this.rover.getSenderModule().sendAcknowledgement(currentStartSeqOfPacketArray, sendingRoverId, (byte) 5);
                                }
                            } else {
                                this.processIncomingBytes(incomingBytes, seq, sendingRoverId);
                            }
                        }

                    }
                }
            } catch (IOException e) {
                if (e.getMessage().equals("Receive timed out") && this.packetArray.size() > 0 && isPacketMissing()) {
                    System.out.println("[!!!] Processing socket timeout");
                    this.rover.getSenderModule().sendNegativeAcknowledgement(missingSequences, sendingRoverId);
                }
            }
        }
    }

    /**
     * Method to check if the reqeust is for file transfer
     * @param   incomingBytes   byte[]
     */
    private boolean requestForFileTransfer(byte[] incomingBytes) {
        return (incomingBytes[RdtProtocol.COMMAND_FLAG_POSITION] == CommandMap.ROVER_COMMANDS.get(CommandMap.Constants.CLICK));
    }

    /** 
     * Method to notify sender to resume sending file
     */
    private void notifySender() {
        synchronized(this.rover) {
            this.rover.notifyAll();
        }
    }

    /**
     * Method to notify sending module to resend missing packets
     * @param missingSequenceArray  byte[]
     */
    private void notifySenderToResendPackets(byte[] missingSequenceArray) {
        System.out.println(">> Notifying sender to resend missing packets");
        this.rover.getSenderModule().setResendFlag(true);
        List<Integer> tempList = new ArrayList<>();
        for (int index = 0; index < missingSequenceArray.length - 4; index += 4) {
            tempList.add(
                ((missingSequenceArray[index] & 0xff) << 24 |
                (missingSequenceArray[index + 1] & 0xff) << 16 |
                (missingSequenceArray[index + 2] & 0xff) << 8 |
                (missingSequenceArray[index + 3] & 0xff) << 0)
            );
        }
        System.out.println("Missing sequence Array Length: " + tempList);
        this.rover.getSenderModule().setMissingSequenceNumbers(tempList);
        notifySender();
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
        return ((byteArr[0] & 0xff) << 24 | (byteArr[1] & 0xff) << 16 | (byteArr[2] & 0xff) << 8 | (byteArr[3] & 0xff) << 0);
    }

    /**
     * Method to check if any packet is missing based on the sequence numbers received
     * @return boolean
     */
    private boolean isPacketMissing() {
        boolean isMissing = false;
        Set<Integer> sequences = packetArray.keySet();
        int minSequence = previousStartSeqOfPacketArray;
        int maxSequence = previousStartSeqOfPacketArray + (BUFFER_SIZE - 1);
        for (int index = minSequence; index <= maxSequence; index++) {
            if (!sequences.contains(index)) {
                isMissing = true;
                missingSequences.add(index);
            }
        }
        if(!isMissing) {
            System.out.println(">> No Missing Packets...");
        } else {
            System.out.println(">> Missing packets with sequence numbers: " + Arrays.toString(missingSequences.toArray()));
        }
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
                System.out.println(">> Packet arrived out of sequence: " + sequenceList.get(index));
                return false;
            }
        }
        System.out.println(">> Packets arrived in sequence....");
        return true;
    }

    /**
     * Method to sort the packets based on the sequence numbers
     * @return TreeMap<Integer, byte[]>
     */
    private TreeMap<Integer, byte[]> arrangePacketsInSequence() {
        TreeMap<Integer, byte[]> sortedPackets = new TreeMap<>();
        System.out.println(">> Sorting packets in sequence....");
        sortedPackets.putAll(packetArray);
        previousStartSeqOfPacketArray = sortedPackets.firstKey();
        currentStartSeqOfPacketArray = sortedPackets.lastKey() + 1;
        return sortedPackets;
    }

    /**
     * Method to check if the incoming packet is an acknowledgement
     * @param incomingBytes byte[]
     * @return              boolean
     */
    private boolean incomingPacketIsAcknowledgement(byte[] incomingBytes) {
        return (incomingBytes[RdtProtocol.ACK_FLAG_POSITION] == TRUE);
    }

    /**
     * Method to check if the incoming packet is a negative acknowledgement
     * @param incomingBytes byte[]
     * @return              boolean
     */
    private boolean incomingPacketIsNegativeAcknowledgement(byte[] incomingBytes) {
        return (incomingBytes[RdtProtocol.NAK_FLAG_POSITION] == TRUE);
    }

    private boolean incomingPacketIsFinishMessage(byte[] incomingBytes) {
        return (incomingBytes[RdtProtocol.FIN_FLAG_POSITION] == TRUE);
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
            packetArray.put(seq, relevantBytes);
        } else if (packetArray.size() == BUFFER_SIZE - 1) {
            packetArray.put(seq, relevantBytes);
            List<Integer> tempList = new ArrayList<>();
            for (Map.Entry<Integer, byte[]> entry : packetArray.entrySet()) {
                tempList.add(entry.getKey());
            }
            previousStartSeqOfPacketArray = currentStartSeqOfPacketArray;
            currentStartSeqOfPacketArray = Collections.max(tempList) + 1;
            if (!isPacketMissing()) {
                if (isPacketInSequence()) {
                    this.writeToFile(packetArray);
                } else {
                    TreeMap<Integer, byte[]> sorted = arrangePacketsInSequence();
                    this.writeToFile(sorted);
                }
                this.runGarbageCollector();
                this.rover.getSenderModule().sendAcknowledgement(currentStartSeqOfPacketArray, sendingRoverId, (byte) 5);
            } else {
                this.rover.getSenderModule().sendNegativeAcknowledgement(missingSequences, sendingRoverId);
            }
            System.out.println(">> Next exptected SEQ: " + currentStartSeqOfPacketArray);
            System.out.println("------------------------------------------------------------------------------");
        }
    }

    /**
     * Method to write bytes to output file
     * @throws IOException
     */
    private void writeToFile(Map<Integer, byte[]> sortedPackets) throws IOException {
        System.out.println(">> Writing to file....");
        for (Map.Entry<Integer, byte[]> entry : sortedPackets.entrySet()) {
            fos.write(entry.getValue());
        }
    }

    /**
     * Method to empty the receiving buffer
     */
    private void runGarbageCollector() {
        System.out.println(">> Cleaning buffer to accept new packets....");
        this.packetArray = new LinkedHashMap<>(BUFFER_SIZE);
    }
}
