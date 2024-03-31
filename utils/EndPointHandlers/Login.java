package utils.EndPointHandlers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.security.MessageDigest;
import java.math.BigInteger;
import java.net.HttpCookie;

import utils.Database;
import utils.UtilityClass;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Random;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import utils.ExpiredSessions.debug;
import java.security.NoSuchAlgorithmException;

public class Login implements HttpHandler {
    private final Database database;

    public Login(Database database) {
        this.database = database;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Check if the request method is POST
        if (UtilityClass.handlesession(exchange, database)) {
            exchange.getResponseHeaders().set("Location", "/filemanager");
            exchange.sendResponseHeaders(302, -1);

        }
        if ("POST".equals(exchange.getRequestMethod())) {
            try {
                handlePostRequest(exchange, database);
            } catch (SQLException e) {
                System.err.println(e);
            }
        } else if ("GET".equals(exchange.getRequestMethod())){
            loginpage(exchange, 405);
        }
    }


    private String generateSessionString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            char randomChar = characters.charAt(randomIndex);
            stringBuilder.append(randomChar);
        }

        return stringBuilder.toString();
    }

    public String generateUniqueSessionId(int length) throws SQLException {
        String sessionId = "";
        try {
            do {
                sessionId = generateSessionString(length);
            } while (!isSessionIdUnique(sessionId));
        } catch (SQLException e) {
            System.out.println("Error checking uniqueness of session id: " + e.getMessage());
        }
        return sessionId;
    }

    private boolean isSessionIdUnique(String sessionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM sessions WHERE session_id = ?";
        ResultSet resultSet = database.executeQuery(sql, sessionId);
        resultSet.next();
        return resultSet.getInt(1) == 0;
    }

    private void handlePostRequest(HttpExchange exchange, Database database) throws IOException, SQLException {

        OutputStream os = exchange.getResponseBody();
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder postData = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            postData.append(line);
        }
        // Parse the POST data as URL-encoded parameters
        Map<String, String> parameters = UtilityClass.parseFormData(postData.toString());
        String username = parameters.get("username");
        String password = parameters.get("password");
        String md5_key = MD5(username+password); // Unique hash signiture for file encryption
        password = UtilityClass.hashPassword(password);
        boolean loginSuccessful = false;
        String user_id = "";
        ResultSet resultSet = database.executeQuery("SELECT * FROM users WHERE username = ?", username);
        
        while (resultSet.next()) {
            user_id = resultSet.getString("id");
            if (username.equals(resultSet.getString("username")) && password.equals(resultSet.getString("password"))) {
                // Successful login
                loginSuccessful = true;
                break;
            }
        }
        if (loginSuccessful) {
            debug.printlog(exchange, "[+] Login success : " + username);
            loginSuccess(exchange, database, user_id, password, md5_key);
        } else {
            exchange.getResponseHeaders().set("Location", "/login");
            exchange.sendResponseHeaders(302, -1);

        }
        exchange.close();
        os.close();
    }

    // Bad code. DO NOT USE IN ACTUAL PRACTISE. MD5 IS A WEAK HASH FOR PASSWORDS!!!!!!!!!
    public static String MD5(String md5) {
       try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
              sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
           }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }

    private void loginSuccess(HttpExchange exchange, Database database, String user_id, String password, String md5_key) throws SQLException, IOException  {
        long currentTimeMillis = System.currentTimeMillis();
        Timestamp currentTimestamp = new Timestamp(currentTimeMillis);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentTimestamp);
        calendar.add(Calendar.DAY_OF_MONTH, 1);

        Timestamp expiryTimestamp = new Timestamp(calendar.getTimeInMillis());
        String expiryTimestampStr = expiryTimestamp.toString();
        String cookieValue = generateUniqueSessionId(128);
        exchange.getResponseHeaders().add("Set-Cookie", "KeyHash=" + md5_key);
        exchange.getResponseHeaders().add("Set-Cookie", "SessionID=" + cookieValue);
        String sql = "INSERT INTO sessions (session_id, user_id, expiry_timestamp, user_agent) VALUES (?, ?, ?, ?)";
                
        List<String> userAgentHeaders = exchange.getRequestHeaders().get("User-Agent");
        // Execute the SQL query
        if (userAgentHeaders != null && !userAgentHeaders.isEmpty()) {
            String userAgent = userAgentHeaders.get(0);
            try {
                database.executeUpdate(sql, cookieValue, user_id, expiryTimestampStr, userAgent);
            } catch (SQLException e) {
                e.printStackTrace(); // Handle the exception appropriately
            }
            debug.printlog(exchange, "Redirecting User to /filemanager");
            exchange.getResponseHeaders().set("Location", "/filemanager");
            exchange.sendResponseHeaders(302, -1);
        } else {
            System.out.println("Redirecting User to /login");
            exchange.getResponseHeaders().set("Location", "/login");
            exchange.sendResponseHeaders(302, -1);
        }
    }

    private void loginpage(HttpExchange exchange, int statusCode) throws IOException {
        UtilityClass.handlesession(exchange, database);
        OutputStream os = exchange.getResponseBody();
        String htmlFilePath = "html/login.html";
        String response = UtilityClass.readFile(htmlFilePath);
        String cssContent = UtilityClass.readFile("static/style.css");
        response = response.replace("{{css_placeholder}}", "<style>" + cssContent + "</style>");
        exchange.sendResponseHeaders(200, response.length());
        os.write(response.getBytes());
        os.close();
        exchange.close();
    }
}