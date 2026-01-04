package at.gl1tchxd.battleship.ui;

import at.gl1tchxd.battleship.network.NetworkController;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * UI component for connecting to a game as a client.
 * Provides input fields for host address and port.
 */
public class ConnectClient {
    private final Stage stage;
    private Table table;
    private Skin skin;

    private final int x;
    private final int y;

    private TextField hostField;
    private TextField portField;
    private TextButton connectButton;
    private Label statusLabel;

    private final NetworkController networkController;
    private ClientCallback callback;

    public ConnectClient(NetworkController networkController, Skin skin, Stage stage, int x, int y) {
        this.networkController = networkController;
        this.stage = stage;
        this.skin = skin;
        this.x = x;
        this.y = y;
        createUI();
    }

    private void createUI() {
        table = new Table();
        table.setFillParent(false);
        table.right();

        // Title
        Label titleLabel = new Label("Join Game", skin);
        table.add(titleLabel).colspan(2).padBottom(20).row();

        // Host input
        Label hostLabel = new Label("Host:", skin);
        hostField = new TextField("localhost", skin);
        hostField.setMessageText("Server address (e.g., localhost)");
        table.add(hostLabel).padRight(10);
        table.add(hostField).width(200).padBottom(10).row();

        // Port input
        Label portLabel = new Label("Port:", skin);
        portField = new TextField("8080", skin);
        portField.setMessageText("Port number (1024-65535)");
        table.add(portLabel).padRight(10);
        table.add(portField).width(200).padBottom(10).row();

        // Connect button
        connectButton = new TextButton("Connect", skin);
        connectButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleConnectClick();
            }
        });
        table.add(connectButton).colspan(2).width(200).height(40).padBottom(10).row();

        // Status label
        statusLabel = new Label("", skin);
        statusLabel.setColor(Color.YELLOW);
        table.add(statusLabel).colspan(2).row();

        // Note: Table will be added to stage by ConnectScreen
    }

    /**
     * Get the table for layout purposes.
     */
    public Table getTable() {
        return table;
    }

    /**
     * Handle the connect button click.
     */
    private void handleConnectClick() {
        try {
            // Parse port
            String host = hostField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());

            // Notify callback that connection is starting
            if (callback != null) {
                callback.onConnectStart();
            }

            // Start connecting
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
            setStatus("Invalid port: Please enter a valid number", true);
        }
    }

    /**
     * Set the status message.
     */
    private void setStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setColor(isError ? Color.RED : Color.YELLOW);
    }

    /**
     * Set the callback for client events.
     */
    public void setCallback(ClientCallback callback) {
        this.callback = callback;
    }

    /**
     * Get the stage for input processing.
     */
    public Stage getStage() {
        return stage;
    }

    /**
     * Render the UI.
     */
    public void render(float delta) {
        stage.act(delta);
        stage.draw();
    }

    /**
     * Resize the UI.
     */
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    /**
     * Get the host value.
     */
    public String getHost() {
        return hostField.getText().trim();
    }

    /**
     * Get the port value.
     */
    public int getPort() {
        try {
            return Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            return 8080;
        }
    }

    /**
     * Set the position of the UI panel.
     */
    public void setPosition(float x, float y) {
        table.setPosition(x, y);
    }

    /**
     * Set the size of the UI panel.
     */
    public void setSize(float width, float height) {
        table.setSize(width, height);
    }

    /**
     * Enable or disable the connect button.
     */
    public void setConnectEnabled(boolean enabled) {
        connectButton.setDisabled(!enabled);
    }

    /**
     * Reset the UI to default values.
     */
    public void reset() {
        hostField.setText("localhost");
        portField.setText("8080");
        statusLabel.setText("");
        connectButton.setDisabled(false);
    }

    /**
     * Dispose of resources.
     */
    public void dispose() {
        // Note: stage and skin are managed by the parent screen
    }

    /**
     * Callback interface for client events.
     */
    public interface ClientCallback {
        void onConnectStart();
        void onConnectSuccess(String host, int port);
        void onConnectError(String errorMessage);
    }
}
