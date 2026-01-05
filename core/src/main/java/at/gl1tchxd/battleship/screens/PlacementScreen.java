package at.gl1tchxd.battleship.screens;

import at.gl1tchxd.battleship.BattleshipGame;
import at.gl1tchxd.battleship.logic.Board;
import at.gl1tchxd.battleship.logic.GamePhase;
import at.gl1tchxd.battleship.logic.Ship;
import at.gl1tchxd.battleship.network.NetworkController;
import at.gl1tchxd.battleship.ui.GridRenderer;
import at.gl1tchxd.battleship.ui.PlacementInfoPanel;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class PlacementScreen implements Screen {
    private final BattleshipGame game;
    private Stage stage;
    private Texture background;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;

    // Helper classes
    private GridRenderer gridRenderer;
    private PlacementInfoPanel infoPanel;

    // Board positioning
    private float boardX;
    private float boardY;
    private float boardSize;
    private int gridSize;

    // Ship selection and placement
    private int selectedShipClass = 0;
    private int selectedShipIndex = 0;
    private boolean horizontalPlacement = true;
    private int hoveredRow = -1;
    private int hoveredCol = -1;

    // Ship config tracking
    private final int[] shipCounts = {1, 1, 1, 1, 1};
    private final int[] shipPlaced = {0, 0, 0, 0, 0};
    private final String[] shipClassNames = {"Carrier", "Battleship", "Cruiser", "Submarine", "Destroyer"};
    private final int[] shipLengths = {5, 4, 3, 3, 2};
    private final boolean[][] shipInstancePlaced = new boolean[5][10]; // Track which specific ships are placed
    private int totalShipCount; // Total number of ship instances across all classes
    private boolean configLoaded = false; // Track if ship config has been loaded from game

    public PlacementScreen(BattleshipGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        background = new Texture(Gdx.files.internal("sprites/shared_background.png"));

        Gdx.input.setInputProcessor(stage);

        // Reset placement state on show to avoid carrying over readiness from previous rounds
        if (game != null && game.getGameController() != null) {
            game.getGameController().setPlacementComplete(false);
            game.getGameController().setOpponentReady(false);
            game.getGameController().setGamePhase(GamePhase.PLACEMENT);
            // also clear any previous boards if necessary
            // Note: initializeGame will create fresh boards when hosting/joining, but ensure clean state here
        }

        // Register disconnection callback so that if connection is severed during placement
        // we return to the ConnectScreen (host/join screen).
        if (game.getNetworkController() != null) {
            game.getNetworkController().setDisconnectionCallback(new NetworkController.DisconnectionCallback() {
                @Override
                public void onDisconnected(boolean opponentDisconnected) {
                    // Only act if we're in placement phase
                    if (game.getGameController().getGamePhase() == GamePhase.PLACEMENT) {
                        Gdx.app.postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                // Reset UI and go back to connect screen
                                game.setScreen(new ConnectScreen(game));
                            }
                        });
                    }
                }
            });
        }
    }

    private boolean uiInitialized = false;

    private void initializeUI() {
        if (uiInitialized) return;
        if (!isGameReady()) return;

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        boardSize = screenHeight * 0.8f;
        boardX = 50;
        boardY = (screenHeight - boardSize) / 2;
        gridSize = game.getGameController().getMyBoard().getSize();

        gridRenderer = new GridRenderer(shapeRenderer, batch, game.getSkin().getFont("default"));
        gridRenderer.setBounds(boardX, boardY, boardSize, gridSize);

        totalShipCount = 0;
        for (int count : shipCounts) {
            totalShipCount += count;
        }

        float panelX = boardX + boardSize + 30;
        float panelMaxHeight = boardSize; // Match board height
        infoPanel = new PlacementInfoPanel(game.getSkin(), stage, shipClassNames, shipLengths, shipCounts);

        // Add the two tables to the stage so we can position them in columns
        stage.addActor(infoPanel.getShipListTable());
        stage.addActor(infoPanel.getControlsTable());

        // Compute available width for side panels and layout in two columns (ship list + controls)
        float spacing = 10f;
        float rightMargin = 20f;
        float remainingWidth = screenWidth - panelX - rightMargin;
        float minShipListWidth = 240f;
        float minControlsWidth = 160f;

        if (remainingWidth >= minShipListWidth + spacing + minControlsWidth) {
            float shipListWidth = Math.max(minShipListWidth, remainingWidth * 0.6f);
            float controlsWidth = remainingWidth - shipListWidth - spacing;

            infoPanel.getShipListTable().pack();
            infoPanel.getShipListTable().setSize(shipListWidth, panelMaxHeight);
            infoPanel.getShipListTable().setPosition(panelX, boardY);

            infoPanel.getControlsTable().pack();
            infoPanel.getControlsTable().setSize(controlsWidth, panelMaxHeight);
            infoPanel.getControlsTable().setPosition(panelX + shipListWidth + spacing, boardY);
        } else {
            // Not enough horizontal space: fall back to compact/default widths using setPosition
            infoPanel.setPosition(panelX, boardY, panelMaxHeight);
            // Ensure the tables are positioned as setPosition determined
            // setPosition already sizes and positions internal tables
            // but we still want them to be visible as stage actors (already added)
        }

        infoPanel.setCallback(new PlacementInfoPanel.PlacementCallback() {
            @Override
            public void onAutoPlace() {
                handleAutoPlace();
            }

            @Override
            public void onConfirmPlacement() {
                handleConfirmPlacement();
            }

            @Override
            public void onReset() {
                handleReset();
            }

            @Override
            public void onExit() {
                exitToMainMenu();
            }
        });


        uiInitialized = true;
    }

    private void exitToMainMenu() {
        // Clean up network connections
        if (game.getNetworkController() != null) {
            if (game.getNetworkController().isHost()) {
                game.getNetworkController().stopHosting();
            } else {
                game.getNetworkController().stopJoining();
            }
        }

        // Return to main menu
        game.setScreen(new MainMenuScreen(game));
    }

    private boolean isGameReady() {
        return game.getGameController().getGame() != null &&
               game.getGameController().getMyBoard() != null;
    }

    private void handleAutoPlace() {
        if (!isGameReady()) return;

        game.getGameController().clear();
        boolean success = game.getGameController().autoPlaceAll();
        if (success) {
            for (int i = 0; i < 5; i++) {
                shipPlaced[i] = shipCounts[i];
                updateShipCountDisplay(i);
            }
            infoPanel.setConfirmEnabled(true);
        }
    }

    private void handleConfirmPlacement() {
        if (!isGameReady()) return;

        game.getGameController().confirmPlacement();
        game.getNetworkController().sendPlayerReady();
    }

    private void handleReset() {
        if (!isGameReady()) return;

        // Clear the board
        game.getGameController().clear();

        // Reset all placement tracking
        for (int i = 0; i < shipPlaced.length; i++) {
            shipPlaced[i] = 0;
        }

        // Reset ship instance tracking
        for (int i = 0; i < shipInstancePlaced.length; i++) {
            for (int j = 0; j < shipInstancePlaced[i].length; j++) {
                shipInstancePlaced[i][j] = false;
            }
        }

        // Reset UI
        infoPanel.resetShipCounts();

        // Select first ship
        selectedShipClass = 0;
        selectedShipIndex = 0;
        updateCurrentShipLabel();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.15f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        // If game not ready yet, show waiting message
        if (!isGameReady()) {
            batch.begin();
            game.getSkin().getFont("default").draw(batch, "Waiting for game configuration...",
                Gdx.graphics.getWidth() / 2f - 150, Gdx.graphics.getHeight() / 2f);
            batch.end();
            stage.act(delta);
            stage.draw();
            return;
        }

        // Initialize UI once game is ready
        initializeUI();
        if (!uiInitialized) {
            stage.act(delta);
            stage.draw();
            return;
        }

        // Load config once game is ready
        if (!configLoaded) {
            loadShipConfig();
            configLoaded = true;
        }

        handleInput();

        // Get placed ships
        boolean[][] shipGrid = null;
        Board board = game.getGameController().getMyBoard();
        if (board != null) {
            shipGrid = new boolean[gridSize][gridSize];
            for (int row = 0; row < gridSize; row++) {
                for (int col = 0; col < gridSize; col++) {
                    Ship ship = board.getShipAt(row, col);
                    shipGrid[row][col] = (ship != null);
                }
            }
        }

        // Draw grid with placed ships (isTrackingBoard = false, no hitGrid during placement)
        gridRenderer.drawGrid(false, shipGrid, null, delta);
        gridRenderer.drawCoordinateLabels();


        // Draw hover preview on top of ships
        if (hoveredRow >= 0 && hoveredCol >= 0 && selectedShipClass < shipLengths.length) {
            int shipLength = shipLengths[selectedShipClass];
            boolean canPlace = checkCanPlace(hoveredRow, hoveredCol, shipLength, horizontalPlacement);
            boolean placementConfirmed = game.getGameController().isPlacementComplete();
            gridRenderer.drawShipPreview(hoveredRow, hoveredCol, shipLength, horizontalPlacement, canPlace, placementConfirmed);
        }

        stage.act(delta);
        stage.draw();

        if (game.getGameController().getGamePhase() == GamePhase.BATTLE) game.setScreen(new GameScreen(game));
    }

    private void loadShipConfig() {
        if (game.getGameController().getGame() != null) {
            int[] config = game.getGameController().getGame().getShipConfig();
            if (config != null) {
                System.arraycopy(config, 0, shipCounts, 0, Math.min(config.length, shipCounts.length));

                // Recalculate total ship count
                totalShipCount = 0;
                for (int count : shipCounts) {
                    totalShipCount += count;
                }

                // Reset selection to valid range
                selectedShipClass = 0;
                selectedShipIndex = 0;

                // Update info panel with new config
                infoPanel.updateAllShipCounts(shipCounts, shipClassNames, shipLengths);
                updateCurrentShipLabel();
            }
        }
    }

    private void handleInput() {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        int[] gridCoords = gridRenderer.screenToGrid(mouseX, mouseY);
        if (gridCoords != null) {
            hoveredRow = gridCoords[0];
            hoveredCol = gridCoords[1];

            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                placeShip(hoveredRow, hoveredCol);
            }

            if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
                horizontalPlacement = !horizontalPlacement;
                infoPanel.setOrientation(horizontalPlacement);
            }

            // Middle click or Delete key - remove ship at this position
            if (Gdx.input.isButtonJustPressed(Input.Buttons.MIDDLE) || Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL)) {
                removeShipAt(hoveredRow, hoveredCol);
            }
        } else {
            hoveredRow = -1;
            hoveredCol = -1;
        }

        // Keyboard shortcuts for ship selection
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) selectShipClass(0);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) selectShipClass(1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) selectShipClass(2);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) selectShipClass(3);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) selectShipClass(4);

        // Tab to cycle through ship instances within selected class
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) {
                selectPreviousShipInstance();
            } else {
                selectNextShipInstance();
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            horizontalPlacement = !horizontalPlacement;
            infoPanel.setOrientation(horizontalPlacement);
        }

         if (game.getGameController().isOpponentReady()) {
             infoPanel.setOpponentReady(true);
         }
    }

    private void placeShip(int row, int col) {
        if (row < 0 || col < 0) return;
        if (!isGameReady()) return;

        // Check if this specific ship was already placed
        boolean wasPlaced = shipInstancePlaced[selectedShipClass][selectedShipIndex];

        int fleetIndex = calculateFleetIndex(selectedShipClass, selectedShipIndex);

        boolean success = game.getGameController().placeShip(
            fleetIndex, row, col, horizontalPlacement
        );

        if (success) {
            // Mark this specific ship instance as placed
            if (!wasPlaced) {
                // First time placing this ship, increment counters
                shipInstancePlaced[selectedShipClass][selectedShipIndex] = true;
                shipPlaced[selectedShipClass]++;
                updateShipCountDisplay(selectedShipClass);
                checkAllShipsPlaced();
            }
            // Ship was moved successfully, automatically select next ship
            if (!(fleetIndex == totalShipCount - 1)) selectNextShipInstance();
        }
    }

    private void removeShipAt(int row, int col) {
        if (row < 0 || col < 0) return;
        if (!isGameReady()) return;

        // Find the ship at this position
        Ship ship = game.getGameController().getMyBoard().getShipAt(row, col);
        if (ship == null) return;

        // Remove the ship from the board
        boolean removed = game.getGameController().getMyBoard().removeShip(ship);
        if (!removed) return;

        // Find which ship instance this was and mark it as not placed
        boolean found = false;
        for (int classIdx = 0; classIdx < 5 && !found; classIdx++) {
            for (int shipIdx = 0; shipIdx < shipCounts[classIdx] && !found; shipIdx++) {
                if (shipInstancePlaced[classIdx][shipIdx]) {
                    int fleetIndex = calculateFleetIndex(classIdx, shipIdx);
                    Ship fleetShip = game.getGameController().getGame().getFleet().get(classIdx)[shipIdx];

                    if (fleetShip == ship) {
                        // Found the ship! Mark it as not placed
                        shipInstancePlaced[classIdx][shipIdx] = false;
                        shipPlaced[classIdx]--;
                        updateShipCountDisplay(classIdx);
                        checkAllShipsPlaced();

                        // Select this ship for potential re-placement
                        selectedShipClass = classIdx;
                        selectedShipIndex = shipIdx;
                        updateCurrentShipLabel();

                        found = true;
                    }
                }
            }
        }
    }

    private int calculateFleetIndex(int shipClass, int index) {
        int fleetIndex = 0;
        for (int i = 0; i < shipClass; i++) {
            fleetIndex += shipCounts[i];
        }
        fleetIndex += index;
        return fleetIndex;
    }

    private void selectShipClass(int shipClass) {
        if (shipClass < 0 || shipClass >= 5) return;

        selectedShipClass = shipClass;
        selectedShipIndex = 0; // Start at first instance

        updateCurrentShipLabel();
    }

    private void selectNextUnplacedShip() {
        // Find next unplaced ship starting from current position
        for (int classIdx = 0; classIdx < 5; classIdx++) {
            for (int shipIdx = 0; shipIdx < shipCounts[classIdx]; shipIdx++) {
                if (!shipInstancePlaced[classIdx][shipIdx]) {
                    selectedShipClass = classIdx;
                    selectedShipIndex = shipIdx;
                    updateCurrentShipLabel();
                    return;
                }
            }
        }
        // If no unplaced ships found, stay on current selection
    }

    private void selectNextShipInstance() {
        // Calculate current flat index
        int currentFlatIndex = calculateFleetIndex(selectedShipClass, selectedShipIndex);

        // Move to next ship (wraps around)
        int nextFlatIndex = (currentFlatIndex + 1) % totalShipCount;

        // Convert flat index back to class and instance
        int[] classAndIndex = flatIndexToClassAndIndex(nextFlatIndex);
        selectedShipClass = classAndIndex[0];
        selectedShipIndex = classAndIndex[1];

        updateCurrentShipLabel();
    }

    private void selectPreviousShipInstance() {
        // Calculate current flat index
        int currentFlatIndex = calculateFleetIndex(selectedShipClass, selectedShipIndex);

        // Move to previous ship (wraps around)
        int prevFlatIndex = currentFlatIndex - 1;
        if (prevFlatIndex < 0) {
            prevFlatIndex = totalShipCount - 1;
        }

        // Convert flat index back to class and instance
        int[] classAndIndex = flatIndexToClassAndIndex(prevFlatIndex);
        selectedShipClass = classAndIndex[0];
        selectedShipIndex = classAndIndex[1];

        updateCurrentShipLabel();
    }

    /**
     * Convert a flat fleet index to [shipClass, shipIndex] array.
     */
    private int[] flatIndexToClassAndIndex(int flatIndex) {
        int accumulated = 0;
        for (int classIdx = 0; classIdx < 5; classIdx++) {
            if (flatIndex < accumulated + shipCounts[classIdx]) {
                return new int[]{classIdx, flatIndex - accumulated};
            }
            accumulated += shipCounts[classIdx];
        }
        // Should not reach here if flatIndex is valid
        return new int[]{0, 0};
    }

    private void updateCurrentShipLabel() {
        String shipName = shipClassNames[selectedShipClass];
        int displayIndex = selectedShipIndex + 1;
        infoPanel.setCurrentShip(shipName, displayIndex);
    }

    private void updateShipCountDisplay(int shipClass) {
        infoPanel.updateShipCount(shipClass, shipPlaced[shipClass], shipCounts[shipClass],
                                  shipClassNames[shipClass], shipLengths[shipClass]);
    }

    private void checkAllShipsPlaced() {
        boolean allPlaced = true;
        for (int i = 0; i < 5; i++) {
            if (shipPlaced[i] < shipCounts[i]) {
                allPlaced = false;
                break;
            }
        }
        infoPanel.setConfirmEnabled(allPlaced);
    }

    private boolean checkCanPlace(int row, int col, int length, boolean horizontal) {
        if (row < 0 || col < 0) return false;
        if (!isGameReady()) return false;
        if (selectedShipClass >= shipCounts.length || shipCounts.length == 0) return false;

        int fleetIndex = calculateFleetIndex(selectedShipClass, selectedShipIndex);

        // Check if fleet has this ship class
        if (!game.getGameController().getGame().getFleet().containsKey(selectedShipClass)) return false;

        Ship[] shipsOfClass = game.getGameController().getGame().getFleet().get(selectedShipClass);
        if (shipsOfClass == null || selectedShipIndex >= shipsOfClass.length) return false;

        Ship ship = shipsOfClass[selectedShipIndex];
        if (ship == null) return false;

        return game.getGameController().getMyBoard().canPlace(ship, row, col, horizontal);
    }


    @Override
    public void resize(int width, int height) {
        // The resize method can be called before show()/initializeUI() completed.
        // Guard against null pointers by updating only when components are available.
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }

        // Recalculate board dimensions (safe to do even if render/UI not initialized)
        boardSize = height * 0.8f;
        boardX = 50;
        boardY = (height - boardSize) / 2;

        // Ensure we have a reasonable gridSize. Try to read from the game controller when available,
        // otherwise keep existing value or fall back to 10.
        if (game != null && game.getGameController() != null && game.getGameController().getMyBoard() != null) {
            gridSize = game.getGameController().getMyBoard().getSize();
        } else if (gridSize <= 0) {
            gridSize = 10; // sensible default until real board is available
        }

        // Update grid renderer bounds only if it has been initialized
        if (gridRenderer != null) {
            gridRenderer.setBounds(boardX, boardY, boardSize, gridSize);
        }

        // Reposition info panel with max height if available
        float panelX = boardX + boardSize + 30;
        float panelMaxHeight = boardSize;
        if (infoPanel != null) {
            float screenWidth = width;
            float spacing = 10f;
            float rightMargin = 20f;
            float remainingWidth = screenWidth - panelX - rightMargin;
            float minShipListWidth = 240f;
            float minControlsWidth = 160f;

            if (remainingWidth >= minShipListWidth + spacing + minControlsWidth) {
                float shipListWidth = Math.max(minShipListWidth, remainingWidth * 0.6f);
                float controlsWidth = remainingWidth - shipListWidth - spacing;

                infoPanel.getShipListTable().setSize(shipListWidth, panelMaxHeight);
                infoPanel.getShipListTable().setPosition(panelX, boardY);

                infoPanel.getControlsTable().setSize(controlsWidth, panelMaxHeight);
                infoPanel.getControlsTable().setPosition(panelX + shipListWidth + spacing, boardY);
            } else {
                // Fallback to default positioning
                infoPanel.setPosition(panelX, boardY, panelMaxHeight);
            }
        }
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        // Clear disconnection callback to avoid holding references when leaving the screen
        if (game.getNetworkController() != null) {
            game.getNetworkController().setDisconnectionCallback(null);
        }
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (background != null) background.dispose();
        if (gridRenderer != null) gridRenderer.dispose();
        if (infoPanel != null) infoPanel.dispose();
    }
}
