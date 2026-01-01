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
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class ConnectScreen implements Screen {
    private final BattleshipGame game;

    private final Stage stage;

    private Texture mainMenuTexture;
    private SpriteBatch batch;
    private FreeTypeFontGenerator fontGen;

    private ConnectHost connectHost;

    public ConnectScreen(BattleshipGame game) {
        this.game = game;
        this.stage = null;
        this.batch = null;
        this.mainMenuTexture = null;
        this.fontGen = null;
        this.connectHost = null;
    }

    @Override
    public void show() {
        if (mainMenuTexture == null) {
            mainMenuTexture = new Texture(Gdx.files.internal("sprites/shared_background.png"));
        }
        if (batch == null) batch = new SpriteBatch();
        if (fontGen == null) fontGen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/BBHBartle-Regular.ttf"));
        if (connectHost == null) {

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