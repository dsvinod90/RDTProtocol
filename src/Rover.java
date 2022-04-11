import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This is the class that outlines the Rover and its functionalities
 * @author Vinod Dalavai - vd1605
 */

public class Rover {
    // Declare class fields
    private byte roverId; // id of the rover which will also be used to get the IP address of the rover
    private String multicastIP; // multicast IP of the rover
    private int port; // port on which the socket will be opened for communications with the rover
    private Receiver receiverModule; // the rdt receiver that is responsible to receive data
    private Sender senderModule; // the rdt sender that is responsible for sending data
    private byte destinationRoverId;

    /**
     * Constructor for the Rover class
     * @param roverId   int
     * @param port      int
     */
    public Rover(byte roverId, byte destinationRoverId, int port, String multicastIP) {
        this.roverId = roverId;
        this.destinationRoverId = destinationRoverId;
        this.port = port;
        this.multicastIP = multicastIP;
    }

    /**
     * Initialize sender and receiver modules
     */
    public void initializeModules() {
        this.receiverModule = new Receiver(this);
        this.senderModule = new Sender(this, this.destinationRoverId);
    }

    /**
     * Getter method for Rover ID
     * @return  int
     */
    public byte getRoverId() {
        return this.roverId;
    }

    /**
     * Getter method for port on which the rover is communicating
     * @return  int
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Getter method for receiver module
     * @return  Receiver
     */
    public Receiver getReceiverModule() {
        return this.receiverModule;
    }

    /**
     * Getter method for sender module
     * @return  Sender
     */
    public Sender getSenderModule() {
        return this.senderModule;
    }

    /**
     * Method that fetches the IP address of the rover based on the roverId
     * @return  String
     */
    public String getIpAddress() {
        return "10.0.0." + this.roverId;
    }

    /**
     * Getter for multicastIP address
     * @return
     */
    public String getMulticastIP() {
        return this.multicastIP;
    }

    /**
     * Method to send file to all the rovers subscribed to multicast
     * @param filePath
     */
    public void sendFile(String filePath) {
        this.senderModule.setFile(new File(filePath));
        ThreadPoolManager.getThread().execute(this.senderModule);
    }

    /**
     * Method to receive data
     */
    public void receiveData() {
        ThreadPoolManager.getThread().execute(this.receiverModule);
    }
}
