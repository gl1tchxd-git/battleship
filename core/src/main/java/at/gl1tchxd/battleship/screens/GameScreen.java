package at.gl1tchxd.battleship.screens;

import at.gl1tchxd.battleship.BattleshipGame;
import at.gl1tchxd.battleship.logic.Board;
import at.gl1tchxd.battleship.logic.GamePhase;
import at.gl1tchxd.battleship.logic.Ship;
import at.gl1tchxd.battleship.network.NetworkController;
import at.gl1tchxd.battleship.ui.GameInfoPanel;
import at.gl1tchxd.battleship.ui.GridRenderer;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.Map;

public class GameScreen implements Screen {
    private final BattleshipGame game;
    private Stage stage;
    private SpriteBatch batch;
    private Texture background;
    private ShapeRenderer shapeRenderer;

    private GridRenderer trackingGridRenderer; // Opponent's board (for attacking)
    private GridRenderer myGridRenderer;       // Own board (showing ships & hits)
    private GameInfoPanel infoPanel;

    private float trackingBoardX;
    private float trackingBoardY;
    private float trackingBoardSize;
    private float myBoardX;
    private float myBoardY;
    private float myBoardSize;
    private int gridSize;

    private int hoveredRow = -1;
    private int hoveredCol = -1;

    private int[] shipCounts = {1, 1, 1, 1, 1};

    public GameScreen(BattleshipGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        batch = new SpriteBatch();
        background = new Texture(Gdx.files.internal("sprites/shared_background.png"));
        shapeRenderer = new ShapeRenderer();

        Gdx.input.setInputProcessor(stage);

        // Register a simple disconnection callback to ensure UI updates if connection is severed
        if (game.getNetworkController() != null) {
            game.getNetworkController().setDisconnectionCallback(new NetworkController.DisconnectionCallback() {
                @Override
                public void onDisconnected(boolean opponentDisconnected) {
                    // If the disconnection occurred during battle, GameController phase was already set
                    // to GAME_WON or GAME_LOST in NetworkController; we just ensure the UI reflects it.
                    // Optionally we could schedule a return to ConnectScreen here, but we'll let the
                    // player view the victory/defeat screen and press ESC/Enter to return.
                }
            });
        }

        initializeUI();
    }

