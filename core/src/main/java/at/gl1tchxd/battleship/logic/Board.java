package at.gl1tchxd.battleship.logic;

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

public class Board {
    private final Ship[][] board;
    private Cell[][] grid;
    private final boolean trackingMode; // true = tracking board (no ships), false = normal board
    private enum Cell { EMPTY, SHIP, HIT, MISS }

    public Board(int boardSize) {
        this(boardSize, false);
    }

    /**
     * Create a board.
     * boardSize size of the board
     * trackingMode if true, this is a tracking-only board (no ship placement, only hit/miss tracking)
     */
    public Board(int boardSize, boolean trackingMode) {
        if (boardSize <= 0) throw new IllegalArgumentException("boardSize must be > 0");
        this.trackingMode = trackingMode;
        board = trackingMode ? null : new Ship[boardSize][boardSize];
        grid = new Cell[boardSize][boardSize];
        for (Cell[] row : grid) Arrays.fill(row, Cell.EMPTY);
    }

    public boolean isTrackingMode() {
        return trackingMode;
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
        // if start or end out of bounds, placement is not possible (but not an exception)
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
                    // Only clear visual state if that cell wasn't hit
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
        if (trackingMode) return null; // tracking boards don't have ships
        if (row < 0 || col < 0) throw new IndexOutOfBoundsException("row and col must be >= 0");
        if (!inBounds(row, col)) throw new IndexOutOfBoundsException("row/col out of bounds");
        return board[row][col];
    }

    // returns the cell state (EMPTY, SHIP, HIT, MISS)
    public String getCellInfo(int row, int col) {
        if (row < 0 || col < 0) throw new IndexOutOfBoundsException("row and col must be >= 0");
        if (!inBounds(row, col)) throw new IndexOutOfBoundsException("row/col out of bounds");
        return grid[row][col].name();
    }

    /**
     * Get cell info suitable for network transmission (hides unhit ships).
     * Returns EMPTY for unhit ship cells, HIT/MISS as normal.
     */
    public String getCellInfoForOpponent(int row, int col) {
        if (row < 0 || col < 0) throw new IndexOutOfBoundsException("row and col must be >= 0");
        if (!inBounds(row, col)) throw new IndexOutOfBoundsException("row/col out of bounds");

        Cell cell = grid[row][col];
        // Hide unhit ships from opponent
        if (cell == Cell.SHIP) return Cell.EMPTY.name();
        return cell.name();
    }

    // Attack a cell; returns true when a ship was hit, false when miss.
    public boolean attack(int row, int col) {
        if (trackingMode) throw new IllegalStateException("Cannot attack tracking board directly, use markCell()");
        if (row < 0 || col < 0) throw new IndexOutOfBoundsException("row and col must be >= 0");
        if (!inBounds(row, col)) throw new IndexOutOfBoundsException("row/col out of bounds");

        Cell current = grid[row][col];
        // if already attacked, return whether it was a hit
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
        if (trackingMode) return null; // tracking boards don't have ships
        Ship[][] copy = new Ship[board.length][board.length];
        for (int r = 0; r < board.length; r++) {
            System.arraycopy(board[r], 0, copy[r], 0, board[r].length);
        }
        return copy;
    }

    public void clear() {
        if (trackingMode) {
            // For tracking boards, just clear the grid
            for (Cell[] row : grid) Arrays.fill(row, Cell.EMPTY);
            return;
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
        // reset grid cells to EMPTY as well
        for (Cell[] row : grid) Arrays.fill(row, Cell.EMPTY);
    }

    @Override
    public String toString() {
        return toString(true);
    }

    /**
     * Render the board as a string. If useAnsi==true, include ANSI color/background codes
     * (helpful in terminals that support them). Default to no-ANSI to avoid raw escape codes
     * in loggers or environments that don't support them.
     */
    public String toString(boolean useAnsi) {
        StringBuilder sb = new StringBuilder();
        // Use grid length because in tracking mode `board` is null
        int n = grid.length;

        // Determine width for row index column
        int idxWidth = Math.max(2, String.valueOf(n - 1).length());

        // ANSI color codes (foreground only)
        final String RESET = "\u001B[0m";
        final String FG_SHIP = "\u001B[95m"; // bright magenta for ships
        final String FG_HIT = "\u001B[91m";  // bright red for hits
        final String FG_MISS = "\u001B[96m"; // bright cyan for misses
        final String FG_EMPTY = "\u001B[90m"; // dark gray for empty cells
        final String FG_INDEX = "\u001B[37m"; // white for indices

        // Header: column indices
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
            // Row index
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
                    sb.append(cellStr).append(' '); // spacing to match header
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

