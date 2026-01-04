package at.gl1tchxd.battleship.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * UI panel for ship placement controls.
 * Displays ship selection, placement status, and control buttons.
 */
public class PlacementInfoPanel {
    private final Table rootTable;
    private final Table contentTable;
    private final ScrollPane scrollPane;
    private final Skin skin;

    private Label opponentStatusLabel;
    private Label currentShipLabel;
    private Label orientationLabel;
    private Label[] shipClassLabels;
    private ImageTextButton confirmButton;
    private ImageTextButton autoPlaceButton;
    private ImageTextButton resetButton;

    private String[] shipClassNames;
    private int[] shipLengths;
    private int[] shipCounts;
    private int[] shipPlaced;

    private PlacementCallback callback;

    public interface PlacementCallback {
        void onAutoPlace();
        void onConfirmPlacement();
        void onReset();
    }

    public PlacementInfoPanel(Skin skin, Stage stage, String[] shipClassNames, int[] shipLengths, int[] shipCounts) {
        this.skin = skin;

        // Content table that will be scrollable
        this.contentTable = new Table();
        this.contentTable.top();

        // Root table that contains the scroll pane
        this.rootTable = new Table();
        this.rootTable.setFillParent(false);

        // Store configuration
        this.shipClassNames = shipClassNames;
        this.shipLengths = shipLengths;
        this.shipCounts = shipCounts;
        this.shipPlaced = new int[shipCounts.length];

        createUI();

        // Create scroll pane with content
        scrollPane = new ScrollPane(contentTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        rootTable.add(scrollPane).expand().fill();

        stage.addActor(rootTable);
    }

    private void createUI() {
        // Opponent status
        opponentStatusLabel = new Label("Opponent: Not Ready", skin);
        opponentStatusLabel.setColor(Color.RED);
        contentTable.add(opponentStatusLabel).padBottom(20).row();

        // Current ship selection
        contentTable.add(new Label("Selected Ship:", skin)).padBottom(10).row();
        currentShipLabel = new Label("Carrier #1", skin);
        currentShipLabel.setColor(Color.YELLOW);
        contentTable.add(currentShipLabel).padBottom(30).row();

        // Placement mode indicator
        Label modeLabel = new Label("Left Click: Place Ship\nRight Click: Rotate", skin);
        modeLabel.setColor(Color.CYAN);
        contentTable.add(modeLabel).padBottom(20).row();

        // Current orientation display
        orientationLabel = new Label("Orientation: Horizontal", skin);
        orientationLabel.setColor(Color.YELLOW);
        contentTable.add(orientationLabel).padBottom(30).row();

        // Ship classes list
        contentTable.add(new Label("Ship Classes:", skin)).padBottom(10).row();
        shipClassLabels = new Label[shipClassNames.length];

        for (int i = 0; i < shipClassNames.length; i++) {
            Table shipRow = new Table();

            // Placeholder for ship image (will be replaced later)
            Label imageLabel = new Label("[IMG]", skin);
            imageLabel.setColor(Color.DARK_GRAY);
            shipRow.add(imageLabel).width(60).padRight(10);

            // Ship class info
            String text = shipClassNames[i] + " (x" + shipLengths[i] + ")";
            shipClassLabels[i] = new Label(text + "\n0/" + shipCounts[i] + " placed", skin);
            shipRow.add(shipClassLabels[i]).expandX().left();

            contentTable.add(shipRow).padBottom(15).row();
        }

        // Auto-place button
        autoPlaceButton = new ImageTextButton("Auto-Place", skin);
        autoPlaceButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (callback != null) {
                    callback.onAutoPlace();
                }
            }
        });
        contentTable.add(autoPlaceButton).width(autoPlaceButton.getWidth() * 0.5f).height(autoPlaceButton.getHeight() * 0.5f).padTop(20).row();

        // Reset button
        resetButton = new ImageTextButton("Reset", skin);
        resetButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (callback != null) {
                    callback.onReset();
                }
            }
        });
        contentTable.add(resetButton).width(autoPlaceButton.getWidth() * 0.5f).height(autoPlaceButton.getHeight() * 0.5f).row();

        // Confirm placement button
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
        contentTable.add(confirmButton).width(autoPlaceButton.getWidth() * 0.5f).height(autoPlaceButton.getHeight() * 0.5f).row();
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
     */
    public void setPosition(float x, float y, float maxHeight) {
        contentTable.pack();
        rootTable.setSize(250, maxHeight);
        rootTable.setPosition(x, y);
    }

    /**
     * Set the callback for button events.
     */
    public void setCallback(PlacementCallback callback) {
        this.callback = callback;
    }

    /**
     * Get the table for advanced positioning.
     */
    public Table getTable() {
        return rootTable;
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
}