    private void initializeUI() {
        if (game.getGameController().getGame() == null) return;

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        gridSize = game.getGameController().getMyBoard().getSize();

        // Load ship config from game
        int[] config = game.getGameController().getGame().getShipConfig();
        if (config != null && config.length == 5) {
            shipCounts = config.clone();
        }

        float padding = 30;

        trackingBoardSize = screenHeight * 0.8f;
        trackingBoardX = padding;
        trackingBoardY = (screenHeight - trackingBoardSize) / 2;

        trackingGridRenderer = new GridRenderer(shapeRenderer, batch, game.getSkin().getFont("default"));
        trackingGridRenderer.setBounds(trackingBoardX, trackingBoardY, trackingBoardSize, gridSize);

        myBoardSize = screenHeight * 0.35f;
        myBoardX = screenWidth - myBoardSize - padding;
        myBoardY = screenHeight - myBoardSize - padding - 30; // 30 for label space

        myGridRenderer = new GridRenderer(shapeRenderer, batch, game.getSkin().getFont("default"));
        myGridRenderer.setBounds(myBoardX, myBoardY, myBoardSize, gridSize);

        float panelX = trackingBoardX + trackingBoardSize + padding;
        float panelWidth = myBoardX - panelX - padding;
        float panelHeight = trackingBoardSize;
        float panelY = trackingBoardY;

        infoPanel = new GameInfoPanel(game.getSkin(), stage, shipCounts);
        infoPanel.setPosition(panelX, panelY, panelWidth, panelHeight);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.15f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        if (game.getGameController().getGame() == null) {
            batch.begin();
            game.getSkin().getFont("default").draw(batch, "Waiting for game...",
                Gdx.graphics.getWidth() / 2f - 100, Gdx.graphics.getHeight() / 2f);
            batch.end();
            stage.act(delta);
            stage.draw();
            return;
        }

        handleInput();
        updateUI();
        drawTrackingBoard();
        drawMyBoard();
        drawBoardLabels();

        stage.act(delta);
        stage.draw();

        // If the game has ended (win or loss), allow returning to connect screen with ESC or Enter
        GamePhase phase = game.getGameController().getGamePhase();
        if (phase == GamePhase.GAME_WON || phase == GamePhase.GAME_LOST) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                // Ensure network cleaned up and go back to connect screen
                if (game.getNetworkController() != null) {
                    if (game.getNetworkController().isHost()) game.getNetworkController().stopHosting();
                    else game.getNetworkController().stopJoining();
                }
                game.setScreen(new ConnectScreen(game));
            }
        }
    }

    private void handleInput() {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        // Check if hovering over tracking board (for attacking)
        int[] gridCoords = trackingGridRenderer.screenToGrid(mouseX, mouseY);
        if (gridCoords != null) {
            hoveredRow = gridCoords[0];
            hoveredCol = gridCoords[1];

            // Left click to attack
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                performAttack(hoveredRow, hoveredCol);
            }
        } else {
            hoveredRow = -1;
            hoveredCol = -1;
        }
    }

    private void performAttack(int row, int col) {
        if (!game.getGameController().isMyTurn()) return;

        // Send attack to opponent via network (defensive null-check)
        NetworkController nc = game.getNetworkController();
        if (nc != null) {
            nc.sendAttack(row, col);
        }
    }

    private void updateUI() {
        // Update turn indicator
        infoPanel.setMyTurn(game.getGameController().isMyTurn());

        // Update my fleet status
        updateMyFleetStatus();

        // Update opponent fleet status from tracking board
        updateOpponentFleetStatus();

        // Update status based on game phase
        GamePhase phase = game.getGameController().getGamePhase();
        switch (phase) {
            case BATTLE:
                if (game.getGameController().isMyTurn()) {
                    infoPanel.setStatus("Click on enemy board to attack!", Color.GREEN);
                } else {
                    infoPanel.setStatus("Waiting for opponent...", Color.YELLOW);
                }
                break;
            case GAME_WON:
                infoPanel.setStatus("VICTORY! Press Enter or ESC to return.", Color.GREEN);
                break;
            case GAME_LOST:
                infoPanel.setStatus("DEFEAT! Press Enter or ESC to return.", Color.RED);
                break;
            default:
                infoPanel.setStatus("Battle in Progress", Color.CYAN);
        }
    }

    private void updateMyFleetStatus() {
        if (game.getGameController().getGame() == null) return;

        Map<Integer, Ship[]> fleet = game.getGameController().getGame().getFleet();
        int[] alivePerClass = new int[5];
        int[] totalPerClass = new int[5];

        for (int classIdx = 0; classIdx < 5; classIdx++) {
            Ship[] ships = fleet.get(classIdx);
            if (ships != null) {
                totalPerClass[classIdx] = ships.length;
                for (Ship ship : ships) {
                    if (!ship.isSunk()) {
                        alivePerClass[classIdx]++;
                    }
                }
            }
            infoPanel.updateMyShipCount(classIdx, alivePerClass[classIdx], totalPerClass[classIdx]);
        }
    }

    private void updateOpponentFleetStatus() {
        Board trackingBoard = game.getGameController().getTrackingBoard();
        if (trackingBoard == null || !trackingBoard.isTrackingMode()) {
            // No tracking board yet, show initial counts from ship config
            for (int i = 0; i < 5; i++) {
                infoPanel.updateOpponentShipCount(i, shipCounts[i], shipCounts[i]);
            }
            return;
        }

        int[][] sunkShips = trackingBoard.getTrackingSunkShips();
        if (sunkShips != null) {
            for (int i = 0; i < 5 && i < sunkShips.length; i++) {
                int sunk = sunkShips[i][0];
                // Use shipCounts as total if tracking board total is 0 (not yet initialized)
                int total = sunkShips[i][1] > 0 ? sunkShips[i][1] : shipCounts[i];
                int remaining = total - sunk;
                infoPanel.updateOpponentShipCount(i, remaining, total);
            }
        } else {
            // No sunk data yet, show initial counts from ship config
            for (int i = 0; i < 5; i++) {
                infoPanel.updateOpponentShipCount(i, shipCounts[i], shipCounts[i]);
            }
        }
    }

    private void drawTrackingBoard() {
        // Draw grid
        trackingGridRenderer.drawGrid();
        trackingGridRenderer.drawCoordinateLabels();

        // Get tracking board hits/misses
        Board trackingBoard = game.getGameController().getTrackingBoard();
        if (trackingBoard != null) {
            int[][] hitGrid = buildHitGrid(trackingBoard);
            trackingGridRenderer.drawHitsAndMisses(hitGrid);
        }

        // Draw hover highlight for attack targeting
        if (hoveredRow >= 0 && hoveredCol >= 0 && game.getGameController().isMyTurn()) {
            drawTargetingHighlight(hoveredRow, hoveredCol);
        }
    }

    private void drawTargetingHighlight(int row, int col) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1, 1, 0, 0.3f); // Yellow transparent

        float cellSize = trackingGridRenderer.getCellSize();
        float x = trackingGridRenderer.getBoardX() + col * cellSize;
        // Flip Y: row 0 should be at top
        float y = trackingGridRenderer.getBoardY() + (gridSize - 1 - row) * cellSize;
        shapeRenderer.rect(x, y, cellSize, cellSize);

        shapeRenderer.end();

        // Draw crosshair
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.RED);
        float centerX = x + cellSize / 2;
        float centerY = y + cellSize / 2;
        float crossSize = cellSize * 0.3f;

        // Crosshair lines
        shapeRenderer.line(centerX - crossSize, centerY, centerX + crossSize, centerY);
        shapeRenderer.line(centerX, centerY - crossSize, centerX, centerY + crossSize);

        shapeRenderer.end();
    }

    private void drawMyBoard() {
        // Draw grid
        myGridRenderer.drawGrid();
        myGridRenderer.drawCoordinateLabels();

        // Draw placed ships
        Board myBoard = game.getGameController().getMyBoard();
        if (myBoard != null) {
            boolean[][] shipGrid = new boolean[gridSize][gridSize];
            for (int row = 0; row < gridSize; row++) {
                for (int col = 0; col < gridSize; col++) {
                    Ship ship = myBoard.getShipAt(row, col);
                    shipGrid[row][col] = (ship != null);
                }
            }
            myGridRenderer.drawShips(shipGrid);

            // Draw hits and misses on my board
            int[][] hitGrid = buildHitGrid(myBoard);
            myGridRenderer.drawHitsAndMisses(hitGrid);
        }
    }

    private int[][] buildHitGrid(Board board) {
        int[][] hitGrid = new int[gridSize][gridSize];
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                String cellInfo = board.getCellInfo(row, col);
                switch (cellInfo) {
                    case "MISS":
                        hitGrid[row][col] = 1;
                        break;
                    case "HIT":
                        hitGrid[row][col] = 2;
                        break;
                    default:
                        hitGrid[row][col] = 0;
                }
            }
        }
        return hitGrid;
    }

    private void drawBoardLabels() {
        batch.begin();

        // Label for tracking board
        game.getSkin().getFont("default").draw(batch, "ENEMY WATERS (Attack Here)",
            trackingBoardX, trackingBoardY + trackingBoardSize + 40);

        // Label for my board
        game.getSkin().getFont("default").draw(batch, "YOUR FLEET",
            myBoardX, myBoardY + myBoardSize + 40);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);

        // Recalculate board dimensions
        gridSize = game.getGameController().getMyBoard().getSize();
        float padding = 30;

        // Tracking board (left column)
        trackingBoardSize = height * 0.8f;
        trackingBoardX = padding;
        trackingBoardY = (height - trackingBoardSize) / 2;
        trackingGridRenderer.setBounds(trackingBoardX, trackingBoardY, trackingBoardSize, gridSize);

        // My board (right column, top)
        myBoardSize = height * 0.35f;
        myBoardX = width - myBoardSize - padding;
        myBoardY = height - myBoardSize - padding - 30;
        myGridRenderer.setBounds(myBoardX, myBoardY, myBoardSize, gridSize);

        // Reposition info panel (center column, same height as tracking board)
        float panelX = trackingBoardX + trackingBoardSize + padding;
        float panelWidth = myBoardX - panelX - padding;
        float panelHeight = trackingBoardSize;
        float panelY = trackingBoardY;
        infoPanel.setPosition(panelX, panelY, panelWidth, panelHeight);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (infoPanel != null) infoPanel.dispose();
    }
}
