package at.gl1tchxd.battleship.screens;

import at.gl1tchxd.battleship.BattleshipGame;
import at.gl1tchxd.battleship.logic.GameController;
import at.gl1tchxd.battleship.network.NetworkController;
import at.gl1tchxd.battleship.ui.ConnectHost;
import at.gl1tchxd.battleship.ui.ConnectClient;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class ConnectScreen implements Screen {
    private final BattleshipGame game;

    private Stage stage;

    private Texture background;
    private SpriteBatch batch;
    private FreeTypeFontGenerator fontGen;
    private Skin skin;

    private ConnectHost connectHost;
    private ConnectClient connectClient;
    private NetworkController networkController;

    private ConnectHost connectHost;
    private ConnectClient connectClient;

    public ConnectScreen(BattleshipGame game) {
        this.game = game;
        this.stage = null;
        this.batch = null;
        this.mainMenuTexture = null;
        this.fontGen = null;
        this.skin = null;
        this.connectHost = null;
        this.connectClient = null;
        this.networkController = null;
        this.background = null;
        this.connectHost = null;
        this.connectClient = null;
    }

    @Override
    public void show() {
        if (stage == null) stage = new Stage();
        if (batch == null) batch = new SpriteBatch();
        if (fontGen == null) fontGen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/BBHBartle-Regular.ttf"));
        if (skin == null) skin = new Skin(Gdx.files.internal("uiskin.json"));
        
        // Initialize network controller with a GameController
        if (networkController == null) {
            GameController gameController = new GameController();
            networkController = new NetworkController(gameController);
        }
        
        if (stage == null) {
            stage = new Stage(new ScreenViewport());
        }
        
        if (connectHost == null) {
            connectHost = new ConnectHost(networkController, skin, stage, 0, 0);
            
            // Set up callback for host events
            connectHost.setCallback(new ConnectHost.HostCallback() {
                @Override
                public void onHostStart() {
                    if (connectClient != null) {
                        connectClient.setConnectEnabled(false);
                    }
                }

                @Override
                public void onHostSuccess(int port, int boardSize, int[] shipConfig) {
                    if (connectClient != null) {
                        connectClient.setConnectEnabled(false);
                    }
                    // TODO: Transition to PlacementScreen when it's available
                    // game.setScreen(new PlacementScreen(game));
        if (background == null) {
            background = new Texture(Gdx.files.internal("sprites/connect_background.png"));
        }
        if (connectHost == null) {
            connectHost = new ConnectHost(game.getNetworkController(), game.getSkin(), stage);
            connectHost.setCallback(new ConnectHost.HostCallback() {
                @Override
                public void onHostSuccess(int port, int boardSize, int[] shipConfig) {
                }

                @Override
                public void onHostError(String errorMessage) {
                    if (connectClient != null) {
                        connectClient.setConnectEnabled(true);
                    }
                }

                @Override
                public void onClientConnected() {
                    // TODO: Transition to PlacementScreen when it's available
                    // game.setScreen(new PlacementScreen(game));
                }
            });
        }
        
        if (connectClient == null) {
            connectClient = new ConnectClient(networkController, skin, stage, 0, 0);
            
            // Set up callback for client events
            connectClient.setCallback(new ConnectClient.ClientCallback() {
                @Override
                public void onConnectStart() {
                    if (connectHost != null) {
                        connectHost.setHostEnabled(false);
                    }
                }

                @Override
                public void onConnectSuccess(String host, int port) {
                    if (connectHost != null) {
                        connectHost.setHostEnabled(false);
                    }
                    // TODO: Transition to PlacementScreen when it's available
                    // game.setScreen(new PlacementScreen(game));
                }

                @Override
                public void onConnectError(String errorMessage) {
                    if (connectHost != null) {
                        connectHost.setHostEnabled(true);
                    }
                }
            });
        }
        
        // Create a parent table to layout both widgets side by side
        Table parentTable = new Table();
        parentTable.setFillParent(true);
        parentTable.center();
        
        // Add host widget on the left
        parentTable.add(connectHost.getTable()).padRight(50);
        
        // Add client widget on the right
        parentTable.add(connectClient.getTable()).padLeft(50);
        
        stage.addActor(parentTable);
        
        // Set input processor to handle the stage
                    game.setScreen(new PlacementScreen(game));
                }
            });
        }
        if (connectClient == null) {
            connectClient = new ConnectClient(game.getNetworkController(), game.getSkin(), stage);
            connectClient.setCallback(new ConnectClient.ClientCallback() {
                @Override
                public void onConnectSuccess(String host, int port) {
                    game.setScreen(new PlacementScreen(game));
                }

                @Override
                public void onConnectError(String errorMessage) {

                }
            });
        }

        // Position both widgets side by side, with their midpoint at screen center
        float screenWidth = Gdx.graphics.getWidth();
        float spacing = 175;
        float widgetY = 100;
        float widgetHeight = 375; // Height from widgetY to near top of screen
        float widgetWidth = 250f; // Same width for both widgets

        Table hostTable = connectHost.getTable();
        Table clientTable = connectClient.getTable();

        float centerX = screenWidth / 2f;
        float hostX = centerX - widgetWidth - spacing / 2f;
        float clientX = centerX + spacing / 2f;

        // Remove tables from stage, set bounds, then re-add
        hostTable.remove();
        clientTable.remove();

        // Set bounds (position + size) before adding back to stage
        hostTable.setBounds(hostX, widgetY, widgetWidth, widgetHeight);
        clientTable.setBounds(clientX, widgetY, widgetWidth, widgetHeight);

        // Re-add to stage
        stage.addActor(hostTable);
        stage.addActor(clientTable);

        // Force layout
        hostTable.invalidate();
        clientTable.invalidate();
        hostTable.layout();
        clientTable.layout();

        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        input(delta);
        draw(delta);
    }

    public void draw(float delta) {
        if (batch == null || fontGen == null || mainMenuTexture == null) return;
        if (batch == null || background == null) return;

        batch.begin();
        batch.draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();
        

        if (stage != null) {
            stage.act(delta);
            stage.draw();
        }
    }

    public void input(float delta) {
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
             this.game.setScreen(new MainMenuScreen(this.game));
        }

    }

    @Override
    public void resize(int width, int height) {
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void pause() {
        //
    }

    @Override
    public void resume() {
        //
    }

    @Override
    public void hide() {
        //
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (mainMenuTexture != null) mainMenuTexture.dispose();
        if (fontGen != null) fontGen.dispose();
        if (skin != null) skin.dispose();
        if (stage != null) stage.dispose();
        if (connectHost != null) connectHost.dispose();
        if (connectClient != null) connectClient.dispose();
        if (background != null) background.dispose();
        if (connectHost != null) connectHost.dispose();
        if (connectClient != null) connectClient.dispose();
        if (stage != null) stage.dispose();
    }
}