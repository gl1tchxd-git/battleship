package at.gl1tchxd.battleship.network;

import at.gl1tchxd.battleship.logic.GameController;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Main networking class that handles P2P TCP connections.
 * Can act as either host (server) or client.
 */
public class NetworkManager {
    private Socket socket;
    private ServerSocket serverSocket;
    private BufferedReader reader;
    private PrintWriter writer;
    private NetworkListener listener;
    private MessageHandler messageHandler;
    private GameController gameController;

    private boolean isHost = false;
    private boolean connected = false;

    public NetworkManager(GameController gameController) {
        this.gameController = gameController;
        this.messageHandler = new MessageHandler(gameController, this);
    }

    /**
     * Start as host - wait for another player to connect.
     * @param port The port to listen on (e.g., 12345)
     * @return true if successfully started listening
     */
    public boolean startHost(int port) throws IOException {
        System.out.println("Starting host on port " + port + "...");

        serverSocket = new ServerSocket(port);
        isHost = true;

        System.out.println("Waiting for opponent to connect...");
        socket = serverSocket.accept(); // Blocks until client connects

        setupConnection();

        System.out.println("Opponent connected from: " + socket.getInetAddress().getHostAddress());

        // Send our player ID
        sendMessage(NetworkMessage.connect(gameController.getPlayerId()));

        return true;
    }

    /**
     * Connect to a host as client.
     * @param host The IP address or hostname of the host (e.g., "192.168.1.100" or "localhost")
     * @param port The port to connect to (e.g., 12345)
     * @return true if successfully connected
     */
    public boolean connectToHost(String host, int port) throws IOException {
        System.out.println("Connecting to " + host + ":" + port + "...");

        socket = new Socket(host, port);
        isHost = false;

        setupConnection();

        System.out.println("Connected to host!");

        // Send our player ID
        sendMessage(NetworkMessage.connect(gameController.getPlayerId()));

        return true;
    }

    /**
     * Setup input/output streams and start listening thread.
     */
    private void setupConnection() throws IOException {
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true); // auto-flush

        listener = new NetworkListener(reader, messageHandler);
        listener.start();

        connected = true;
    }

    /**
     * Send a message to the opponent.
     */
    public void sendMessage(NetworkMessage message) {
        if (!connected || writer == null) {
            System.err.println("Cannot send message - not connected");
            return;
        }

        System.out.println("Sending: " + message);
        writer.println(message.serialize());
    }

    /**
     * Send attack to opponent.
     */
    public void sendAttack(int row, int col) {
        sendMessage(NetworkMessage.attack(row, col));
    }

    /**
     * Send ready signal after ship placement is complete.
     */
    public void sendReady() {
        gameController.confirmPlacement();
        sendMessage(NetworkMessage.ready(gameController.getPlayerId()));
    }

    /**
     * Close the connection and cleanup resources.
     */
    public void disconnect() {
        if (connected) {
            sendMessage(NetworkMessage.disconnect(gameController.getPlayerId()));
        }

        connected = false;

        if (listener != null) {
            listener.stopListening();
        }

        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }

        System.out.println("Disconnected");
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public boolean isHost() {
        return isHost;
    }

    public String getOpponentAddress() {
        if (socket != null) {
            return socket.getInetAddress().getHostAddress();
        }
        return null;
    }
}

