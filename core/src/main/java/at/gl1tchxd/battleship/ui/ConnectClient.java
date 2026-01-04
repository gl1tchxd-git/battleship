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
    private TextField hostField;
    private TextField portField;
    private ImageTextButton connectButton;
    private Label statusLabel;

    private final NetworkController networkController;

    private ClientCallback callback;

    /** Width of the entire widget. Labels and inputs each take half this width. */
    private float widgetWidth = 250f;

    public interface ClientCallback {
        void onConnectSuccess(String host, int port);
        void onConnectError(String errorMessage);
    }

    public ConnectClient(NetworkController networkController, Skin skin, Stage stage) {
        this.networkController = networkController;
        this.stage = stage;
        this.skin = skin;
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

        createUIContent();

        stage.addActor(table);
    }

    private void createUIContent() {
        float halfWidth = widgetWidth / 2f;

        // Top section for inputs
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

        // Bottom section for button and status
        Table bottomSection = new Table();
        bottomSection.top();

        connectButton = new ImageTextButton("Connect", skin);
        connectButton.getLabel().setFontScale(1.5f);
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
        bottomSection.add(connectButton).height(connectButton.getHeight() * 0.8f).width(connectButton.getWidth() * 0.8f).padBottom(10).row();

        statusLabel = new Label("", skin);
        statusLabel.setColor(Color.YELLOW);
        statusLabel.setWrap(true);
        bottomSection.add(statusLabel).width(widgetWidth).height(60).center().row();

        // Add sections to main table - topSection expands to fill available space, bottomSection at bottom
        table.add(topSection).expandX().expandY().top().left().row();
        table.add(bottomSection).expandX().height(150).bottom().row();
    }

    private void handleConnectClick() {
        try {
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
            setStatus("Invalid port: Please enter a valid number", true);
        }
    }

    /**
     * Set the status message.
     */
            setStatus("Invalid port number", true);
        }
    }

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
    public int getPort() {
        return Integer.parseInt(portField.getText().trim());
    }

    public Table getTable() {
        return table;
    }

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
    /** Set the widget width. Labels and inputs each take half this width. */
    public void setWidgetWidth(float width) {
        this.widgetWidth = width;
        rebuildUI();
    }

    /** Get the current widget width. */
    public float getWidgetWidth() {
        return widgetWidth;
    }

    /** Rebuild the UI with the current widgetWidth. */
    private void rebuildUI() {
        table.clear();
        createUIContent();
    }

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
    public void dispose() {
        // Stage and skin are owned by the game/screen, so don't dispose them here
    }
}
