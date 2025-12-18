package at.gl1tchxd.battleship.network;

import at.gl1tchxd.battleship.logic.GameController;

/**
 * Example usage of the networking code for P2P battleship game.
 *
 * This example demonstrates how to:
 * 1. Set up a host or join a game
 * 2. Handle network events
 * 3. Send and receive attacks
 * 4. Manage game flow over network
 */
public class NetworkUsageExample {

    public static void main(String[] args) {
        // Example 1: Host a game
        exampleHostGame();

        // Example 2: Join a game
        // exampleJoinGame();
    }

    /**
     * Example: How to host a game and handle network events
     */
    public static void exampleHostGame() {
        // Create game controller
        GameController gameController = new GameController();

        // Create network game controller with custom event handlers
        NetworkGameController networkController = new NetworkGameController(gameController) {
            @Override
            protected void onOpponentConnected(String opponentId) {
                System.out.println("Opponent connected: " + opponentId);

                // Initialize the game and send config to opponent
                int boardSize = 10;
                int[] shipConfig = {1, 2, 1, 1, 1}; // 1x5-ship, 2x4-ships, etc.
                gameController.initializeGame(boardSize, shipConfig);
                sendGameInit(boardSize, shipConfig);
            }

            @Override
            protected void onGameInitReceived(int boardSize, int[] shipConfig) {
                // Opponent sent their game init (should match ours in P2P)
                System.out.println("Game initialized: " + boardSize + "x" + boardSize);
            }

            @Override
            protected void onOpponentReady() {
                System.out.println("Opponent has finished placement!");
                // Start battle phase - determine who goes first
                if (Math.random() > 0.5) {
                    gameController.setCurrentTurn(getPlayerId());
                    sendTurnChange(getPlayerId());
                } else {
                    gameController.setCurrentTurn(getNetworkManager().getOpponentId());
                    sendTurnChange(getNetworkManager().getOpponentId());
                }
            }

            @Override
            protected void onAttackReceived(int row, int col, GameController.AttackResult result) {
                System.out.println("Enemy attacked " + row + "," + col + " - " +
                    (result.isHit() ? "HIT!" : "MISS"));

                if (result.isGameWon()) {
                    System.out.println("You lost the game!");
                }
            }

            @Override
            protected void onAttackResultReceived(boolean hit, boolean shipSunk, boolean gameWon, int shipLength) {
                System.out.println("Your attack result: " + (hit ? "HIT" : "MISS"));
                if (shipSunk) {
                    System.out.println("You sunk a ship of length " + shipLength + "!");
                }
                if (gameWon) {
                    System.out.println("You won the game!");
                }
            }

            @Override
            protected void onChatMessageReceived(String senderId, String message) {
                System.out.println("[" + senderId + "]: " + message);
            }

            @Override
            protected void onOpponentDisconnected(String reason) {
                System.out.println("Opponent disconnected: " + reason);
            }
        };

        try {
            // Host on port 8888
            networkController.hostGame(8888);
            System.out.println("Hosting game on port 8888...");
            System.out.println("Your player ID: " + networkController.getPlayerId());

            // Wait for opponent and game to proceed...
            // In a real game, this would be handled by your game loop/UI

        } catch (Exception e) {
            System.err.println("Failed to host game: " + e.getMessage());
        }
    }

    /**
     * Example: How to join a hosted game
     */
    public static void exampleJoinGame() {
        GameController gameController = new GameController();

        NetworkGameController networkController = new NetworkGameController(gameController) {
            @Override
            protected void onOpponentConnected(String opponentId) {
                System.out.println("Connected to host: " + opponentId);
            }

            @Override
            protected void onGameInitReceived(int boardSize, int[] shipConfig) {
                // Host sent game configuration
                System.out.println("Received game config from host");
                // Game is automatically initialized by NetworkGameController
            }
        };

        try {
            // Connect to host at localhost:8888
            networkController.joinGame("localhost", 8888);
            System.out.println("Joining game at localhost:8888...");
            System.out.println("Your player ID: " + networkController.getPlayerId());

        } catch (Exception e) {
            System.err.println("Failed to join game: " + e.getMessage());
        }
    }

    /**
     * Example: Complete game flow for making an attack
     */
    public static void exampleAttackFlow(NetworkGameController networkController, GameController gameController) {
        // Check if it's your turn
        if (gameController.isMyTurn()) {
            // Make an attack (example: row 5, col 3)
            int row = 5;
            int col = 3;

            // Send attack to opponent
            networkController.sendAttack(row, col);
            System.out.println("Attacking " + row + "," + col);

            // The opponent will receive the attack, process it, and send back a result
            // When you receive the result, it will trigger onAttackResultReceived()
        } else {
            System.out.println("Not your turn!");
        }
    }

    /**
     * Example: Placement phase completion
     */
    public static void examplePlacementFlow(NetworkGameController networkController, GameController gameController) {
        // After player places all ships
        // Place ships using gameController.placeShip(...)

        // When done, notify opponent
        gameController.confirmPlacement(); // Sets local ready flag
        networkController.sendPlacementReady(); // Tells opponent we're ready

        // When both players are ready, battle phase starts
        if (gameController.isPlacementComplete() && gameController.isOpponentReady()) {
            System.out.println("Both players ready - battle begins!");
        }
    }
}

