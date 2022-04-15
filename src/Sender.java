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
    private static int nextSeq = 0; //next expected sequence number
    private static int sendersSeq = 0; // seq of the sender
    private static boolean resend = false; // flag to resend packets
    private List<Integer> missingSequenceNumbers = new ArrayList<>(); //sequences missing

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

    public void setMissingSequenceNumbers(List<Integer> missingSequences) {
        this.missingSequenceNumbers = missingSequences;
    }

    /**
     * Setter for resend flag
     * @param value boolean
     */
    public void setResendFlag(boolean value) {
        resend = value;
    }

    /**
     * Method to send data on the socket
     */
    public void run() {
        try {
            FileInputStream fis = new FileInputStream(this.file);
            int sequence = 0;
            byte[] tempBuffer = new byte[RdtProtocol.DATAGRAM_LENGTH];
            boolean quit = false;
            while (!quit) {
                for(int count = 0; count < Receiver.BUFFER_SIZE; count++) {
                    if (fis.read(tempBuffer) == -1) quit = true;
                    RdtProtocol protocol = new RdtProtocol(tempBuffer, rover.getRoverId(), destinationRoverId);
                    protocol.setSeq(++sequence);
                    nextSeq = sequence+1;
                    protocol.prepareSegment();
                    this.segments.add(protocol);
                    // System.out.println("Byte stream length: " + protocol.getByteStream().length);
                    packet = new DatagramPacket(
                        protocol.getByteStream(),
                        protocol.getByteStream().length,
                        this.address,
                        this.rover.getPort()
                    );
                    this.socket.send(packet);
                    tempBuffer = new byte[RdtProtocol.DATAGRAM_LENGTH];
                }
                this.waitForAcknowledgement();
            }

            System.out.println(">> Data sent successfully: Closing socket");
            this.sendFinishPacket();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to wait for acknowledgement to be received from the receiver thread
     */
    private void waitForAcknowledgement() {
        synchronized(this.rover) {
            try {
                this.rover.wait();
                if (resend) {
                    this.resendMissingPackets();
                    this.waitForAcknowledgement();
                }
                System.out.println(">> Resume sending");
                sleep(50);   
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void resendMissingPackets() {
        System.out.println(">> Inside Resend Packet:");
        for (RdtProtocol protocol : this.segments) {
            for (int missingSequence : missingSequenceNumbers) {
                if (missingSequence == 0) continue;
                if (missingSequence == protocol.getSeq()) {
                    packet = new DatagramPacket(
                        protocol.getByteStream(),
                        protocol.getByteStream().length,
                        this.address,
                        this.rover.getPort()
                    );
                    try {
                        System.out.println(">> Re-sending missing packet: " + missingSequence);
                        this.socket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    break;
                }
            }
        }
        resend = false;
    }

    /**
     * Method to send acknowledgement
     * @param ack                   int
     * @param destinationRoverId    byte
     */
    public void sendAcknowledgement(int ack, byte destinationRoverId) {
        RdtProtocol protocol = new RdtProtocol(null, this.rover.getRoverId(), destinationRoverId);
        protocol.setSeq(sendersSeq + 1);
        protocol.setAck(true);
        protocol.setAcknowledgementNumber(ack);
        protocol.prepareSegment();
        this.segments.add(protocol);
        System.out.println(">> Sending ACK to: " + this.getIpAddressFromRoverId(destinationRoverId));
        packet = new DatagramPacket(protocol.getByteStream(), protocol.getByteStream().length, this.address, this.receiverPort);
        try {
            this.socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to send negative acknowledgement
     * @param missingSequences      List<Integer>
     * @param destinationRoverId    byte
     */
    public void sendNegativeAcknowledgement(List<Integer> missingSequences, byte destinationRoverId) {
        RdtProtocol protocol = new RdtProtocol(this.convertIntegerListToByteArray(missingSequences), this.rover.getRoverId(), destinationRoverId);
        protocol.setSeq(sendersSeq + 1);
        protocol.setNak(true);
        protocol.prepareSegment();
        this.segments.add(protocol);
        System.out.println(">> Sending NAK to: " + this.getIpAddressFromRoverId(destinationRoverId));
        packet = new DatagramPacket(protocol.getByteStream(), protocol.getByteStream().length, this.address, this.receiverPort);
        try{
            this.socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFinishPacket() {
        RdtProtocol protocol = new RdtProtocol(null, this.rover.getRoverId(), this.destinationRoverId);
        protocol.setSeq(sendersSeq + 1);
        protocol.setFin(true);
        protocol.prepareSegment();
        this.segments.add(protocol);
        System.out.println(">> Sending FIN to: " + this.getIpAddressFromRoverId(destinationRoverId));
        packet = new DatagramPacket(protocol.getByteStream(), protocol.getByteStream().length, this.address, this.receiverPort);
        try{
            this.socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to check if received acknowledgement is as expected
     * @param ack   int
     * @return      boolean
     */
    public boolean verifyAcknowledgement(int ack) {
        return ack == nextSeq;
    }

    /**
     * Method to slice an array into equal size arrays depending on the size of the Datagram
     * @param byteArray byte[]
     * @param start     int
     * @return          byte[]
     */
    private byte[] sliceInEqualParts(byte[] byteArray, int start) {
        byte[] temp = new byte[RdtProtocol.DATAGRAM_LENGTH];
        for (int index = 0; index < temp.length; index++) {
            try{
                temp[index] = byteArray[index + start];
            } catch (ArrayIndexOutOfBoundsException ex) {
                // temp[index] = (byte) 101;
                // temp[index+1] = (byte) 111;
                // temp[index+2] = (byte) 102;
                break;
            }
        }
        return temp;
    }

    /**
     * Method to convert Rover ID to IP Address
     * @param id    byte
     * @return      String
     */
    private String getIpAddressFromRoverId(byte id) {
        return ("10.0.0." + id);
    }

    /**
     * Method to convert a list of integers to an array of bytes
     * @param numbers
     * @return
     */
    private byte[] convertIntegerListToByteArray(List<Integer> numbers) {
        byte[] byteArray = new byte[numbers.size() * 4];
        int index = 0;
        for (int number : numbers) {
           byteArray[index++] = (byte) (number >> 24);
           byteArray[index++] = (byte) (number >> 16);
           byteArray[index++] = (byte) (number >> 8);
           byteArray[index++] = (byte) (number);
        }
        return byteArray;
    }
}
