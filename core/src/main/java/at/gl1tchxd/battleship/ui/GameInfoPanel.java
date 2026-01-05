package at.gl1tchxd.battleship.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
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
    private final Skin skin;

    private Label turnLabel;
    private Label statusLabel;
    private Label[] opponentShipLabels;
    private Image[] opponentShipImages;

    private final String[] shipClassNames = {"Carrier", "Battleship", "Cruiser", "Submarine", "Destroyer"};
    private final int[] shipLengths = {5, 4, 3, 3, 2};
    // Map ship class index to rank: Carrier=rank-5, Battleship=rank-4, Cruiser=rank-3, Submarine=rank-2, Destroyer=rank-1
    private final int[] shipRanks = {5, 4, 3, 2, 1};
    private int[] opponentShipCounts;
    private int[] opponentShipsRemaining;

    // Rank textures for ship images
    private Texture[] rankTextures;

    public GameInfoPanel(Skin skin, Stage stage, int[] shipCounts) {
        this.skin = skin;
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

        // Add content table directly without scroll pane to avoid weird background
        rootTable.add(contentTable).expand().fill().top();

        stage.addActor(rootTable);
    }

    private void createShipTextures() {
        // Load rank textures (rank-1 to rank-5)
        rankTextures = new Texture[5];
        for (int i = 0; i < 5; i++) {
            rankTextures[i] = new Texture(Gdx.files.internal("sprites/shared_rank-" + (i + 1) + ".png"));
        }
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

        // Opponent ships section
        contentTable.add(new Label("=== ENEMY FLEET ===", skin)).padBottom(15).row();
        opponentShipLabels = new Label[5];
        opponentShipImages = new Image[5];

        for (int i = 0; i < 5; i++) {
            Table shipRow = new Table();

            // Ship image using rank texture
            int rankIndex = shipRanks[i] - 1;
            opponentShipImages[i] = new Image(new TextureRegionDrawable(new TextureRegion(rankTextures[rankIndex])));
            // Scale up the rank images
            shipRow.add(opponentShipImages[i]).size(48, 48).padRight(10);

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
     * Note: MY FLEET section was removed, this method is now a no-op.
     * @param shipClass Ship class index (0-4)
     * @param remaining Ships still alive
     * @param total Total ships of this class
     */
    public void updateMyShipCount(int shipClass, int remaining, int total) {
        // MY FLEET section removed - no action needed
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

            // Update label and image color based on remaining ships
            if (remaining == 0) {
                opponentShipLabels[shipClass].setColor(Color.RED);
                opponentShipImages[shipClass].setColor(0.5f, 0.2f, 0.2f, 1f); // Dark red tint for sunk
            } else if (remaining < total) {
                opponentShipLabels[shipClass].setColor(Color.YELLOW);
                opponentShipImages[shipClass].setColor(1f, 0.8f, 0.2f, 1f); // Yellow-orange tint for damaged
            } else {
                opponentShipLabels[shipClass].setColor(Color.WHITE);
                opponentShipImages[shipClass].setColor(Color.WHITE); // Normal color
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
     * Note: MY FLEET section was removed, this method is now a no-op.
     */
    public void updateMyFleetStatus(int[] alivePerClass, int[] totalPerClass) {
        // MY FLEET section removed - no action needed
    }

    public void dispose() {
        if (rankTextures != null) {
            for (Texture texture : rankTextures) {
                if (texture != null) texture.dispose();
            }
        }
    }
}

