package at.gl1tchxd.battleship.logic;

public class Board {
    private final Ship[][] board;

    public Board() {
        board = new Ship[8][8];
    }

    private boolean inBounds(int row, int col) {
        return row >= 0 && row < board.length && col >= 0 && col < board[0].length;
    }

    public boolean canPlace(Ship ship, int row, int col, boolean horizontal) {
        if (ship == null) return false;
        int endRow = horizontal ? row : row + ship.getLength() - 1;
        int endCol = horizontal ? col + ship.getLength() - 1 : col;
        if (!inBounds(row, col) || !inBounds(endRow, endCol)) return false;

        for (int i = 0; i < ship.getLength(); i++) {
            int rr = horizontal ? row : row + i;
            int cc = horizontal ? col + i : col;
            if (board[rr][cc] != null) return false;
        }
        return true;
    }

    public boolean placeShip(Ship ship, int row, int col, boolean horizontal) {
        if (!canPlace(ship, row, col, horizontal)) return false;
        ship.place(row, col, horizontal);
        for (int i = 0; i < ship.getLength(); i++) {
            int currentRow = horizontal ? row : row + i;
            int currentCol = horizontal ? col + i : col;
            board[currentRow][currentCol] = ship;
        }
        return true;
    }

    public boolean attack(int row, int col) {
        if (!inBounds(row, col)) return false;
        Ship s = board[row][col];
        if (s == null) return false;
        return s.hitAt(row, col);
    }
}
