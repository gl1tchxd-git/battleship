package at.gl1tchxd.battleship.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * UI panel for ship placement controls.
 * Displays ship selection, placement status, and control buttons.
 */
public class PlacementInfoPanel {
    // Keep contentTable as the ship list table (was previously the scrollable content)
    private final Table contentTable;
    // New controls table for status labels and buttons
    private final Table controlsTable;
    private final Skin skin;

    private Label opponentStatusLabel;
    private Label currentShipLabel;
    private Label orientationLabel;
    private Label[] shipClassLabels;
    private Image[] shipClassImages;
    private ImageTextButton confirmButton;
    private ImageTextButton autoPlaceButton;
    private ImageTextButton resetButton;

    private String[] shipClassNames;
    private int[] shipLengths;
    private int[] shipCounts;
    private int[] shipPlaced;
    // Map ship class index to rank: Carrier=rank-5, Battleship=rank-4, Cruiser=rank-3, Submarine=rank-2, Destroyer=rank-1
    private final int[] shipRanks = {5, 4, 3, 2, 1};
    private Texture[] rankTextures;

    private PlacementCallback callback;

    public interface PlacementCallback {
        void onAutoPlace();
        void onConfirmPlacement();
        void onReset();
        void onExit();
    }

    public PlacementInfoPanel(Skin skin, Stage stage, String[] shipClassNames, int[] shipLengths, int[] shipCounts) {
        this.skin = skin;

        // Content table will now hold the ship-class list (non-scrollable)
        this.contentTable = new Table();
        this.contentTable.top();

        // Controls table holds status, orientation and buttons
        this.controlsTable = new Table();
        this.controlsTable.top();

        // Store configuration
        this.shipClassNames = shipClassNames;
        this.shipLengths = shipLengths;
        this.shipCounts = shipCounts;
        this.shipPlaced = new int[shipCounts.length];

        // Load rank textures
        loadRankTextures();

        createUI();

        // NOTE: Do not add actors to the stage here. PlacementScreen will add and position
        // the two tables (contentTable and controlsTable) so they can be laid out in columns.
    }

    private void loadRankTextures() {
        rankTextures = new Texture[5];
        for (int i = 0; i < 5; i++) {
            rankTextures[i] = new Texture(Gdx.files.internal("sprites/shared_rank-" + (i + 1) + ".png"));
        }
    }

