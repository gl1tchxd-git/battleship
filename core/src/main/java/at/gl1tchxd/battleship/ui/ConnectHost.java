package at.gl1tchxd.battleship.ui;

import at.gl1tchxd.battleship.BattleshipGame;
import at.gl1tchxd.battleship.network.NetworkController;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.Arrays;

public class ConnectHost {
    private final Stage stage;
    private Table table;
    private Skin skin;
    private final BattleshipGame game;

    private TextField portField;
    private TextField boardSizeField;
    private TextField[] shipConfigField;
    private ImageTextButton hostButton;
    private Label statusLabel;

    private final NetworkController networkController;

    private HostCallback callback;

    private float widgetWidth = 250f;

    public interface HostCallback {
        void onHostSuccess(int port, int boardSize, int[] shipConfig);
        void onHostError(String errorMessage);
        void onClientConnected();
    }

    public ConnectHost(BattleshipGame game, NetworkController networkController, Skin skin, Stage stage) {
        this.game = game;
        this.networkController = networkController;
        this.stage = stage;
        this.skin = skin;
        createUI();
    }

    private void createUI() {
        table = new Table();
        table.setFillParent(false);

        createUIContent();

        stage.addActor(table);
    }

    private void createUIContent() {
        float halfWidth = widgetWidth / 2f;

        Table topSection = new Table();
        topSection.top().left();

        Label portLabel = new Label("Port:", skin);
        portLabel.setAlignment(com.badlogic.gdx.utils.Align.right);
        portField = new TextField("8080", skin);
        portField.setMessageText("Port number (1024-65535)");
        topSection.add(portLabel).width(halfWidth).padRight(5).padBottom(10);
        topSection.add(portField).width(halfWidth).left().padBottom(10).row();

        Label shipConfigLabel = new Label("Ship Amount:", skin);
        shipConfigLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        topSection.add(shipConfigLabel).colspan(2).center().padBottom(5).row();

        String[] defaultShips = {"1", "1", "1", "1", "1"};
        String[] shipLabels = {"Carrier:", "Battleship:", "Cruiser:", "Submarine:", "Destroyer:"};
        shipConfigField = new TextField[defaultShips.length];

        for (int i = 0; i < defaultShips.length; i++) {
            Label label = new Label(shipLabels[i], skin);
            label.setAlignment(com.badlogic.gdx.utils.Align.right);
            shipConfigField[i] = new TextField(defaultShips[i], skin);
            shipConfigField[i].setMessageText("Ship length");
            topSection.add(label).width(halfWidth).padRight(5).padBottom(5);
            topSection.add(shipConfigField[i]).width(halfWidth).left().padBottom(5).row();
        }

        Table bottomSection = new Table();
        bottomSection.top();

        hostButton = new ImageTextButton("Start Hosting", skin);
        hostButton.getLabel().setFontScale(1.5f);
        hostButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClickSound();
                handleHostClick();
            }
        });
        bottomSection.add(hostButton).height(hostButton.getHeight() * 0.8f).width(hostButton.getWidth() * 0.8f).padBottom(10).row();

        statusLabel = new Label("", skin);
        statusLabel.setColor(Color.YELLOW);
        statusLabel.setWrap(true);
        bottomSection.add(statusLabel).width(widgetWidth).height(60).center().row();

        if (networkController != null && networkController.isHost()) {
            hostButton.getLabel().setText("Cancel");
            hostButton.setDisabled(false);
            setInputsEnabled(false);
            setStatus("Hosting on port " + portField.getText().trim() + ". Waiting for player...", false);
        }

        table.add(topSection).expandX().expandY().top().left().row();
        table.add(bottomSection).expandX().height(150).bottom().row();
    }

    private void handleHostClick() {
        if (networkController != null && networkController.isHost()) {
            networkController.stopHosting();
            hostButton.getLabel().setText("Start Hosting");
            hostButton.setDisabled(false);
            setInputsEnabled(true);
            setStatus("Hosting canceled", false);
            return;
        }

        try {
            int port = getPort();
            int[] shipConfig = getShipConfig();

            setStatus("Starting server on port " + port + "...", false);
            hostButton.setDisabled(true);

            try {
                networkController.hostGame(port, 10, shipConfig);
                hostButton.getLabel().setText("Cancel");
                hostButton.setDisabled(false);
                setInputsEnabled(false);

                setStatus("Hosting on port " + port + ". Waiting for player...", false);

                networkController.setConnectionCallback(new NetworkController.ConnectionCallback() {
                    @Override
                    public void onClientConnected() {
                        Gdx.app.postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                setStatus("Client connected! Starting game...", false);
                                if (callback != null) {
                                    callback.onClientConnected();
                                }
                            }
                        });
                    }
                });

                if (callback != null) {
                    callback.onHostSuccess(port, 10, shipConfig);
                }
            } catch (Exception e) {
                setStatus("Failed to host: " + e.getMessage(), true);
                hostButton.setDisabled(false);
                hostButton.getLabel().setText("Start Hosting");
                setInputsEnabled(true);

                if (callback != null) {
                    callback.onHostError(e.getMessage());
                }
            }

        } catch (NumberFormatException e) {
            setStatus("Invalid input: Please enter valid numbers", true);
        }
    }

    private void setInputsEnabled(boolean enabled) {
        portField.setDisabled(!enabled);

        if (boardSizeField != null) boardSizeField.setDisabled(!enabled);
        if (shipConfigField != null) {
            for (TextField t : shipConfigField) {
                if (t != null) t.setDisabled(!enabled);
            }
        }
    }

    private void setStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setColor(isError ? Color.RED : Color.YELLOW);
    }

    public void setCallback(HostCallback callback) {
        this.callback = callback;
    }

    public int getPort() {
        return Integer.parseInt(portField.getText().trim());
    }

    public int getBoardSize() {
        return Integer.parseInt(boardSizeField.getText().trim());
    }

    public int[] getShipConfig() {
        int[] shipConfig = new int[shipConfigField.length];
        for (int i = 0; i < shipConfigField.length; i++) {
            shipConfig[i] = Integer.parseInt(shipConfigField[i].getText().trim());
        }
        return shipConfig;
    }

    public Table getTable() {
        return table;
    }

    public void setPosition(float x, float y) {
        table.setPosition(x, y);
    }

    public void setSize(float width, float height) {
        table.setSize(width, height);
    }

    public void setWidgetWidth(float width) {
        this.widgetWidth = width;
        rebuildUI();
    }

    public float getWidgetWidth() {
        return widgetWidth;
    }

    private void rebuildUI() {
        table.clear();
        createUIContent();
    }

    public void setHostEnabled(boolean enabled) {
        hostButton.setDisabled(!enabled);
    }

    public void reset() {
        portField.setText("8080");
        boardSizeField.setText("10");
        String[] defaultShips = {"1", "1", "1", "1", "1"};
        for (int i = 0; i < shipConfigField.length; i++) {
            shipConfigField[i].setText(defaultShips[i]);
        }
        statusLabel.setText("");
        hostButton.setDisabled(false);
        hostButton.getLabel().setText("Start Hosting");
        setInputsEnabled(true);
    }

    public void dispose() {
        //
    }
}
