package at.gl1tchxd.battleship.network;

/**
 * Interface for listening to network events.
 * Implement this interface to handle incoming packets and connection events.
 */
public interface NetworkListener {

    /**
     * Called when a packet is received from the opponent.
     * @param packet The received game packet
     */
    void onPacketReceived(GamePacket packet);

    /**
     * Called when successfully connected to opponent.
     * @param opponentId The ID of the connected opponent
     */
    void onConnected(String opponentId);

    /**
     * Called when disconnected from opponent.
     * @param reason The reason for disconnection
     */
    void onDisconnected(String reason);

    /**
     * Called when a connection error occurs.
     * @param error The error message
     */
    void onError(String error);
}

