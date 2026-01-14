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
    private AttackSoundCallback attackSoundCallback;

    public interface ConnectionCallback {
        void onClientConnected();
    }

    public interface DisconnectionCallback {
        void onDisconnected(boolean opponentDisconnected);
    }

    public interface AttackSoundCallback {
        void onAttackSound(boolean hit);
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
                onOpponentDisconnected(true);
            }
        });
    }

    public void joinGame(String host, int port) throws IOException {
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
                onOpponentDisconnected(true);
            }
        });
    }

    public void stopHosting() {
        if (serverSocket != null) {
            serverSocket.stop();
            serverSocket = null;
        }
        isHost = false;
        handshakeComplete = false;
    }

    public void stopJoining() {
        if (clientSocket != null) {
            clientSocket.stop();
            clientSocket = null;
        }
        isHost = false;
        handshakeComplete = false;
    }

    public void setConnectionCallback(ConnectionCallback callback) {
        this.connectionCallback = callback;
    }

    public void setDisconnectionCallback(DisconnectionCallback callback) {
        this.disconnectionCallback = callback;
    }

    public void setAttackSoundCallback(AttackSoundCallback callback) {
        this.attackSoundCallback = callback;
    }

    private void onOpponentDisconnected(boolean opponentDisconnected) {
        if (gameController.getGamePhase() == GamePhase.PLACEMENT) {
            if (isHost) {
                stopHosting();
            } else {
                stopJoining();
            }

            if (disconnectionCallback != null) {
                disconnectionCallback.onDisconnected(opponentDisconnected);
            }
            gameController.resetGame();
            return;
        }

        if (opponentDisconnected) {
            gameController.setGamePhase(GamePhase.GAME_WON);
        } else {
            gameController.setGamePhase(GamePhase.GAME_LOST);
        }

        if (isHost) {
            stopHosting();
        } else {
            stopJoining();
        }

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
        } else if (!isHost && !handshakeComplete) {
            handshakeComplete = true;
        }
    }

    private void sendHandshake() {
        Map<String, Object> data = new HashMap<>();
        data.put("playerId", playerId);
        Message message = new Message(MessageType.HANDSHAKE, data);
        sendMessage(message);
    }

    private void sendGameConfig() {
        Map<String, Object> data = new HashMap<>();
        data.put("boardSize", gameController.getGame().getBoard().getSize());
        data.put("shipConfig", gameController.getGame().getShipConfig());
        Message message = new Message(MessageType.GAME_CONFIG, data);
        sendMessage(message);
    }

    private void handleGameConfig(Message message) {
        if (!isHost) {
            int boardSize = (Integer) message.data.get("boardSize");
            int[] shipConfig = (int[]) message.data.get("shipConfig");
            gameController.initializeGame(boardSize, shipConfig);
        }
    }

    private void handlePlayerReady(Message message) {
        gameController.setOpponentReady(true);

        if (isHost && gameController.isPlacementComplete() && gameController.isOpponentReady()) {
            sendBattleStart();
        }
    }

    private void sendBattleStart() {
        String startingPlayerId = playerId;

        gameController.setCurrentTurn(startingPlayerId);
        gameController.setGamePhase(GamePhase.BATTLE);

        Map<String, Object> data = new HashMap<>();
        data.put("startingPlayerId", startingPlayerId);
        Message message = new Message(MessageType.BATTLE_START, data);
        sendMessage(message);
    }

    private void handleBattleStart(Message message) {
        String startingPlayerId = (String) message.data.get("startingPlayerId");
        gameController.setCurrentTurn(startingPlayerId);
        gameController.setGamePhase(GamePhase.BATTLE);
    }

    private void handleAttack(Message message) {
        int row = (int) message.data.get("row");
        int col = (int) message.data.get("col");

        GameController.AttackResult result = gameController.attackWithResult(row, col);

        Map<String, Object> data = new HashMap<>();
        data.put("isHit", result.isHit());
        data.put("isShipSunk", result.isShipSunk());
        data.put("isGameWon", result.isGameWon());
        data.put("shipsSunk", gameController.exportSunk());
        data.put("row", row);
        data.put("col", col);

        Message response = new Message(MessageType.ATTACK_RESULT, data);
        sendMessage(response);

        if (!result.isGameWon() && !result.isHit()) {
            gameController.setCurrentTurn(playerId);
        }
    }

    private void handleAttackResult(Message message) {
        boolean isGameWon = (boolean) message.data.get("isGameWon");
        int row = (int) message.data.get("row");
        int col = (int) message.data.get("col");
        boolean isHit = (boolean) message.data.get("isHit");

        boolean alreadyAttacked = false;
        if (gameController.getTrackingBoard() != null) {
            String cellInfo = gameController.getTrackingBoard().getCellInfo(row, col);
            alreadyAttacked = cellInfo.equals("HIT") || cellInfo.equals("MISS");
        }

        if (!alreadyAttacked && attackSoundCallback != null) {
            attackSoundCallback.onAttackSound(isHit);
        }

        if (isGameWon) {
            gameController.setGamePhase(GamePhase.GAME_WON);
        } else if (!isHit) {
            gameController.setCurrentTurn(gameController.getOpponentId());
        }

        gameController.recordAttackResult(row, col, isHit);
        gameController.getTrackingBoard().setTrackingSunkShips((int[][]) message.data.get("shipsSunk"));
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

        if (isHost && gameController.isPlacementComplete() && gameController.isOpponentReady()) {
            sendBattleStart();
        }
    }
}
