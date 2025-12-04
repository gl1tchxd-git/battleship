// java
package at.gl1tchxd.battleship.logic;

public class Ship {
    private final int length;
    private final boolean[] hits;
    private int row = -1, col = -1;
    private boolean horizontal;

    public Ship(int length) {
        this.length = length;
        this.hits = new boolean[length];
    }

    public void place(int row, int col, boolean horizontal) {
        this.row = row;
        this.col = col;
        this.horizontal = horizontal;
    }

    public boolean isPlaced() {
        return row >= 0 && col >= 0;
    }

    public boolean occupies(int row, int col) {
        if (!isPlaced()) return false;
        if (horizontal) {
            return row == this.row && col >= this.col && col < this.col + length;
        } else {
            return col == this.col && row >= this.row && row < this.row + length;
        }
    }

    public boolean hitAt(int row, int col) {
        if (!occupies(row, col)) return false;
        int offset = horizontal ? col - this.col : row - this.row;
        hits[offset] = true;
        return true;
    }

    public boolean isSunk() {
        for (boolean h : hits) if (!h) return false;
        return true;
    }

    public int getLength() {
        return length;
    }
}
