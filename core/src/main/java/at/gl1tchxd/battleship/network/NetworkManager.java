package at.gl1tchxd.battleship.network;

import java.io.*;
import java.net.*;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Simple P2P network manager for battleship game.
 * Handles TCP socket connections between two players.
 */
public class NetworkManager {

    private final String playerId;
    private NetworkListener listener;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ServerSocket serverSocket;

    private Thread serverThread;
    private Thread receiveThread;
    private BlockingQueue<GamePacket> sendQueue;
    private Thread sendThread;

    private volatile boolean running = false;
    private String opponentId;

    /**
     * Create a new NetworkManager with a random player ID.
     */
    public NetworkManager() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Create a new NetworkManager with a specific player ID.
     * @param playerId The player's unique identifier
     */
    public NetworkManager(String playerId) {
        this.playerId = playerId;
        this.sendQueue = new LinkedBlockingQueue<>();
    }

    /**
     * Set the network listener for receiving events.
     */
    public void setListener(NetworkListener listener) {
        this.listener = listener;
    }

    /**
     * Get this player's ID.
     */
    public String getPlayerId() {
        return playerId;
    }

    /**
     * Get the opponent's ID.
     */
    public String getOpponentId() {
        return opponentId;
    }

    /**
     * Start hosting a game on the specified port.
     * @param port The port to listen on
     */
    public void host(int port) throws IOException {
        if (running) {
            throw new IllegalStateException("Already connected or hosting");
        }

        serverSocket = new ServerSocket(port);
        running = true;

        serverThread = new Thread(() -> {
            try {
                System.out.println("Waiting for opponent to connect on port " + port + "...");
                socket = serverSocket.accept();
                System.out.println("Opponent connected from: " + socket.getInetAddress());

                setupStreams();
                startCommunication();

                // Send connection accept
                GamePacket connectPacket = new GamePacket(PacketType.CONNECT_ACCEPT, playerId);
                send(connectPacket);

            } catch (IOException e) {
                if (running) {
                    notifyError("Host error: " + e.getMessage());
                }
            }
        }, "Server-Thread");

        serverThread.start();
    }

    /**
     * Connect to a hosted game.
     * @param host The host address
     * @param port The host port
     */
    public void connect(String host, int port) throws IOException {
        if (running) {
            throw new IllegalStateException("Already connected or hosting");
        }

        running = true;

        try {
            System.out.println("Connecting to " + host + ":" + port + "...");
            socket = new Socket(host, port);
            System.out.println("Connected to host!");

            setupStreams();
            startCommunication();

            // Send connection request
            GamePacket connectPacket = new GamePacket(PacketType.CONNECT_REQUEST, playerId);
            send(connectPacket);

        } catch (IOException e) {
            running = false;
            throw e;
        }
    }

    /**
     * Setup input/output streams for the socket.
     */
    private void setupStreams() throws IOException {
        // Output stream must be created BEFORE input stream
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
    }

    /**
     * Start the send and receive threads.
     */
    private void startCommunication() {
        // Send thread
        sendThread = new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    GamePacket packet = sendQueue.take();
                    out.writeObject(packet);
                    out.flush();
                    out.reset(); // Prevent memory leak from caching
                } catch (InterruptedException e) {
                    break;
                } catch (IOException e) {
                    if (running) {
                        notifyError("Send error: " + e.getMessage());
                        disconnect();
                    }
                    break;
                }
            }
        }, "Send-Thread");
        sendThread.start();

        // Receive thread
        receiveThread = new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    Object obj = in.readObject();
                    if (obj instanceof GamePacket) {
                        GamePacket packet = (GamePacket) obj;
                        handlePacket(packet);
                    }
                } catch (EOFException | SocketException e) {
                    if (running) {
                        notifyDisconnected("Connection closed");
                        disconnect();
                    }
                    break;
                } catch (IOException | ClassNotFoundException e) {
                    if (running) {
                        notifyError("Receive error: " + e.getMessage());
                        disconnect();
                    }
                    break;
                }
            }
        }, "Receive-Thread");
        receiveThread.start();
    }

    /**
     * Handle incoming packets.
     */
    private void handlePacket(GamePacket packet) {
        // Handle connection packets
        if (packet.getType() == PacketType.CONNECT_REQUEST ||
            packet.getType() == PacketType.CONNECT_ACCEPT) {
            opponentId = packet.getSenderId();
            notifyConnected(opponentId);
        } else if (packet.getType() == PacketType.DISCONNECT) {
            notifyDisconnected("Opponent disconnected");
            disconnect();
            return;
        }

        // Forward to listener
        if (listener != null) {
            listener.onPacketReceived(packet);
        }
    }

    /**
     * Send a packet to the opponent.
     * @param packet The packet to send
     */
    public void send(GamePacket packet) {
        if (!running) {
            System.err.println("Cannot send packet: not connected");
            return;
        }
        sendQueue.offer(packet);
    }

    /**
     * Check if connected to an opponent.
     */
    public boolean isConnected() {
        return running && socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Disconnect from the opponent.
     */
    public void disconnect() {
        if (!running) {
            return;
        }

        running = false;

        // Send disconnect packet if possible
        try {
            if (out != null) {
                GamePacket disconnectPacket = new GamePacket(PacketType.DISCONNECT, playerId);
                out.writeObject(disconnectPacket);
                out.flush();
            }
        } catch (IOException e) {
            // Ignore, we're disconnecting anyway
        }

        // Close threads
        if (sendThread != null) sendThread.interrupt();
        if (receiveThread != null) receiveThread.interrupt();
        if (serverThread != null) serverThread.interrupt();

        // Close streams and sockets
        closeQuietly(in);
        closeQuietly(out);
        closeQuietly(socket);
        closeQuietly(serverSocket);

        in = null;
        out = null;
        socket = null;
        serverSocket = null;
        opponentId = null;
        sendQueue.clear();

        System.out.println("Disconnected");
    }

    /**
     * Close a closeable resource without throwing exceptions.
     */
    private void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    // Notification methods
    private void notifyConnected(String opponentId) {
        if (listener != null) {
            listener.onConnected(opponentId);
        }
    }

    private void notifyDisconnected(String reason) {
        if (listener != null) {
            listener.onDisconnected(reason);
        }
    }

    private void notifyError(String error) {
        if (listener != null) {
            listener.onError(error);
        }
    }
}