    private void createUI() {
        // Build controls table (status, current selection, mode, orientation)
        opponentStatusLabel = new Label("Opponent: Not Ready", skin);
        opponentStatusLabel.setColor(Color.RED);
        controlsTable.add(opponentStatusLabel).padBottom(20).row();

        // Current ship selection
        controlsTable.add(new Label("Selected Ship:", skin)).padBottom(10).row();
        currentShipLabel = new Label("Carrier #1", skin);
        currentShipLabel.setColor(Color.YELLOW);
        controlsTable.add(currentShipLabel).padBottom(30).row();

        // Placement mode indicator
        Label modeLabel = new Label("Left Click: Place Ship\nRight Click: Rotate", skin);
        modeLabel.setColor(Color.CYAN);
        controlsTable.add(modeLabel).padBottom(20).row();

        // Current orientation display
        orientationLabel = new Label("Orientation: Horizontal", skin);
        orientationLabel.setColor(Color.YELLOW);
        controlsTable.add(orientationLabel).padBottom(30).row();

        // Ship classes list header
        contentTable.add(new Label("Ship Classes:", skin)).padBottom(10).left().row();
        shipClassLabels = new Label[shipClassNames.length];
        shipClassImages = new Image[shipClassNames.length];

        for (int i = 0; i < shipClassNames.length; i++) {
            Table shipRow = new Table();
            shipRow.left(); // Left align contents within the row

            // Ship image using rank texture
            int rankIndex = shipRanks[i] - 1; // rank-1 is index 0, rank-5 is index 4
            shipClassImages[i] = new Image(new TextureRegionDrawable(new TextureRegion(rankTextures[rankIndex])));
            shipRow.add(shipClassImages[i]).size(48, 48).padRight(10);

            // Ship class info
            String text = shipClassNames[i] + " (x" + shipLengths[i] + ")";
            shipClassLabels[i] = new Label(text + "\n0/" + shipCounts[i] + " placed", skin);
            shipRow.add(shipClassLabels[i]).left();

            contentTable.add(shipRow).left().expandX().fillX().padBottom(15).row();
        }

        // Buttons go into the controls table
        autoPlaceButton = new ImageTextButton("Auto-Place", skin);
        autoPlaceButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (callback != null) {
                    callback.onAutoPlace();
                }
            }
        });
        controlsTable.add(autoPlaceButton).width(autoPlaceButton.getWidth() * 0.5f).height(autoPlaceButton.getHeight() * 0.5f).padTop(20).row();

        resetButton = new ImageTextButton("Reset", skin);
        resetButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (callback != null) {
                    callback.onReset();
                }
            }
        });
        controlsTable.add(resetButton).width(autoPlaceButton.getWidth() * 0.5f).height(autoPlaceButton.getHeight() * 0.5f).row();

        confirmButton = new ImageTextButton("Confirm", skin);
        confirmButton.setDisabled(true);
        confirmButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (callback != null && !confirmButton.isDisabled()) {
                    callback.onConfirmPlacement();
                }
            }
        });
        controlsTable.add(confirmButton).width(autoPlaceButton.getWidth() * 0.5f).height(autoPlaceButton.getHeight() * 0.5f).row();

        ImageTextButton exitButton = new ImageTextButton("EXIT", skin);
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (callback != null) {
                    callback.onExit();
                }
            }
        });
        controlsTable.add(exitButton).width(autoPlaceButton.getWidth() * 0.5f).height(autoPlaceButton.getHeight() * 0.5f).row();
    }

    /**
     * Update the display for the currently selected ship.
     */
    public void setCurrentShip(String shipName, int shipNumber) {
        currentShipLabel.setText(shipName + " #" + shipNumber);
    }

    /**
     * Update the orientation display.
     */
    public void setOrientation(boolean horizontal) {
        orientationLabel.setText("Orientation: " + (horizontal ? "Horizontal" : "Vertical"));
    }

    /**
     * Update the opponent ready status.
     */
    public void setOpponentReady(boolean ready) {
        if (ready) {
            opponentStatusLabel.setText("Opponent: Ready");
            opponentStatusLabel.setColor(Color.GREEN);
        } else {
            opponentStatusLabel.setText("Opponent: Not Ready");
            opponentStatusLabel.setColor(Color.RED);
        }
    }

    /**
     * Update ship placement counts.
     */
    public void setShipPlaced(int shipClass, int placed) {
        if (shipClass >= 0 && shipClass < shipClassNames.length) {
            shipPlaced[shipClass] = placed;
            updateShipClassLabel(shipClass);
            updateConfirmButton();
        }
    }

    /**
     * Update ship class placement count (alternative method used by PlacementScreen).
     */
    public void updateShipCount(int shipClass, int placed, int total, String className, int length) {
        if (shipClass >= 0 && shipClass < shipClassLabels.length) {
            String text = className + " (x" + length + ")";
            shipClassLabels[shipClass].setText(text + "\n" + placed + "/" + total + " placed");
            shipPlaced[shipClass] = placed;
        }
    }

    /**
     * Set the total ship counts for each class.
     */
    public void setShipCounts(int[] counts) {
        if (counts != null && counts.length == shipClassNames.length) {
            this.shipCounts = counts;
            for (int i = 0; i < shipClassNames.length; i++) {
                updateShipClassLabel(i);
            }
        }
    }

    private void updateShipClassLabel(int shipClass) {
        String text = shipClassNames[shipClass] + " (x" + shipLengths[shipClass] + ")";
        shipClassLabels[shipClass].setText(text + "\n" + shipPlaced[shipClass] + "/" + shipCounts[shipClass] + " placed");
    }

    private void updateConfirmButton() {
        boolean allPlaced = true;
        for (int i = 0; i < shipClassNames.length; i++) {
            if (shipPlaced[i] < shipCounts[i]) {
                allPlaced = false;
                break;
            }
        }
        confirmButton.setDisabled(!allPlaced);
    }

    /**
     * Enable or disable the confirm button.
     */
    public void setConfirmEnabled(boolean enabled) {
        confirmButton.setDisabled(!enabled);
    }

    /**
     * Reset all ship counts to zero.
     */
    public void resetShipCounts() {
        for (int i = 0; i < shipPlaced.length; i++) {
            shipPlaced[i] = 0;
            updateShipClassLabel(i);
        }
        confirmButton.setDisabled(true);
    }

    /**
     * Update all ship counts with new configuration (for client receiving config from host).
     */
    public void updateAllShipCounts(int[] newShipCounts, String[] names, int[] lengths) {
        this.shipCounts = newShipCounts;
        this.shipClassNames = names;
        this.shipLengths = lengths;
        this.shipPlaced = new int[newShipCounts.length];

        // Update all ship class labels
        for (int i = 0; i < Math.min(shipClassLabels.length, newShipCounts.length); i++) {
            String text = shipClassNames[i] + " (x" + shipLengths[i] + ")";
            shipClassLabels[i].setText(text + "\n0/" + shipCounts[i] + " placed");
        }
    }

    /**
     * Position the UI panel with size constraints.
     * This method keeps a simple default behavior for backward compatibility: it will
     * position and size the two internal tables next to each other within a default total width.
     */
    public void setPosition(float x, float y, float maxHeight) {
        // Default total width used historically
        float totalWidth = 250f;
        float shipListWidth = totalWidth * 0.65f;
        float controlsWidth = totalWidth - shipListWidth - 10f; // spacing

        contentTable.pack();
        contentTable.setSize(shipListWidth, maxHeight);
        contentTable.setPosition(x, y);

        controlsTable.pack();
        controlsTable.setSize(Math.max(controlsWidth, 120f), maxHeight);
        controlsTable.setPosition(x + shipListWidth + 10f, y);
    }

    /**
     * Set the callback for button events.
     */
    public void setCallback(PlacementCallback callback) {
        this.callback = callback;
    }

    /**
     * Get the table for the ship-class list (middle column).
     */
    public Table getShipListTable() {
        return contentTable;
    }

    /**
     * Get the controls table (right column).
     */
    public Table getControlsTable() {
        return controlsTable;
    }

    public String[] getShipClassNames() {
        return shipClassNames;
    }

    public int[] getShipLengths() {
        return shipLengths;
    }

    public int[] getShipCounts() {
        return shipCounts;
    }

    public int[] getShipPlaced() {
        return shipPlaced;
    }

    public void dispose() {
        if (rankTextures != null) {
            for (Texture texture : rankTextures) {
                if (texture != null) texture.dispose();
            }
        }
    }
}
