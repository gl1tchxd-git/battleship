package at.gl1tchxd.battleship.ui;

import at.gl1tchxd.battleship.BattleshipGame;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;


public class PlacementInfoPanel {
    private final Table contentTable;
    private final Table controlsTable;
    private final Skin skin;
    private final BattleshipGame game;

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
    private final int[] shipRanks = {5, 4, 3, 2, 1};
    private Texture[] rankTextures;

    private PlacementCallback callback;

    public interface PlacementCallback {
        void onAutoPlace();
        void onConfirmPlacement();
        void onReset();
        void onExit();
    }

    public PlacementInfoPanel(BattleshipGame game, Skin skin, Stage stage, String[] shipClassNames, int[] shipLengths, int[] shipCounts) {
        this.game = game;
        this.skin = skin;

        this.contentTable = new Table();
        this.contentTable.top();

        this.controlsTable = new Table();
        this.controlsTable.top();

        this.shipClassNames = shipClassNames;
        this.shipLengths = shipLengths;
        this.shipCounts = shipCounts;
        this.shipPlaced = new int[shipCounts.length];

        loadRankTextures();
        createUI();
    }

    private void loadRankTextures() {
        rankTextures = new Texture[5];
        for (int i = 0; i < 5; i++) {
            rankTextures[i] = new Texture(Gdx.files.internal("sprites/shared_rank-" + (i + 1) + ".png"));
        }
    }

    private void createUI() {
        opponentStatusLabel = new Label("Opponent: Not Ready", skin);
        opponentStatusLabel.setColor(Color.RED);
        controlsTable.add(opponentStatusLabel).padBottom(20).row();

        controlsTable.add(new Label("Selected Ship:", skin)).padBottom(10).row();
        currentShipLabel = new Label("Carrier #1", skin);
        currentShipLabel.setColor(Color.YELLOW);
        controlsTable.add(currentShipLabel).padBottom(30).row();

        Label modeLabel = new Label("Left Click: Place Ship\nRight Click: Rotate", skin);
        modeLabel.setColor(Color.CYAN);
        controlsTable.add(modeLabel).padBottom(20).row();

        orientationLabel = new Label("Orientation: Horizontal", skin);
        orientationLabel.setColor(Color.YELLOW);
        controlsTable.add(orientationLabel).padBottom(30).row();

        contentTable.add(new Label("Ship Classes:", skin)).padBottom(10).left().row();
        shipClassLabels = new Label[shipClassNames.length];
        shipClassImages = new Image[shipClassNames.length];

        for (int i = 0; i < shipClassNames.length; i++) {
            Table shipRow = new Table();
            shipRow.left();

            int rankIndex = shipRanks[i] - 1;
            shipClassImages[i] = new Image(new TextureRegionDrawable(new TextureRegion(rankTextures[rankIndex])));
            shipRow.add(shipClassImages[i]).size(48, 48).padRight(10);

            String text = shipClassNames[i] + " (x" + shipLengths[i] + ")";
            shipClassLabels[i] = new Label(text + "\n0/" + shipCounts[i] + " placed", skin);
            shipRow.add(shipClassLabels[i]).left();

            contentTable.add(shipRow).left().expandX().fillX().padBottom(15).row();
        }

        autoPlaceButton = new ImageTextButton("Auto-Place", skin);
        autoPlaceButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClickSound();
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
                game.playClickSound();
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
                game.playClickSound();
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
                game.playClickSound();
                if (callback != null) {
                    callback.onExit();
                }
            }
        });
        controlsTable.add(exitButton).width(autoPlaceButton.getWidth() * 0.5f).height(autoPlaceButton.getHeight() * 0.5f).row();
    }

    public void setCurrentShip(String shipName, int shipNumber) {
        currentShipLabel.setText(shipName + " #" + shipNumber);
    }

    public void setOrientation(boolean horizontal) {
        orientationLabel.setText("Orientation: " + (horizontal ? "Horizontal" : "Vertical"));
    }

    public void setOpponentReady(boolean ready) {
        if (ready) {
            opponentStatusLabel.setText("Opponent: Ready");
            opponentStatusLabel.setColor(Color.GREEN);
        } else {
            opponentStatusLabel.setText("Opponent: Not Ready");
            opponentStatusLabel.setColor(Color.RED);
        }
    }

    public void setShipPlaced(int shipClass, int placed) {
        if (shipClass >= 0 && shipClass < shipClassNames.length) {
            shipPlaced[shipClass] = placed;
            updateShipClassLabel(shipClass);
            updateConfirmButton();
        }
    }

    public void updateShipCount(int shipClass, int placed, int total, String className, int length) {
        if (shipClass >= 0 && shipClass < shipClassLabels.length) {
            String text = className + " (x" + length + ")";
            shipClassLabels[shipClass].setText(text + "\n" + placed + "/" + total + " placed");
            shipPlaced[shipClass] = placed;
        }
    }

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

    public void setConfirmEnabled(boolean enabled) {
        confirmButton.setDisabled(!enabled);
    }

    public void resetShipCounts() {
        for (int i = 0; i < shipPlaced.length; i++) {
            shipPlaced[i] = 0;
            updateShipClassLabel(i);
        }
        confirmButton.setDisabled(true);
    }

    public void updateAllShipCounts(int[] newShipCounts, String[] names, int[] lengths) {
        this.shipCounts = newShipCounts;
        this.shipClassNames = names;
        this.shipLengths = lengths;
        this.shipPlaced = new int[newShipCounts.length];

        for (int i = 0; i < Math.min(shipClassLabels.length, newShipCounts.length); i++) {
            String text = shipClassNames[i] + " (x" + shipLengths[i] + ")";
            shipClassLabels[i].setText(text + "\n0/" + shipCounts[i] + " placed");
        }
    }

    public void setPosition(float x, float y, float maxHeight) {
        float totalWidth = 250f;
        float shipListWidth = totalWidth * 0.65f;
        float controlsWidth = totalWidth - shipListWidth - 10f;

        contentTable.pack();
        contentTable.setSize(shipListWidth, maxHeight);
        contentTable.setPosition(x, y);

        controlsTable.pack();
        controlsTable.setSize(Math.max(controlsWidth, 120f), maxHeight);
        controlsTable.setPosition(x + shipListWidth + 10f, y);
    }

    public void setCallback(PlacementCallback callback) {
        this.callback = callback;
    }

    public Table getShipListTable() {
        return contentTable;
    }

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
