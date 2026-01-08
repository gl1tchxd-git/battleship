package at.gl1tchxd.battleship.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class GridRenderer {
    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch batch;
    private final BitmapFont font;

    private float boardX;
    private float boardY;
    private float boardSize;
    private float cellSize;
    private int gridSize;

    private Texture blueEmptyTexture;
    private Texture greyEmptyTexture;
    private Texture blueShipTexture;
    private Texture greyShipTexture;
    private Texture blueHoleTexture;
    private Texture greyHoleTexture;
    private Texture blueBurntTexture;
    private Texture greyBurntTexture;

    private Texture blueHoleAnimSheet;
    private Texture greyHoleAnimSheet;
    private Texture blueBurntAnimSheet;
    private Texture greyBurntAnimSheet;
    private Texture blueShipAnimSheet;
    private Texture greyShipAnimSheet;

    private Animation<TextureRegion> blueHoleAnim;
    private Animation<TextureRegion> greyHoleAnim;
    private Animation<TextureRegion> blueBurntAnim;
    private Animation<TextureRegion> greyBurntAnim;
    private Animation<TextureRegion> blueShipAnim;
    private Animation<TextureRegion> greyShipAnim;

    private CellAnimationState[][] cellAnimations;
    private static final int FRAME_COUNT = 8;
    private static final int FRAME_WIDTH = 80;
    private static final int FRAME_HEIGHT = 80;
    private static final float FRAME_DURATION = 0.1f;

    private static class CellAnimationState {
        boolean isAnimating = false;
        float animationTime = 0f;
        AnimationType type = AnimationType.NONE;
        int lastHitStatus = 0;
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
        blueEmptyTexture = new Texture(Gdx.files.internal("sprites/grid_blue_empty.png"));
        greyEmptyTexture = new Texture(Gdx.files.internal("sprites/grid_grey_empty.png"));
        blueShipTexture = new Texture(Gdx.files.internal("sprites/grid_blue_ship.png"));
        greyShipTexture = new Texture(Gdx.files.internal("sprites/grid_grey_ship.png"));
        blueHoleTexture = new Texture(Gdx.files.internal("sprites/grid_blue_hole.png"));
        greyHoleTexture = new Texture(Gdx.files.internal("sprites/grid_grey_hole.png"));
        blueBurntTexture = new Texture(Gdx.files.internal("sprites/grid_blue_burnt.png"));
        greyBurntTexture = new Texture(Gdx.files.internal("sprites/grid_grey_burnt.png"));

        blueHoleAnimSheet = new Texture(Gdx.files.internal("sprites/grid_blue_hole_anim.png"));
        greyHoleAnimSheet = new Texture(Gdx.files.internal("sprites/grid_grey_hole_anim.png"));
        blueBurntAnimSheet = new Texture(Gdx.files.internal("sprites/grid_blue_burnt_anim.png"));
        greyBurntAnimSheet = new Texture(Gdx.files.internal("sprites/grid_grey_burnt_anim.png"));
        blueShipAnimSheet = new Texture(Gdx.files.internal("sprites/grid_blue_ship_anim.png"));
        greyShipAnimSheet = new Texture(Gdx.files.internal("sprites/grid_grey_ship_anim.png"));

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

    public void setBounds(float x, float y, float size, int gridSize) {
        this.boardX = x;
        this.boardY = y;
        this.boardSize = size;
        this.gridSize = gridSize;
        this.cellSize = size / gridSize;

        cellAnimations = new CellAnimationState[gridSize][gridSize];
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                cellAnimations[row][col] = new CellAnimationState();
            }
        }
    }

    public void drawGrid(boolean isTrackingBoard, boolean[][] shipGrid, int[][] hitGrid, float delta) {
        batch.begin();

        batch.setColor(Color.WHITE);

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                float x = boardX + col * cellSize;
                float y = boardY + (gridSize - 1 - row) * cellSize;

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

        int hitStatus = (hitGrid != null && row < hitGrid.length && col < hitGrid[row].length)
                        ? hitGrid[row][col] : 0;

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

        if (animState.isAnimating) {
            animState.animationTime += delta;
            float totalAnimDuration = FRAME_DURATION * FRAME_COUNT;
            if (animState.animationTime >= totalAnimDuration) {
                animState.isAnimating = false;
            }
        }

        boolean hasShip = !isTrackingBoard && shipGrid != null &&
                          row < shipGrid.length && col < shipGrid[row].length &&
                          shipGrid[row][col];

        if (hitStatus == 1) {
            if (animState.isAnimating && animState.type == AnimationType.BURNT) {
                Animation<TextureRegion> anim = useBlue ? blueBurntAnim : greyBurntAnim;
                return anim.getKeyFrame(animState.animationTime, false);
            } else {
                return new TextureRegion(useBlue ? blueBurntTexture : greyBurntTexture);
            }
        } else if (hitStatus == 2) {

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

        if (hasShip) {
            return new TextureRegion(useBlue ? blueShipTexture : greyShipTexture);
        }

        return new TextureRegion(useBlue ? blueEmptyTexture : greyEmptyTexture);
    }

    public void drawCoordinateLabels() {
        batch.begin();
        batch.setColor(Color.WHITE);

        for (int i = 0; i < gridSize; i++) {
            float x = boardX + i * cellSize + cellSize / 2 - 5;
            float y = boardY + boardSize + 20;
            font.draw(batch, String.valueOf(i + 1), x, y);

            x = boardX - 20;

            float screenY = boardY + (gridSize - 1 - i) * cellSize + cellSize / 2 + 5;
            font.draw(batch, String.valueOf(i + 1), x, screenY);
        }
        batch.end();
    }

    public void drawShipPreview(int row, int col, int length, boolean horizontal, boolean valid, boolean placementConfirmed) {
        batch.begin();

        Color oldColor = batch.getColor().cpy();

        if (placementConfirmed || !valid) {
            batch.setColor(1.0f, 0.3f, 0.3f, 0.7f);
        } else {
            batch.setColor(1.0f, 1.0f, 1.0f, 0.8f);
        }

        for (int i = 0; i < length; i++) {
            int r = horizontal ? row : row + i;
            int c = horizontal ? col + i : col;

            if (r >= 0 && r < gridSize && c >= 0 && c < gridSize) {
                float x = boardX + c * cellSize;
                float y = boardY + (gridSize - 1 - r) * cellSize;

                boolean useBlue = (r + c) % 2 == 0;
                Texture shipTexture = useBlue ? blueShipTexture : greyShipTexture;

                batch.draw(shipTexture, x, y, cellSize, cellSize);
            }
        }

        batch.setColor(oldColor);
        batch.end();
    }

    public void drawTargetingHighlight(int row, int col) {
        float x = boardX + col * cellSize;
        float y = boardY + (gridSize - 1 - row) * cellSize;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.RED);

        float centerX = x + cellSize / 2;
        float centerY = y + cellSize / 2;
        float crossSize = cellSize * 0.4f;
        float thickness = 5f;

        shapeRenderer.rectLine(centerX - crossSize, centerY, centerX + crossSize, centerY, thickness);
        shapeRenderer.rectLine(centerX, centerY - crossSize, centerX, centerY + crossSize, thickness);

        shapeRenderer.end();
    }

    public int[] screenToGrid(float screenX, float screenY) {
        if (screenX >= boardX && screenX <= boardX + boardSize &&
            screenY >= boardY && screenY <= boardY + boardSize) {

            int col = (int)((screenX - boardX) / cellSize);
            int screenRow = (int)((screenY - boardY) / cellSize);
            int row = gridSize - 1 - screenRow;

            if (row >= 0 && row < gridSize && col >= 0 && col < gridSize) {
                return new int[]{row, col};
            }
        }
        return null;
    }

    public float getBoardX() { return boardX; }
    public float getBoardY() { return boardY; }
    public float getBoardSize() { return boardSize; }
    public float getCellSize() { return cellSize; }
    public int getGridSize() { return gridSize; }

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
