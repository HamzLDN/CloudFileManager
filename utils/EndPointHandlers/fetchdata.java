/* This file when fetch the data for the client. USE FOR APIS!!!!! */


package utils.EndPointHandlers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import utils.Database;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.nio.file.Paths;
import java.nio.file.Path;
import utils.UtilityClass;
import java.net.URI;
import java.util.Arrays;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.util.stream.StreamSupport;
import java.util.Base64;
import java.util.zip.Deflater;
import java.io.ByteArrayOutputStream;
import utils.ExpiredSessions.debug;
public class fetchdata implements HttpHandler {
    private final Database database;

    public fetchdata(Database database) {
        this.database = database;
    }
    private String navigateuser(HttpExchange exchange, String endpoint) {
        Path path = FileSystems.getDefault().getPath(endpoint);
        if (Files.isDirectory(path)) {
            return "dir";
        } else if (Files.exists(path)) {
            return "file";
        } else {
            return "";
        }
    }
    private static boolean verifyuser(HttpExchange exchange, Database database, String username) {
        List<String> user;
        try {
            user = UtilityClass.getuserinfo(exchange, database);
        } catch (Exception e) {
            return false;
        }
        if (!user.get(0).equals(username)) {
            return false;
        }
        return true;
    }

    public static String extractFileExtension(String filePath) {
        // Create a Path instance from the file path string
        Path path = Paths.get(filePath);
        // Get the file name from the path
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex != -1 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        } else {
            return "";
        }
    }

    public static byte[] compressBytes(byte[] inputBytes) throws IOException {
        Deflater deflater = new Deflater();
        deflater.setInput(inputBytes);
        deflater.finish();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(inputBytes.length);

        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }

        deflater.end();
        outputStream.close();

        return outputStream.toByteArray();
    }

    // FIX FUNCTION. HIGHLY NESTED AND NOT OPTIMIZED. SHOULD BE REFACTORED INTO SEPARATE METHODS FOR EACH TASK IT PERFORMS
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String endpoint = uri.getPath();
        String[] parts = Arrays.stream(endpoint.split("/"))
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        System.out.println(parts.length);
        if (parts.length == 1) {
            exchange.close();
        }
        parts[0] = "UserFiles";
        String username = parts[1];
        String filepath =  String.join("/", parts);
        parts[0] = "";
        String userpath = String.join("/", parts);
        List<String> fileList = null;
        String fileListJson = null;
        if (!verifyuser(exchange,database,username)){
            exchange.close();
            return;
        }
        if (navigateuser(exchange, filepath).equals("dir")) {
            System.out.println(filepath);
            fileList = listFilesAndDirectories(filepath);
            if (fileList.size() == 0) {
                fileListJson = send_json("error", "");
            } else {
                fileListJson = convertListToJson(fileList, userpath);
            }
            
        } else if (navigateuser(exchange, filepath).equals("file")) {
            String filecontent;
            String ext = extractFileExtension(filepath);
            if (ext.endsWith("png") || ext.endsWith("jpg") || ext.endsWith("jpeg") || ext.endsWith("gif") || ext.endsWith("mov")) {
                debug.printerrlog(exchange, "Image being sent png/jpg/jpeg/gif");
                byte[] contentbytes = Files.readAllBytes(Paths.get(filepath));
                filecontent = encodeToBase64(contentbytes);
            } else {
                filecontent = new String(Files.readAllBytes(Paths.get(filepath)), StandardCharsets.UTF_8);
            }
            byte[] data = send_json("content", filecontent).getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); // Enable CORS
            debug.printerrlog(exchange, "CORS ENABLED");
            try (OutputStream os = exchange.getResponseBody()) {
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseHeaders().set("Transfer-Encoding", "chunked");

                int chunkSize = 1024;
                for (int i = 0; i < data.length; i += chunkSize) {
                    int length = Math.min(chunkSize, data.length - i);
                    os.write(data, i, length);
                    os.flush();
                }
                os.close();
            } catch (IOException e) {
                debug.printlog(exchange,"An error occurred while trying to write the response: " + e);
            }
            exchange.close();
            return;
        } else {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            String json_data = send_json("status", "Error! Cannot get the file contents");
            exchange.sendResponseHeaders(200, json_data.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json_data.getBytes());
                os.close();
            }
            
            exchange.close();
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, fileListJson.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(fileListJson.getBytes());
        }
        exchange.close();
    }

    public static String encodeToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String send_json(String key, String file_content) {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(key, file_content);
        jsonArray.add(jsonObject);
        return jsonArray.toJSONString();
    }

    public static String convertListToJson(List<String> list, String user) {
        System.out.println(user);
        JSONArray jsonArray = new JSONArray();
        for (String item : list) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("file", item);
            jsonObject.put("timestamp", System.currentTimeMillis());
            jsonArray.add(jsonObject);
        }

        return jsonArray.toJSONString();
    }
    private List<String> listFilesAndDirectories(String directoryPath) throws IOException {
        Path directory = FileSystems.getDefault().getPath(directoryPath);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            return StreamSupport.stream(directoryStream.spliterator(), false)
                    .filter(path -> {
                        try {
                            return !Files.isHidden(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }

}
