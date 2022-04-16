import java.util.HashMap;
import java.util.Map;

public class CommandMap {
    public static Map<String, Integer> FILE_EXTENSIONS = new HashMap<>();
    public static Map<String, Integer> ROVER_COMMANDS = new HashMap<>();

    public CommandMap() {
        setFileExtensions();
        setRoverCommands();
    }

    private static void setFileExtensions() {
        FILE_EXTENSIONS.put("txt", 0);
        FILE_EXTENSIONS.put("jpg", 1);
        FILE_EXTENSIONS.put("pdf", 2);
        FILE_EXTENSIONS.put("png", 3);
    }

    private static void setRoverCommands() {
        ROVER_COMMANDS.put("Standby", 0);
        ROVER_COMMANDS.put("Move Forward", 1);
        ROVER_COMMANDS.put("Move Back", 2);
        ROVER_COMMANDS.put("Turn Left", 3);
        ROVER_COMMANDS.put("Turn Right", 4);
        ROVER_COMMANDS.put("Capture", 5);
    }

    class Constants {
        public static final String ALL     = "all";
        public static final String BACK    = "Move Back";
        public static final String LEFT    = "Turn Left";
        public static final String RIGHT   = "Turn Right";
        public static final String CLICK   = "Capture";
        public static final String SENDER  = "sender";
        public static final String FORWARD = "Move Forward";
        public static final String STANDBY = "Standby";
    }
}
