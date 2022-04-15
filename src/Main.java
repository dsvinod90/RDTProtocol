import java.util.Random;

public class Main {
    public static void main(String[] args) {
        System.out.println("Initializing commands....");
        new CommandMap();
        byte currentRoverId = Byte.parseByte(args[0]);
        byte toRoverId;
        if (args[1].equalsIgnoreCase(CommandMap.Constants.ALL)) {
            toRoverId = (byte) 0;
        } else {
            toRoverId = Byte.parseByte(args[1]);
        }
        int currentRoverPort = Integer.parseInt(args[2]);
        String multicastIP = args[3];
        String role = args[4];
        String command = args[5];
        Rover.createInstance(currentRoverId, toRoverId, currentRoverPort, multicastIP, role);
        Rover rover = Rover.fetchInstance();
        System.out.println("Rover deployed on the lunar surface");
        ThreadPoolManager.getThread().submit(rover);
        System.out.println("Rover has been configured: \n" +
        "\t Object ID: " + rover + "\n" +
        "\t Rover ID: " + rover.getRoverId() + "\n" +
        "\t IP Address: " + rover.getIpAddress());
        System.out.println();
        if (Integer.parseInt(command) == CommandMap.ROVER_COMMANDS.get(CommandMap.Constants.CLICK) && role.equalsIgnoreCase(CommandMap.Constants.SENDER)) {
            Random random = new Random();
            String filePath = 
            "/Users/VinodDalavai/Documents/2nd Semester/Networks-651/Project3/LunarRover/src/surface_2" +
            // random.nextInt(1) + 
            ".jpg";
            System.out.println("Sending file " + filePath);
            rover.setFilePath(filePath);
            rover.sendFile();
        }
    }
}
