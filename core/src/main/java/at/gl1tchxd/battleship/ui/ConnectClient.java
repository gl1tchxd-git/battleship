package at.gl1tchxd.battleship.ui;

import at.gl1tchxd.battleship.BattleshipGame;
import at.gl1tchxd.battleship.network.NetworkController;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class ConnectClient {
    private final Stage stage;
    private Table table;
    private Skin skin;
    private final BattleshipGame game;

    private TextField hostField;
    private TextField portField;
    private ImageTextButton connectButton;
    private Label statusLabel;

    private final NetworkController networkController;

    private ClientCallback callback;

    private float widgetWidth = 250f;

    public interface ClientCallback {
        void onConnectSuccess(String host, int port);
        void onConnectError(String errorMessage);
    }

    public ConnectClient(BattleshipGame game, NetworkController networkController, Skin skin, Stage stage) {
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

        Label hostLabel = new Label("Host:", skin);
        hostLabel.setAlignment(com.badlogic.gdx.utils.Align.right);
        hostField = new TextField("localhost", skin);
        hostField.setMessageText("Hostname or IP address");
        topSection.add(hostLabel).width(halfWidth).padRight(5).padBottom(10);
        topSection.add(hostField).width(halfWidth).left().padBottom(10).row();

        Label portLabel = new Label("Port:", skin);
        portLabel.setAlignment(com.badlogic.gdx.utils.Align.right);
        portField = new TextField("8080", skin);
        portField.setMessageText("Port number (1024-65535)");
        topSection.add(portLabel).width(halfWidth).padRight(5).padBottom(10);
        topSection.add(portField).width(halfWidth).left().padBottom(10).row();

        Table bottomSection = new Table();
        bottomSection.top();

        connectButton = new ImageTextButton("Connect", skin);
        connectButton.getLabel().setFontScale(1.5f);
        connectButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClickSound();
                handleConnectClick();
            }
        });
        bottomSection.add(connectButton).height(connectButton.getHeight() * 0.8f).width(connectButton.getWidth() * 0.8f).padBottom(10).row();

        statusLabel = new Label("", skin);
        statusLabel.setColor(Color.YELLOW);
        statusLabel.setWrap(true);
        bottomSection.add(statusLabel).width(widgetWidth).height(60).center().row();

        if (networkController != null && networkController.isHost()) {
            connectButton.setDisabled(true);
            setStatus("This instance is currently hosting a game — you cannot join a game here.", true);
        }

        table.add(topSection).expandX().expandY().top().left().row();
        table.add(bottomSection).expandX().height(150).bottom().row();
    }

    private void handleConnectClick() {
        try {
            if (networkController != null && networkController.isHost()) {
                setStatus("Cannot join while hosting a game on this instance.", true);
                connectButton.setDisabled(true);
                if (callback != null) {
                    callback.onConnectError("Instance is hosting");
                }
                return;
            }

            String host = getHost();
            int port = getPort();

            if (host.isEmpty()) {
                setStatus("Please enter a host address", true);
                return;
            }

            setStatus("Connecting to " + host + ":" + port + "...", false);
            connectButton.setDisabled(true);

            try {
                networkController.joinGame(host, port);
                setStatus("Connected to " + host + ":" + port, false);

                if (callback != null) {
                    callback.onConnectSuccess(host, port);
                }
            } catch (Exception e) {
                setStatus("Failed to connect: " + e.getMessage(), true);
                connectButton.setDisabled(false);

                if (callback != null) {
                    callback.onConnectError(e.getMessage());
                }
            }

        } catch (NumberFormatException e) {
            setStatus("Invalid port number", true);
        }
    }

    private void setStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setColor(isError ? Color.RED : Color.YELLOW);
    }

    public void setCallback(ClientCallback callback) {
        this.callback = callback;
    }

    public String getHost() {
        return hostField.getText().trim();
    }

    public int getPort() {
        return Integer.parseInt(portField.getText().trim());
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

    public void setConnectEnabled(boolean enabled) {
        connectButton.setDisabled(!enabled);
    }

    public void reset() {
        hostField.setText("localhost");
        portField.setText("8080");
        statusLabel.setText("");
        connectButton.setDisabled(false);

        if (networkController != null && networkController.isHost()) {
            connectButton.setDisabled(true);
            setStatus("This instance is currently hosting a game — you cannot join a game here.", true);
        }
    }

    public void dispose() {
        //
    }
}
