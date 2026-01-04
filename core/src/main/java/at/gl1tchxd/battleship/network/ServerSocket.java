package at.gl1tchxd.battleship.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.util.HashMap;

public class ServerSocket implements Socket {
    Server server;
    Kryo kryo;
    private MessageHandler messageHandler;
    private Connection clientConnection;

    public interface MessageHandler {
        void onClientConnected(Connection connection);
        void onMessageReceived(Connection connection, Message message);
        void onClientDisconnected(Connection connection);
    }

    public ServerSocket(int port, MessageHandler messageHandler) throws IOException {
        this.messageHandler = messageHandler;
        this.server = new Server();

        kryo = this.server.getKryo();
        kryo.register(Message.class);
        kryo.register(MessageType.class);
        kryo.register(java.util.HashMap.class);
        kryo.register(String.class);
        kryo.register(Integer.class);
        kryo.register(Boolean.class);
        kryo.register(int[].class);
        kryo.register(int[][].class);

        setupListener();

        this.server.start();
        this.server.bind(port);
    }

    private void setupListener() {
        server.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                clientConnection = connection;
                if (messageHandler != null) {
                    messageHandler.onClientConnected(connection);
                }
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Message && messageHandler != null) {
                    messageHandler.onMessageReceived(connection, (Message) object);
                }
            }

            @Override
            public void disconnected(Connection connection) {
                System.out.println("Client disconnected");
                clientConnection = null;
                if (messageHandler != null) {
                    messageHandler.onClientDisconnected(connection);
                }
            }
        });
    }

    public void sendMessage(Message message) {
        if (clientConnection != null) {
            clientConnection.sendTCP(message);
        } else {
            server.sendToAllTCP(message);
        }
    }

    @Override
    public Server getSocket() {
        return server;
    }

    public Connection getClientConnection() {
        return clientConnection;
    }

    /**
     * Stop and close the server, disconnecting any clients and releasing ports.
     */
    public void stop() {
        try {
            if (server != null) {
                // Stop the server thread and close sockets
                server.stop();
                try {
                    server.close();
                } catch (Exception ignore) {
                    // close may throw if already closed; ignore to be robust
                }
            }
        } catch (Exception e) {
            System.out.println("Error while stopping server: " + e.getMessage());
        } finally {
            clientConnection = null;
            server = null;
        }
    }
}
