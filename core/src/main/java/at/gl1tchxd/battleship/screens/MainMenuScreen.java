// Java
package at.gl1tchxd.battleship.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MainMenuScreen implements Screen {
    private final Game game;
    private SpriteBatch batch;
    private BitmapFont font;

    public MainMenuScreen(Game game) {
        this.game = game;
        // Do NOT create GL resources here
        batch = null;
        font = null;
    }

    @Override
    public void show() {
        // Create GL resources after context is available
        if (batch == null) batch = new SpriteBatch();
        if (font == null) font = new BitmapFont();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        draw();
    }

    public void draw() {
        if (batch == null || font == null) return;
        batch.begin();
        font.draw(batch, "Battleship Game - Press Enter to Start", 100, 150);
        batch.end();
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
        if (font != null) font.dispose();
    }
}