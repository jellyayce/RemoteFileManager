package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import util.FileLogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.concurrent.Executors;


//The main server class for the Remote File Manager.
public class FileServer {

    private static final int PORT = 5000;

    /**
     * The entry point for the server application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT + "...");

            HttpFileServer httpFileServer = new HttpFileServer();
            new Thread(httpFileServer).start();
            System.out.println("HTTP File Server started on port 8080...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}

//Handles client requests in a separate thread.
class ClientHandler implements Runnable {

    private Socket clientSocket;
    private BufferedReader input;
    private PrintWriter output;
    private FileLogger logger;

    private static final String SERVER_FILES_DIR = "server_files";

    /**
     * Constructs a ClientHandler for the given socket.
     *
     * @param clientSocket the client socket
     */
    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.logger = new FileLogger();
        
        File dir = new File(SERVER_FILES_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Override
    public void run() {
        try {
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            output = new PrintWriter(clientSocket.getOutputStream(), true);

            String commandLine;
            while ((commandLine = input.readLine()) != null) {
                logger.log("Received command from client: " + commandLine);
                String[] parts = commandLine.split(" ", 2);
                String command = parts[0].toUpperCase();

                switch (command) {
                    case "LIST":
                        handleList();
                        break;
                    case "UPLOAD":
                        if (parts.length > 1) {
                            handleUpload(parts[1]);
                        } else {
                            output.println("Error: No filename provided for UPLOAD.");
                        }
                        break;
                    case "DOWNLOAD":
                        if (parts.length > 1) {
                            handleDownload(parts[1]);
                        } else {
                            output.println("Error: No filename provided for DOWNLOAD.");
                            output.println("END");
                        }
                        break;
                    case "DELETE":
                        if (parts.length > 1) {
                            handleDelete(parts[1]);
                        } else {
                            output.println("Error: No filename provided for DELETE.");
                        }
                        break;
                    case "EXIT":
                        logger.log("Client requested exit.");
                        closeConnections();
                        return;
                    default:
                        output.println("Unknown command: " + command);
                }
            }
        } catch (IOException e) {
            logger.log("Client connection error: " + e.getMessage());
        } finally {
            closeConnections();
        }
    }

    private void handleList() {
        File dir = new File(SERVER_FILES_DIR);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            output.println("No files found on server.");
        } else {
            for (File file : files) {
                if (file.isFile()) {
                    output.println(file.getName() + " (" + file.length() + " bytes)");
                }
            }
        }
        output.println("END");
    }

    private void handleUpload(String filename) {
        File file = new File(SERVER_FILES_DIR, filename);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            String line;
            while ((line = input.readLine()) != null) {
                if ("END".equals(line)) {
                    break;
                }
                writer.write(line);
                writer.newLine();
            }
            output.println("File " + filename + " uploaded successfully.");
            logger.log("File uploaded: " + filename);
            broadcastNotification("[NOTIFICATION] " + filename + " was uploaded");
        } catch (IOException e) {
            output.println("Error saving file: " + e.getMessage());
            logger.log("Error uploading file " + filename + ": " + e.getMessage());
        }
    }

    private void handleDownload(String filename) {
        File file = new File(SERVER_FILES_DIR, filename);
        if (!file.exists() || !file.isFile()) {
            output.println("Error: File not found.");
            output.println("END");
            logger.log("Download failed, file not found: " + filename);
            return;
        }

        try (BufferedReader fileReader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = fileReader.readLine()) != null) {
                output.println(line);
            }
            output.println("END");
            logger.log("File downloaded: " + filename);
        } catch (IOException e) {
            output.println("Error reading file: " + e.getMessage());
            output.println("END");
            logger.log("Error downloading file " + filename + ": " + e.getMessage());
        }
    }

    private void handleDelete(String filename) {
        File file = new File(SERVER_FILES_DIR, filename);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                output.println("File " + filename + " deleted successfully.");
                logger.log("File deleted: " + filename);
                broadcastNotification("[NOTIFICATION] " + filename + " was deleted");
            } else {
                output.println("Error: Could not delete " + filename);
                logger.log("Failed to delete file: " + filename);
            }
        } else {
            output.println("Error: File not found.");
            logger.log("Delete failed, file not found: " + filename);
        }
    }

    private void broadcastNotification(String message) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            byte[] buffer = message.getBytes();
            InetAddress address = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 5001);
            socket.send(packet);
        } catch (IOException e) {
            logger.log("Failed to send UDP broadcast: " + e.getMessage());
        }
    }

    private void closeConnections() {
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (clientSocket != null) clientSocket.close();
            logger.log("Connection closed.");
            logger.close();
        } catch (IOException e) {
            System.err.println("Error closing connections: " + e.getMessage());
        }
    }
}

//An HTTP server that serves files from the server_files directory.
class HttpFileServer implements Runnable {

    private static final int PORT = 8080;
    private static final String SERVER_FILES_DIR = "server_files";

    
    //Starts the HTTP server on port 8080.
    @Override
    public void run() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/", new RootHandler());
            server.createContext("/files/", new FileHandler());
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
        } catch (IOException e) {
            System.err.println("HTTP Server error: " + e.getMessage());
        }
    }

    
    //Handles requests to the root path (/) by listing available files.
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<html><body>\n");
            htmlBuilder.append("<pre>\n");
            htmlBuilder.append("=== NetDrive File Server ===\n");
            htmlBuilder.append("Available Files:\n");

            File dir = new File(SERVER_FILES_DIR);
            File[] files = dir.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        String filename = file.getName();
                        String link = "http://localhost:8080/files/" + filename;
                        htmlBuilder.append(String.format("- %-15s <a href=\"%s\">%s</a>\n", filename, link, link));
                    }
                }
            }

            htmlBuilder.append("</pre>\n");
            htmlBuilder.append("</body></html>\n");

            String response = htmlBuilder.toString();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            byte[] responseBytes = response.getBytes("UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }

    
    //Handles file download requests (/files/filename).
    static class FileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String filename = path.substring("/files/".length());
            File file = new File(SERVER_FILES_DIR, filename);

            if (!file.exists() || !file.isFile()) {
                String response = "404 Not Found";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            String contentType = filename.endsWith(".txt") ? "text/plain" : "application/octet-stream";
            exchange.getResponseHeaders().set("Content-Type", contentType);
            
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            exchange.sendResponseHeaders(200, fileBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        }
    }
}
