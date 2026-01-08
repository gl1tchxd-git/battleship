
package at.gl1tchxd.battleship.screens;

import at.gl1tchxd.battleship.BattleshipGame;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class MainMenuScreen implements Screen {
    private final BattleshipGame game;

    private SpriteBatch batch;
    private Stage stage;
    private Texture background;

    public MainMenuScreen(BattleshipGame game) {
        this.game = game;
        this.batch = game.batch;
    }

    @Override
    public void show() {
        if (batch == null) batch = new SpriteBatch();
        if (stage == null) {
            stage = new Stage();
            Gdx.input.setInputProcessor(stage);
        }
        if (background == null) {
            background = new Texture("sprites/main-menu_background.png");
        }

        ImageTextButton startButton = new ImageTextButton("START", game.getSkin());
        startButton.getLabel().setFontScale(2f);
        startButton.setPosition(Gdx.graphics.getWidth() / 2f - startButton.getWidth() / 2f, Gdx.graphics.getHeight() / 2f - 200);
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClickSound();
                game.setScreen(new ConnectScreen(game));
            }
        });
        stage.addActor(startButton);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        input(delta);
        draw(delta);
    }

    public void draw(float delta) {
        batch.begin();
        batch.draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        stage.act(delta);
        stage.draw();
    }

    public void input(float delta) {
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE)) {
             this.game.setScreen(new ConnectScreen(this.game));
        }
    }

    @Override
    public void resize(int width, int height) {
        if (batch != null) {
            batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        }
    }

    @Override
    public void pause() {

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
    }
}