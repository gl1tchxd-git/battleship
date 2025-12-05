package at.gl1tchxd.battleship.network;

/**
 * Defines the message protocol for P2P communication.
 * Format: TYPE:param1:param2:...
 */
public class NetworkMessage {

    // Message types
    public static final String CONNECT = "CONNECT";           // CONNECT:playerId
    public static final String READY = "READY";               // READY:playerId
    public static final String ATTACK = "ATTACK";             // ATTACK:row:col
    public static final String ATTACK_RESULT = "RESULT";      // RESULT:hit:sunk:gameOver:shipName
    public static final String TURN_UPDATE = "TURN";          // TURN:playerId
    public static final String GAME_OVER = "GAME_OVER";       // GAME_OVER:winnerId
    public static final String DISCONNECT = "DISCONNECT";     // DISCONNECT:playerId
    public static final String RESET = "RESET";               // RESET

    private final String type;
    private final String[] params;

    public NetworkMessage(String type, String... params) {
        this.type = type;
        this.params = params;
    }

    /**
     * Parse a message string into a NetworkMessage object.
     */
    public static NetworkMessage parse(String message) {
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }

        String[] parts = message.split(":", -1); // -1 to include empty strings
        String type = parts[0];
        String[] params = new String[parts.length - 1];
        System.arraycopy(parts, 1, params, 0, params.length);

        return new NetworkMessage(type, params);
    }

    /**
     * Serialize this message to a string for transmission.
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder(type);
        for (String param : params) {
            sb.append(":").append(param != null ? param : "");
        }
        return sb.toString();
    }

    public String getType() {
        return type;
    }

    public String[] getParams() {
        return params;
    }

    public String getParam(int index) {
        if (index < 0 || index >= params.length) {
            return null;
        }
        return params[index];
    }

    public int getParamAsInt(int index) {
        String param = getParam(index);
        return param != null ? Integer.parseInt(param) : -1;
    }

    public boolean getParamAsBoolean(int index) {
        String param = getParam(index);
        return param != null && Boolean.parseBoolean(param);
    }

    @Override
    public String toString() {
        return serialize();
    }

    // Factory methods for common messages
    public static NetworkMessage connect(String playerId) {
        return new NetworkMessage(CONNECT, playerId);
    }

    public static NetworkMessage ready(String playerId) {
        return new NetworkMessage(READY, playerId);
    }

    public static NetworkMessage attack(int row, int col) {
        return new NetworkMessage(ATTACK, String.valueOf(row), String.valueOf(col));
    }

    public static NetworkMessage attackResult(boolean hit, boolean sunk, boolean gameOver, String shipName) {
        return new NetworkMessage(ATTACK_RESULT,
            String.valueOf(hit),
            String.valueOf(sunk),
            String.valueOf(gameOver),
            shipName != null ? shipName : "");
    }

    public static NetworkMessage turnUpdate(String playerId) {
        return new NetworkMessage(TURN_UPDATE, playerId);
    }

    public static NetworkMessage gameOver(String winnerId) {
        return new NetworkMessage(GAME_OVER, winnerId);
    }

    public static NetworkMessage disconnect(String playerId) {
        return new NetworkMessage(DISCONNECT, playerId);
    }

    public static NetworkMessage reset() {
        return new NetworkMessage(RESET);
    }
}

