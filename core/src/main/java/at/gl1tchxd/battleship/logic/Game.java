package at.gl1tchxd.battleship.logic;

import java.util.*;

public class Game {
    private final int boardSize;
    private final Board board;
    private final Ship[] fleet;
    private final int[] shipConfig;
    private final Random rand = new Random();

    public Game(int boardSize, int[] shipConfig) {
        if (boardSize <= 0) throw new IllegalArgumentException("boardSize must be > 0");
        if (shipConfig == null) throw new IllegalArgumentException("shipConfig must not be null");
        if (shipConfig.length != 5) throw new IllegalArgumentException("shipConfig must have 5 elements for standard classes");

        this.boardSize = boardSize;
        this.board = new Board(boardSize);

        int[] lengths = new int[] {5, 4, 3, 3, 2};
        int totalCells = 0;
        for (int i = 0; i < shipConfig.length; i++) {
            int count = shipConfig[i];
            if (count < 0) throw new IllegalArgumentException("ship counts must be >= 0");
            totalCells += count * lengths[i];
        }

        int maxAllowed = (int) Math.floor(boardSize * boardSize * 0.7);
        if (totalCells > maxAllowed) {
            throw new IllegalArgumentException("Total ship cells (" + totalCells + ") exceed 70% of board area (" + maxAllowed + ")");
        }

        List<Ship> list = new ArrayList<>();
        for (int i = 0; i < shipConfig.length; i++) {
            int count = shipConfig[i];
            int len = lengths[i];
            for (int c = 0; c < count; c++) {
                list.add(new Ship(len));
            }
        }
        this.fleet = list.toArray(new Ship[0]);
        this.shipConfig = Arrays.copyOf(shipConfig, shipConfig.length);
    }

    public boolean placeShip(int index, int row, int col, boolean horizontal) {
        if (!validIndex(index)) throw new IndexOutOfBoundsException("index out of range: " + index);
        if (row < 0 || col < 0) throw new IndexOutOfBoundsException("row and col must be >= 0");
        Ship ship = fleet[index];
        if (ship == null) throw new IllegalStateException("ship at index is null");

        if (ship.isPlaced()) {
            int oldRow = ship.getStartRow();
            int oldCol = ship.getStartCol();
            boolean oldHorizontal = ship.isHorizontal();

            board.removeShip(ship);

            if (board.canPlace(ship, row, col, horizontal) && board.placeShip(ship, row, col, horizontal)) {
                return true;
            }

            board.placeShip(ship, oldRow, oldCol, oldHorizontal);
            return false;
        }

        if (board.canPlace(ship, row, col, horizontal)) {
            return board.placeShip(ship, row, col, horizontal);
        }
        return false;
    }

    public boolean removeShip(int index) {
        if (!validIndex(index)) throw new IndexOutOfBoundsException("index out of range: " + index);
        Ship ship = fleet[index];
        if (ship == null) throw new IllegalStateException("ship at index is null");
        return board.removeShip(ship);
    }

    public boolean autoPlaceAll() {
        if (fleet == null) throw new IllegalStateException("fleet is null");
        for (Ship ship : fleet) {
            boolean placed = false;
            for (int tries = 0; tries < 200 && !placed; tries++) {
                int row = rand.nextInt(boardSize);
                int col = rand.nextInt(boardSize);
                boolean horizontal = rand.nextBoolean();
                if (board.canPlace(ship, row, col, horizontal)) {
                    board.placeShip(ship, row, col, horizontal);
                    placed = true;
                }
            }
            if (!placed) return false;
        }
        return true;
    }
    public int[] getShipConfig() { return shipConfig; }
    public Board getBoard() { return board; }
    public Map<Integer, Ship[]> getFleet() {
        Map<Integer, Ship[]> sorted_fleet = new HashMap<Integer, Ship[]>(5);
        int cut_min = 0;
        for (int i = 0; i < 5; i++) {
            sorted_fleet.put(i, Arrays.copyOfRange(fleet, cut_min, cut_min + shipConfig[i]));
            cut_min += shipConfig[i];
        }
        return sorted_fleet;
    }

    private boolean validIndex(int index) {
        return index >= 0 && index < fleet.length;
    }
}