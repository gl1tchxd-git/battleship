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

    public interface ConnectionCallback {
        void onClientConnected();
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
                onOpponentDisconnected();
            }
        });
    }

    public void joinGame(String host, int port) throws IOException {
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
                onOpponentDisconnected();
            }
        });
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

    public void setConnectionCallback(ConnectionCallback callback) {
        this.connectionCallback = callback;
    }

    protected void onOpponentDisconnected() {
        gameController.setGamePhase(GamePhase.GAME_WON);
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
