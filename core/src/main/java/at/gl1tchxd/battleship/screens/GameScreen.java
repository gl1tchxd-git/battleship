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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.Map;

public class GameScreen implements Screen {
    private final BattleshipGame game;
    private Stage stage;
    private SpriteBatch batch;
    private Texture background;
    private ShapeRenderer shapeRenderer;

    private GridRenderer trackingGridRenderer;
    private GridRenderer myGridRenderer;
    private GameInfoPanel infoPanel;
    private ImageTextButton exitButton;

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

    private static final float END_SCREEN_DELAY = 1.0f;
    private float endGameTimer = 0f;
    private boolean gameEnded = false;
    private boolean gameWon = false;

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

        if (game.getNetworkController() != null) {
            game.getNetworkController().setDisconnectionCallback(new NetworkController.DisconnectionCallback() {
                @Override
                public void onDisconnected(boolean opponentDisconnected) {
                    //
                }
            });

            game.getNetworkController().setAttackSoundCallback(new NetworkController.AttackSoundCallback() {
                @Override
                public void onAttackSound(boolean hit) {
                    if (hit) {
                        game.playHitSound();
                    } else {
                        game.playMissSound();
                    }
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

        int[] config = game.getGameController().getGame().getShipConfig();
        if (config != null && config.length == 5) {
            shipCounts = config.clone();
        }

        float padding = 40;
        float borderPadding = 50;
        float columnPadding = 60;
        float gridVerticalOffset = 20;

        trackingBoardSize = screenHeight * 0.65f;
        trackingBoardX = padding + borderPadding;
        trackingBoardY = (screenHeight - trackingBoardSize) / 2 - gridVerticalOffset;

        trackingGridRenderer = new GridRenderer(shapeRenderer, batch, game.getSkin().getFont("default"));
        trackingGridRenderer.setBounds(trackingBoardX, trackingBoardY, trackingBoardSize, gridSize);

        myBoardSize = screenHeight * 0.35f;
        myBoardX = screenWidth - myBoardSize - padding - borderPadding;
        myBoardY = screenHeight - myBoardSize - padding - borderPadding - 30 - gridVerticalOffset;

        myGridRenderer = new GridRenderer(shapeRenderer, batch, game.getSkin().getFont("default"));
        myGridRenderer.setBounds(myBoardX, myBoardY, myBoardSize, gridSize);

        float panelX = trackingBoardX + trackingBoardSize + columnPadding;
        float panelWidth = myBoardX - panelX - columnPadding;
        float panelHeight = trackingBoardSize;
        float panelY = trackingBoardY;

        infoPanel = new GameInfoPanel(game.getSkin(), stage, shipCounts);
        infoPanel.setPosition(panelX, panelY, panelWidth, panelHeight);

        exitButton = new ImageTextButton("EXIT", game.getSkin());
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClickSound();
                exitToMainMenu();
            }
        });

        float buttonWidth = exitButton.getWidth() * 0.5f;
        float buttonHeight = exitButton.getHeight() * 0.5f;
        exitButton.setSize(buttonWidth, buttonHeight);

        float exitButtonX = myBoardX;
        float exitButtonY = myBoardY - buttonHeight - 20;
        exitButton.setPosition(exitButtonX, exitButtonY);

        stage.addActor(exitButton);
    }

    private void exitToMainMenu() {
        game.getGameController().resetGame();

        if (game.getNetworkController() != null) {
            if (game.getNetworkController().isHost()) {
                game.getNetworkController().stopHosting();
            } else {
                game.getNetworkController().stopJoining();
            }
        }

        game.setScreen(new MainMenuScreen(game));
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


        GamePhase phase = game.getGameController().getGamePhase();
        if (phase == GamePhase.GAME_WON || phase == GamePhase.GAME_LOST) {
            if (!gameEnded) {

                gameEnded = true;
                gameWon = (phase == GamePhase.GAME_WON);
                endGameTimer = 0f;
            } else {

                endGameTimer += delta;
                if (endGameTimer >= END_SCREEN_DELAY) {

                    game.setScreen(new EndScreen(game, gameWon));
                }
            }
        }
    }

