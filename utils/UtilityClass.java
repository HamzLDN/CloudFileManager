package utils;
import com.sun.net.httpserver.HttpExchange;

import utils.ExpiredSessions.debug;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
public class UtilityClass {
    
    public static String readFile(String filePath) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(filePath));
        return new String(encoded);
    }

    public static List<String> getuserinfo(HttpExchange exchange, Database database) {
        String sessionID = UtilityClass.getSessionIDCookieValue(exchange, "SessionID");
        List<String> resultList = new ArrayList<>();
        String sql_query = "SELECT users.*, sessions.* " +
                    "FROM users " +
                    "JOIN sessions ON users.id = sessions.user_id " +
                    "WHERE sessions.session_id = ?";
        try (ResultSet resultSet = database.executeQuery(sql_query, sessionID)) {
            while (resultSet.next()) {
                String username = resultSet.getString("username");
                String firstname = resultSet.getString("firstname");
                String lastname = resultSet.getString("lastname");
                String sessionId = resultSet.getString("session_id");
                resultList.add(username);
                resultList.add(firstname);
                resultList.add(lastname);
                resultList.add(sessionId);
            }
        } catch (SQLException e) {
            return null;
        }
        return resultList;
        
    }

    public static String hashPassword(String password) { // using sha-512 to hash the password
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512 algorithm not available", e);
        }
    }

    public static Map<String, String> parseFormData(String formData) {
        Map<String, String> parameters = new HashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                parameters.put(key, value);
            }
            
        }
        return parameters;
    }
    
    public static String getSessionIDCookieValue(HttpExchange exchange, String cookiename) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        // Check if cookies exist
        if (cookies != null) {
            for (String cookie : cookies) {
                String[] cookieValues = cookie.split(";");
                for (String cookieValue : cookieValues) {
                    String[] keyValue = cookieValue.split("=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();
                        if (key.equals(cookiename)) {
                            return value;
                        }
                    }
                }
            }
        }
        return "";
    }
    public static boolean IsValid(String fileName) {
        String regex = "^[a-zA-Z0-9_-]+(?:\\.[a-zA-Z0-9_-]+)?$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(fileName);
        return matcher.matches();
    }

    private static boolean HandleUser(HttpExchange exchange, String sessionID, String sessionIDFromSessions, String DBuserAgent, String DeviceUserAgent) throws IOException {
        if (sessionID.equals("")&&sessionIDFromSessions.equals("")) {
            debug.printlog(exchange, "[-] User has no local sessions or invalid: " + sessionID + " : " + sessionIDFromSessions);
            exchange.getResponseHeaders().add("Set-Cookie", "SessionID=; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT");
            return false;
        }
        if (sessionID.equals(sessionIDFromSessions) && (DBuserAgent.equals(DeviceUserAgent))) {
            debug.printlog(exchange, "[+] UserAgent matches the current user. Session is valid.");
            return true;
        } else {
            debug.printlog(exchange, "[-] The session does not have a matching UserAgent or it's expired. Session is invalid.");
            exchange.getResponseHeaders().add("Set-Cookie", "SessionID=; Path=/");
            exchange.getResponseHeaders().set("Location", "/");
            exchange.sendResponseHeaders(302, -1);
            return false;
        }
    }
    public static Map<String, String> getVars(String input) {
        String regex = "\\.\\{\\{(.*?)}}"; // capture content between .{{ and }}
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        Map<String, String> varsMap = new HashMap<>();

        while (matcher.find()) {
            String placeholder = matcher.group();
            String capturedContent = matcher.group(1);
            varsMap.put(placeholder, capturedContent);
        }

        return varsMap;
    }

    public static String render_html(String htmlContent, Map<String, Object> variables) {
        Map<String, String> varsMap = UtilityClass.getVars(htmlContent);
        for (Map.Entry<String, String> entry : varsMap.entrySet()) {
            String placeholder = entry.getKey();
            String value = entry.getValue();
            CharSequence placeholderSequence = placeholder;
            CharSequence replacementSequence = (CharSequence) variables.get(value);
            if (replacementSequence == null) {
                replacementSequence = "";
            }
            htmlContent = htmlContent.replace(placeholderSequence, replacementSequence);
        }
        return htmlContent;
    }

    public static String sanitizeFilePath(String userFilePath) {
        Path path = Paths.get(userFilePath);
        Path sanitizedPath = path.normalize();
        return sanitizedPath.toString();
    }

    public static String sanitizepath(String filepath) {
        String[] parts = Arrays.stream(filepath.split("/"))
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        int position = 0;
        for (String a: parts) {
            if (a.equals("..")) {
                if (position!=0) position--;
                continue;
            } else {
                parts[position] = a;
            }
            position++;
        }
        String[] newpath = new String[position];
        for (int i=0;i<position;i++) {
            newpath[i]=parts[i];
        }
        return String.join("/", newpath);
    }

    public static boolean handlesession(HttpExchange exchange, Database database) {
        try {
            String sessionIDFromSessions = "";
            String DBuserAgent = "";
            String sessionID = UtilityClass.getSessionIDCookieValue(exchange, "SessionID");
            debug.printlog(exchange, "Checking Session ID : "+sessionID);
            List<String> userAgentHeaders = exchange.getRequestHeaders().get("User-Agent");
            String DeviceUserAgent = userAgentHeaders.get(0);
            
            String sql = "select session_id, user_agent from sessions where session_id = ?";
            ResultSet resultSet = database.executeQuery(sql, sessionID);
            while (resultSet.next()) {
                sessionIDFromSessions = resultSet.getString("session_id");
                DBuserAgent = resultSet.getString("user_agent");
            }
            return HandleUser(exchange, sessionID, sessionIDFromSessions, DBuserAgent, DeviceUserAgent);

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (SQLException e) {
            
        }
        return false;
    }
}