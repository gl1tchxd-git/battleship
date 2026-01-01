package at.gl1tchxd.battleship.screens;

import at.gl1tchxd.battleship.BattleshipGame;
import at.gl1tchxd.battleship.network.NetworkController;
import at.gl1tchxd.battleship.ui.ConnectHost;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class ConnectScreen implements Screen {
    private final BattleshipGame game;
    private Texture mainMenuTexture;
    private SpriteBatch batch;
    private FreeTypeFontGenerator fontGen;

    private ConnectHost connectHost;

    public ConnectScreen(BattleshipGame game) {
        this.game = game;
        batch = null;
        mainMenuTexture = null;
        fontGen = null;
        connectHost = null;
    }

    @Override
    public void show() {
        if (mainMenuTexture == null) {
            mainMenuTexture = new Texture("sprites/main_menu_background.jpg");
        }
        if (batch == null) batch = new SpriteBatch();
        if (fontGen == null) fontGen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/BBHBartle-Regular.ttf"));
        if (connectHost == null) {
            connectHost = new ConnectHost(game.getNetworkController());
            connectHost.setCallback(new ConnectHost.HostCallback() {
                @Override
                public void onHostSuccess(int port, int boardSize, int[] shipConfig) {
                    Gdx.app.log("ConnectScreen", "Successfully connected to host");
                }

                @Override
                public void onHostError(String error) {
                    Gdx.app.error("ConnectScreen", "Failed to connect to host: " + error);
                }
            });
            Gdx.input.setInputProcessor(connectHost.getStage());
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        input(delta);
        draw(delta);
    }

    public void draw(float delta) {
        if (batch == null || fontGen == null || mainMenuTexture == null || connectHost == null) return;
        batch.begin();
        batch.draw(mainMenuTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();
        connectHost.render(delta);
    }

    public void input(float delta) {
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE)) {
             this.game.setScreen(new ConnectScreen(this.game));
        }

    }

    @Override
    public void resize(int width, int height) {
        if (connectHost != null) {
            connectHost.getStage().getViewport().update(width, height, true);
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
        if (connectHost != null) connectHost.dispose();
    }
}