public class Main {
    private static final String filePath = "/Users/VinodDalavai/Documents/2nd Semester/Networks-651/Project3/LunarRover/src/sample.JPG";
    public static void main(String[] args) {
        byte sendingRoverId = Byte.parseByte(args[0]);
        byte receivingRoverId;
        if (args[1].equalsIgnoreCase("all")) {
            receivingRoverId = (byte) 0;
        } else {
            receivingRoverId = Byte.parseByte(args[1]);
        }
        int receiverPort = Integer.parseInt(args[2]);
        String multicastIP = args[3];
        String action = args[4];
        Rover.createInstance(sendingRoverId, receivingRoverId, receiverPort, multicastIP);
        Rover rover = Rover.fetchInstance();
        System.out.println("Rover deployed on the lunar surface");
        ThreadPoolManager.getThread().submit(rover);
        System.out.println("Rover has been configured: \n" +
        "\t Object ID: " + rover + "\n" +
        "\t Rover ID: " + rover.getRoverId() + "\n" +
        "\t IP Address: " + rover.getIpAddress());
        System.out.println();
        if(action.equalsIgnoreCase("send")) {
            rover.setFilePath(filePath);
            rover.sendFile();
        }
    }
}
