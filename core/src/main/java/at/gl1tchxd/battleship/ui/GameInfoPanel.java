package at.gl1tchxd.battleship.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

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
    private final int[] shipRanks = {5, 4, 3, 2, 1};
    private int[] opponentShipCounts;
    private int[] opponentShipsRemaining;

    private Texture[] rankTextures;

    public GameInfoPanel(Skin skin, Stage stage, int[] shipCounts) {
        this.skin = skin;
        this.opponentShipCounts = shipCounts.clone();
        this.opponentShipsRemaining = shipCounts.clone();

        createShipTextures();

        this.contentTable = new Table();
        this.contentTable.top();

        this.rootTable = new Table();
        this.rootTable.setFillParent(false);

        createUI();

        rootTable.add(contentTable).expand().fill().top();

        stage.addActor(rootTable);
    }

    private void createShipTextures() {
        rankTextures = new Texture[5];
        for (int i = 0; i < 5; i++) {
            rankTextures[i] = new Texture(Gdx.files.internal("sprites/shared_rank-" + (i + 1) + ".png"));
        }
    }

    private void createUI() {
        turnLabel = new Label("Your Turn", skin);
        turnLabel.setColor(Color.GREEN);
        contentTable.add(turnLabel).padBottom(20).row();

        statusLabel = new Label("Battle in Progress", skin);
        statusLabel.setColor(Color.CYAN);
        contentTable.add(statusLabel).padBottom(30).row();

        contentTable.add(new Label("=== ENEMY FLEET ===", skin)).padBottom(15).row();
        opponentShipLabels = new Label[5];
        opponentShipImages = new Image[5];

        for (int i = 0; i < 5; i++) {
            Table shipRow = new Table();

            int rankIndex = shipRanks[i] - 1;
            opponentShipImages[i] = new Image(new TextureRegionDrawable(new TextureRegion(rankTextures[rankIndex])));
            shipRow.add(opponentShipImages[i]).size(48, 48).padRight(10);

            opponentShipLabels[i] = new Label(shipClassNames[i] + ": " + opponentShipsRemaining[i] + "/" + opponentShipCounts[i], skin);
            opponentShipLabels[i].setColor(Color.WHITE);
            shipRow.add(opponentShipLabels[i]).expandX().left();

            contentTable.add(shipRow).padBottom(10).left().row();
        }
    }

    public void setPosition(float x, float y, float width, float height) {
        rootTable.setPosition(x, y);
        rootTable.setSize(width, height);
    }

    public void setMyTurn(boolean myTurn) {
        if (myTurn) {
            turnLabel.setText("YOUR TURN");
            turnLabel.setColor(Color.GREEN);
        } else {
            turnLabel.setText("OPPONENT'S TURN");
            turnLabel.setColor(Color.YELLOW);
        }
    }

    public void updateMyShipCount(int shipClass, int remaining, int total) {
    }

    public void updateOpponentShipCount(int shipClass, int remaining, int total) {
        if (shipClass >= 0 && shipClass < 5) {
            opponentShipsRemaining[shipClass] = remaining;
            opponentShipCounts[shipClass] = total;
            opponentShipLabels[shipClass].setText(shipClassNames[shipClass] + ": " + remaining + "/" + total);

            if (remaining == 0) {
                opponentShipLabels[shipClass].setColor(Color.RED);
                opponentShipImages[shipClass].setColor(0.5f, 0.2f, 0.2f, 1f);
            } else if (remaining < total) {
                opponentShipLabels[shipClass].setColor(Color.YELLOW);
                opponentShipImages[shipClass].setColor(1f, 0.8f, 0.2f, 1f);
            } else {
                opponentShipLabels[shipClass].setColor(Color.WHITE);
                opponentShipImages[shipClass].setColor(Color.WHITE);
            }
        }
    }

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

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public void setStatus(String status, Color color) {
        statusLabel.setText(status);
        statusLabel.setColor(color);
    }

    public void updateMyFleetStatus(int[] alivePerClass, int[] totalPerClass) {
    }

    public void dispose() {
        if (rankTextures != null) {
            for (Texture texture : rankTextures) {
                if (texture != null) texture.dispose();
            }
        }
    }
}

