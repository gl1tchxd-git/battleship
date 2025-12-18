package at.gl1tchxd.battleship.network;

import at.gl1tchxd.battleship.logic.GameController;
import at.gl1tchxd.battleship.logic.GamePhase;

/**
 * Helper class that bridges NetworkManager with GameController.
 * Provides simple methods for common network operations.
 */
public class NetworkGameController implements NetworkListener {

    private final NetworkManager networkManager;
    private final GameController gameController;

    public NetworkGameController(GameController gameController) {
        this.gameController = gameController;
        this.networkManager = new NetworkManager();
        this.networkManager.setListener(this);
    }

    public NetworkGameController(GameController gameController, String playerId) {
        this.gameController = gameController;
        this.networkManager = new NetworkManager(playerId);
        this.networkManager.setListener(this);
    }

    /**
     * Get the network manager for direct access if needed.
     */
    public NetworkManager getNetworkManager() {
        return networkManager;
    }

    /**
     * Get this player's ID.
     */
    public String getPlayerId() {
        return networkManager.getPlayerId();
    }

    /**
     * Host a game on the specified port.
     */
    public void hostGame(int port) throws Exception {
        gameController.setPlayerId(networkManager.getPlayerId());
        networkManager.host(port);
    }

    /**
     * Connect to a hosted game.
     */
    public void joinGame(String host, int port) throws Exception {
        gameController.setPlayerId(networkManager.getPlayerId());
        networkManager.connect(host, port);
    }

    /**
     * Send game initialization to opponent.
     */
    public void sendGameInit(int boardSize, int[] shipConfig) {
        GamePacket packet = new GamePacket(PacketType.GAME_INIT, networkManager.getPlayerId())
                .setBoardSize(boardSize)
                .setShipConfig(shipConfig);
        networkManager.send(packet);
    }

    /**
     * Notify opponent that placement is complete.
     */
    public void sendPlacementReady() {
        GamePacket packet = new GamePacket(PacketType.PLACEMENT_READY, networkManager.getPlayerId());
        networkManager.send(packet);
    }

    /**
     * Send an attack to the opponent.
     */
    public void sendAttack(int row, int col) {
        GamePacket packet = new GamePacket(PacketType.ATTACK, networkManager.getPlayerId())
                .setAttackCoordinates(row, col);
        networkManager.send(packet);
    }

    /**
     * Send attack result back to opponent.
     */
    public void sendAttackResult(GameController.AttackResult result) {
        int shipLength = 0;
        if (result.isShipSunk() && result.getHitShip() != null) {
            shipLength = result.getHitShip().getLength();
        }

        GamePacket packet = new GamePacket(PacketType.ATTACK_RESULT, networkManager.getPlayerId())
                .setAttackResult(result.isHit(), result.isShipSunk(), result.isGameWon(), shipLength);
        networkManager.send(packet);
    }

    /**
     * Send turn change notification.
     */
    public void sendTurnChange(String newTurnPlayerId) {
        GamePacket packet = new GamePacket(PacketType.TURN_CHANGE, networkManager.getPlayerId())
                .setCurrentTurnPlayerId(newTurnPlayerId);
        networkManager.send(packet);
    }

    /**
     * Send game over notification.
     */
    public void sendGameOver(boolean won) {
        GamePacket packet = new GamePacket(PacketType.GAME_OVER, networkManager.getPlayerId())
                .setMessage(won ? "victory" : "defeat");
        networkManager.send(packet);
    }

    /**
     * Send a chat message.
     */
    public void sendChatMessage(String message) {
        GamePacket packet = new GamePacket(PacketType.CHAT_MESSAGE, networkManager.getPlayerId())
                .setMessage(message);
        networkManager.send(packet);
    }

    /**
     * Disconnect from the game.
     */
    public void disconnect() {
        networkManager.disconnect();
    }

    /**
     * Check if connected.
     */
    public boolean isConnected() {
        return networkManager.isConnected();
    }

    // NetworkListener implementation

