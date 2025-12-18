package at.gl1tchxd.battleship.screens;

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
    private final Game game;
    private Texture mainMenuTexture;
    private SpriteBatch batch;
    private FreeTypeFontGenerator fontGen;

    public ConnectScreen(Game game) {
        this.game = game;
        batch = null;
        mainMenuTexture = null;
        fontGen = null;
    }

    @Override
    public void show() {
        if (mainMenuTexture == null) {
            mainMenuTexture = new Texture("sprites/main_menu_background.jpg");
        }
        if (batch == null) batch = new SpriteBatch();
        if (fontGen == null) fontGen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/BBHBartle-Regular.ttf"));
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        input(delta);
        draw();
    }

    public void draw() {
        if (batch == null || fontGen == null || mainMenuTexture == null) return;
        batch.begin();
        batch.draw(mainMenuTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.size = 32;
        param.color = com.badlogic.gdx.graphics.Color.BLACK;
        fontGen.generateFont(param).draw(batch, "Connecting...", (int) (Gdx.graphics.getWidth() * 0.05), (int) (Gdx.graphics.getHeight() * 0.9));
        param.size = 16;
        fontGen.generateFont(param).draw(batch, "Press space to start", (int) (Gdx.graphics.getWidth() * 0.05), (int) (Gdx.graphics.getHeight() * 0.9) - 30);
        batch.end();
    }

    public void input(float delta) {
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE)) {
             this.game.setScreen(new ConnectScreen(this.game));
        }

    }

    @Override
    public void resize(int width, int height) {
        //
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
    }
}