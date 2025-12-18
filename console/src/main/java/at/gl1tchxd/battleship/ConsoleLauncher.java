package at.gl1tchxd.battleship;

import at.gl1tchxd.battleship.logic.GameController;
import at.gl1tchxd.battleship.network.NetworkController;

import java.util.Arrays;

public class ConsoleLauncher {

    private static final int DEFAULT_PORT = 8888;
    private static final int DEFAULT_BOARD_SIZE = 10;
    private static final int[] DEFAULT_SHIP_CONFIG = {1, 1, 1, 1, 1};
    private static final String[] SHIP_CLASS_NAMES = {
        "carrier (length 5)",
        "battleship (length 4)",
        "cruiser (length 3)",
        "submarine (length 3)",
        "destroyer (length 2)"
    };

    public static void main(String[] args) {
        System.out.println("Welcome to the Battleship console launcher.");
        String command = promptCommand();

        if ("host".equals(command)) {
            int port = promptIntWithDefault("Port to host on", DEFAULT_PORT);
            int boardSize = promptIntWithDefault("Board size", DEFAULT_BOARD_SIZE);
            int[] shipConfig = promptShipConfig();
            exampleHostGame(port, boardSize, shipConfig);

        } else {
            String host = promptLineWithDefault("Host to connect to", "localhost");
            int port = promptIntWithDefault("Port to connect to", DEFAULT_PORT);
            exampleJoinGame(host, port);
        }
    }

    private static String promptCommand() {
        System.out.println("Type 'host' to create a game or 'join' to connect to one.");
        while (true) {
            System.out.print("> ");
            String input = SavitchIn.readLineWord().trim().toLowerCase();
            if ("host".equals(input) || "join".equals(input)) {
                return input;
            }
            System.out.println("Please enter either 'host' or 'join'.");
        }
    }

    private static String promptLineWithDefault(String prompt, String defaultValue) {
        System.out.println(prompt + " [" + defaultValue + "]: ");
        String input = SavitchIn.readLine();
        if (input == null) {
            return defaultValue;
        }
        input = input.trim();
        if (input.isEmpty()) {
            return defaultValue;
        }
        return input;
    }

    private static int promptIntWithDefault(String prompt, int defaultValue) {
        while (true) {
            System.out.println(prompt + " [" + defaultValue + "]: ");
            String input = SavitchIn.readLine();
            if (input == null) {
                return defaultValue;
            }
            input = input.trim();
            if (input.isEmpty()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Enter a whole number (e.g., 42). Please try again.");
            }
        }
    }

    private static int[] promptShipConfig() {
        String defaultDisplay = Arrays.toString(DEFAULT_SHIP_CONFIG);
        System.out.println("Enter the number of ships for each class in order: carrier (5), battleship (4), cruiser (3), submarine (3), destroyer (2)");
        System.out.println("Separate values with spaces or commas (default " + defaultDisplay + ").");
        while (true) {
            String input = SavitchIn.readLine();
            if (input == null) {
                return DEFAULT_SHIP_CONFIG.clone();
            }
            input = input.trim();
            if (input.isEmpty()) {
                return DEFAULT_SHIP_CONFIG.clone();
            }
            String normalized = input.replaceAll("[\\[\\](){}]", " ");
            normalized = normalized.replace(',', ' ');
            String[] tokens = normalized.trim().split("\\s+");
            if (tokens.length != SHIP_CLASS_NAMES.length) {
                System.out.println("Enter exactly " + SHIP_CLASS_NAMES.length + " numbers.");
                continue;
            }
            int[] config = new int[tokens.length];
            boolean valid = true;
            for (int i = 0; i < tokens.length; i++) {
                try {
                    config[i] = Integer.parseInt(tokens[i]);
                    if (config[i] < 0) {
                        throw new NumberFormatException("negative");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Count for " + SHIP_CLASS_NAMES[i] + " must be a non-negative whole number.");
                    valid = false;
                    break;
                }
            }
            if (valid) {
                return config;
            }
        }
    }

    /**
     * Example: How to host a game and handle network events
     */
    public static void exampleHostGame(int port, int boardSize, int[] shipConfig) {
        GameController gameController = new GameController();

        NetworkController networkController = new NetworkController(gameController) {
            @Override
            protected void onOpponentConnected(String opponentId) {
                System.out.println("Opponent connected: " + opponentId);

                gameController.initializeGame(boardSize, shipConfig);
                sendGameInit(boardSize, shipConfig);
            }

            @Override
            protected void onGameInitReceived(int boardSize, int[] shipConfig) {
                System.out.println("Game initialized: " + boardSize + "x" + boardSize);
            }

            @Override
            protected void onOpponentReady() {
                System.out.println("Opponent has finished placement!");
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
            networkController.hostGame(port);
            System.out.println("Hosting game on port " + port + "...");
            System.out.println("Your player ID: " + networkController.getPlayerId());
        } catch (Exception e) {
            System.err.println("Failed to host game: " + e.getMessage());
        }
    }

    /**
     * Example: How to join a hosted game
     */
    public static void exampleJoinGame(String host, int port) {
        GameController gameController = new GameController();

        NetworkController networkController = new NetworkController(gameController) {
            @Override
            protected void onOpponentConnected(String opponentId) {
                System.out.println("Connected to host: " + opponentId);
            }

            @Override
            protected void onGameInitReceived(int boardSize, int[] shipConfig) {
                System.out.println("Received game config from host");
            }
        };

        try {
            networkController.joinGame(host, port);
            System.out.println("Joining game at " + host + ":" + port + "...");
            System.out.println("Your player ID: " + networkController.getPlayerId());
        } catch (Exception e) {
            System.err.println("Failed to join game: " + e.getMessage());
        }
    }

    /**
     * Example: Complete game flow for making an attack
     */
    public static void exampleAttackFlow(NetworkController networkController, GameController gameController) {
        if (gameController.isMyTurn()) {
            int row = 5;
            int col = 3;
            networkController.sendAttack(row, col);
            System.out.println("Attacking " + row + "," + col);
        } else {
            System.out.println("Not your turn!");
        }
    }

    /**
     * Example: Placement phase completion
     */
    public static void examplePlacementFlow(NetworkController networkController, GameController gameController) {
        gameController.confirmPlacement();
        networkController.sendPlacementReady();

        if (gameController.isPlacementComplete() && gameController.isOpponentReady()) {
            System.out.println("Both players ready - battle begins!");
        }
    }
}
