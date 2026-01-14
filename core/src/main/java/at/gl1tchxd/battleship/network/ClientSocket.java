package at.gl1tchxd.battleship.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.IOException;
import java.util.HashMap;

public class ClientSocket implements Socket{
    Client client;
    Kryo kryo;
    private MessageHandler messageHandler;
    private Connection serverConnection;

    public interface MessageHandler {
        void onConnectedToServer(Connection connection);
        void onMessageReceived(Connection connection, Message message);
        void onDisconnectedFromServer(Connection connection);
    }

    public ClientSocket(String host, int port, MessageHandler messageHandler) throws IOException {
        this.messageHandler = messageHandler;
        this.client = new Client();

        kryo = this.client.getKryo();
        kryo.register(Message.class);
        kryo.register(MessageType.class);
        kryo.register(java.util.HashMap.class);
        kryo.register(String.class);
        kryo.register(Integer.class);
        kryo.register(Boolean.class);
        kryo.register(int[].class);
        kryo.register(int[][].class);

        setupListener();

        this.client.start();
        this.client.connect(5000, host, port);
    }

    private void setupListener() {
        client.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                serverConnection = connection;
                if (messageHandler != null) {
                    messageHandler.onConnectedToServer(connection);
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
                serverConnection = null;
                if (messageHandler != null) {
                    messageHandler.onDisconnectedFromServer(connection);
                }
            }
        });
    }

    public void sendMessage(Message message) {
        if (serverConnection != null) {
            serverConnection.sendTCP(message);
        } else {
            client.sendTCP(message);
        }
    }

    @Override
    public Client getSocket() {
        return client;
    }

    public Connection getServerConnection() {
        return serverConnection;
    }

    public void stop() {
        try {
            if (client != null) {
                try {
                    client.close();
                } catch (Exception ignore) {
                }
                try {
                    client.stop();
                } catch (Exception ignore) {
                }
            }
        } catch (Exception e) {
        } finally {
            serverConnection = null;
            client = null;
        }
    }
}
