package at.gl1tchxd.battleship.network;

import at.gl1tchxd.battleship.logic.GameController;

/**
 * Example usage of the P2P networking system.
 *
 * This demonstrates how to integrate networking with your GameController.
 */
public class NetworkExample {

    /**
     * Example: Starting a game as HOST
     */
    public static void startAsHost() {
        // 1. Initialize game controller
        GameController gameController = new GameController();
        gameController.setPlayerId("Player1");
        gameController.initializeGame(10, new int[]{5, 4, 3, 3, 2});

        // 2. Create network manager
        NetworkManager networkManager = new NetworkManager(gameController);

        // 3. Start hosting on port 12345
        try {
            networkManager.startHost(12345);
            System.out.println("Hosting game on port 12345");
            System.out.println("Share your IP address with the other player!");

            // 4. Place your ships (this would be done through UI)
            // gameController.placeShip(0, 0, 0, true);
            // ... place all ships ...

            // 5. When ready, send ready signal
            networkManager.sendReady();

            // 6. After opponent is ready and it's your turn, attack
            // if (gameController.isMyTurn()) {
            //     networkManager.sendAttack(5, 5);
            //     gameController.recordAttackResult(5, 5, true); // Call this after receiving result
            // }

        } catch (Exception e) {
            System.err.println("Failed to start host: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example: Joining a game as CLIENT
     */
    public static void joinAsClient(String hostIP) {
        // 1. Initialize game controller
        GameController gameController = new GameController();
        gameController.setPlayerId("Player2");
        gameController.initializeGame(10, new int[]{5, 4, 3, 3, 2});

        // 2. Create network manager
        NetworkManager networkManager = new NetworkManager(gameController);

        // 3. Connect to host
        try {
            networkManager.connectToHost(hostIP, 12345);
            System.out.println("Connected to host at " + hostIP);

            // 4. Place your ships
            // gameController.placeShip(0, 0, 0, true);
            // ... place all ships ...

            // 5. When ready, send ready signal
            networkManager.sendReady();

            // 6. Wait for your turn, then attack
            // if (gameController.isMyTurn()) {
            //     networkManager.sendAttack(3, 7);
            //     gameController.recordAttackResult(3, 7, false); // Call this after receiving result
            // }

        } catch (Exception e) {
            System.err.println("Failed to connect: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example: Complete game flow integration with UI callbacks
     */
    public static class NetworkGameSession {
        private GameController gameController;
        private NetworkManager networkManager;

        public NetworkGameSession(String playerId) {
            gameController = new GameController();
            gameController.setPlayerId(playerId);
            gameController.initializeGame(10, new int[]{5, 4, 3, 3, 2});
            networkManager = new NetworkManager(gameController);
        }

        // Host a game
        public void hostGame(int port) throws Exception {
            networkManager.startHost(port);
        }

        // Join a game
        public void joinGame(String host, int port) throws Exception {
            networkManager.connectToHost(host, port);
        }

        // Place a ship during setup phase
        public boolean placeShip(int shipIndex, int row, int col, boolean horizontal) {
            return gameController.placeShip(shipIndex, row, col, horizontal);
        }

        // Signal ready after all ships placed
        public void ready() {
            networkManager.sendReady();
        }

        // Attack opponent (call this when player clicks enemy board)
        public void attack(int row, int col) {
            if (!gameController.isMyTurn()) {
                System.out.println("Not your turn!");
                return;
            }

            networkManager.sendAttack(row, col);

            // The result will come back via MessageHandler
            // UI should listen for result and call recordAttackResult
        }

        // Record attack result on tracking board
        public void recordResult(int row, int col, boolean hit) {
            gameController.recordAttackResult(row, col, hit);
        }

        // Check game state
        public boolean isMyTurn() {
            return gameController.isMyTurn();
        }

        public boolean isGameOver() {
            return gameController.isGameOver();
        }

        // Disconnect
        public void disconnect() {
            networkManager.disconnect();
        }

        public GameController getGameController() {
            return gameController;
        }
    }

    /**
     * Quick test - start two instances and they can play each other
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage:");
            System.out.println("  java NetworkExample host          - Start as host");
            System.out.println("  java NetworkExample join <IP>     - Join host at IP address");
            return;
        }

        String mode = args[0];

        if (mode.equals("host")) {
            startAsHost();
        } else if (mode.equals("join") && args.length > 1) {
            joinAsClient(args[1]);
        } else {
            System.out.println("Invalid arguments");
        }

        // Keep program running
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }
    }
}

