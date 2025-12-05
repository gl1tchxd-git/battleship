package at.gl1tchxd.battleship.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Background thread that listens for incoming network messages.
 */
public class NetworkListener extends Thread {
    private final BufferedReader reader;
    private final MessageHandler messageHandler;
    private volatile boolean running = true;

    public NetworkListener(BufferedReader reader, MessageHandler messageHandler) {
        this.reader = reader;
        this.messageHandler = messageHandler;
        this.setDaemon(true); // Thread dies when main program exits
        this.setName("NetworkListener");
    }

    @Override
    public void run() {
        System.out.println("Network listener started");

        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                try {
                    NetworkMessage message = NetworkMessage.parse(line);
                    messageHandler.handleMessage(message);
                } catch (Exception e) {
                    System.err.println("Error parsing message: " + line);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Connection lost: " + e.getMessage());
            }
        } finally {
            System.out.println("Network listener stopped");
        }
    }

    /**
     * Stop listening for messages.
     */
    public void stopListening() {
        running = false;
        this.interrupt();
    }

    public boolean isRunning() {
        return running;
    }
}

