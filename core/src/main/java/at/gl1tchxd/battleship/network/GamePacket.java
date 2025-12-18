package at.gl1tchxd.battleship.network;

import java.io.Serializable;

/**
 * Lightweight data packet for P2P battleship game communication.
 * Contains only essential information without full Ship objects.
 */
public class GamePacket implements Serializable {
    private static final long serialVersionUID = 1L;

    private final PacketType type;
    private final String senderId;

    // Game initialization data
    private int boardSize;
    private int[] shipConfig;

    // Attack data
    private int row;
    private int col;

    // Attack result data
    private boolean hit;
    private boolean shipSunk;
    private boolean gameWon;
    private int shipLength; // Only send ship length if sunk, not the full Ship object

    // Turn management
    private String currentTurnPlayerId;

    // Chat/general message
    private String message;

    // Connection data
    private String playerId;
    private String opponentId;

    // Board state (for sync)
    private int[][] boardState;

    public GamePacket(PacketType type, String senderId) {
        this.type = type;
        this.senderId = senderId;
    }

    // Getters
    public PacketType getType() { return type; }
    public String getSenderId() { return senderId; }
    public int getBoardSize() { return boardSize; }
    public int[] getShipConfig() { return shipConfig; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public boolean isHit() { return hit; }
    public boolean isShipSunk() { return shipSunk; }
    public boolean isGameWon() { return gameWon; }
    public int getShipLength() { return shipLength; }
    public String getCurrentTurnPlayerId() { return currentTurnPlayerId; }
    public String getMessage() { return message; }
    public String getPlayerId() { return playerId; }
    public String getOpponentId() { return opponentId; }
    public int[][] getBoardState() { return boardState; }

    // Setters for building packets
    public GamePacket setBoardSize(int boardSize) {
        this.boardSize = boardSize;
        return this;
    }

    public GamePacket setShipConfig(int[] shipConfig) {
        this.shipConfig = shipConfig;
        return this;
    }

    public GamePacket setAttackCoordinates(int row, int col) {
        this.row = row;
        this.col = col;
        return this;
    }

    public GamePacket setAttackResult(boolean hit, boolean shipSunk, boolean gameWon, int shipLength) {
        this.hit = hit;
        this.shipSunk = shipSunk;
        this.gameWon = gameWon;
        this.shipLength = shipLength;
        return this;
    }

    public GamePacket setCurrentTurnPlayerId(String playerId) {
        this.currentTurnPlayerId = playerId;
        return this;
    }

    public GamePacket setMessage(String message) {
        this.message = message;
        return this;
    }

    public GamePacket setPlayerId(String playerId) {
        this.playerId = playerId;
        return this;
    }

    public GamePacket setOpponentId(String opponentId) {
        this.opponentId = opponentId;
        return this;
    }

    public GamePacket setBoardState(int[][] boardState) {
        this.boardState = boardState;
        return this;
    }

    @Override
    public String toString() {
        return "GamePacket{" +
                "type=" + type +
                ", senderId='" + senderId + '\'' +
                ", row=" + row +
                ", col=" + col +
                ", hit=" + hit +
                ", shipSunk=" + shipSunk +
                '}';
    }
}

