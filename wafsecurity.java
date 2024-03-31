import java.util.List;
import java.util.Map;
public class wafsecurity {
    private static boolean scan_ips(Map<String, Float> connections) {
        float rps = 25.0;
        for (Map.Entry<String, Float> entry : connections.entrySet()) {
            if (entry.getValue() > rps) return false;
        }
    }
    // public static boolean 
}
