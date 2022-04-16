import java.io.File;
import java.util.concurrent.ExecutorService;

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
    private byte command; // command instructed by the operator of the Rover

    /**
     * Constructor for the Rover class
     * @param roverId   int
     * @param port      int
     */
    public Rover(byte roverId, byte destinationRoverId, int port, String multicastIP, String action, byte command) {
        this.roverId = roverId;
        this.destinationRoverId = destinationRoverId;
        this.destinationPort = MULTICAST_PORT;
        this.listeningPort = port;
        this.multicastIP = multicastIP;
        this.action = action;
        this.command = command;
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
    public static void createInstance(byte roverId, byte destinationRoverId, int port, String multicastIP, String action, byte command) {
        if (rover == null) {
            rover = new Rover(roverId, destinationRoverId, port, multicastIP, action, command);
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
        this.senderModule.setSendFile(true);
        this.senderModule.setCommandFlag((byte) 5);
        this.receiverModule.setFileExtension(getFileExtension());
        ExecutorService service = ThreadPoolManager.getThread();
        service.execute(this.senderModule);
        System.out.println("Sender terminating...");
        service.shutdown();
    }

    public void sendCommand() {
        System.out.println("Sending command to Rover: " + this.command);
        this.senderModule.setSendFile(false);
        this.senderModule.setCommandFlag(this.command);
        ExecutorService service = ThreadPoolManager.getThread();
        service.execute(this.senderModule);
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
}
