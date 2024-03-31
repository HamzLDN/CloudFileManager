package utils.ExpiredSessions;
import com.sun.net.httpserver.HttpExchange;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class debug {
    public static String getCurrentDateInFormat(String format) {
        LocalDateTime currentDate = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return currentDate.format(formatter);
    }
    public static void printlog(HttpExchange exchange, String message) {
        // 
        String currentDateString = debug.getCurrentDateInFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("[" + currentDateString + "] : " + exchange.getRemoteAddress() + " - " + message);
    }
    public static void printerrlog(HttpExchange exchange, String message) {
        
    }
}
