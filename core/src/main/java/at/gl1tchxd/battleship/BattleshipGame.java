package at.gl1tchxd.battleship;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class BattleshipGame extends Game {
    public SpriteBatch batch;
    public BitmapFont font;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont(); // Uses libGDX's default Arial font

        // Set the initial screen (main menu, game screen, etc.)
        // this.setScreen(new MainMenuScreen(this));
    }

    @Override
    public void render() {
        // Clear the screen with dark blue color
        Gdx.gl.glClearColor(0.1f, 0.2f, 0.4f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        super.render(); // Important: this calls the active screen's render method
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}