    private void handleInput() {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        int[] gridCoords = trackingGridRenderer.screenToGrid(mouseX, mouseY);
        if (gridCoords != null) {
            hoveredRow = gridCoords[0];
            hoveredCol = gridCoords[1];

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

        Board trackingBoard = game.getGameController().getTrackingBoard();
        if (trackingBoard != null) {
            String cellInfo = trackingBoard.getCellInfo(row, col);
            if (cellInfo.equals("HIT") || cellInfo.equals("MISS")) {
                return;
            }
        }

        if (game.getNetworkController() != null) {
            if (game.getGameController().getTrackingBoard().getCellInfo(row, col) != "EMPTY");
            game.getNetworkController().sendAttack(row, col);
        }
    }

    private void updateUI() {
        infoPanel.setMyTurn(game.getGameController().isMyTurn());
        updateMyFleetStatus();
        updateOpponentFleetStatus();

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
            for (int i = 0; i < 5; i++) {
                infoPanel.updateOpponentShipCount(i, shipCounts[i], shipCounts[i]);
            }
            return;
        }

        int[][] sunkShips = trackingBoard.getTrackingSunkShips();
        if (sunkShips != null) {
            for (int i = 0; i < 5 && i < sunkShips.length; i++) {
                int sunk = sunkShips[i][0];
                int total = sunkShips[i][1] > 0 ? sunkShips[i][1] : shipCounts[i];
                int remaining = total - sunk;
                infoPanel.updateOpponentShipCount(i, remaining, total);
            }
        } else {
            for (int i = 0; i < 5; i++) {
                infoPanel.updateOpponentShipCount(i, shipCounts[i], shipCounts[i]);
            }
        }
    }

    private void drawTrackingBoard() {
        Board trackingBoard = game.getGameController().getTrackingBoard();
        int[][] hitGrid = null;
        if (trackingBoard != null) {
            hitGrid = buildHitGrid(trackingBoard);
        }

        trackingGridRenderer.drawGrid(true, null, hitGrid, Gdx.graphics.getDeltaTime());
        trackingGridRenderer.drawCoordinateLabels();

        if (hoveredRow >= 0 && hoveredCol >= 0 && game.getGameController().isMyTurn()) {
            trackingGridRenderer.drawTargetingHighlight(hoveredRow, hoveredCol);
        }
    }

    private void drawMyBoard() {
        Board myBoard = game.getGameController().getMyBoard();
        boolean[][] shipGrid = null;
        int[][] hitGrid = null;

        if (myBoard != null) {
            shipGrid = new boolean[gridSize][gridSize];
            for (int row = 0; row < gridSize; row++) {
                for (int col = 0; col < gridSize; col++) {
                    Ship ship = myBoard.getShipAt(row, col);
                    shipGrid[row][col] = (ship != null);
                }
            }

            hitGrid = buildHitGrid(myBoard);
        }

        myGridRenderer.drawGrid(false, shipGrid, hitGrid, Gdx.graphics.getDeltaTime());
        myGridRenderer.drawCoordinateLabels();
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

        game.getSkin().getFont("default").draw(batch, "ENEMY WATERS (Attack Here)",
            trackingBoardX, trackingBoardY + trackingBoardSize + 40);

        game.getSkin().getFont("default").draw(batch, "YOUR FLEET",
            myBoardX, myBoardY + myBoardSize + 40);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);

        gridSize = game.getGameController().getMyBoard().getSize();

        float padding = 40;
        float borderPadding = 50;
        float columnPadding = 60;
        float gridVerticalOffset = 20;

        trackingBoardSize = height * 0.65f;
        trackingBoardX = padding + borderPadding;
        trackingBoardY = (height - trackingBoardSize) / 2 - gridVerticalOffset;
        trackingGridRenderer.setBounds(trackingBoardX, trackingBoardY, trackingBoardSize, gridSize);

        myBoardSize = height * 0.35f;
        myBoardX = width - myBoardSize - padding - borderPadding;
        myBoardY = height - myBoardSize - padding - borderPadding - 30 - gridVerticalOffset;
        myGridRenderer.setBounds(myBoardX, myBoardY, myBoardSize, gridSize);

        float panelX = trackingBoardX + trackingBoardSize + columnPadding;
        float panelWidth = myBoardX - panelX - columnPadding;
        float panelHeight = trackingBoardSize;
        float panelY = trackingBoardY;
        infoPanel.setPosition(panelX, panelY, panelWidth, panelHeight);

        if (exitButton != null) {
            float exitButtonX = myBoardX;
            float exitButtonY = myBoardY - exitButton.getHeight() - 20;
            exitButton.setPosition(exitButtonX, exitButtonY);
        }
    }

    @Override
    public void pause() {
        //
    }

    @Override
    public void resume() {
        //
    }

    @Override
    public void hide() {
        //
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (infoPanel != null) infoPanel.dispose();
        if (trackingGridRenderer != null) trackingGridRenderer.dispose();
        if (myGridRenderer != null) myGridRenderer.dispose();
    }
}
