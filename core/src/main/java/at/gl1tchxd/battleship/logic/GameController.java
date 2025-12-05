package at.gl1tchxd.battleship.logic;

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

    // Player ID management
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

    // Turn management
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

    // Board access
    public Board getMyBoard() {
        return myBoard;
    }

    public Board getTrackingBoard() {
        return trackingBoard;
    }

    /**
     * Record the result of attacking the opponent (updates tracking board).
     * Call this when you receive the attack result from the opponent.
     */
    public void recordAttackResult(int row, int col, boolean hit) {
        if (trackingBoard == null) throw new IllegalStateException("Tracking board not initialized");
        trackingBoard.markCell(row, col, hit);
    }

    /**
     * Receive an attack from the opponent on your board.
     * Returns the result to send back to the opponent.
     */
    public AttackResult receiveAttack(int row, int col) {
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
            currentPhase = GamePhase.GAME_OVER;
        }

        return new AttackResult(hit, shipSunk, gameWon, hitShip);
    }

    // Placement phase
    public boolean isPlacementComplete() {
        return placementComplete;
    }

    public void confirmPlacement() {
        if (game == null) throw new IllegalStateException("Game not initialized");
        placementComplete = true;

        // Check if both players are ready to start battle
        if (opponentReady) {
            currentPhase = GamePhase.BATTLE;
        }
    }

    public boolean isOpponentReady() {
        return opponentReady;
    }

    public void setOpponentReady(boolean ready) {
        this.opponentReady = ready;

        // If both ready, move to battle phase
        if (ready && placementComplete) {
            currentPhase = GamePhase.BATTLE;
        }
    }

    // Game phase tracking
    public GamePhase getGamePhase() {
        return currentPhase;
    }

    public void setGamePhase(GamePhase phase) {
        this.currentPhase = phase;
    }

    /**
     * Export board state for network transmission (hides unhit ships).
     * Returns a 2D array where: 0=EMPTY, 1=MISS, 2=HIT
     */
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

    /**
     * Export fleet information for game state sync.
     */
    public Ship[] exportFleet() {
        if (game == null) return null;
        return game.getFleet();
    }

    // Reset
    public void resetGame() {
        if (myBoard != null) myBoard.clear();
        if (trackingBoard != null) trackingBoard.clear();
        placementComplete = false;
        opponentReady = false;
        currentPhase = GamePhase.PLACEMENT;
        currentTurnPlayerId = null;
        game = null;
    }

    public boolean initializeGame(int boardSize, int[] shipConfig) {
        if (boardSize <= 0) throw new IllegalArgumentException("boardSize must be > 0");
        if (shipConfig == null) throw new IllegalArgumentException("shipConfig must not be null");
        try {
            game = new Game(boardSize, shipConfig);
            myBoard = game.getBoard();
            trackingBoard = new Board(boardSize, true); // Create tracking board
            currentPhase = GamePhase.PLACEMENT;
            return true;
        } catch (IllegalArgumentException e) {
            game = null;
            throw e;
        }
    }

    public boolean placeShip(int index, int row, int col, boolean horizontal) {
        if (game == null) throw new IllegalStateException("Game not initialized");
        return game.placeShip(index, row, col, horizontal);
    }

    /**
     * Attack opponent's board (through network).
     * This updates your tracking board after receiving the result.
     */
    public boolean attack(int row, int col) {
        if (game == null) throw new IllegalStateException("Game not initialized");
        // Note: This method should be called AFTER network sends attack and receives result
        // The actual hit/miss will be recorded via recordAttackResult()
        return true; // Placeholder - network layer handles actual attack
    }

    /**
     * Attack and return detailed result information.
     * For P2P: This is called when YOU are attacked (opponent attacks your board).
     */
    public AttackResult attackWithResult(int row, int col) {
        return receiveAttack(row, col);
    }

    /**
     * Check if all ships on MY board are sunk (I lost).
     */
    public boolean isGameOver() {
        if (game == null) return false;

        Ship[] fleet = game.getFleet();
        for (Ship ship : fleet) {
            if (!ship.isSunk()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get count of MY ships still afloat.
     */
    public int getRemainingShips() {
        if (game == null) return 0;

        int count = 0;
        Ship[] fleet = game.getFleet();
        for (Ship ship : fleet) {
            if (!ship.isSunk()) {
                count++;
            }
        }
        return count;
    }

    public Game getGame() {
        return game;
    }

    @Override
    public String toString() {
        if (game == null) return "Game not initialized";
        return game.getBoard().toString();
    }

    /**
     * Result of an attack action with all relevant state information.
     */
    public static class AttackResult {
        private final boolean hit;
        private final boolean shipSunk;
        private final boolean gameWon;
        private final Ship hitShip;

        public AttackResult(boolean hit, boolean shipSunk, boolean gameWon, Ship hitShip) {
            this.hit = hit;
            this.shipSunk = shipSunk;
            this.gameWon = gameWon;
            this.hitShip = hitShip;
        }

        public boolean isHit() { return hit; }
        public boolean isShipSunk() { return shipSunk; }
        public boolean isGameWon() { return gameWon; }
        public Ship getHitShip() { return hitShip; }
    }
}
