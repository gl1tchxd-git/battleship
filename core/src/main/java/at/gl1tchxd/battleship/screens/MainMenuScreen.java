// Java
package at.gl1tchxd.battleship.screens;

import at.gl1tchxd.battleship.BattleshipGame;
import at.gl1tchxd.battleship.logic.GameController;
import at.gl1tchxd.battleship.network.NetworkController;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class MainMenuScreen implements Screen {
    private final BattleshipGame game;

    private Texture mainMenuTexture;
    private SpriteBatch batch;
    private FreeTypeFontGenerator fontGen;

    public MainMenuScreen(BattleshipGame game) {
        this.game = game;
        batch = null;
        mainMenuTexture = null;
        fontGen = null;
    }

    @Override
    public void show() {
        if (mainMenuTexture == null) {
            mainMenuTexture = new Texture(Gdx.files.internal("sprites/main-menu_background.png"));
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
        param.color = com.badlogic.gdx.graphics.Color.BLACK;
        param.size = 28;
        BitmapFont font = fontGen.generateFont(param);
        com.badlogic.gdx.graphics.g2d.GlyphLayout glyphLayout = new GlyphLayout(font, "Press space to start");
        font.draw(batch, "Press space to start", (Gdx.graphics.getWidth() - glyphLayout.width) / 2f, Gdx.graphics.getHeight() * 0.65f);
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