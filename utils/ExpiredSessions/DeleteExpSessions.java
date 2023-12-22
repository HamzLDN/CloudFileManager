package utils.ExpiredSessions;
import utils.Database;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
public class DeleteExpSessions extends Thread {
    
    private Database db;
        public DeleteExpSessions(Database db) {
            this.db = db;
        }
    public void run() { // Need the run function to start the thread.
        maintainSession(db);
    }
    public static boolean checkexpr(String[] args) {
        String expirationDateString = "2023-12-12 23:05:00";
        String currentDateString = getCurrentDateInFormat("yyyy-MM-dd HH:mm:ss");
        

        // Parse the strings into LocalDateTime objects
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime expirationDate = LocalDateTime.parse(expirationDateString, formatter);
        LocalDateTime currentDate = LocalDateTime.parse(currentDateString, formatter);

        // Compare the two dates
        return currentDate.isAfter(expirationDate);
    }
    private static boolean checkexpr(String timestamp) throws ParseException {
        // Parse the expiration timestamp from the input string
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // Set the time zone for the input timestamp
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        
        Date sessiondate = sdf.parse(timestamp);
        
        ZonedDateTime currentDateTime = ZonedDateTime.now();
        // Set the time zone for the current date and time
        currentDateTime = currentDateTime.withZoneSameInstant(java.time.ZoneId.of("UTC"));
        
        return currentDateTime.isAfter(sessiondate.toInstant().atZone(java.time.ZoneId.of("UTC")));
    }

    public static void DeleteSessions(Database db) {
        
        String sql_sessions = "select * from sessions";
        try (java.sql.ResultSet database = db.executeQuery(sql_sessions)) {
            while (database.next()) {
                String sessionid = database.getString("session_id");
                String Timestamp = database.getString("expiry_timestamp");

                if (checkexpr(Timestamp)) {
                    delete_query(db, "delete from sessions WHERE session_id = ?", sessionid);
                }
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }
    

    public static String getCurrentDateInFormat(String format) {
        // Get the current date and time
        LocalDateTime currentDate = LocalDateTime.now();

        // Define the desired date and time format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

        // Format the current date and time using the formatter
        return currentDate.format(formatter);
    }

    public static void maintainSession(Database db) {
        while (true) {
            DeleteSessions(db);
            try {
            Thread.sleep(5 *60 * 1000); // Sleeps every 5 mins and then carries on
            } catch (Exception e) {
                System.err.println("Error occurred in thread sleep.");
                break;
            }

        }

    }

    private static boolean delete_query(Database db, String sql, String... parameters) {
        try (java.sql.ResultSet database = db.executeQuery(sql, parameters[0])) {
        } catch (Exception e) {
            System.err.println(e);
        }
        return false;
    }
}