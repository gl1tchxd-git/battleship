package at.gl1tchxd.battleship;

import at.gl1tchxd.battleship.screens.MainMenuScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class BattleshipGame extends Game {
    private SpriteBatch batch;
    private FitViewport viewport;

    @Override
    public void create() {
        this.batch = new SpriteBatch(); // Add this line
        this.viewport = new FitViewport(8, 5);
        this.setScreen(new MainMenuScreen(this));
    }

    @Override
    public void render() {
        // Clear the screen with dark blue color
        Gdx.gl.glClearColor(0.1f, 0.2f, 0.4f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        super.render(); // Important: this calls the active screen's render method
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
    }
}