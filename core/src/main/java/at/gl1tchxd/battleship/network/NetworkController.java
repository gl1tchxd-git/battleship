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
            public void onClientConnected(Connection connection) {}

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
            sendGameConfig();
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
    }

    private void handleAttack(Message message) {
        GameController.AttackResult result = gameController.attackWithResult((int) message.data.get("row"), (int) message.data.get("col"));
        Map<String, Object> data = new HashMap<>();
        data.put("isHit", result.isHit());
        data.put("isShipSunk", result.isShipSunk());
        data.put("isGameWon", result.isGameWon());
        data.put("shipsSunk", gameController.exportSunk());

        Message response = new Message(MessageType.ATTACK_RESULT, data);
        sendMessage(response);
    }

    private void handleAttackResult(Message message) {
        gameController.setGamePhase(GamePhase.GAME_WON);
        gameController.recordAttackResult((int) message.data.get("row"), (int) message.data.get("col"), (boolean) message.data.get("isHit"));
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

    protected void onOpponentDisconnected() {
        gameController.setGamePhase(GamePhase.GAME_WON);
    }

    // Additional helper methods for sending game messages
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
    }
}
