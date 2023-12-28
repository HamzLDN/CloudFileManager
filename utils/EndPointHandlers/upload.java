package utils.EndPointHandlers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import utils.Database;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import utils.UtilityClass;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
public class upload implements HttpHandler {
    private final Database database;

    public upload(Database database) {
        this.database = database;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        List<String>  userinfo = UtilityClass.getuserinfo(exchange,database);
        String username = userinfo.get(0);
        if ("POST".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] receivedData;
            try (InputStream is = exchange.getRequestBody()) {
                receivedData = is.readAllBytes();
            }
            System.out.println(new String(receivedData, StandardCharsets.UTF_8));
            boolean valid_path = extractFilename(receivedData, username);
            if (valid_path) {
                String response = "Data received successfully!";
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                String response = "Failed to upload file";
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }

        } else {
            exchange.sendResponseHeaders(405, -1);
        }
        exchange.close();
    }
    private static String getuser(String filepath) {
        String[] parts = Arrays.stream(filepath.split("/"))
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        return parts[1];
    }

    private static boolean extractFilename(byte[] input, String usernmae) {
        String inputString = new String(input, StandardCharsets.UTF_8);
        Pattern pattern = Pattern.compile("'(.*?)'");
        Matcher matcher = pattern.matcher(inputString);
        if (matcher.find()) {
            String filename = matcher.group(1);
            filename = UtilityClass.sanitizepath(filename);
            if (getuser(filename).equals(usernmae)){
                String remainingData = inputString.substring(matcher.end());
                byte[] bufferdata = decodeBase64(remainingData);
                writeToFile(bufferdata, filename);
                return true;
            }
            else {
                return false;
            }

        }
        return false;
    }

    public static byte[] decodeBase64(String encodedContent) {
        byte[] decodedBytes = Base64.getDecoder().decode(encodedContent);
        return decodedBytes;
    }
    public static boolean validatePath(String filePathString) {
        // Convert the file path string to a Path object
        File filePath = new File(filePathString);
        System.out.println(filePath);
        if (filePath != null) {
            return true;
        } else {
            return false;
        }
    }
    public static void writeToFile(byte[] content, String fileName) {
        try {
            fileName = UtilityClass.sanitizeFilePath(fileName);
            if (validatePath(fileName)) {
                Files.createDirectories(Path.of(fileName).getParent());
                Files.write(Path.of(fileName), content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);     
            } else {
                System.out.println("PathNotValid");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}