package at.gl1tchxd.battleship.screens;

import at.gl1tchxd.battleship.BattleshipGame;
import at.gl1tchxd.battleship.ui.ConnectHost;
import at.gl1tchxd.battleship.ui.ConnectClient;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class ConnectScreen implements Screen {
    private final BattleshipGame game;

    private Stage stage;

    private Texture background;
    private SpriteBatch batch;

    private ConnectHost connectHost;
    private ConnectClient connectClient;

    public ConnectScreen(BattleshipGame game) {
        this.game = game;
        this.stage = null;
        this.batch = null;
        this.background = null;
        this.connectHost = null;
        this.connectClient = null;
    }

    @Override
    public void show() {
        if (stage == null) stage = new Stage();
        if (batch == null) batch = new SpriteBatch();
        if (background == null) {
            background = new Texture(Gdx.files.internal("sprites/connect_background.png"));
        }
        if (connectHost == null) {
            connectHost = new ConnectHost(game, game.getNetworkController(), game.getSkin(), stage);
            connectHost.setCallback(new ConnectHost.HostCallback() {
                @Override
                public void onHostSuccess(int port, int boardSize, int[] shipConfig) {
                }

                @Override
                public void onHostError(String errorMessage) {
                }

                @Override
                public void onClientConnected() {
                    game.setScreen(new PlacementScreen(game));
                }
            });
        }
        if (connectClient == null) {
            connectClient = new ConnectClient(game, game.getNetworkController(), game.getSkin(), stage);
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

        float screenWidth = Gdx.graphics.getWidth();
        float spacing = 175;
        float widgetY = 100;
        float widgetHeight = 375;
        float widgetWidth = 250f;

        Table hostTable = connectHost.getTable();
        Table clientTable = connectClient.getTable();

        float centerX = screenWidth / 2f;
        float hostX = centerX - widgetWidth - spacing / 2f;
        float clientX = centerX + spacing / 2f;

        hostTable.remove();
        clientTable.remove();

        hostTable.setBounds(hostX, widgetY, widgetWidth, widgetHeight);
        clientTable.setBounds(clientX, widgetY, widgetWidth, widgetHeight);

        stage.addActor(hostTable);
        stage.addActor(clientTable);

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
        if (background != null) background.dispose();
        if (connectHost != null) connectHost.dispose();
        if (connectClient != null) connectClient.dispose();
        if (stage != null) stage.dispose();
    }
}