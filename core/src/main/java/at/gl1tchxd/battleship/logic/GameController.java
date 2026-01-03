package at.gl1tchxd.battleship.logic;

import java.util.Map;

public class GameController {
    private Game game;
    private Board myBoard;
    private Board trackingBoard;

    private String playerId;
    private String opponentId;
    private String currentTurnPlayerId;
    private boolean placementComplete = false;
    private boolean opponentReady = false;
    private GamePhase currentPhase = GamePhase.WAITING_FOR_OPPONENT;

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setOpponentId(String opponentId) {
        this.opponentId = opponentId;
    }

    public String getOpponentId() {
        return opponentId;
    }

    public boolean isMyTurn() {
        return playerId != null && playerId.equals(currentTurnPlayerId);
    }

    public void setCurrentTurn(String playerId) {
        this.currentTurnPlayerId = playerId;
    }

    public void switchTurn() {
        if (currentTurnPlayerId == null) return;
        currentTurnPlayerId = currentTurnPlayerId.equals(playerId) ? opponentId : playerId;
    }

    public Board getMyBoard() {
        return myBoard;
    }

    public Board getTrackingBoard() {
        return trackingBoard;
    }

    public void recordAttackResult(int row, int col, boolean hit) {
        if (trackingBoard == null) throw new IllegalStateException("Tracking board not initialized");
        trackingBoard.markCell(row, col, hit);
    }

    public boolean isPlacementComplete() {
        return placementComplete;
    }

    public void confirmPlacement() {
        if (game == null) throw new IllegalStateException("Game not initialized");
        placementComplete = true;
        tryStartBattle();
    }

    public boolean isOpponentReady() {
        return opponentReady;
    }

    public void setOpponentReady(boolean ready) {
        this.opponentReady = ready;
        tryStartBattle();
    }

    private void tryStartBattle() {
        if (placementComplete && opponentReady) {
            currentPhase = GamePhase.BATTLE;
        }
    }

    public GamePhase getGamePhase() {
        return currentPhase;
    }

    public void setGamePhase(GamePhase phase) {
        this.currentPhase = phase;
    }

    public int[][] exportBoardState() {
        if (myBoard == null) return null;

        int size = myBoard.getSize();
        int[][] state = new int[size][size];

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                String cellInfo = myBoard.getCellInfoForOpponent(r, c);
                switch (cellInfo) {
                    case "EMPTY": state[r][c] = 0; break;
                    case "MISS":  state[r][c] = 1; break;
                    case "HIT":   state[r][c] = 2; break;
                    default:      state[r][c] = 0; break;
                }
            }
        }

        return state;
    }

    public Map<Integer, Ship[]> exportFleet() {
        if (game == null) return null;
        return game.getFleet();
    }

    public void resetGame() {
        if (myBoard != null) myBoard.clear();
        if (trackingBoard != null) trackingBoard.clear();
        placementComplete = false;
        opponentReady = false;
        currentPhase = GamePhase.PLACEMENT;
        currentTurnPlayerId = null;
        game = null;
    }

    public void initializeGame(int boardSize, int[] shipConfig) {
        try {
            game = new Game(boardSize, shipConfig);
            myBoard = game.getBoard();
            trackingBoard = new Board(boardSize, true);
            currentPhase = GamePhase.PLACEMENT;
        } catch (IllegalArgumentException e) {
            game = null;
            throw e;
        }
    }

    public boolean placeShip(int index, int row, int col, boolean horizontal) {
        if (game == null) throw new IllegalStateException("Game not initialized");
        return game.placeShip(index, row, col, horizontal);
    }

    public AttackResult attackWithResult(int row, int col) {
        if (myBoard == null) throw new IllegalStateException("My board not initialized");

        boolean hit = myBoard.attack(row, col);
        Ship hitShip = null;
        boolean shipSunk = false;

        if (hit) {
            hitShip = myBoard.getShipAt(row, col);
            if (hitShip != null && hitShip.isSunk()) {
                shipSunk = true;
            }
        }

        boolean gameWon = isGameOver();
        if (gameWon) {
            currentPhase = GamePhase.GAME_LOST;
        }

        return new AttackResult(hit, shipSunk, gameWon);
    }

    public boolean isGameOver() {
        if (game == null) return false;

        Map<Integer, Ship[]> fleet = game.getFleet();
        for (Ship[] ships : fleet.values()) {
            for (Ship ship : ships) {
                if (!ship.isSunk()) {
                    return false;
                }
            }
        }
        return true;
    }

    public int getRemainingShips() {
        if (game == null) return 0;

        int count = 0;
        Map<Integer, Ship[]> fleet = game.getFleet();
        for (Ship[] ships : fleet.values()) {
            for (Ship ship : ships) {
                if (!ship.isSunk()) {
                    count++;
                }
            }
        }
        return count;
    }


    public Game getGame() {
        return game;
    }

    public int[][] exportSunk() {
        Map<Integer, Ship[]> fleet = null;
        int[][] result = new int[5][2];
        for (int i = 0; i < 5; i++) {
            int sunkCount = 0;
            int totalCount = 0;
            Ship[] ships = fleet.get(i);
            if (ships != null) {
                totalCount = ships.length;
                for (Ship ship : ships) {
                    if (ship.isSunk()) {
                        sunkCount++;
                    }
                }
            }
            result[i][0] = sunkCount;
            result[i][1] = totalCount;
        }
        return result;
    }

    @Override
    public String toString() {
        if (game == null) return "Game not initialized";
        return game.getBoard().toString(true);
    }

    public static class AttackResult {
        private final boolean hit;
        private final boolean shipSunk;
        private final boolean gameWon;

        public AttackResult(boolean hit, boolean shipSunk, boolean gameWon) {
            this.hit = hit;
            this.shipSunk = shipSunk;
            this.gameWon = gameWon;
        }

        public boolean isHit() { return hit; }
        public boolean isShipSunk() { return shipSunk; }
        public boolean isGameWon() { return gameWon; }
    }
}
