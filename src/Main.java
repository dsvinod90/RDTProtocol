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
        String filePath = null;
        Rover.createInstance(currentRoverId, toRoverId, currentRoverPort, multicastIP, role, Byte.valueOf(command));
        Rover rover = Rover.fetchInstance();
        System.out.println("Rover deployed on the lunar surface");
        ThreadPoolManager.getThread().submit(rover);
        System.out.println("Rover has been configured: \n" +
        "\t Object ID: " + rover + "\n" +
        "\t Rover ID: " + rover.getRoverId() + "\n" +
        "\t IP Address: " + rover.getIpAddress());
        System.out.println();
        if (role.equalsIgnoreCase(CommandMap.Constants.SENDER)) {
            if (Integer.parseInt(command) == CommandMap.ROVER_COMMANDS.get(CommandMap.Constants.CLICK)) {
                if (args.length == 7 && !args[6].equals(" ")) {
                    filePath = args[6];
                } else {
                    Random random = new Random();
                    filePath = "/Users/VinodDalavai/Documents/2nd Semester/Networks-651/Project3/LunarRover/src/surface_" + random.nextInt(3) + ".jpg";
                }
                System.out.println("Sending file " + filePath);
                rover.setFilePath(filePath);
                rover.sendFile();
            } else {
                rover.sendCommand();
            }
        }
    }
}
