package at.gl1tchxd.battleship.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Renders the game board grid with cells, labels, ships, and hit/miss markers.
 * Can be reused for both placement and battle screens.
 */
public class GridRenderer {
    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch batch;
    private final BitmapFont font;

    private float boardX;
    private float boardY;
    private float boardSize;
    private float cellSize;
    private int gridSize;

    public GridRenderer(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font) {
        this.shapeRenderer = shapeRenderer;
        this.batch = batch;
        this.font = font;
    }

    /**
     * Set the bounds and size of the grid.
     */
    public void setBounds(float x, float y, float size, int gridSize) {
        this.boardX = x;
        this.boardY = y;
        this.boardSize = size;
        this.gridSize = gridSize;
        this.cellSize = size / gridSize;
    }

    /**
     * Draw the complete grid with checkerboard pattern and grid lines.
     */
    public void drawGrid() {
        // Draw cells with checkerboard pattern
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                float x = boardX + col * cellSize;
                float y = boardY + row * cellSize;

                if ((row + col) % 2 == 0) {
                    shapeRenderer.setColor(0.2f, 0.3f, 0.4f, 1);
                } else {
                    shapeRenderer.setColor(0.25f, 0.35f, 0.45f, 1);
                }
                shapeRenderer.rect(x, y, cellSize, cellSize);
            }
        }
        shapeRenderer.end();

        // Draw grid lines
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.4f, 0.5f, 0.6f, 1);

        for (int i = 0; i <= gridSize; i++) {
            // Vertical lines
            float x = boardX + i * cellSize;
            shapeRenderer.line(x, boardY, x, boardY + boardSize);

            // Horizontal lines
            float y = boardY + i * cellSize;
            shapeRenderer.line(boardX, y, boardX + boardSize, y);
        }
        shapeRenderer.end();
    }

    /**
     * Draw coordinate labels (row and column numbers, 1-based for display).
     * Row 1 is at the top, column 1 is on the left.
     */
    public void drawCoordinateLabels() {
        batch.begin();
        for (int i = 0; i < gridSize; i++) {
            // Column numbers (top) - 1-based display
            float x = boardX + i * cellSize + cellSize / 2 - 5;
            float y = boardY + boardSize + 20;
            font.draw(batch, String.valueOf(i + 1), x, y);

            // Row numbers (left) - 1-based display, row 1 at top
            x = boardX - 20;
            // Flip: screen row 0 (bottom in screen coords) shows logical row (gridSize-1)
            // We want logical row 0 at top, so for screen position at top (gridSize-1-i in old system)
            // we display (i+1)
            float screenY = boardY + (gridSize - 1 - i) * cellSize + cellSize / 2 + 5;
            font.draw(batch, String.valueOf(i + 1), x, screenY);
        }
        batch.end();
    }

    /**
     * Draw a ship placement preview.
     * @param row Starting row (0 at top)
     * @param col Starting column (0 at left)
     * @param length Length of the ship
     * @param horizontal True if horizontal, false if vertical
     * @param valid True if placement is valid (green), false if invalid (red)
     */
    public void drawShipPreview(int row, int col, int length, boolean horizontal, boolean valid) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (valid) {
            shapeRenderer.setColor(0, 1, 0, 0.3f); // Green transparent
        } else {
            shapeRenderer.setColor(1, 0, 0, 0.3f); // Red transparent
        }

        for (int i = 0; i < length; i++) {
            int r = horizontal ? row : row + i;
            int c = horizontal ? col + i : col;

            if (r >= 0 && r < gridSize && c >= 0 && c < gridSize) {
                float x = boardX + c * cellSize;
                // Flip Y: row 0 should be at top (highest screen Y)
                float y = boardY + (gridSize - 1 - r) * cellSize;
                shapeRenderer.rect(x, y, cellSize, cellSize);
            }
        }

        shapeRenderer.end();
    }

    /**
     * Draw placed ships on the board.
     * @param shipGrid 2D boolean array where true = ship present (row 0 = top)
     */
    public void drawShips(boolean[][] shipGrid) {
        if (shipGrid == null) return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.8f, 0.8f, 0.2f, 0.8f); // Yellow for ships

        for (int row = 0; row < gridSize && row < shipGrid.length; row++) {
            for (int col = 0; col < gridSize && col < shipGrid[row].length; col++) {
                if (shipGrid[row][col]) {
                    float x = boardX + col * cellSize + 2;
                    // Flip Y: row 0 should be at top (highest screen Y)
                    float y = boardY + (gridSize - 1 - row) * cellSize + 2;
                    shapeRenderer.rect(x, y, cellSize - 4, cellSize - 4);
                }
            }
        }

        shapeRenderer.end();
    }

    /**
     * Draw hit and miss markers.
     * @param hitGrid 2D int array: 0 = empty, 1 = miss, 2 = hit (row 0 = top)
     */
    public void drawHitsAndMisses(int[][] hitGrid) {
        if (hitGrid == null) return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int row = 0; row < gridSize && row < hitGrid.length; row++) {
            for (int col = 0; col < gridSize && col < hitGrid[row].length; col++) {
                // Flip Y: row 0 should be at top (highest screen Y)
                float screenY = boardY + (gridSize - 1 - row) * cellSize;
                if (hitGrid[row][col] == 1) {
                    // Draw miss (circle)
                    shapeRenderer.setColor(0.5f, 0.7f, 1, 1);
                    float x = boardX + col * cellSize + cellSize / 2;
                    float y = screenY + cellSize / 2;
                    shapeRenderer.circle(x, y, cellSize / 4);
                } else if (hitGrid[row][col] == 2) {
                    // Draw hit (X)
                    shapeRenderer.setColor(1, 0, 0, 1);
                    float x = boardX + col * cellSize;
                    float y = screenY;
                    float padding = cellSize * 0.2f;
                    shapeRenderer.rectLine(x + padding, y + padding,
                                          x + cellSize - padding, y + cellSize - padding, 3);
                    shapeRenderer.rectLine(x + cellSize - padding, y + padding,
                                          x + padding, y + cellSize - padding, 3);
                }
            }
        }

        shapeRenderer.end();
    }

    /**
     * Convert screen coordinates to grid coordinates.
     * @return Array [row, col] where row 0 is at the top, col 0 is at the left, or null if outside grid
     */
    public int[] screenToGrid(float screenX, float screenY) {
        if (screenX >= boardX && screenX <= boardX + boardSize &&
            screenY >= boardY && screenY <= boardY + boardSize) {

            int col = (int)((screenX - boardX) / cellSize);
            int screenRow = (int)((screenY - boardY) / cellSize);
            // Flip: screen row 0 is at bottom, but we want logical row 0 at top
            int row = gridSize - 1 - screenRow;

            if (row >= 0 && row < gridSize && col >= 0 && col < gridSize) {
                return new int[]{row, col};
            }
        }
        return null;
    }

    // Getters
    public float getBoardX() { return boardX; }
    public float getBoardY() { return boardY; }
    public float getBoardSize() { return boardSize; }
    public float getCellSize() { return cellSize; }
    public int getGridSize() { return gridSize; }
}
