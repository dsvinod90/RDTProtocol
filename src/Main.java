public class Main {
    public static void main(String[] args) {
        byte sendingRoverId = Byte.parseByte(args[0]);
        byte receivingRoverId;
        if (args[1].equalsIgnoreCase("all")) {
            receivingRoverId = (byte) 0;
        } else {
            receivingRoverId = Byte.parseByte(args[1]);
        }
        int port = Integer.parseInt(args[2]);
        String multicastIp = args[3];
        String action = args[4];
        Rover rover = new Rover(sendingRoverId, receivingRoverId, port, multicastIp);
        System.out.println("This Rover: " + rover.hashCode());
        System.out.println("This Rover's ID: " + rover.getRoverId());
        System.out.println("This Rover's IP: " + rover.getIpAddress());
        rover.initializeModules();
        rover.receiveData();
        if(action.equalsIgnoreCase("send")) { rover.sendFile("/Users/VinodDalavai/Documents/2nd Semester/Networks-651/Project3/LunarRover/src/sample.JPG"); }
    }
}
