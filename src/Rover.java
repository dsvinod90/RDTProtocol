import java.io.File;

/**
 * This is the class that outlines the Rover and its functionalities
 * @author Vinod Dalavai - vd1605
 */

public class Rover extends Thread {
    private int MULTICAST_PORT = 520;
    // Declare class fields
    private byte roverId; // id of the rover which will also be used to get the IP address of the rover
    private String multicastIP; // multicast IP of the rover
    private int listeningPort; // port on which the socket will be opened for communications with the rover
    private int destinationPort; // port to which the file will be sent
    private Receiver receiverModule; // the rdt receiver that is responsible to receive data
    private Sender senderModule; // the rdt sender that is responsible for sending data
    private byte destinationRoverId; // Rover to which data needs to be sent
    public static Rover rover = null; // an instance of this class
    private String filePath = null; // path of the file to be sent by this rover
    private String action = null; // Sender or Receiver

    /**
     * Constructor for the Rover class
     * @param roverId   int
     * @param port      int
     */
    public Rover(byte roverId, byte destinationRoverId, int port, String multicastIP, String action) {
        this.roverId = roverId;
        this.destinationRoverId = destinationRoverId;
        this.destinationPort = MULTICAST_PORT;
        this.listeningPort = port;
        this.multicastIP = multicastIP;
        this.action = action;
        Receiver.createInstance(this);
        this.receiverModule = Receiver.fetchInstance();
        System.out.println("Initializing Receiver Module: " + this.receiverModule.hashCode());
        System.out.println("------------------------------------------------");
        this.senderModule = new Sender(this, this.destinationRoverId, this.destinationPort);
        System.out.println("Initializing Sender Module: " + this.senderModule.hashCode());
        System.out.println("------------------------------------------------");
        System.out.println("Listening for incoming data on port " + this.listeningPort);
        System.out.println();
    }

    /**
     * Method to create an instance of Rover and assign it to the static variable
     * @param roverId               byte
     * @param destinationRoverId    byte
     * @param port                  int
     * @param multicastIP           String
     */
    public static void createInstance(byte roverId, byte destinationRoverId, int port, String multicastIP, String action) {
        if (rover == null) {
            rover = new Rover(roverId, destinationRoverId, port, multicastIP, action);
        }
    }

    /**
     * Static method that returns the current instance of this class
     * @return Rover
     */
    public static Rover fetchInstance() {
        if (rover != null) return rover;
        return null;
    }

    /**
     * Initialize sender and receiver modules and starts listening on the defined port
     */
    @Override
    public void run() {
        receiveData();
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
        return this.listeningPort;
    }

    /**
     * Getter method for action
     * @return  String
     */
    public String getAction() {
        return this.action;
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
     * @return  String
     */
    public String getMulticastIP() {
        return this.multicastIP;
    }

    /**
     * Method to set the file path of the file that needs to be sent to NASA
     * @param path
     */
    public void setFilePath(String path) {
        this.filePath = path;
    }

    /**
     * Method to send file to all the rovers subscribed to multicast
     * @param filePath  String
     */
    public void sendFile() {
        this.senderModule.setFile(new File(this.filePath));
        this.receiverModule.setFileExtension(getFileExtension());
        ThreadPoolManager.getThread().execute(this.senderModule);
    }

    /**
     * Method to get the file extension of the file that needs to be sent to Nasa
     * @return  String
     */
    public String getFileExtension() {
        String[] filePathArray = this.filePath.split("\\.");
        int lengthOfFilePathArray = filePathArray.length;
        return filePathArray[lengthOfFilePathArray - 1];
    }

    /**
     * Method to receive data
     */
    private void receiveData() {
        ThreadPoolManager.getThread().execute(this.receiverModule);
    }

    /**
     * Only for testing pursposes
     * @param args
     */
    public static void main(String[] args) {
        Rover rover = new Rover((byte)1, (byte)2, 3000, "224.0.0.1", "sender");
        rover.setFilePath("randomfile.txt");
        System.out.println(rover.getFileExtension());
    }
}
