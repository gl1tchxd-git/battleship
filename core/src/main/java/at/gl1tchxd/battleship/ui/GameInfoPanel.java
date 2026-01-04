package at.gl1tchxd.battleship.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * UI panel for the main game screen.
 * Displays ship status (alive/remaining), turn indicator, and game status.
 */
public class GameInfoPanel {
    private final Table rootTable;
    private final Table contentTable;
    private final ScrollPane scrollPane;
    private final Skin skin;

    private Label turnLabel;
    private Label statusLabel;
    private Label[] myShipLabels;
    private Label[] opponentShipLabels;
    private Image[] myShipImages;
    private Image[] opponentShipImages;

    private final String[] shipClassNames = {"Carrier", "Battleship", "Cruiser", "Submarine", "Destroyer"};
    private final int[] shipLengths = {5, 4, 3, 3, 2};
    private int[] myShipCounts;
    private int[] myShipsRemaining;
    private int[] opponentShipCounts;
    private int[] opponentShipsRemaining;

    // Ship color textures
    private Texture shipAliveTexture;
    private Texture shipSunkTexture;

    public GameInfoPanel(Skin skin, Stage stage, int[] shipCounts) {
        this.skin = skin;
        this.myShipCounts = shipCounts.clone();
        this.myShipsRemaining = shipCounts.clone();
        this.opponentShipCounts = shipCounts.clone();
        this.opponentShipsRemaining = shipCounts.clone();

        createShipTextures();

        // Content table that will be scrollable
        this.contentTable = new Table();
        this.contentTable.top();

        // Root table that contains the scroll pane
        this.rootTable = new Table();
        this.rootTable.setFillParent(false);

        createUI();

        // Create scroll pane with content
        scrollPane = new ScrollPane(contentTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        rootTable.add(scrollPane).expand().fill();

        stage.addActor(rootTable);
    }

    private void createShipTextures() {
        // Create alive ship texture (green)
        Pixmap alivePixmap = new Pixmap(40, 10, Pixmap.Format.RGBA8888);
        alivePixmap.setColor(0.2f, 0.8f, 0.2f, 1f);
        alivePixmap.fill();
        shipAliveTexture = new Texture(alivePixmap);
        alivePixmap.dispose();

        // Create sunk ship texture (red/dark)
        Pixmap sunkPixmap = new Pixmap(40, 10, Pixmap.Format.RGBA8888);
        sunkPixmap.setColor(0.5f, 0.2f, 0.2f, 1f);
        sunkPixmap.fill();
        shipSunkTexture = new Texture(sunkPixmap);
        sunkPixmap.dispose();
    }

    private void createUI() {
        // Turn indicator
        turnLabel = new Label("Your Turn", skin);
        turnLabel.setColor(Color.GREEN);
        contentTable.add(turnLabel).padBottom(20).row();

        // Status label
        statusLabel = new Label("Battle in Progress", skin);
        statusLabel.setColor(Color.CYAN);
        contentTable.add(statusLabel).padBottom(30).row();

        // My ships section
        contentTable.add(new Label("=== YOUR FLEET ===", skin)).padBottom(15).row();
        myShipLabels = new Label[5];
        myShipImages = new Image[5];

        for (int i = 0; i < 5; i++) {
            Table shipRow = new Table();

            // Ship image (represents ship status)
            myShipImages[i] = new Image(new TextureRegionDrawable(new TextureRegion(shipAliveTexture)));
            shipRow.add(myShipImages[i]).width(shipLengths[i] * 10).height(12).padRight(10);

            // Ship class info
            myShipLabels[i] = new Label(shipClassNames[i] + ": " + myShipsRemaining[i] + "/" + myShipCounts[i], skin);
            myShipLabels[i].setColor(Color.WHITE);
            shipRow.add(myShipLabels[i]).expandX().left();

            contentTable.add(shipRow).padBottom(10).left().row();
        }

        contentTable.add(new Label("", skin)).padBottom(20).row(); // Spacer

        // Opponent ships section
        contentTable.add(new Label("=== ENEMY FLEET ===", skin)).padBottom(15).row();
        opponentShipLabels = new Label[5];
        opponentShipImages = new Image[5];

        for (int i = 0; i < 5; i++) {
            Table shipRow = new Table();

            // Ship image (represents ship status)
            opponentShipImages[i] = new Image(new TextureRegionDrawable(new TextureRegion(shipAliveTexture)));
            shipRow.add(opponentShipImages[i]).width(shipLengths[i] * 10).height(12).padRight(10);

            // Ship class info
            opponentShipLabels[i] = new Label(shipClassNames[i] + ": " + opponentShipsRemaining[i] + "/" + opponentShipCounts[i], skin);
            opponentShipLabels[i].setColor(Color.WHITE);
            shipRow.add(opponentShipLabels[i]).expandX().left();

            contentTable.add(shipRow).padBottom(10).left().row();
        }
    }

    /**
     * Set the position and size of the panel.
     */
    public void setPosition(float x, float y, float width, float height) {
        rootTable.setPosition(x, y);
        rootTable.setSize(width, height);
    }

    /**
     * Update turn indicator.
     */
    public void setMyTurn(boolean myTurn) {
        if (myTurn) {
            turnLabel.setText("YOUR TURN");
            turnLabel.setColor(Color.GREEN);
        } else {
            turnLabel.setText("OPPONENT'S TURN");
            turnLabel.setColor(Color.YELLOW);
        }
    }

    /**
     * Update my ship counts.
     * @param shipClass Ship class index (0-4)
     * @param remaining Ships still alive
     * @param total Total ships of this class
     */
    public void updateMyShipCount(int shipClass, int remaining, int total) {
        if (shipClass >= 0 && shipClass < 5) {
            myShipsRemaining[shipClass] = remaining;
            myShipCounts[shipClass] = total;
            myShipLabels[shipClass].setText(shipClassNames[shipClass] + ": " + remaining + "/" + total);

            // Update image color based on remaining ships
            if (remaining == 0) {
                myShipLabels[shipClass].setColor(Color.RED);
                myShipImages[shipClass].setDrawable(new TextureRegionDrawable(new TextureRegion(shipSunkTexture)));
            } else if (remaining < total) {
                myShipLabels[shipClass].setColor(Color.ORANGE);
            } else {
                myShipLabels[shipClass].setColor(Color.WHITE);
                myShipImages[shipClass].setDrawable(new TextureRegionDrawable(new TextureRegion(shipAliveTexture)));
            }
        }
    }

    /**
     * Update opponent ship counts.
     * @param shipClass Ship class index (0-4)
     * @param remaining Ships still alive
     * @param total Total ships of this class
     */
    public void updateOpponentShipCount(int shipClass, int remaining, int total) {
        if (shipClass >= 0 && shipClass < 5) {
            opponentShipsRemaining[shipClass] = remaining;
            opponentShipCounts[shipClass] = total;
            opponentShipLabels[shipClass].setText(shipClassNames[shipClass] + ": " + remaining + "/" + total);

            // Update image color based on remaining ships
            if (remaining == 0) {
                opponentShipLabels[shipClass].setColor(Color.RED);
                opponentShipImages[shipClass].setDrawable(new TextureRegionDrawable(new TextureRegion(shipSunkTexture)));
            } else if (remaining < total) {
                opponentShipLabels[shipClass].setColor(Color.ORANGE);
            } else {
                opponentShipLabels[shipClass].setColor(Color.WHITE);
                opponentShipImages[shipClass].setDrawable(new TextureRegionDrawable(new TextureRegion(shipAliveTexture)));
            }
        }
    }

    /**
     * Update all opponent ship counts at once.
     */
    public void updateOpponentShipCounts(int[][] sunkShips) {
        if (sunkShips == null) return;

        for (int i = 0; i < 5 && i < sunkShips.length; i++) {
            if (sunkShips[i] != null && sunkShips[i].length >= 2) {
                int sunk = sunkShips[i][0];
                int total = sunkShips[i][1];
                int remaining = total - sunk;
                updateOpponentShipCount(i, remaining, total);
            }
        }
    }

    /**
     * Set status message.
     */
    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    /**
     * Set status message with color.
     */
    public void setStatus(String status, Color color) {
        statusLabel.setText(status);
        statusLabel.setColor(color);
    }

    /**
     * Update all my ship counts based on game state.
     */
    public void updateMyFleetStatus(int[] alivePerClass, int[] totalPerClass) {
        for (int i = 0; i < 5; i++) {
            updateMyShipCount(i, alivePerClass[i], totalPerClass[i]);
        }
    }

    public void dispose() {
        if (shipAliveTexture != null) shipAliveTexture.dispose();
        if (shipSunkTexture != null) shipSunkTexture.dispose();
    }
}

