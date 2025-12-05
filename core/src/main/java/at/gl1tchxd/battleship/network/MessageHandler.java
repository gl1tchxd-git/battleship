package at.gl1tchxd.battleship.network;

import at.gl1tchxd.battleship.logic.GameController;
import at.gl1tchxd.battleship.logic.GameController.AttackResult;
import at.gl1tchxd.battleship.logic.GamePhase;

/**
 * Handles incoming network messages and calls appropriate GameController methods.
 */
public class MessageHandler {
    private final GameController gameController;
    private final NetworkManager networkManager;

    public MessageHandler(GameController gameController, NetworkManager networkManager) {
        this.gameController = gameController;
        this.networkManager = networkManager;
    }

    /**
     * Process an incoming message and update game state accordingly.
     */
    public void handleMessage(NetworkMessage message) {
        System.out.println("Received: " + message);

        switch (message.getType()) {
            case NetworkMessage.CONNECT:
                handleConnect(message);
                break;

            case NetworkMessage.READY:
                handleReady(message);
                break;

            case NetworkMessage.ATTACK:
                handleAttack(message);
                break;

            case NetworkMessage.ATTACK_RESULT:
                handleAttackResult(message);
                break;

            case NetworkMessage.TURN_UPDATE:
                handleTurnUpdate(message);
                break;

            case NetworkMessage.GAME_OVER:
                handleGameOver(message);
                break;

            case NetworkMessage.DISCONNECT:
                handleDisconnect(message);
                break;

            case NetworkMessage.RESET:
                handleReset(message);
                break;

            default:
                System.err.println("Unknown message type: " + message.getType());
        }
    }

    /**
     * Handle opponent connection.
     * Message format: CONNECT:playerId
     */
    private void handleConnect(NetworkMessage message) {
        String opponentId = message.getParam(0);
        gameController.setOpponentId(opponentId);
        System.out.println("Opponent connected: " + opponentId);

        // Send back our player ID
        networkManager.sendMessage(NetworkMessage.connect(gameController.getPlayerId()));
    }

    /**
     * Handle opponent ready signal.
     * Message format: READY:playerId
     */
    private void handleReady(NetworkMessage message) {
        String opponentId = message.getParam(0);
        System.out.println("Opponent ready: " + opponentId);

        gameController.setOpponentReady(true);

        // If we're also ready, game can start
        if (gameController.isPlacementComplete()) {
            gameController.setGamePhase(GamePhase.BATTLE);
            System.out.println("Both players ready - battle begins!");
        }
    }

    /**
     * Handle incoming attack from opponent.
     * Message format: ATTACK:row:col
     */
    private void handleAttack(NetworkMessage message) {
        int row = message.getParamAsInt(0);
        int col = message.getParamAsInt(1);

        System.out.println("Opponent attacks: (" + row + ", " + col + ")");

        // Process attack on our board
        AttackResult result = gameController.receiveAttack(row, col);

        // Send result back to opponent
        String shipName = (result.getShip() != null) ? result.getShip().getName() : null;
        NetworkMessage response = NetworkMessage.attackResult(
            result.isHit(),
            result.isShipSunk(),
            result.isGameWon(),
            shipName
        );
        networkManager.sendMessage(response);

        // Check if opponent won
        if (result.isGameWon()) {
            gameController.setGamePhase(GamePhase.GAME_OVER);
            System.out.println("Game Over - Opponent wins!");
        } else {
            // Switch turn back to opponent after they get the result
            gameController.switchTurn();
            networkManager.sendMessage(NetworkMessage.turnUpdate(gameController.getOpponentId()));
        }
    }

    /**
     * Handle attack result from opponent.
     * Message format: RESULT:hit:sunk:gameOver:shipName
     */
    private void handleAttackResult(NetworkMessage message) {
        boolean hit = message.getParamAsBoolean(0);
        boolean sunk = message.getParamAsBoolean(1);
        boolean gameOver = message.getParamAsBoolean(2);
        String shipName = message.getParam(3);

        System.out.println("Attack result: " + (hit ? "HIT" : "MISS") +
                         (sunk ? " - Ship sunk: " + shipName : ""));

        // Note: You'll need to store the last attack coordinates to record the result
        // This is a simplified version - you may need to add lastAttackRow/Col tracking
        // For now, the UI layer should handle this by calling recordAttackResult directly

        if (gameOver) {
            gameController.setGamePhase(GamePhase.GAME_OVER);
            System.out.println("Game Over - You win!");
        }
    }

    /**
     * Handle turn update.
     * Message format: TURN:playerId
     */
    private void handleTurnUpdate(NetworkMessage message) {
        String turnPlayerId = message.getParam(0);
        gameController.setCurrentTurn(turnPlayerId);

        if (gameController.isMyTurn()) {
            System.out.println("It's your turn!");
        } else {
            System.out.println("Opponent's turn");
        }
    }

    /**
     * Handle game over message.
     * Message format: GAME_OVER:winnerId
     */
    private void handleGameOver(NetworkMessage message) {
        String winnerId = message.getParam(0);
        gameController.setGamePhase(GamePhase.GAME_OVER);

        if (winnerId.equals(gameController.getPlayerId())) {
            System.out.println("You won the game!");
        } else {
            System.out.println("You lost the game.");
        }
    }

    /**
     * Handle opponent disconnect.
     * Message format: DISCONNECT:playerId
     */
    private void handleDisconnect(NetworkMessage message) {
        String playerId = message.getParam(0);
        System.out.println("Opponent disconnected: " + playerId);
        // UI should handle this by showing a disconnect message
    }

    /**
     * Handle game reset.
     * Message format: RESET
     */
    private void handleReset(NetworkMessage message) {
        System.out.println("Game reset requested");
        gameController.resetGame();
    }
}
