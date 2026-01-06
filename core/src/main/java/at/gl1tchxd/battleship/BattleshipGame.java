package at.gl1tchxd.battleship;

import at.gl1tchxd.battleship.logic.GameController;
import at.gl1tchxd.battleship.network.NetworkController;
import at.gl1tchxd.battleship.screens.MainMenuScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class BattleshipGame extends Game {
    public SpriteBatch batch;
    private ScreenViewport viewport;
    private Skin skin;
    private GameController gameController;
    private NetworkController networkController;

    // Sound effects
    private Sound clickSound;
    private Sound hitSound;
    private Sound missSound;
    private Sound gameWonSound;
    private Sound gameLostSound;

    @Override
    public void create() {
        this.batch = new SpriteBatch();
        this.viewport = new ScreenViewport();
        this.skin = new Skin();
        createDefaultSkin();

        // Load sound effects
        this.clickSound = Gdx.audio.newSound(Gdx.files.internal("sounds/click.mp3"));
        this.hitSound = Gdx.audio.newSound(Gdx.files.internal("sounds/hit.mp3"));
        this.missSound = Gdx.audio.newSound(Gdx.files.internal("sounds/miss.mp3"));
        this.gameWonSound = Gdx.audio.newSound(Gdx.files.internal("sounds/game-won.mp3"));
        this.gameLostSound = Gdx.audio.newSound(Gdx.files.internal("sounds/game-lost.mp3"));

        this.gameController = new GameController();
        this.networkController = new NetworkController(gameController);
        this.setScreen(new MainMenuScreen(this));
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.1f, 0.2f, 0.4f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        super.render();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        super.resize(width, height);
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
        if (skin != null) {
            skin.dispose();
        }
        if (clickSound != null) {
            clickSound.dispose();
        }
        if (hitSound != null) {
            hitSound.dispose();
        }
        if (missSound != null) {
            missSound.dispose();
        }
        if (gameWonSound != null) {
            gameWonSound.dispose();
        }
        if (gameLostSound != null) {
            gameLostSound.dispose();
        }
    }

    public GameController getGameController() {
        return gameController;
    }

    public NetworkController getNetworkController() {
        return networkController;
    }

    public Skin getSkin() {
        return skin;
    }

    // Sound effect methods
    public void playClickSound() {
        if (clickSound != null) {
            clickSound.play();
        }
    }

    public void playHitSound() {
        if (hitSound != null) {
            hitSound.play();
        }
    }

    public void playMissSound() {
        if (missSound != null) {
            missSound.play();
        }
    }

    public void playGameWonSound() {
        if (gameWonSound != null) {
            gameWonSound.play();
        }
    }

    public void playGameLostSound() {
        if (gameLostSound != null) {
            gameLostSound.play();
        }
    }

    private void createDefaultSkin() {
        FreeTypeFontGenerator fontGen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/minecraft.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter params = new FreeTypeFontGenerator.FreeTypeFontParameter();
        params.padLeft = 2;
        params.shadowColor = Color.CLEAR;
        params.borderColor = Color.CLEAR;
        params.color = new Color(85 / 255f, 87 / 255f, 87 / 255f, 1f);
        BitmapFont font = fontGen.generateFont(params);
        skin.add("default", font);

        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new com.badlogic.gdx.graphics.Texture(pixmap));
        pixmap.dispose();

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        skin.add("default", labelStyle);

        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = Color.WHITE;

        int underlineThickness = 2;
        int uw = 9, uh = 9;

        com.badlogic.gdx.graphics.Pixmap underlinePixmap = new com.badlogic.gdx.graphics.Pixmap(uw, uh, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        underlinePixmap.setColor(0, 0, 0, 0);
        underlinePixmap.fill();
        underlinePixmap.setColor(125 / 255f, 125 / 255f, 125 / 255f, 1f);
        underlinePixmap.fillRectangle(0, uh - underlineThickness, uw, underlineThickness);
        com.badlogic.gdx.graphics.Texture underlineTexture = new com.badlogic.gdx.graphics.Texture(underlinePixmap);
        underlinePixmap.dispose();
        com.badlogic.gdx.graphics.g2d.NinePatch underlinePatch = new com.badlogic.gdx.graphics.g2d.NinePatch(new TextureRegion(underlineTexture), 0, 0, 0, underlineThickness);
        com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable underlineDrawable = new com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable(underlinePatch);

        com.badlogic.gdx.graphics.Pixmap focusedPixmap = new com.badlogic.gdx.graphics.Pixmap(uw, uh, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        focusedPixmap.setColor(0, 0, 0, 0);
        focusedPixmap.fill();
        focusedPixmap.setColor(85 / 255f, 87 / 255f, 87 / 255f, 1f);
        focusedPixmap.fillRectangle(0, uh - underlineThickness, uw, underlineThickness);
        com.badlogic.gdx.graphics.Texture focusedTexture = new com.badlogic.gdx.graphics.Texture(focusedPixmap);
        focusedPixmap.dispose();
        com.badlogic.gdx.graphics.g2d.NinePatch focusedPatch = new com.badlogic.gdx.graphics.g2d.NinePatch(new TextureRegion(focusedTexture), 0, 0, 0, underlineThickness);
        com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable focusedDrawable = new com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable(focusedPatch);

        textFieldStyle.background = underlineDrawable;
        textFieldStyle.focusedBackground = focusedDrawable;

        // Create a simple cursor (1px wide vertical line)
        com.badlogic.gdx.graphics.Pixmap cursorPixmap = new com.badlogic.gdx.graphics.Pixmap(2, (int)font.getLineHeight(), com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        cursorPixmap.setColor(85 / 255f, 87 / 255f, 87 / 255f, 1f);
        cursorPixmap.fill();
        com.badlogic.gdx.graphics.Texture cursorTexture = new com.badlogic.gdx.graphics.Texture(cursorPixmap);
        cursorPixmap.dispose();
        textFieldStyle.cursor = new TextureRegionDrawable(new TextureRegion(cursorTexture));

        skin.add("default", textFieldStyle);

        ImageTextButton.ImageTextButtonStyle imageButtonStyle = new ImageTextButton.ImageTextButtonStyle();
        imageButtonStyle.font = font;
        imageButtonStyle.downFontColor = new Color(209 / 255f, 214 / 255f, 214 / 255f,1f);
        imageButtonStyle.up = new TextureRegionDrawable(new TextureRegion(new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("sprites/shared_button_up.png"))));
        imageButtonStyle.down = new TextureRegionDrawable(new TextureRegion(new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("sprites/shared_button_down.png"))));
        skin.add("default", imageButtonStyle);

        ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle();
        scrollPaneStyle.background = skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 0.5f));
        scrollPaneStyle.vScroll = skin.newDrawable("white", new Color(0.3f, 0.3f, 0.3f, 0.9f));
        scrollPaneStyle.vScrollKnob = skin.newDrawable("white", new Color(0.5f, 0.5f, 0.5f, 0.9f));
        skin.add("default", scrollPaneStyle);

        // dispose font generator (fonts/textures are still in use)
        fontGen.dispose();
    }
}
