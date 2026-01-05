package at.gl1tchxd.battleship.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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

    // Textures for grid cells (static)
    private Texture blueEmptyTexture;
    private Texture greyEmptyTexture;
    private Texture blueShipTexture;
    private Texture greyShipTexture;
    private Texture blueHoleTexture;
    private Texture greyHoleTexture;
    private Texture blueBurntTexture;
    private Texture greyBurntTexture;

    // Animation sprite sheets
    private Texture blueHoleAnimSheet;
    private Texture greyHoleAnimSheet;
    private Texture blueBurntAnimSheet;
    private Texture greyBurntAnimSheet;
    private Texture blueShipAnimSheet;
    private Texture greyShipAnimSheet;

    // Animations
    private Animation<TextureRegion> blueHoleAnim;
    private Animation<TextureRegion> greyHoleAnim;
    private Animation<TextureRegion> blueBurntAnim;
    private Animation<TextureRegion> greyBurntAnim;
    private Animation<TextureRegion> blueShipAnim;
    private Animation<TextureRegion> greyShipAnim;

    // Animation tracking for each cell
    private CellAnimationState[][] cellAnimations;
    private static final int FRAME_COUNT = 8;
    private static final int FRAME_WIDTH = 80;
    private static final int FRAME_HEIGHT = 80;
    private static final float FRAME_DURATION = 0.1f; // 0.1s per frame = 0.8s total animation

    // Helper class to track animation state per cell
    private static class CellAnimationState {
        boolean isAnimating = false;
        float animationTime = 0f;
        AnimationType type = AnimationType.NONE;
        int lastHitStatus = 0; // Track previous state to detect changes
    }

    private enum AnimationType {
        NONE, HOLE, BURNT, SHIP
    }

    public GridRenderer(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font) {
        this.shapeRenderer = shapeRenderer;
        this.batch = batch;
        this.font = font;

        loadTextures();
    }

    private void loadTextures() {
        // Load static textures
        blueEmptyTexture = new Texture(Gdx.files.internal("sprites/grid_blue_empty.png"));
        greyEmptyTexture = new Texture(Gdx.files.internal("sprites/grid_grey_empty.png"));
        blueShipTexture = new Texture(Gdx.files.internal("sprites/grid_blue_ship.png"));
        greyShipTexture = new Texture(Gdx.files.internal("sprites/grid_grey_ship.png"));
        blueHoleTexture = new Texture(Gdx.files.internal("sprites/grid_blue_hole.png"));
        greyHoleTexture = new Texture(Gdx.files.internal("sprites/grid_grey_hole.png"));
        blueBurntTexture = new Texture(Gdx.files.internal("sprites/grid_blue_burnt.png"));
        greyBurntTexture = new Texture(Gdx.files.internal("sprites/grid_grey_burnt.png"));

        // Load animation sprite sheets
        blueHoleAnimSheet = new Texture(Gdx.files.internal("sprites/grid_blue_hole_anim.png"));
        greyHoleAnimSheet = new Texture(Gdx.files.internal("sprites/grid_grey_hole_anim.png"));
        blueBurntAnimSheet = new Texture(Gdx.files.internal("sprites/grid_blue_burnt_anim.png"));
        greyBurntAnimSheet = new Texture(Gdx.files.internal("sprites/grid_grey_burnt_anim.png"));
        blueShipAnimSheet = new Texture(Gdx.files.internal("sprites/grid_blue_ship_anim.png"));
        greyShipAnimSheet = new Texture(Gdx.files.internal("sprites/grid_grey_ship_anim.png"));

        // Create animations from sprite sheets
        blueHoleAnim = createAnimation(blueHoleAnimSheet);
        greyHoleAnim = createAnimation(greyHoleAnimSheet);
        blueBurntAnim = createAnimation(blueBurntAnimSheet);
        greyBurntAnim = createAnimation(greyBurntAnimSheet);
        blueShipAnim = createAnimation(blueShipAnimSheet);
        greyShipAnim = createAnimation(greyShipAnimSheet);
    }

    private Animation<TextureRegion> createAnimation(Texture spriteSheet) {
        TextureRegion[] frames = new TextureRegion[FRAME_COUNT];
        for (int i = 0; i < FRAME_COUNT; i++) {
            frames[i] = new TextureRegion(spriteSheet, i * FRAME_WIDTH, 0, FRAME_WIDTH, FRAME_HEIGHT);
        }
        return new Animation<>(FRAME_DURATION, frames);
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

        // Initialize animation tracking array
        cellAnimations = new CellAnimationState[gridSize][gridSize];
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                cellAnimations[row][col] = new CellAnimationState();
            }
        }
    }

    /**
     * Draw the complete grid with texture-based cells.
     * @param isTrackingBoard true for tracking board (opponent), false for own board
     * @param shipGrid optional ship grid for own board (shows ship textures)
     * @param hitGrid optional hit grid (0 = empty, 1 = miss, 2 = hit)
     * @param delta time delta for animations
     */
    public void drawGrid(boolean isTrackingBoard, boolean[][] shipGrid, int[][] hitGrid, float delta) {
        batch.begin();

        // Ensure color is reset to white (no tint)
        batch.setColor(Color.WHITE);

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                float x = boardX + col * cellSize;
                float y = boardY + (gridSize - 1 - row) * cellSize;

                // Determine which texture/animation frame to use
                TextureRegion region = getCellTextureRegion(row, col, isTrackingBoard, shipGrid, hitGrid, delta);

                if (region != null) {
                    batch.draw(region, x, y, cellSize, cellSize);
                }
            }
        }

        batch.end();
    }

    private TextureRegion getCellTextureRegion(int row, int col, boolean isTrackingBoard, boolean[][] shipGrid, int[][] hitGrid, float delta) {
        boolean useBlue = (row + col) % 2 == 0;
        CellAnimationState animState = cellAnimations[row][col];

        // Check hit grid state
        int hitStatus = (hitGrid != null && row < hitGrid.length && col < hitGrid[row].length)
                        ? hitGrid[row][col] : 0;

        // Detect state change and trigger animation
        if (hitStatus != animState.lastHitStatus && hitStatus != 0) {
            animState.isAnimating = true;
            animState.animationTime = 0f;
            animState.lastHitStatus = hitStatus;
            if (hitStatus == 1) {
                animState.type = AnimationType.BURNT;
            } else if (hitStatus == 2) {
                animState.type = AnimationType.HOLE;
            }
        }

        // Update animation state
        if (animState.isAnimating) {
            animState.animationTime += delta;
            float totalAnimDuration = FRAME_DURATION * FRAME_COUNT;
            if (animState.animationTime >= totalAnimDuration) {
                animState.isAnimating = false;
            }
        }

        // Check if there's a ship at this location (for own board)
        boolean hasShip = !isTrackingBoard && shipGrid != null &&
                          row < shipGrid.length && col < shipGrid[row].length &&
                          shipGrid[row][col];

        // If hit or miss, show animation or final texture
        if (hitStatus == 1) { // Miss (burnt)
            if (animState.isAnimating && animState.type == AnimationType.BURNT) {
                Animation<TextureRegion> anim = useBlue ? blueBurntAnim : greyBurntAnim;
                return anim.getKeyFrame(animState.animationTime, false);
            } else {
                return new TextureRegion(useBlue ? blueBurntTexture : greyBurntTexture);
            }
        } else if (hitStatus == 2) { // Hit (hole)
            // On own board with ship: use ship_anim (ship → hole transition)
            // On tracking board or empty cell: use hole_anim (empty → hole)
            if (hasShip) {
                if (animState.isAnimating && animState.type == AnimationType.HOLE) {
                    Animation<TextureRegion> anim = useBlue ? blueShipAnim : greyShipAnim;
                    return anim.getKeyFrame(animState.animationTime, false);
                } else {
                    return new TextureRegion(useBlue ? blueHoleTexture : greyHoleTexture);
                }
            } else {
                if (animState.isAnimating && animState.type == AnimationType.HOLE) {
                    Animation<TextureRegion> anim = useBlue ? blueHoleAnim : greyHoleAnim;
                    return anim.getKeyFrame(animState.animationTime, false);
                } else {
                    return new TextureRegion(useBlue ? blueHoleTexture : greyHoleTexture);
                }
            }
        }

        // For own board, show ships (when not hit)
        if (hasShip) {
            return new TextureRegion(useBlue ? blueShipTexture : greyShipTexture);
        }

        // Default: empty cell
        return new TextureRegion(useBlue ? blueEmptyTexture : greyEmptyTexture);
    }

    /**
     * Draw coordinate labels (row and column numbers, 1-based for display).
     * Row 1 is at the top, column 1 is on the left.
     */
    public void drawCoordinateLabels() {
        batch.begin();
        batch.setColor(Color.WHITE); // Ensure labels are drawn in white

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
     * Draw a ship placement preview using ship PNG textures with color tinting.
     * @param row Starting row (0 at top)
     * @param col Starting column (0 at left)
     * @param length Length of the ship
     * @param horizontal True if horizontal, false if vertical
     * @param valid True if placement is valid (normal), false if invalid (red tint)
     * @param placementConfirmed True if placement has been confirmed (red tint)
     */
    public void drawShipPreview(int row, int col, int length, boolean horizontal, boolean valid, boolean placementConfirmed) {
        batch.begin();

        // Save the current batch color
        Color oldColor = batch.getColor().cpy();

        // Set color tint based on validity and confirmation state
        if (placementConfirmed || !valid) {
            // Red tint for invalid placement or when confirmed
            batch.setColor(1.0f, 0.3f, 0.3f, 0.7f);
        } else {
            // Normal color for valid placement
            batch.setColor(1.0f, 1.0f, 1.0f, 0.8f);
        }

        for (int i = 0; i < length; i++) {
            int r = horizontal ? row : row + i;
            int c = horizontal ? col + i : col;

            if (r >= 0 && r < gridSize && c >= 0 && c < gridSize) {
                float x = boardX + c * cellSize;
                // Flip Y: row 0 should be at top (highest screen Y)
                float y = boardY + (gridSize - 1 - r) * cellSize;

                // Use appropriate ship texture based on checkerboard pattern
                boolean useBlue = (r + c) % 2 == 0;
                Texture shipTexture = useBlue ? blueShipTexture : greyShipTexture;

                batch.draw(shipTexture, x, y, cellSize, cellSize);
            }
        }

        // Restore original color
        batch.setColor(oldColor);
        batch.end();
    }

    /**
     * Draw targeting highlight with thick red crosshair for attacking.
     * @param row Row to highlight (0 at top)
     * @param col Column to highlight (0 at left)
     */
    public void drawTargetingHighlight(int row, int col) {
        float x = boardX + col * cellSize;
        float y = boardY + (gridSize - 1 - row) * cellSize;

        // Draw crosshair
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.RED);

        float centerX = x + cellSize / 2;
        float centerY = y + cellSize / 2;
        float crossSize = cellSize * 0.4f;
        float thickness = 5f; // Thicker crosshair

        // Horizontal line (thicker)
        shapeRenderer.rectLine(centerX - crossSize, centerY, centerX + crossSize, centerY, thickness);
        // Vertical line (thicker)
        shapeRenderer.rectLine(centerX, centerY - crossSize, centerX, centerY + crossSize, thickness);

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

    /**
     * Dispose of textures to prevent memory leaks.
     */
    public void dispose() {
        if (blueEmptyTexture != null) blueEmptyTexture.dispose();
        if (greyEmptyTexture != null) greyEmptyTexture.dispose();
        if (blueShipTexture != null) blueShipTexture.dispose();
        if (greyShipTexture != null) greyShipTexture.dispose();
        if (blueHoleTexture != null) blueHoleTexture.dispose();
        if (greyHoleTexture != null) greyHoleTexture.dispose();
        if (blueBurntTexture != null) blueBurntTexture.dispose();
        if (greyBurntTexture != null) greyBurntTexture.dispose();
        if (blueHoleAnimSheet != null) blueHoleAnimSheet.dispose();
        if (greyHoleAnimSheet != null) greyHoleAnimSheet.dispose();
        if (blueBurntAnimSheet != null) blueBurntAnimSheet.dispose();
        if (greyBurntAnimSheet != null) greyBurntAnimSheet.dispose();
        if (blueShipAnimSheet != null) blueShipAnimSheet.dispose();
        if (greyShipAnimSheet != null) greyShipAnimSheet.dispose();
    }
}
