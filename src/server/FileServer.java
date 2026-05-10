package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The main server class for the Remote File Manager.
 */
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
