package at.gl1tchxd.battleship.ui;

import at.gl1tchxd.battleship.network.NetworkController;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * UI component for hosting a game.
 * Provides input fields for port, board size, and ship configuration.
 */
public class ConnectHost {
    private final Stage stage;
    private Table table;
    private Skin skin;

    private TextField portField;
    private TextField boardSizeField;
    private TextField shipConfigField;
    private TextButton hostButton;
    private Label statusLabel;

    private final NetworkController networkController;
    private HostCallback callback;

    /**
     * Callback interface for host events.
     */
    public interface HostCallback {
        void onHostSuccess(int port, int boardSize, int[] shipConfig);
        void onHostError(String error);
    }

    public ConnectHost(NetworkController networkController) {
        this.networkController = networkController;
        this.stage = new Stage(new ScreenViewport());
        createDefaultSkin();
        createUI();
    }

    /**
     * Create a simple default skin for the UI.
     */
    private void createDefaultSkin() {
        skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default", font);

        // Create a white 1x1 pixel texture for drawables
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new com.badlogic.gdx.graphics.Texture(pixmap));
        pixmap.dispose();

        // Label style
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        // TextField style
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.cursor = skin.newDrawable("white", Color.WHITE);
        textFieldStyle.selection = skin.newDrawable("white", new Color(0.3f, 0.3f, 0.8f, 1f));
        textFieldStyle.background = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.8f));
        skin.add("default", textFieldStyle);

        // TextButton style
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.up = skin.newDrawable("white", new Color(0.3f, 0.3f, 0.3f, 0.9f));
        buttonStyle.down = skin.newDrawable("white", new Color(0.5f, 0.5f, 0.5f, 0.9f));
        buttonStyle.over = skin.newDrawable("white", new Color(0.4f, 0.4f, 0.4f, 0.9f));
        skin.add("default", buttonStyle);
    }

    /**
     * Create the UI components.
     */
    private void createUI() {
        table = new Table();
        table.setFillParent(true);
        table.center();

        // Title
        Label titleLabel = new Label("Host Game", skin);
        table.add(titleLabel).colspan(2).padBottom(20).row();

        // Port input
        Label portLabel = new Label("Port:", skin);
        portField = new TextField("8080", skin);
        portField.setMessageText("Port number (1024-65535)");
        table.add(portLabel).padRight(10);
        table.add(portField).width(200).padBottom(10).row();

        // Board size input
        Label boardSizeLabel = new Label("Board Size:", skin);
        boardSizeField = new TextField("10", skin);
        boardSizeField.setMessageText("Board size (e.g., 10)");
        table.add(boardSizeLabel).padRight(10);
        table.add(boardSizeField).width(200).padBottom(10).row();

        // Ship configuration input
        Label shipConfigLabel = new Label("Ship Config:", skin);
        shipConfigField = new TextField("5,4,3,3,2", skin);
        shipConfigField.setMessageText("Comma-separated ship lengths");
        table.add(shipConfigLabel).padRight(10);
        table.add(shipConfigField).width(200).padBottom(10).row();

        // Help text
        Label helpLabel = new Label("Example: 5,4,3,3,2 = 1 carrier, 1 battleship, 2 cruisers, 1 destroyer", skin);
        helpLabel.setFontScale(0.7f);
        table.add(helpLabel).colspan(2).padBottom(20).row();

        // Host button
        hostButton = new TextButton("Start Hosting", skin);
        hostButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleHostClick();
            }
        });
        table.add(hostButton).colspan(2).width(200).height(40).padBottom(10).row();

        // Status label
        statusLabel = new Label("", skin);
        statusLabel.setColor(Color.YELLOW);
        table.add(statusLabel).colspan(2).row();

        stage.addActor(table);
    }

    /**
     * Handle the host button click.
     */
    private void handleHostClick() {
        try {
            // Parse port
            int port = Integer.parseInt(portField.getText().trim());
            if (port < 1024 || port > 65535) {
                setStatus("Port must be between 1024 and 65535", true);
                return;
            }

            // Parse board size
            int boardSize = Integer.parseInt(boardSizeField.getText().trim());
            if (boardSize < 5 || boardSize > 20) {
                setStatus("Board size must be between 5 and 20", true);
                return;
            }

            // Parse ship configuration
            String[] shipConfigStr = shipConfigField.getText().trim().split(",");
            int[] shipConfig = new int[shipConfigStr.length];
            for (int i = 0; i < shipConfigStr.length; i++) {
                shipConfig[i] = Integer.parseInt(shipConfigStr[i].trim());
                if (shipConfig[i] < 1 || shipConfig[i] > boardSize) {
                    setStatus("Ship length must be between 1 and " + boardSize, true);
                    return;
                }
            }

            // Start hosting
            setStatus("Starting server on port " + port + "...", false);
            hostButton.setDisabled(true);

            try {
                networkController.hostGame(port);
                networkController.sendGameInit(boardSize, shipConfig);
                setStatus("Hosting on port " + port + ". Waiting for player...", false);

                if (callback != null) {
                    callback.onHostSuccess(port, boardSize, shipConfig);
                }
            } catch (Exception e) {
                setStatus("Failed to host: " + e.getMessage(), true);
                hostButton.setDisabled(false);

                if (callback != null) {
                    callback.onHostError(e.getMessage());
                }
            }

        } catch (NumberFormatException e) {
            setStatus("Invalid input: Please enter valid numbers", true);
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
     * Set the callback for host events.
     */
    public void setCallback(HostCallback callback) {
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
     * Get the board size value.
     */
    public int getBoardSize() {
        try {
            return Integer.parseInt(boardSizeField.getText().trim());
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    /**
     * Get the ship configuration.
     */
    public int[] getShipConfig() {
        try {
            String[] shipConfigStr = shipConfigField.getText().trim().split(",");
            int[] shipConfig = new int[shipConfigStr.length];
            for (int i = 0; i < shipConfigStr.length; i++) {
                shipConfig[i] = Integer.parseInt(shipConfigStr[i].trim());
            }
            return shipConfig;
        } catch (Exception e) {
            return new int[]{5, 4, 3, 3, 2}; // Default configuration
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
     * Enable or disable the host button.
     */
    public void setHostEnabled(boolean enabled) {
        hostButton.setDisabled(!enabled);
    }

    /**
     * Reset the UI to default values.
     */
    public void reset() {
        portField.setText("8080");
        boardSizeField.setText("10");
        shipConfigField.setText("5,4,3,3,2");
        statusLabel.setText("");
        hostButton.setDisabled(false);
    }

    /**
     * Dispose of resources.
     */
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}