    @Override
    public void onPacketReceived(GamePacket packet) {
        try {
            handleGamePacket(packet);
        } catch (Exception e) {
            System.err.println("Error handling packet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onConnected(String opponentId) {
        System.out.println("Connected to opponent: " + opponentId);
        gameController.setOpponentId(opponentId);
        gameController.setGamePhase(GamePhase.PLACEMENT);
        onOpponentConnected(opponentId);
    }

    @Override
    public void onDisconnected(String reason) {
        System.out.println("Disconnected: " + reason);
        onOpponentDisconnected(reason);
    }

    @Override
    public void onError(String error) {
        System.err.println("Network error: " + error);
        onNetworkError(error);
    }

    // Packet handling

    private void handleGamePacket(GamePacket packet) {
        switch (packet.getType()) {
            case GAME_INIT:
                handleGameInit(packet);
                break;
            case PLACEMENT_READY:
                handlePlacementReady(packet);
                break;
            case ATTACK:
                handleAttack(packet);
                break;
            case ATTACK_RESULT:
                handleAttackResult(packet);
                break;
            case TURN_CHANGE:
                handleTurnChange(packet);
                break;
            case GAME_OVER:
                handleGameOver(packet);
                break;
            case CHAT_MESSAGE:
                handleChatMessage(packet);
                break;
            default:
                System.out.println("Unhandled packet type: " + packet.getType());
        }
    }

    private void handleGameInit(GamePacket packet) {
        System.out.println("Received game initialization from opponent");
        gameController.initializeGame(packet.getBoardSize(), packet.getShipConfig());
        onGameInitReceived(packet.getBoardSize(), packet.getShipConfig());
    }

    private void handlePlacementReady(GamePacket packet) {
        System.out.println("Opponent is ready!");
        gameController.setOpponentReady(true);
        onOpponentReady();
    }

    private void handleAttack(GamePacket packet) {
        System.out.println("Received attack at: " + packet.getRow() + ", " + packet.getCol());

        // Process attack on our board
        GameController.AttackResult result = gameController.receiveAttack(packet.getRow(), packet.getCol());

        // Send result back
        sendAttackResult(result);

        // If we lost, notify
        if (result.isGameWon()) {
            gameController.setGamePhase(GamePhase.GAME_OVER);
        }

        onAttackReceived(packet.getRow(), packet.getCol(), result);
    }

    private void handleAttackResult(GamePacket packet) {
        System.out.println("Attack result: " + (packet.isHit() ? "HIT" : "MISS"));

        // Record result on tracking board
        gameController.recordAttackResult(packet.getRow(), packet.getCol(), packet.isHit());

        // If we won
        if (packet.isGameWon()) {
            gameController.setGamePhase(GamePhase.GAME_OVER);
        }

        onAttackResultReceived(packet.isHit(), packet.isShipSunk(), packet.isGameWon(), packet.getShipLength());
    }

    private void handleTurnChange(GamePacket packet) {
        gameController.setCurrentTurn(packet.getCurrentTurnPlayerId());
        onTurnChanged(packet.getCurrentTurnPlayerId());
    }

    private void handleGameOver(GamePacket packet) {
        gameController.setGamePhase(GamePhase.GAME_OVER);
        onGameOverReceived(packet.getMessage());
    }

    private void handleChatMessage(GamePacket packet) {
        onChatMessageReceived(packet.getSenderId(), packet.getMessage());
    }

    // Override these methods in subclasses or use listeners

    protected void onOpponentConnected(String opponentId) {}
    protected void onOpponentDisconnected(String reason) {}
    protected void onNetworkError(String error) {}
    protected void onGameInitReceived(int boardSize, int[] shipConfig) {}
    protected void onOpponentReady() {}
    protected void onAttackReceived(int row, int col, GameController.AttackResult result) {}
    protected void onAttackResultReceived(boolean hit, boolean shipSunk, boolean gameWon, int shipLength) {}
    protected void onTurnChanged(String newTurnPlayerId) {}
    protected void onGameOverReceived(String message) {}
    protected void onChatMessageReceived(String senderId, String message) {}
}

