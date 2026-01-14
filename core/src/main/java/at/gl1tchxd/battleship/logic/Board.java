package at.gl1tchxd.battleship.logic;

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

public class Board {
    private final Ship[][] board;
    private Cell[][] grid;
    private final boolean trackingMode;
    private int[][] trackingSunkShips;
    private enum Cell { EMPTY, SHIP, HIT, MISS }

    public Board(int boardSize) {
        this(boardSize, false);
    }

    public Board(int boardSize, boolean trackingMode) {
        if (boardSize <= 0) throw new IllegalArgumentException("boardSize must be > 0");
        this.trackingMode = trackingMode;
        this.trackingSunkShips = new int[5][2];
        board = trackingMode ? null : new Ship[boardSize][boardSize];
        grid = new Cell[boardSize][boardSize];
        for (Cell[] row : grid) Arrays.fill(row, Cell.EMPTY);
    }

    public boolean isTrackingMode() {
        return trackingMode;
    }

    public void setTrackingSunkShips(int[][] trackingSunkShips) {
        if (!trackingMode) throw new IllegalStateException("Cannot set trackingSunkShips in non-tracking mode");
        this.trackingSunkShips = trackingSunkShips;
    }

    public int[][] getTrackingSunkShips() {
        if (!trackingMode) throw new IllegalStateException("Cannot get trackingSunkShips in non-tracking mode");
        return trackingSunkShips;
    }

    public int getSize() {
        return grid.length;
    }

    private boolean inBounds(int row, int col) {
        return row >= 0 && row < grid.length && col >= 0 && col < grid[0].length;
    }
    public void markCell(int row, int col, boolean hit) {
        if (!trackingMode) throw new IllegalStateException("markCell() only available in tracking mode");
        if (row < 0 || col < 0) throw new IndexOutOfBoundsException("row and col must be >= 0");
        if (!inBounds(row, col)) throw new IndexOutOfBoundsException("row/col out of bounds");

        grid[row][col] = hit ? Cell.HIT : Cell.MISS;
    }

    public boolean canPlace(Ship ship, int row, int col, boolean horizontal) {
        if (trackingMode) throw new IllegalStateException("Cannot place ships on tracking board");
        if (ship == null) throw new IllegalArgumentException("ship must not be null");
        if (row < 0 || col < 0) throw new IndexOutOfBoundsException("row and col must be >= 0");
        int endRow = horizontal ? row : row + ship.getLength() - 1;
        int endCol = horizontal ? col + ship.getLength() - 1 : col;
        if (!inBounds(row, col) || !inBounds(endRow, endCol)) return false;

        for (int i = 0; i < ship.getLength(); i++) {
            int currentRow = horizontal ? row : row + i;
            int currentCol = horizontal ? col + i : col;
            if (board[currentRow][currentCol] != null) return false;
        }
        return true;
    }

    public boolean placeShip(Ship ship, int row, int col, boolean horizontal) {
        if (trackingMode) throw new IllegalStateException("Cannot place ships on tracking board");
        if (ship == null) throw new IllegalArgumentException("ship must not be null");
        if (row < 0 || col < 0) throw new IndexOutOfBoundsException("row and col must be >= 0");
        if (ship.isPlaced()) throw new IllegalStateException("ship is already placed");
        if (!canPlace(ship, row, col, horizontal)) return false;
        ship.place(row, col, horizontal);
        for (int i = 0; i < ship.getLength(); i++) {
            int currentRow = horizontal ? row : row + i;
            int currentCol = horizontal ? col + i : col;
            board[currentRow][currentCol] = ship;
            grid[currentRow][currentCol] = Cell.SHIP;
        }
        return true;
    }

