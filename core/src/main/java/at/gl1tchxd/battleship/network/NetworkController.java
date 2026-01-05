package at.gl1tchxd.battleship.network;

import at.gl1tchxd.battleship.logic.GameController;
import at.gl1tchxd.battleship.logic.GamePhase;
import com.esotericsoftware.kryonet.Connection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NetworkController {
    GameController gameController;
    private boolean isHost;
    private ServerSocket serverSocket;
    private ClientSocket clientSocket;
    private String playerId;
    private boolean handshakeComplete = false;
    private ConnectionCallback connectionCallback;
    private DisconnectionCallback disconnectionCallback;

    public interface ConnectionCallback {
        void onClientConnected();
    }

    /**
     * Called when a disconnection occurs.
     * opponentDisconnected == true -> the opponent disconnected (we remained connected)
     * opponentDisconnected == false -> we were disconnected from the opponent
     */
    public interface DisconnectionCallback {
        void onDisconnected(boolean opponentDisconnected);
    }

    public NetworkController(GameController gameController) {
        this.gameController = gameController;
        this.playerId = UUID.randomUUID().toString();
        gameController.setPlayerId(playerId);
    }

    public void hostGame(int port, int boardSize, int[] shipConfig) throws IOException {
        this.isHost = true;
        gameController.initializeGame(boardSize, shipConfig);

        serverSocket = new ServerSocket(port, new ServerSocket.MessageHandler() {
            @Override
            public void onClientConnected(Connection connection) {
                // Send game config immediately when client connects
                sendGameConfig();

                if (connectionCallback != null) {
                    connectionCallback.onClientConnected();
                }
            }

            @Override
            public void onMessageReceived(Connection connection, Message message) {
                handleMessage(message);
            }

            @Override
            public void onClientDisconnected(Connection connection) {
                // The client disconnected (opponent disconnected)
                onOpponentDisconnected(true);
            }
        });
    }

    public void joinGame(String host, int port) throws IOException {
        // Prevent joining if this instance is already hosting a server
        if (isHost || serverSocket != null) {
            throw new IOException("Cannot join a game while this instance is hosting.");
        }

        this.isHost = false;

        clientSocket = new ClientSocket(host, port, new ClientSocket.MessageHandler() {
            @Override
            public void onConnectedToServer(Connection connection) {
                sendHandshake();
            }

            @Override
            public void onMessageReceived(Connection connection, Message message) {
                handleMessage(message);
            }

            @Override
            public void onDisconnectedFromServer(Connection connection) {
                // The server (opponent) disconnected - from client's perspective, opponent left
                onOpponentDisconnected(true);
            }
        });
    }

    /**
     * Stop hosting the current server (if any) and reset hosting state.
     */
    public void stopHosting() {
        if (serverSocket != null) {
            serverSocket.stop();
            serverSocket = null;
        }
        isHost = false;
        // Reset any handshake state so a future join/host starts clean
        handshakeComplete = false;
    }

    /**
     * Stop a pending/active client connection (if any).
     */
    public void stopJoining() {
        if (clientSocket != null) {
            clientSocket.stop();
            clientSocket = null;
        }
        isHost = false;
        handshakeComplete = false;
    }

    /**
     * Set a callback to be invoked when a client connects (host) or disconnects.
     */
    public void setConnectionCallback(ConnectionCallback callback) {
        this.connectionCallback = callback;
    }

    public void setDisconnectionCallback(DisconnectionCallback callback) {
        this.disconnectionCallback = callback;
    }

    private void onOpponentDisconnected(boolean opponentDisconnected) {
        // If we're in placement phase and a disconnection occurred, notify UI to return to connect
        if (gameController.getGamePhase() == GamePhase.PLACEMENT) {
            // Close any active sockets so the ConnectScreen shows correct state
            if (isHost) {
                stopHosting();
            } else {
                stopJoining();
            }

            if (disconnectionCallback != null) {
                disconnectionCallback.onDisconnected(opponentDisconnected);
            }
            // Reset game state for the local player so the UI is clean
            gameController.resetGame();
            return;
        }

        // If in battle, decide winner/loser based on who disconnected
        if (opponentDisconnected) {
            // Opponent disconnected -> we (local player) win
            gameController.setGamePhase(GamePhase.GAME_WON);
        } else {
            // We were disconnected -> local player loses
            gameController.setGamePhase(GamePhase.GAME_LOST);
        }

        // Close any active sockets after the battle ended to clean up network resources
        if (isHost) {
            stopHosting();
        } else {
            stopJoining();
        }

        // Notify listeners regardless so UI can react (e.g., show messages)
        if (disconnectionCallback != null) {
            disconnectionCallback.onDisconnected(opponentDisconnected);
        }
    }

    private void handleMessage(Message message) {
        switch (message.messageType) {
            case HANDSHAKE:
                handleHandshake(message);
                break;
            case GAME_CONFIG:
                handleGameConfig(message);
                break;
            case PLAYER_READY:
                handlePlayerReady(message);
                break;
            case BATTLE_START:
                handleBattleStart(message);
                break;
            case ATTACK:
                handleAttack(message);
                break;
            case ATTACK_RESULT:
                handleAttackResult(message);
                break;
        }
    }

    private void handleHandshake(Message message) {
        String opponentId = (String) message.data.get("playerId");

        gameController.setOpponentId(opponentId);

        if (isHost && !handshakeComplete) {
            sendHandshake();
            handshakeComplete = true;
            // Game config already sent on connection
        } else if (!isHost && !handshakeComplete) {
            handshakeComplete = true;
        }
    }

    private void sendHandshake() {
        Map<String, Object> data = new HashMap<>();
        data.put("playerId", playerId);
        Message message = new Message(MessageType.HANDSHAKE, data);
        sendMessage(message);
        System.out.println("Sent handshake with player ID: " + playerId);
    }

    private void sendGameConfig() {
        Map<String, Object> data = new HashMap<>();
        data.put("boardSize", gameController.getGame().getBoard().getSize());
        data.put("shipConfig", gameController.getGame().getShipConfig());
        Message message = new Message(MessageType.GAME_CONFIG, data);
        sendMessage(message);
        System.out.println("Sent game config: boardSize=" + data.get("boardSize") + ", shipConfig length=" + ((int[])data.get("shipConfig")).length);
    }

    private void handleGameConfig(Message message) {
        if (!isHost) {
            System.out.println("Received game config message");
            int boardSize = (Integer) message.data.get("boardSize");
            int[] shipConfig = (int[]) message.data.get("shipConfig");
            System.out.println("Initializing game with boardSize=" + boardSize + ", shipConfig length=" + shipConfig.length);
            gameController.initializeGame(boardSize, shipConfig);
            System.out.println("Game initialized successfully");
        }
    }

    private void handlePlayerReady(Message message) {
        gameController.setOpponentReady(true);

        // If host and both players are ready, send battle start message
        if (isHost && gameController.isPlacementComplete() && gameController.isOpponentReady()) {
            sendBattleStart();
        }
    }

    private void sendBattleStart() {
        // Host always goes first
        String startingPlayerId = playerId;

        // Set locally for host
        gameController.setCurrentTurn(startingPlayerId);
        gameController.setGamePhase(GamePhase.BATTLE);

        // Send to client
        Map<String, Object> data = new HashMap<>();
        data.put("startingPlayerId", startingPlayerId);
        Message message = new Message(MessageType.BATTLE_START, data);
        sendMessage(message);

        System.out.println("Battle started! Starting player: " + startingPlayerId);
    }

    private void handleBattleStart(Message message) {
        String startingPlayerId = (String) message.data.get("startingPlayerId");
        gameController.setCurrentTurn(startingPlayerId);
        gameController.setGamePhase(GamePhase.BATTLE);

        System.out.println("Battle started! Starting player: " + startingPlayerId +
                          " (I am: " + playerId + ", my turn: " + gameController.isMyTurn() + ")");
    }

    private void handleAttack(Message message) {
        int row = (int) message.data.get("row");
        int col = (int) message.data.get("col");
        System.out.println("Received attack at (" + row + ", " + col + ")");

        GameController.AttackResult result = gameController.attackWithResult(row, col);
        System.out.println("Attack result: hit=" + result.isHit() + ", sunk=" + result.isShipSunk() + ", gameWon=" + result.isGameWon());

        Map<String, Object> data = new HashMap<>();
        data.put("isHit", result.isHit());
        data.put("isShipSunk", result.isShipSunk());
        data.put("isGameWon", result.isGameWon());
        data.put("shipsSunk", gameController.exportSunk());
        data.put("row", row);
        data.put("col", col);

        Message response = new Message(MessageType.ATTACK_RESULT, data);
        sendMessage(response);
        System.out.println("Sent attack result back");

        // After being attacked, it's our turn only if they missed (and game not over)
        if (!result.isGameWon() && !result.isHit()) {
            gameController.setCurrentTurn(playerId);
            System.out.println("They missed! It's now my turn");
        } else if (result.isHit()) {
            System.out.println("They hit! They keep their turn");
        }
    }

    private void handleAttackResult(Message message) {
        boolean isGameWon = (boolean) message.data.get("isGameWon");
        int row = (int) message.data.get("row");
        int col = (int) message.data.get("col");
        boolean isHit = (boolean) message.data.get("isHit");

        System.out.println("Received attack result for (" + row + ", " + col + "): hit=" + isHit + ", gameWon=" + isGameWon);

        if (isGameWon) {
            gameController.setGamePhase(GamePhase.GAME_WON);
        } else if (!isHit) {
            gameController.setCurrentTurn(gameController.getOpponentId());
            System.out.println("I missed! It's now opponent's turn");
        } else {
            System.out.println("I hit! I keep my turn");
        }

        gameController.recordAttackResult(row, col, isHit);
        gameController.getTrackingBoard().setTrackingSunkShips((int[][]) message.data.get("shipsSunk"));
        System.out.println("Tracking board updated");
    }

    private void sendMessage(Message message) {
        if (isHost && serverSocket != null) {
            serverSocket.sendMessage(message);
        } else if (!isHost && clientSocket != null) {
            clientSocket.sendMessage(message);
        }
    }

    public String getPlayerId() {
        return playerId;
    }

    public boolean isHandshakeComplete() {
        return handshakeComplete;
    }

    public boolean isHost() {
        return isHost;
    }


    public void sendAttack(int row, int col) {
        Map<String, Object> data = new HashMap<>();
        data.put("row", row);
        data.put("col", col);
        Message message = new Message(MessageType.ATTACK, data);
        sendMessage(message);
    }

    public void sendPlayerReady() {
        Map<String, Object> data = new HashMap<>();
        data.put("playerId", playerId);
        Message message = new Message(MessageType.PLAYER_READY, data);
        sendMessage(message);

        // If host and both players are now ready, send battle start message
        if (isHost && gameController.isPlacementComplete() && gameController.isOpponentReady()) {
            sendBattleStart();
        }
    }
}