    public boolean removeShip(Ship ship) {
        if (trackingMode) throw new IllegalStateException("Cannot remove ships from tracking board");
        if (ship == null) throw new IllegalArgumentException("ship must not be null");
        if (!ship.isPlaced()) return false;
        boolean removed = false;
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                if (board[r][c] == ship) {
                    board[r][c] = null;
                    if (grid[r][c] != Cell.HIT) {
                        grid[r][c] = Cell.EMPTY;
                    }
                    removed = true;
                }
            }
        }
        if (removed) {
            ship.place(-1, -1, false);
        }
        return removed;
    }

    public Ship getShipAt(int row, int col) {
        if (trackingMode) return null;
        if (row < 0 || col < 0) throw new IndexOutOfBoundsException("row and col must be >= 0");
        if (!inBounds(row, col)) throw new IndexOutOfBoundsException("row/col out of bounds");
        return board[row][col];
    }

    public String getCellInfo(int row, int col) {
        if (row < 0 || col < 0) throw new IndexOutOfBoundsException("row and col must be >= 0");
        if (!inBounds(row, col)) throw new IndexOutOfBoundsException("row/col out of bounds");
        return grid[row][col].name();
    }

    public String getCellInfoForOpponent(int row, int col) {
        if (row < 0 || col < 0) throw new IndexOutOfBoundsException("row and col must be >= 0");
        if (!inBounds(row, col)) throw new IndexOutOfBoundsException("row/col out of bounds");

        Cell cell = grid[row][col];
        if (cell == Cell.SHIP) return Cell.EMPTY.name();
        return cell.name();
    }

    public boolean attack(int row, int col) {
        if (trackingMode) throw new IllegalStateException("Cannot attack tracking board directly, use markCell()");
        if (row < 0 || col < 0) throw new IndexOutOfBoundsException("row and col must be >= 0");
        if (!inBounds(row, col)) throw new IndexOutOfBoundsException("row/col out of bounds");

        Cell current = grid[row][col];
        if (current == Cell.HIT) return true;
        if (current == Cell.MISS) return false;

        Ship s = board[row][col];
        if (s == null) {
            grid[row][col] = Cell.MISS;
            return false;
        }

        s.hitAt(row, col);
        grid[row][col] = Cell.HIT;
        return true;
    }


    public Ship[][] getGridCopy() {
        if (trackingMode) return null;
        Ship[][] copy = new Ship[board.length][board.length];
        for (int r = 0; r < board.length; r++) {
            System.arraycopy(board[r], 0, copy[r], 0, board[r].length);
        }
        return copy;
    }

    public boolean clear() {
        if (trackingMode) {
            for (Cell[] row : grid) Arrays.fill(row, Cell.EMPTY);
            return true;
        }
        Set<Ship> seen = new HashSet<>();
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                Ship s = board[r][c];
                if (s != null) seen.add(s);
                board[r][c] = null;
            }
        }
        for (Ship s : seen) {
            s.place(-1, -1, false);
        }
        for (Cell[] row : grid) Arrays.fill(row, Cell.EMPTY);
        return true;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean useAnsi) {
        StringBuilder sb = new StringBuilder();
        int n = grid.length;
        int idxWidth = Math.max(2, String.valueOf(n - 1).length());

        final String RESET = "\u001B[0m";
        final String FG_SHIP = "\u001B[95m";
        final String FG_HIT = "\u001B[91m";
        final String FG_MISS = "\u001B[96m";
        final String FG_EMPTY = "\u001B[90m";
        final String FG_INDEX = "\u001B[37m";

        if (useAnsi) {
            for (int i = 0; i < idxWidth + 1; i++) sb.append(' ');
            sb.append(FG_INDEX);
            for (int c = 0; c < n; c++) {
                sb.append(String.format("%2d ", c));
            }
            sb.append(RESET).append('\n');
        } else {
            for (int i = 0; i < idxWidth + 1; i++) sb.append(' ');
            for (int c = 0; c < n; c++) {
                sb.append(String.format("%2d ", c));
            }
            sb.append('\n');
        }

        for (int r = 0; r < n; r++) {
            if (useAnsi) sb.append(FG_INDEX).append(String.format("%" + idxWidth + "d ", r)).append(RESET);
            else sb.append(String.format("%" + idxWidth + "d ", r));

            for (int c = 0; c < n; c++) {
                String cellStr;
                switch (grid[r][c]) {
                    case EMPTY: cellStr = ". "; break;
                    case SHIP:  cellStr = "S "; break;
                    case HIT:   cellStr = "X "; break;
                    case MISS:  cellStr = "o "; break;
                    default:    cellStr = "? "; break;
                }

                if (!useAnsi) {
                    sb.append(cellStr).append(' ');
                    continue;
                }

                String fg;
                switch (grid[r][c]) {
                    case EMPTY: fg = FG_EMPTY; break;
                    case SHIP:  fg = FG_SHIP;  break;
                    case HIT:   fg = FG_HIT;   break;
                    case MISS:  fg = FG_MISS;  break;
                    default:    fg = FG_EMPTY; break;
                }

                sb.append(fg).append(cellStr).append(RESET).append(' ');
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}

